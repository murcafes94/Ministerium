#!/usr/bin/env python3
"""Build a Ministerium semantic Bible SQLite package.

This tool intentionally contains no Bible text. It converts user-supplied/licensed
sources into the storage contract consumed by SqliteBibleRepository.
"""

from __future__ import annotations

import argparse
import html
import re
import sqlite3
import sys
import zipfile
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple
import xml.etree.ElementTree as ET


CATHOLIC_ORDER = [
    "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
    "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "TOB", "JDT", "EST", "1MA",
    "2MA", "JOB", "PSA", "PRO", "ECC", "SNG", "WIS", "SIR", "ISA", "JER",
    "LAM", "BAR", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC",
    "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL", "MAT", "MRK", "LUK", "JHN",
    "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH",
    "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN",
    "3JN", "JUD", "REV",
]
ORDER = {book: i + 1 for i, book in enumerate(CATHOLIC_ORDER)}

OSIS_TO_USFM = {
    "Gen": "GEN", "Exod": "EXO", "Lev": "LEV", "Num": "NUM", "Deut": "DEU",
    "Josh": "JOS", "Judg": "JDG", "Ruth": "RUT", "1Sam": "1SA", "2Sam": "2SA",
    "1Kgs": "1KI", "2Kgs": "2KI", "1Chr": "1CH", "2Chr": "2CH", "Ezra": "EZR",
    "Neh": "NEH", "Tob": "TOB", "Jdt": "JDT", "Esth": "EST", "1Macc": "1MA",
    "2Macc": "2MA", "Job": "JOB", "Ps": "PSA", "Prov": "PRO", "Eccl": "ECC",
    "Song": "SNG", "Wis": "WIS", "Sir": "SIR", "Isa": "ISA", "Jer": "JER",
    "Lam": "LAM", "Bar": "BAR", "Ezek": "EZK", "Dan": "DAN", "Hos": "HOS",
    "Joel": "JOL", "Amos": "AMO", "Obad": "OBA", "Jonah": "JON", "Mic": "MIC",
    "Nah": "NAM", "Hab": "HAB", "Zeph": "ZEP", "Hag": "HAG", "Zech": "ZEC",
    "Mal": "MAL", "Matt": "MAT", "Mark": "MRK", "Luke": "LUK", "John": "JHN",
    "Acts": "ACT", "Rom": "ROM", "1Cor": "1CO", "2Cor": "2CO", "Gal": "GAL",
    "Eph": "EPH", "Phil": "PHP", "Col": "COL", "1Thess": "1TH", "2Thess": "2TH",
    "1Tim": "1TI", "2Tim": "2TI", "Titus": "TIT", "Phlm": "PHM", "Heb": "HEB",
    "Jas": "JAS", "1Pet": "1PE", "2Pet": "2PE", "1John": "1JN", "2John": "2JN",
    "3John": "3JN", "Jude": "JUD", "Rev": "REV",
}


def local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def clean_text(value: str) -> str:
    return re.sub(r"\s+", " ", html.unescape(value or "")).strip()


def normalize_book(value: str) -> str:
    value = clean_text(value)
    if value in OSIS_TO_USFM:
        return OSIS_TO_USFM[value]
    upper = value.upper().replace(" ", "")
    return upper[:3] if len(upper) >= 3 else upper


class Builder:
    def __init__(self, output: Path, schema: Path, metadata: Dict[str, str]):
        if output.exists():
            output.unlink()
        self.db = sqlite3.connect(str(output))
        self.db.executescript(schema.read_text(encoding="utf-8"))
        self.edition_id = metadata["edition_id"]
        for key, value in metadata.items():
            self.db.execute("INSERT INTO metadata(key,value) VALUES (?,?)", (key, value))
        self.book_names: Dict[str, str] = {}
        self.chapter_ids: Dict[Tuple[str, int], int] = {}
        self.verse_order: Dict[Tuple[str, int], int] = {}
        self.paragraph_next = False

    def set_book_name(self, book: str, name: Optional[str]) -> None:
        if name:
            self.book_names[book] = clean_text(name)

    def ensure_book(self, book: str) -> None:
        name = self.book_names.get(book, book)
        testament = "NT" if ORDER.get(book, 999) >= ORDER["MAT"] else "OT"
        self.db.execute(
            "INSERT OR IGNORE INTO books(book_key,name,short_name,testament,canonical_order,chapter_count) "
            "VALUES (?,?,?,?,?,0)",
            (book, name, name, testament, ORDER.get(book, 999)),
        )

    def ensure_chapter(self, book: str, chapter: int) -> int:
        key = (book, chapter)
        if key in self.chapter_ids:
            return self.chapter_ids[key]
        self.ensure_book(book)
        self.db.execute(
            "INSERT OR IGNORE INTO chapters(book_key,chapter_number) VALUES (?,?)", (book, chapter)
        )
        row = self.db.execute(
            "SELECT id FROM chapters WHERE book_key=? AND chapter_number=?", (book, chapter)
        ).fetchone()
        assert row
        self.chapter_ids[key] = int(row[0])
        self.db.execute(
            "UPDATE books SET chapter_count = MAX(chapter_count, ?) WHERE book_key=?",
            (chapter, book),
        )
        return int(row[0])

    def add_verse(self, book: str, chapter: int, label: str, text: str,
                  is_heading: bool = False, paragraph_start: Optional[bool] = None) -> None:
        text = clean_text(text)
        label = clean_text(label)
        if not text or not label:
            return
        chapter_id = self.ensure_chapter(book, chapter)
        key = (book, chapter)
        order = self.verse_order.get(key, 0) + 1
        self.verse_order[key] = order
        stable = f"{self.edition_id}:{book}:{chapter}:{label}"
        paragraph = self.paragraph_next if paragraph_start is None else paragraph_start
        self.paragraph_next = False
        self.db.execute(
            "INSERT OR REPLACE INTO verses(stable_id,chapter_id,verse_label,verse_order,text,is_heading,paragraph_start) "
            "VALUES (?,?,?,?,?,?,?)",
            (stable, chapter_id, label, order, text, int(is_heading), int(paragraph)),
        )

    def finish(self) -> None:
        self.db.commit()
        self.db.execute("PRAGMA optimize")
        self.db.close()


def import_usfm(builder: Builder, source: Path) -> None:
    files = sorted(source.glob("*.usfm")) if source.is_dir() else [source]
    if source.is_dir() and not files:
        files = sorted(source.glob("*.sfm"))
    for path in files:
        book: Optional[str] = None
        chapter = 0
        current_label: Optional[str] = None
        current_text: List[str] = []

        def flush() -> None:
            nonlocal current_label, current_text
            if book and chapter and current_label:
                builder.add_verse(book, chapter, current_label, " ".join(current_text))
            current_label = None
            current_text = []

        for raw in path.read_text(encoding="utf-8-sig", errors="replace").splitlines():
            line = raw.strip()
            if not line:
                continue
            m = re.match(r"\\id\s+([^\s]+)", line)
            if m:
                flush(); book = normalize_book(m.group(1)); continue
            m = re.match(r"\\(?:h|toc1)\s+(.+)", line)
            if m and book:
                builder.set_book_name(book, m.group(1)); continue
            m = re.match(r"\\c\s+(\d+)", line)
            if m:
                flush(); chapter = int(m.group(1)); continue
            if re.match(r"\\(?:p|m|q\d?|pi\d?|li\d?)\b", line):
                builder.paragraph_next = True
                line = re.sub(r"^\\[^\s]+\s*", "", line)
                if not line:
                    continue
            m = re.match(r"\\v\s+([^\s]+)\s*(.*)", line)
            if m:
                flush(); current_label = m.group(1); current_text = [m.group(2)]; continue
            if current_label:
                # Remove common inline USFM character markers but preserve their text.
                line = re.sub(r"\\[a-zA-Z0-9+*-]+\s*", "", line)
                current_text.append(line)
        flush()


def _xml_text_without_notes(element: ET.Element) -> str:
    parts: List[str] = []
    if element.text:
        parts.append(element.text)
    for child in element:
        if local(child.tag) not in {"note", "reference"}:
            parts.append(_xml_text_without_notes(child))
        if child.tail:
            parts.append(child.tail)
    return clean_text(" ".join(parts))


def import_osis(builder: Builder, source: Path) -> None:
    root = ET.parse(source).getroot()
    for div in root.iter():
        if local(div.tag) != "div" or div.attrib.get("type") != "book":
            continue
        osis_book = div.attrib.get("osisID") or div.attrib.get("n") or ""
        book = normalize_book(osis_book)
        builder.set_book_name(book, osis_book)
        for verse in div.iter():
            if local(verse.tag) != "verse":
                continue
            osis_id = verse.attrib.get("osisID", "")
            match = re.search(r"\.([0-9]+)\.([^\.\s]+)$", osis_id)
            if not match:
                continue
            builder.add_verse(book, int(match.group(1)), match.group(2), _xml_text_without_notes(verse))


def import_usx(builder: Builder, source: Path) -> None:
    files = sorted(source.glob("*.usx")) if source.is_dir() else [source]
    for path in files:
        root = ET.parse(path).getroot()
        book_el = next((e for e in root.iter() if local(e.tag) == "book"), None)
        if book_el is None:
            continue
        book = normalize_book(book_el.attrib.get("code", ""))
        builder.set_book_name(book, clean_text(book_el.text or "") or book)
        current_chapter = 0
        current_label: Optional[str] = None
        buffer: List[str] = []

        def flush() -> None:
            nonlocal current_label, buffer
            if current_chapter and current_label:
                builder.add_verse(book, current_chapter, current_label, " ".join(buffer))
            current_label = None
            buffer = []

        for el in root.iter():
            tag = local(el.tag)
            if tag == "chapter" and "number" in el.attrib:
                flush(); current_chapter = int(re.sub(r"\D.*$", "", el.attrib["number"]) or 0)
            elif tag == "verse":
                if "number" in el.attrib:
                    flush(); current_label = el.attrib["number"]
                    if el.tail: buffer.append(el.tail)
                elif "eid" in el.attrib:
                    flush()
            elif current_label and tag not in {"note", "ref"}:
                if el.text: buffer.append(el.text)
                if el.tail: buffer.append(el.tail)
        flush()


def import_epub(builder: Builder, source: Path) -> None:
    """Best-effort structural EPUB importer.

    It only accepts EPUBs whose XHTML exposes semantic verse IDs such as
    JHN.1.1, JHN_1_1 or JHN-1-1. If no such IDs are found we fail explicitly
    instead of guessing from page layout. Edition-specific profiles can be
    added later for protected/user-provided EPUBs without embedding their text.
    """
    verse_re = re.compile(r"\b([1-3]?[A-Za-z]{2,8})[._-](\d+)[._-](\d+[A-Za-z]?)\b")
    tag_re = re.compile(r"<(?P<tag>[A-Za-z0-9]+)[^>]*\bid=[\"'](?P<id>[^\"']+)[\"'][^>]*>(?P<body>.*?)</(?P=tag)>", re.S | re.I)
    strip_tags = re.compile(r"<[^>]+>")
    found = 0
    with zipfile.ZipFile(source) as zf:
        names = [n for n in zf.namelist() if n.lower().endswith((".xhtml", ".html", ".htm"))]
        for name in names:
            data = zf.read(name).decode("utf-8", errors="replace")
            for match in tag_re.finditer(data):
                id_match = verse_re.search(match.group("id"))
                if not id_match:
                    continue
                book = normalize_book(id_match.group(1))
                chapter = int(id_match.group(2))
                label = id_match.group(3)
                text = clean_text(strip_tags.sub(" ", match.group("body")))
                if text:
                    builder.add_verse(book, chapter, label, text)
                    found += 1
    if not found:
        raise RuntimeError(
            "EPUB has no recognized semantic verse IDs; add an edition-specific import profile rather than guessing from pagination"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--format", choices=["usfm", "usx", "osis", "epub"], required=True)
    parser.add_argument("--edition-id", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--abbreviation", default="")
    parser.add_argument("--language", required=True)
    parser.add_argument("--canon", default="catholic")
    parser.add_argument("--version", default="1")
    parser.add_argument("--copyright-notice", default="")
    parser.add_argument("--license-id", default="")
    parser.add_argument("--min-app-version", default="3.1.0")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    schema = Path(__file__).with_name("schema.sql")
    metadata = {
        "edition_id": args.edition_id,
        "name": args.name,
        "abbreviation": args.abbreviation,
        "language": args.language,
        "canon": args.canon,
        "source_format": args.format,
        "version": args.version,
        "copyright_notice": args.copyright_notice,
        "license_id": args.license_id,
        "content_hash": "",
        "min_app_version": args.min_app_version,
    }
    builder = Builder(args.output, schema, metadata)
    try:
        if args.format == "usfm": import_usfm(builder, args.source)
        elif args.format == "usx": import_usx(builder, args.source)
        elif args.format == "osis": import_osis(builder, args.source)
        elif args.format == "epub": import_epub(builder, args.source)
        builder.finish()
    except Exception:
        builder.db.close()
        if args.output.exists():
            args.output.unlink()
        raise
    print(args.output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
