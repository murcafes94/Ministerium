#!/usr/bin/env python3
"""Extract the Spanish and Latin CIC text from the bilingual annotated PDF."""

from __future__ import annotations

import argparse
import html
import json
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path


NUMBER = re.compile(r"^(\d{1,4})\s*[*†]?$")


def text(node: ET.Element) -> str:
    return re.sub(r"\s+", " ", "".join(node.itertext()).replace("\u00a0", " ")).strip()


def join_lines(lines: list[tuple[int, str]]) -> list[str]:
    current = ""
    for _, value in lines:
        if not value:
            continue
        if current.endswith("-") and value[:1].islower():
            current = current[:-1] + value
        else:
            current += (" " if current else "") + value
    current = re.sub(r"\s+", " ", current).strip()
    # El PDF incluye en algunos saltos de columna dos signos de sección sueltos.
    # Se eliminan cuando preceden a un nuevo apartado y se conservan como «§§»
    # cuando forman parte de una referencia a varios parágrafos.
    current = re.sub(r"\s*§\s+§\s+(?=§\s*\d+\.)", " ", current)
    current = re.sub(r"§\s+§(?=\s+\d)", "§§", current)
    current = re.sub(r"\s+§\s+§(?:\s+|$)", " ", current).strip()
    current = current.replace(" § §", "")
    # En cambios de columna el PDF deja a veces «§§» sin número: no forma
    # parte del canon. Las referencias auténticas siempre llevan el número.
    current = re.sub(r"\s*§§(?!\s*\d)", " ", current)
    current = re.sub(r"\s+", " ", current).strip()
    # Se conserva el canon como un bloque continuo. El PDF no distingue de
    # forma semántica el «§ 2» propio de una referencia como «can. 142, § 2»;
    # dividirlo automáticamente produciría parágrafos falsos.
    return [current] if current else []


def extract(pdf: Path) -> tuple[dict[int, list[str]], dict[int, list[str]]]:
    with tempfile.TemporaryDirectory() as temp:
        target = Path(temp) / "code"
        subprocess.run([
            "pdftohtml", "-xml", "-hidden", "-nodrm", "-enc", "UTF-8",
            str(pdf), str(target)
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        root = ET.parse(target.with_suffix(".xml")).getroot()

    fonts: dict[str, dict[str, str]] = {}
    for page in root.findall("page"):
        for font in page.findall("fontspec"):
            fonts[font.attrib["id"]] = font.attrib

    spanish: dict[int, list[tuple[int, str]]] = defaultdict(list)
    latin: dict[int, list[tuple[int, str]]] = defaultdict(list)
    current = 0
    done = False
    for page in root.findall("page"):
        if done:
            break
        rows: dict[int, list[ET.Element]] = defaultdict(list)
        for node in page.findall("text"):
            top = int(node.attrib.get("top", "0"))
            if 55 <= top <= 720:
                rows[top].append(node)
        for top in sorted(rows):
            nodes = sorted(rows[top], key=lambda n: int(n.attrib.get("left", "0")))
            for node in nodes:
                value = text(node)
                font = fonts.get(node.attrib.get("font", ""), {})
                marker = NUMBER.match(value)
                if (marker and font.get("size") in {"16", "17"}
                        and "LFNMCH+Times" in font.get("family", "")):
                    current = int(marker.group(1))
                    continue
                if (current == 1752 and marker
                        and "Optima" in font.get("family", "")):
                    done = True
                    break
            if done:
                break
            if not current:
                continue
            spanish_parts: list[tuple[int, str]] = []
            latin_parts: list[tuple[int, str]] = []
            for node in nodes:
                font = fonts.get(node.attrib.get("font", ""), {})
                value = text(node)
                if not value or value == "§":
                    continue
                family = font.get("family", "")
                item = (int(node.attrib.get("left", "0")), value)
                if font.get("size") == "13" and "+Times" in family:
                    spanish_parts.append(item)
                elif font.get("size") == "12" and "+Times" in family:
                    latin_parts.append(item)
            if spanish_parts:
                left = min(item[0] for item in spanish_parts)
                column = 227 if left >= 220 else 57
                spanish[current].append((max(0, left - column),
                                         " ".join(value for _, value in spanish_parts)))
            if latin_parts:
                left = min(item[0] for item in latin_parts)
                column = 306 if left >= 220 else 51
                latin[current].append((max(0, left - column),
                                       " ".join(value for _, value in latin_parts)))

    return ({canon: join_lines(lines) for canon, lines in spanish.items()},
            {canon: join_lines(lines) for canon, lines in latin.items()})


def write_assets(spanish: dict[int, list[str]], latin: dict[int, list[str]],
                 output: Path, overrides_file: Path | None = None) -> None:
    overrides: dict[str, object] = {"canons": {}, "notes": {}}
    if overrides_file and overrides_file.exists():
        overrides = json.loads(overrides_file.read_text(encoding="utf-8"))
    amended = overrides.get("canons", {})
    notes = overrides.get("notes", {})
    output.mkdir(parents=True, exist_ok=True)
    chunks: dict[int, list[str]] = defaultdict(list)
    index = ["# canon\tfile\tfragment"]
    for canon in range(1, 1753):
        metadata = amended.get(str(canon), {})
        spanish_paragraphs = metadata.get("es", spanish.get(canon, []))
        latin_paragraphs = metadata.get("la", latin.get(canon, []))
        if not spanish_paragraphs:
            raise RuntimeError(f"Canon {canon} has no extracted Spanish text")
        if not latin_paragraphs:
            raise RuntimeError(f"Canon {canon} has no extracted Latin text")
        chunk = (canon - 1) // 100 + 1
        fragment = f"canon-{canon}"
        spanish_body = "".join(
            f"<p>{html.escape(paragraph)}</p>" for paragraph in spanish_paragraphs)
        latin_body = "".join(
            f"<p>{html.escape(paragraph)}</p>" for paragraph in latin_paragraphs)
        reform_notice = ""
        if metadata:
            spanish_status = metadata.get("spanish_status", "vatican")
            spanish_note = ("Publicación en español del Vaticano"
                            if spanish_status == "vatican"
                            else "Traducción humana de referencia; el texto normativo es el latino")
            reform_notice = (
                '<aside class="reform-note">'
                f'<strong>Texto vigente desde {html.escape(metadata["date"])}</strong><br>'
                f'{html.escape(metadata["reform"])}. {spanish_note}. '
                '<span class="historical-warning">El comentario de 2001 puede referirse '
                'a la redacción anterior.</span> '
                f'<a href="{html.escape(metadata["source"])}">Fuente oficial</a>'
                '</aside>')
        complementary = notes.get(str(canon), {})
        if complementary:
            reform_notice += (
                '<aside class="complementary-note"><strong>Norma complementaria vigente</strong><br>'
                f'{html.escape(complementary["text"])} '
                f'<a href="{html.escape(complementary["source"])}">Fuente oficial</a>'
                '</aside>')
        chunks[chunk].append(
            f'<article class="canon" id="{fragment}"><h1>Canon {canon}</h1>{reform_notice}'
            f'<div class="canon-columns">'
            f'<section class="canon-language spanish" lang="es">'
            f'<h2>Español</h2>{spanish_body}</section>'
            f'<section class="canon-language latin" lang="la">'
            f'<h2>Latín · texto normativo</h2>{latin_body}</section></div>'
            f'<p><a class="comment-link" href="ministerium://canon-comment/{canon}">'
            f'Ver comentario de este canon</a></p></article>')
        index.append(f"{canon}\tcanon-text/canons-{chunk:02d}.html\t{fragment}")

    css = """
html,body{margin:0;padding:0;background:#fff5df;color:#4f4132}
body{font-family:serif;line-height:1.7;padding:16px;box-sizing:border-box}
.canon{display:none;width:100%;max-width:none;box-sizing:border-box}
.canon:target{display:block}.canon:first-child{display:block}.canon:target~.canon:first-child{display:none}
h1{font-family:serif;font-size:1em;letter-spacing:.12em;color:#4f4132;margin:0 0 1em}
.canon-columns{display:block}.canon-language{box-sizing:border-box;border:1px solid #d8c9b5;
border-radius:10px;padding:16px;background:rgba(255,255,255,.32);margin-bottom:14px}
h2{font-family:sans-serif;font-size:.82em;letter-spacing:.08em;text-transform:uppercase;
color:#6e1d2a;margin:0 0 1.2em;padding-bottom:.55em;border-bottom:1px solid #d8c9b5}
p{font-size:1.04em;margin:0 0 1.15em;text-align:justify;hyphens:auto}
.reform-note,.complementary-note{font-family:sans-serif;font-size:.82em;line-height:1.5;
padding:12px 14px;margin:0 0 15px;border-radius:9px;background:#f1e4c9;border-left:4px solid #6e1d2a}
.complementary-note{background:#eee8dc;border-left-color:#9a742b}.reform-note a,.complementary-note a{color:#6e1d2a}
.historical-warning{font-weight:600}
.comment-link{display:inline-block;text-decoration:none;background:#6e1d2a;color:white;
padding:.7em 1em;border-radius:8px;font-family:sans-serif;font-size:.85em;text-align:center}
img,table{max-width:100%;height:auto}*{overflow-wrap:anywhere}
@media(min-width:760px){body{padding:24px}.canon-columns{display:grid;
grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:20px;align-items:start}
.canon-language{margin:0;padding:20px}p{font-size:1.08em}}
""".strip()
    for chunk, articles in chunks.items():
        document = ("<!doctype html><html><head><meta charset=\"utf-8\">"
                    "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    f"<style>{css}</style></head><body>"
                    + "".join(articles) + "</body></html>")
        (output / f"canons-{chunk:02d}.html").write_text(document, encoding="utf-8")
    (output.parent / "canon-text-index.tsv").write_text(
        "\n".join(index) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--overrides", type=Path)
    parser.add_argument("--base-json", type=Path,
                        help="Official Vatican Spanish/Latin base generated from the archive")
    args = parser.parse_args()
    if args.base_json:
        base = json.loads(args.base_json.read_text(encoding="utf-8"))
        spanish = {int(canon): value for canon, value in base["es"].items()}
        latin = {int(canon): value for canon, value in base["la"].items()}
    else:
        spanish, latin = extract(args.pdf)
    write_assets(spanish, latin, args.output, args.overrides)
    print(f"Extracted {len(spanish)} Spanish and {len(latin)} Latin canons")


if __name__ == "__main__":
    main()
