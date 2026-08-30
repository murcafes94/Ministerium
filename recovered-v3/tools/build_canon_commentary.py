#!/usr/bin/env python3
"""Extract only the doctrinal comments from the annotated CIC PDF.

The book uses Times for the bilingual canons, Optima for comment numbers and
AGaramond for the commentary.  Using those typographic roles avoids mixing the
Latin/Spanish code with its study notes.
"""

from __future__ import annotations

import argparse
import html
import re
import subprocess
import tempfile
import unicodedata
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path


MARKER = re.compile(r"^(\d{1,4})(?:\s*[-–]\s*(\d{1,4}))?$")


def plain(node: ET.Element) -> str:
    return "".join(node.itertext()).replace("\u00a0", " ").strip()


def normalized_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def join_lines(lines: list[tuple[int, str]]) -> str:
    paragraphs: list[str] = []
    current = ""
    for left, value in lines:
        value = normalized_space(value)
        if not value:
            continue
        begins_paragraph = left >= 76 and bool(current)
        if begins_paragraph:
            paragraphs.append(current.strip())
            current = ""
        if current.endswith("-") and value[:1].islower():
            current = current[:-1] + value
        else:
            current += (" " if current else "") + value
    if current.strip():
        paragraphs.append(current.strip())
    return "\n".join(paragraphs)


def extract(pdf: Path) -> list[tuple[str, list[int], str]]:
    with tempfile.TemporaryDirectory() as temp:
        target = Path(temp) / "comments"
        subprocess.run([
            "pdftohtml", "-xml", "-hidden", "-nodrm", "-enc", "UTF-8",
            str(pdf), str(target)
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        root = ET.parse(target.with_suffix(".xml")).getroot()

    fonts: dict[str, str] = {}
    for page in root.findall("page"):
        for font in page.findall("fontspec"):
            fonts[font.attrib["id"]] = font.attrib.get("family", "")

    groups: list[tuple[str, list[tuple[int, str]]]] = []
    current_marker = ""
    current_lines: list[tuple[int, str]] = []
    for page in root.findall("page"):
        rows: dict[int, list[tuple[int, str, str]]] = defaultdict(list)
        for node in page.findall("text"):
            top = int(node.attrib.get("top", "0"))
            if top < 55 or top > 725:
                continue
            value = plain(node)
            if not value:
                continue
            left = int(node.attrib.get("left", "0"))
            family = fonts.get(node.attrib.get("font", ""), "")
            rows[top].append((left, family, value))

        for top in sorted(rows):
            cells = sorted(rows[top], key=lambda item: item[0])
            markers = [value for _, family, value in cells
                       if "Optima" in family and MARKER.match(value)]
            if markers:
                if current_marker and current_lines:
                    groups.append((current_marker, current_lines))
                current_marker = markers[0].replace("–", "-").replace(" ", "")
                current_lines = []
            if not current_marker:
                continue
            comment_cells = [(left, value) for left, family, value in cells
                             if "AGaramond" in family and "Semibold" not in family]
            if comment_cells:
                left = min(item[0] for item in comment_cells)
                current_lines.append((left, normalized_space(
                    "".join(("" if index == 0 else " ") + value
                            for index, (_, value) in enumerate(comment_cells)))))

    if current_marker and current_lines:
        groups.append((current_marker, current_lines))

    result: list[tuple[str, list[int], str]] = []
    for marker, lines in groups:
        match = MARKER.match(marker)
        if not match:
            continue
        first = int(match.group(1))
        last = int(match.group(2) or first)
        text = join_lines(lines)
        if not text:
            continue
        result.append((marker, list(range(first, last + 1)), text))
    return result


def write_assets(groups: list[tuple[str, list[int], str]], output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    index_lines = ["# canon\tfile\tfragment\tcommented_canons"]
    chunks: dict[int, list[str]] = defaultdict(list)
    for marker, canons, text in groups:
        chunk = (canons[0] - 1) // 100 + 1
        fragment = "comment-" + marker
        paragraphs = "".join(f"<p>{html.escape(paragraph)}</p>"
                             for paragraph in text.splitlines() if paragraph.strip())
        chunks[chunk].append(
            f'<article class="canon-comment" id="{fragment}">'
            f'<h1>Comentario al canon {html.escape(marker)}</h1>'
            f'<p class="source">Comentarios al Código de Derecho Canónico, '
            f'edición de estudio (2001)</p>{paragraphs}</article>')
        file_name = f"canon-comments-{chunk:02d}.html"
        for canon in canons:
            index_lines.append(f"{canon}\tcanon-comments/{file_name}\t{fragment}\t{marker}")

    style = """
html,body{margin:0;padding:0;background:#fffdf7;color:#2a2521}
body{font-family:serif;line-height:1.62;padding:18px;box-sizing:border-box}
.canon-comment{display:block;max-width:none;width:100%;box-sizing:border-box;
padding:18px;border:1px solid #d8c9b5;border-left:5px solid #6e1d2a;
border-radius:10px;background:#fff}
h1{font-size:1.35em;line-height:1.25;color:#6e1d2a;margin:.1em 0 .35em}
.source{font-size:.85em;color:#6f665e;font-style:italic;margin-top:0}
p{margin:.7em 0}img,table{max-width:100%;height:auto}*{overflow-wrap:anywhere}
""".strip()
    for chunk, articles in chunks.items():
        document = ("<!doctype html><html><head><meta charset=\"utf-8\">"
                    "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    f"<style>{style}</style></head><body>"
                    + "".join(articles) + "</body></html>")
        (output / f"canon-comments-{chunk:02d}.html").write_text(
            document, encoding="utf-8")
    (output.parent / "canon-commentary-index.tsv").write_text(
        "\n".join(index_lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    groups = extract(args.pdf)
    write_assets(groups, args.output)
    mapped = sum(len(canons) for _, canons, _ in groups)
    print(f"Extracted {len(groups)} comment groups mapped to {mapped} canons")


if __name__ == "__main__":
    main()
