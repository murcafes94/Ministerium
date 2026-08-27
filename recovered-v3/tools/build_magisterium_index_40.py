#!/usr/bin/env python3
"""Build the offline full-text Magisterium search index for Ministerium 4.0."""

from __future__ import annotations

import argparse
import posixpath
import re
import unicodedata
import zipfile
from pathlib import Path
from urllib.parse import unquote
from xml.etree import ElementTree

from bs4 import BeautifulSoup

BOOKS = (
    ("vatican_ii", "Concilio Vaticano II", "epubs/Concilio-Vaticano-II.epub"),
    ("catechism", "Catecismo de la Iglesia Católica", "epubs/Catecismo-Iglesia-Catolica.epub"),
    ("catechism_compendium", "Compendio del Catecismo", "epubs/Compendio-Catecismo.epub"),
    ("social_doctrine", "Compendio de la Doctrina Social", "epubs/Compendio-Doctrina-Social.epub"),
)
BLOCK_TAGS = ("h1", "h2", "h3", "h4", "h5", "p", "li", "blockquote")
CHUNK_TARGET = 1500
CHUNK_OVERLAP_BLOCKS = 2


def fold(value: str) -> str:
    value = "".join(
        ch for ch in unicodedata.normalize("NFD", value)
        if unicodedata.category(ch) != "Mn"
    ).lower()
    return re.sub(r"[^a-z0-9]+", " ", value).strip()


def clean(value: str) -> str:
    return re.sub(r"\s+", " ", value or "").strip().replace("\t", " ")


def zip_path(base: str, relative: str) -> str:
    decoded = unquote(relative.split("#", 1)[0])
    return posixpath.normpath(posixpath.join(base, decoded))


def opf_path(book: zipfile.ZipFile) -> str:
    try:
        container = ElementTree.fromstring(book.read("META-INF/container.xml"))
        rootfile = container.find(".//{*}rootfile")
        if rootfile is not None and rootfile.attrib.get("full-path"):
            return rootfile.attrib["full-path"]
    except (KeyError, ElementTree.ParseError):
        pass
    candidates = [name for name in book.namelist() if name.lower().endswith(".opf")]
    if not candidates:
        raise ValueError("EPUB without OPF package")
    return candidates[0]


def reading_order(book: zipfile.ZipFile) -> list[str]:
    package_path = opf_path(book)
    package = ElementTree.fromstring(book.read(package_path))
    base = posixpath.dirname(package_path)
    manifest: dict[str, tuple[str, str]] = {}
    for item in package.findall(".//{*}manifest/{*}item"):
        item_id = item.attrib.get("id", "")
        href = item.attrib.get("href", "")
        media = item.attrib.get("media-type", "")
        if item_id and href:
            manifest[item_id] = (zip_path(base, href), media)

    ordered: list[str] = []
    for itemref in package.findall(".//{*}spine/{*}itemref"):
        item = manifest.get(itemref.attrib.get("idref", ""))
        if item and ("html" in item[1] or item[0].lower().endswith((".html", ".htm", ".xhtml"))):
            ordered.append(item[0])

    for path, media in manifest.values():
        if path not in ordered and ("html" in media or path.lower().endswith((".html", ".htm", ".xhtml"))):
            ordered.append(path)
    return ordered


def title_for(soup: BeautifulSoup, fallback: str) -> str:
    heading = soup.find(("h1", "h2", "h3"))
    if heading:
        value = clean(heading.get_text(" ", strip=True))
        if value:
            return value[:180]
    if soup.title:
        value = clean(soup.title.get_text(" ", strip=True))
        if value:
            return value[:180]
    return Path(fallback).stem.replace("_", " ").replace("-", " ")[:180]


def reference_for(volume_id: str, title: str, text: str) -> str:
    if volume_id == "catechism":
        match = re.search(r"(?:^|\s)(?:n(?:ú|u)m\.?\s*)?(\d{1,4})(?=\s|[.,;:)])", text[:700], re.I)
        if match:
            return "CEC " + match.group(1)
    if volume_id == "catechism_compendium":
        match = re.search(r"(?:^|\s)(\d{1,3})\s*[.)-]", text[:700])
        if match:
            return "Compendio " + match.group(1)
    if volume_id == "social_doctrine":
        match = re.search(r"(?:^|\s)(\d{1,3})\s*[.)-]", text[:700])
        if match:
            return "DSI " + match.group(1)
    short = clean(title)
    return short[:100] if short else "Sección"


def content_blocks(soup: BeautifulSoup) -> list[str]:
    for tag in soup(("script", "style", "nav", "svg", "noscript")):
        tag.decompose()
    blocks: list[str] = []
    for node in soup.find_all(BLOCK_TAGS):
        # Ignore paragraphs already represented by a parent list item or blockquote.
        if node.name == "p" and node.find_parent(("li", "blockquote")):
            continue
        value = clean(node.get_text(" ", strip=True))
        if len(value) >= 3:
            blocks.append(value)
    if not blocks:
        value = clean(soup.get_text(" ", strip=True))
        if value:
            blocks.append(value)
    return blocks


def chunks(blocks: list[str]) -> list[str]:
    result: list[str] = []
    current: list[str] = []
    length = 0
    for block in blocks:
        if current and length + len(block) + 1 > CHUNK_TARGET:
            result.append(" ".join(current))
            current = current[-CHUNK_OVERLAP_BLOCKS:]
            length = sum(len(item) + 1 for item in current)
        current.append(block)
        length += len(block) + 1
    if current:
        result.append(" ".join(current))
    return result


def compact_snippet(text: str, maximum: int = 340) -> str:
    value = clean(text)
    return value if len(value) <= maximum else value[: maximum - 1].rstrip() + "…"


def build(assets: Path, output: Path) -> tuple[int, int]:
    rows: list[str] = []
    file_count = 0
    for volume_id, volume_title, relative in BOOKS:
        epub_path = assets / relative
        if not epub_path.is_file():
            raise FileNotFoundError(f"Missing Magisterium source: {epub_path}")
        with zipfile.ZipFile(epub_path) as book:
            names = set(book.namelist())
            for file_path in reading_order(book):
                if file_path not in names:
                    continue
                try:
                    soup = BeautifulSoup(book.read(file_path), "html.parser")
                except Exception as error:
                    print(f"warning: cannot parse {relative}:{file_path}: {error}")
                    continue
                page_title = title_for(soup, file_path)
                blocks = content_blocks(soup)
                if not blocks:
                    continue
                file_count += 1
                for chunk in chunks(blocks):
                    normalized = fold(" ".join((volume_title, page_title, chunk)))
                    if len(normalized) < 20:
                        continue
                    reference = reference_for(volume_id, page_title, chunk)
                    fields = (
                        volume_id,
                        file_path,
                        clean(page_title),
                        clean(reference),
                        normalized,
                        compact_snippet(chunk),
                    )
                    rows.append("\t".join(field.replace("\n", " ").replace("\r", " ") for field in fields))

    output.parent.mkdir(parents=True, exist_ok=True)
    header = (
        "# Ministerium Magisterium index v1\n"
        "# volume_id\tfile_path\ttitle\treference\tnormalized_text\tsnippet\n"
    )
    output.write_text(header + "\n".join(rows) + ("\n" if rows else ""), encoding="utf-8")
    return file_count, len(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--assets", type=Path, default=Path("app/src/main/assets"))
    parser.add_argument("--output", type=Path, default=Path("app/src/main/assets/magisterium-index.tsv"))
    args = parser.parse_args()
    files, rows = build(args.assets, args.output)
    if rows < 20:
        raise SystemExit(f"Magisterium index unexpectedly small: {rows} rows")
    print(f"Magisterium index OK: {files} XHTML files, {rows} searchable chunks -> {args.output}")


if __name__ == "__main__":
    main()
