#!/usr/bin/env python3
"""Build a runtime-only Bible chapter search index from the bundled EPUB.

The generated asset is produced in CI and is not committed. It contains no new
source: it is a chapter-level search projection of the Bible EPUB already
bundled by Ministerium.
"""

from __future__ import annotations

import html
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
EPUB = ASSETS / "epubs" / "Biblia-de-Jerusalen.epub"
INDEX = ASSETS / "bible-index.json"
OUTPUT = ASSETS / "bible-search-index.tsv"

TAG_RE = re.compile(r"<[^>]+>", re.S)
SCRIPT_RE = re.compile(r"<(script|style)\b[^>]*>.*?</\1>", re.I | re.S)
SUP_RE = re.compile(r"<sup\b[^>]*>.*?</sup>", re.I | re.S)
SPACE_RE = re.compile(r"\s+")


def member_name(names: list[str], relative: str) -> str:
    normalized = relative.lstrip("/").replace("\\", "/")
    candidates = [normalized, "OEBPS/" + normalized, "OPS/" + normalized]
    for candidate in candidates:
        if candidate in names:
            return candidate
    suffix = "/" + normalized
    for name in names:
        if name.endswith(suffix) or name == normalized:
            return name
    raise KeyError(relative)


def anchor_position(document: str, fragment: str) -> int:
    if not fragment:
        return 0
    for quote in ('"', "'"):
        marker = f"id={quote}{fragment}{quote}"
        position = document.find(marker)
        if position >= 0:
            tag = document.rfind("<", 0, position)
            return tag if tag >= 0 else position
    return -1


def plain_text(source: str) -> str:
    source = SCRIPT_RE.sub(" ", source)
    # Verse numbers are useful for navigation but not for phrase matching.
    source = SUP_RE.sub(" ", source)
    source = source.replace("<br>", " ").replace("<br/>", " ").replace("<br />", " ")
    value = TAG_RE.sub(" ", source)
    value = html.unescape(value)
    return SPACE_RE.sub(" ", value).strip()


def main() -> int:
    if not EPUB.is_file():
        raise SystemExit(f"Missing Bible EPUB: {EPUB}")
    data = json.loads(INDEX.read_text(encoding="utf-8"))
    books = data.get("books", [])

    with zipfile.ZipFile(EPUB) as archive:
        names = archive.namelist()
        documents: dict[str, str] = {}

        def read(relative: str) -> str:
            if relative not in documents:
                name = member_name(names, relative)
                documents[relative] = archive.read(name).decode("utf-8", errors="replace")
            return documents[relative]

        rows: list[str] = []
        for book_index, book in enumerate(books):
            chapters = book.get("chapters", [])
            abbreviation = str(book.get("abbreviation") or book.get("title") or "").strip()
            for chapter_index, chapter in enumerate(chapters):
                file_name = chapter["file"]
                document = read(file_name)
                start = anchor_position(document, str(chapter.get("fragment", "")))
                if start < 0:
                    start = 0
                end = len(document)
                if chapter_index + 1 < len(chapters):
                    next_chapter = chapters[chapter_index + 1]
                    if next_chapter.get("file") == file_name:
                        candidate = anchor_position(document, str(next_chapter.get("fragment", "")))
                        if candidate > start:
                            end = candidate
                text = plain_text(document[start:end]).replace("\t", " ").replace("\n", " ")
                reference = f"{abbreviation} {chapter.get('number', chapter_index + 1)}"
                rows.append(f"{book_index}\t{chapter_index}\t{reference}\t{text}")

    OUTPUT.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    print(f"Bible search index: {len(rows)} chapters, {OUTPUT.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
