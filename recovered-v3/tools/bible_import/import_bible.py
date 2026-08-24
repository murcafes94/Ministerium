#!/usr/bin/env python3
"""Build a Ministerium semantic Bible SQLite package from USFM, USX or OSIS.

This tool contains no Bible text. Input licensing remains the responsibility of the
package author. It uses only the Python standard library so it can run in CI.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sqlite3
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Iterator, Optional


@dataclass
class Verse:
    book_id: str
    chapter: int
    label: str
    order: int
    text: str


USFM_ID = re.compile(r"^\\id\s+([A-Za-z0-9]{3})")
USFM_CHAPTER = re.compile(r"^\\c\s+(\d+)")
USFM_VERSE = re.compile(r"^\\v\s+([^\s]+)\s*(.*)$")
USFM_REMOVE_MARKERS = re.compile(r"\\(?:[A-Za-z0-9]+\*?)(?:\s+)?")


def clean_text(value: str) -> str:
    value = re.sub(r"\s+", " ", value or " ").strip()
    return value


def usfm_text(value: str) -> str:
    value = re.sub(r"\\f\s+.*?\\f\*", " ", value, flags=re.DOTALL)
    value = re.sub(r"\\x\s+.*?\\x\*", " ", value, flags=re.DOTALL)
    value = USFM_REMOVE_MARKERS.sub("", value)
    return clean_text(value)


def parse_usfm(paths: Iterable[Path]) -> Iterator[Verse]:
    for path in paths:
        book_id: Optional[str] = None
        chapter: Optional[int] = None
        current: Optional[Verse] = None
        order = 0
        with path.open("r", encoding="utf-8-sig") as handle:
            for raw in handle:
                line = raw.rstrip("\r\n")
                match = USFM_ID.match(line)
                if match:
                    if current is not None:
                        yield current
                        current = None
                    book_id = match.group(1).upper()
                    continue
                match = USFM_CHAPTER.match(line)
                if match:
                    if current is not None:
                        yield current
                        current = None
                    chapter = int(match.group(1))
                    order = 0
                    continue
                match = USFM_VERSE.match(line)
                if match and book_id and chapter:
                    if current is not None:
                        yield current
                    order += 1
                    current = Verse(book_id, chapter, match.group(1), order, usfm_text(match.group(2)))
                    continue
                if current is not None and line and not line.startswith("\\c"):
                    extra = usfm_text(line)
                    if extra:
                        current.text = clean_text(current.text + " " + extra)
        if current is not None:
            yield current


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def parse_usx(paths: Iterable[Path]) -> Iterator[Verse]:
    """Parse USX milestone verses while preserving text between verse markers."""
    for path in paths:
        root = ET.parse(path).getroot()
        book_id = None
        chapter = None
        current_label = None
        pieces: list[str] = []
        order = 0

        def flush() -> Optional[Verse]:
            nonlocal pieces
            if book_id and chapter and current_label is not None:
                verse = Verse(book_id, chapter, current_label, order, clean_text(" ".join(pieces)))
                pieces = []
                return verse
            pieces = []
            return None

        for element in root.iter():
            name = local_name(element.tag)
            if name == "book":
                book_id = (element.attrib.get("code") or "").upper() or book_id
            elif name == "chapter" and element.attrib.get("number"):
                pending = flush()
                if pending:
                    yield pending
                current_label = None
                chapter = int(re.sub(r"\D.*$", "", element.attrib["number"]))
                order = 0
            elif name == "verse" and element.attrib.get("number"):
                pending = flush()
                if pending:
                    yield pending
                current_label = element.attrib["number"]
                order += 1
            if current_label is not None and element.text and name != "verse":
                pieces.append(element.text)
            if current_label is not None and element.tail:
                pieces.append(element.tail)
        pending = flush()
        if pending:
            yield pending


def osis_book_id(value: str) -> str:
    value = (value or "").split(".", 1)[0].upper()
    aliases = {
        "GEN": "GEN", "EXOD": "EXO", "LEV": "LEV", "NUM": "NUM", "DEUT": "DEU",
        "JOSH": "JOS", "JUDG": "JDG", "RUTH": "RUT", "1SAM": "1SA", "2SAM": "2SA",
        "1KGS": "1KI", "2KGS": "2KI", "1CHR": "1CH", "2CHR": "2CH", "EZRA": "EZR",
        "NEH": "NEH", "TOB": "TOB", "JDT": "JDT", "ESTH": "EST", "1MACC": "1MA",
        "2MACC": "2MA", "JOB": "JOB", "PS": "PSA", "PSA": "PSA", "PROV": "PRO",
        "ECCL": "ECC", "QOH": "ECC", "SONG": "SNG", "WIS": "WIS", "SIR": "SIR",
        "ISA": "ISA", "JER": "JER", "LAM": "LAM", "BAR": "BAR", "EZEK": "EZK",
        "DAN": "DAN", "HOS": "HOS", "JOEL": "JOL", "AMOS": "AMO", "OBAD": "OBA",
        "JONAH": "JON", "MIC": "MIC", "NAH": "NAM", "HAB": "HAB", "ZEPH": "ZEP",
        "HAG": "HAG", "ZECH": "ZEC", "MAL": "MAL", "MATT": "MAT", "MARK": "MRK",
        "LUKE": "LUK", "JOHN": "JHN", "ACTS": "ACT", "ROM": "ROM", "1COR": "1CO",
        "2COR": "2CO", "GAL": "GAL", "EPH": "EPH", "PHIL": "PHP", "COL": "COL",
        "1THESS": "1TH", "2THESS": "2TH", "1TIM": "1TI", "2TIM": "2TI", "TITUS": "TIT",
        "PHLM": "PHM", "HEB": "HEB", "JAS": "JAS", "1PET": "1PE", "2PET": "2PE",
        "1JOHN": "1JN", "2JOHN": "2JN", "3JOHN": "3JN", "JUDE": "JUD", "REV": "REV",
    }
    return aliases.get(value, value[:3])


def parse_osis(paths: Iterable[Path]) -> Iterator[Verse]:
    for path in paths:
        root = ET.parse(path).getroot()
        order_by_chapter: dict[tuple[str, int], int] = {}
        for element in root.iter():
            if local_name(element.tag) != "verse":
                continue
            osis_id = element.attrib.get("osisID") or element.attrib.get("sID")
            if not osis_id:
                continue
            first_id = osis_id.split()[0]
            parts = first_id.split(".")
            if len(parts) < 3:
                continue
            book_id = osis_book_id(parts[0])
            try:
                chapter = int(parts[1])
            except ValueError:
                continue
            label = parts[2]
            text = clean_text(" ".join(element.itertext()))
            if not text:
                # Milestone OSIS needs a corpus-specific normalizer; do not fabricate text.
                continue
            key = (book_id, chapter)
            order_by_chapter[key] = order_by_chapter.get(key, 0) + 1
            yield Verse(book_id, chapter, label, order_by_chapter[key], text)


BOOK_NAMES = {
    "GEN": ("Génesis", "Gn", "OT"), "EXO": ("Éxodo", "Ex", "OT"),
    "LEV": ("Levítico", "Lv", "OT"), "NUM": ("Números", "Nm", "OT"),
    "DEU": ("Deuteronomio", "Dt", "OT"), "TOB": ("Tobías", "Tb", "OT"),
    "JDT": ("Judit", "Jdt", "OT"), "WIS": ("Sabiduría", "Sb", "OT"),
    "SIR": ("Sirácida", "Si", "OT"), "1MA": ("1 Macabeos", "1 M", "OT"),
    "2MA": ("2 Macabeos", "2 M", "OT"), "MAT": ("Mateo", "Mt", "NT"),
    "MRK": ("Marcos", "Mc", "NT"), "LUK": ("Lucas", "Lc", "NT"),
    "JHN": ("Juan", "Jn", "NT"), "ACT": ("Hechos", "Hch", "NT"),
    "ROM": ("Romanos", "Rm", "NT"), "REV": ("Apocalipsis", "Ap", "NT"),
}


def schema_path() -> Path:
    return Path(__file__).with_name("schema.sql")


def write_package(output: Path, verses: Iterable[Verse], args: argparse.Namespace) -> None:
    if output.exists():
        output.unlink()
    connection = sqlite3.connect(str(output))
    try:
        connection.executescript(schema_path().read_text(encoding="utf-8"))
        metadata = {
            "edition_id": args.edition_id,
            "name": args.name,
            "abbreviation": args.abbreviation or args.edition_id,
            "language": args.language,
            "canon": args.canon,
            "source_format": args.format.upper(),
            "version": args.version,
            "copyright_notice": args.copyright_notice or "",
            "license_id": args.license_id or "",
            "content_hash": "PENDING",
            "min_app_version": args.min_app_version,
        }
        connection.executemany("INSERT INTO metadata(key,value) VALUES (?,?)", metadata.items())
        book_seen: set[str] = set()
        chapter_ids: dict[tuple[str, int], int] = {}
        chapter_counts: dict[str, set[int]] = {}
        book_order = 0
        count = 0
        for verse in verses:
            if not verse.text:
                continue
            if verse.book_id not in book_seen:
                book_order += 1
                name, short, testament = BOOK_NAMES.get(
                    verse.book_id, (verse.book_id, verse.book_id, "NT" if book_order > 46 else "OT"))
                connection.execute(
                    "INSERT INTO books(book_key,name,short_name,testament,canonical_order,chapter_count) "
                    "VALUES (?,?,?,?,?,0)",
                    (verse.book_id, name, short, testament, book_order),
                )
                book_seen.add(verse.book_id)
            key = (verse.book_id, verse.chapter)
            if key not in chapter_ids:
                cursor = connection.execute(
                    "INSERT INTO chapters(book_key,chapter_number) VALUES (?,?)",
                    (verse.book_id, verse.chapter),
                )
                chapter_ids[key] = int(cursor.lastrowid)
                chapter_counts.setdefault(verse.book_id, set()).add(verse.chapter)
            stable_id = f"{args.edition_id}:{verse.book_id}:{verse.chapter}:{verse.label}"
            connection.execute(
                "INSERT OR REPLACE INTO verses(stable_id,chapter_id,verse_label,verse_order,text) "
                "VALUES (?,?,?,?,?)",
                (stable_id, chapter_ids[key], verse.label, verse.order, verse.text),
            )
            count += 1
        for book_id, chapters in chapter_counts.items():
            connection.execute(
                "UPDATE books SET chapter_count=? WHERE book_key=?", (len(chapters), book_id)
            )
        connection.commit()
        if count == 0:
            raise ValueError("No verses were imported; verify the input format")
    finally:
        connection.close()

    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    connection = sqlite3.connect(str(output))
    try:
        connection.execute("UPDATE metadata SET value=? WHERE key='content_hash'", (digest,))
        connection.commit()
    finally:
        connection.close()
    print(f"Built {output} ({count} verses; sha256 pre-metadata={digest})")


def discover_inputs(source: Path, fmt: str) -> list[Path]:
    if source.is_file():
        return [source]
    patterns = {"usfm": ("*.usfm", "*.sfm", "*.txt"), "usx": ("*.usx", "*.xml"), "osis": ("*.xml", "*.osis")}
    found: list[Path] = []
    for pattern in patterns[fmt]:
        found.extend(source.rglob(pattern))
    return sorted(set(found))


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a Ministerium Bible package")
    parser.add_argument("--format", choices=("usfm", "usx", "osis"), required=True)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--edition-id", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--abbreviation")
    parser.add_argument("--language", default="es")
    parser.add_argument("--canon", default="catholic-73")
    parser.add_argument("--version", default="1")
    parser.add_argument("--license-id")
    parser.add_argument("--copyright-notice")
    parser.add_argument("--min-app-version", default="3.1.0")
    args = parser.parse_args()
    inputs = discover_inputs(args.source, args.format)
    if not inputs:
        parser.error("No input files found")
    parse = {"usfm": parse_usfm, "usx": parse_usx, "osis": parse_osis}[args.format]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    write_package(args.output, parse(inputs), args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
