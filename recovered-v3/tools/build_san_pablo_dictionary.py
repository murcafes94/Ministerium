#!/usr/bin/env python3
"""Convierte el Diccionario bíblico abreviado de San Pablo en un EPUB textual.

La fuente PDF contiene texto real y distingue cada lema con un cuerpo tipográfico
propio. Se usa esa estructura para reconstruir los artículos, eliminando números de
página y la sección final de índice. El PDF original no se incorpora a la aplicación.
"""

import html
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


def clean(value):
    return re.sub(r"\s+", " ", value.replace("\x00", " ").replace("\xa0", " ")).strip()


def join_fragments(nodes):
    raw = "".join("".join(node.itertext()) for node in sorted(nodes, key=lambda n: int(n.attrib["left"])))
    value = clean(raw)
    return re.sub(r"\s+([,.;:!?])", r"\1", value)


def append_reflowed(target, text):
    if not target:
        return text
    if target.endswith("-") and len(target) > 1 and text:
        if target[-2].isalpha() and text[0].islower():
            return target[:-1] + text
        if target[-2].isdigit() and text[0].isdigit():
            return target + text
    return target + " " + text


def pdf_to_xml(pdf_path):
    with tempfile.TemporaryDirectory(prefix="ministerium-san-pablo-") as tmp:
        output = Path(tmp) / "dictionary.xml"
        subprocess.run(
            ["pdftohtml", "-xml", "-i", "-hidden", str(pdf_path), str(output)],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return ET.parse(output).getroot()


def logical_lines(page, fonts):
    nodes = []
    for node in page.findall("text"):
        value = clean("".join(node.itertext()))
        if not value:
            continue
        top = int(node.attrib.get("top", "0"))
        left = int(node.attrib.get("left", "0"))
        size = fonts.get(node.attrib.get("font"), 0)
        if top > 1080 and 420 < left < 500 and re.fullmatch(r"\d{1,4}", value):
            continue
        nodes.append((top, left, size, node))

    groups = []
    for top, left, size, node in sorted(nodes, key=lambda item: (item[0], item[1])):
        if groups and abs(groups[-1]["top"] - top) <= 4:
            groups[-1]["nodes"].append(node)
            groups[-1]["left"] = min(groups[-1]["left"], left)
            groups[-1]["sizes"].append(size)
        else:
            groups.append({"top": top, "left": left, "sizes": [size], "nodes": [node]})

    for group in groups:
        group["text"] = join_fragments(group["nodes"])
        group["heading"] = 23 in group["sizes"] and group["left"] < 250
    return groups


def paragraphs_for(lines):
    paragraphs = []
    current = ""
    current_class = ""
    previous = None

    def flush():
        nonlocal current, current_class
        if current:
            paragraphs.append((current_class, current))
        current = ""
        current_class = ""

    for line in lines:
        text = line["text"]
        if not text or (41 in line["sizes"] and re.fullmatch(r"[A-Z]", text)):
            continue
        reference = text.startswith("→")
        gap = 0
        if previous is not None and previous["page"] == line["page"]:
            gap = line["top"] - previous["top"]
        indented_paragraph = (
            previous is not None
            and previous["page"] == line["page"]
            and line["left"] > 110
            and previous["text"].rstrip().endswith((".", "!", "?", "»", ")"))
        )
        numbered_after_reference = (
            current_class == "dictionary-reference"
            and re.match(r"^\d+\.\s", text)
            and current.rstrip().endswith(".")
        )
        if current and (reference or gap > 38 or indented_paragraph or numbered_after_reference):
            flush()
        if reference and not current:
            current_class = "dictionary-reference"
        current = append_reflowed(current, text)
        previous = line
    flush()
    return paragraphs


def parse_entries(pdf_path):
    root = pdf_to_xml(pdf_path)
    fonts = {}
    entries = []
    current_term = None
    current_lines = []
    started = False

    def flush():
        nonlocal current_term, current_lines
        if current_term:
            entries.append({"term": current_term, "paragraphs": paragraphs_for(current_lines)})
        current_term = None
        current_lines = []

    for page in root.findall("page"):
        page_number = int(page.attrib["number"])
        for spec in page.findall("fontspec"):
            fonts[spec.attrib["id"]] = int(spec.attrib["size"])
        for line in logical_lines(page, fonts):
            line["page"] = page_number
            if line["heading"]:
                term = line["text"]
                if term == "Aarón":
                    started = True
                if not started:
                    continue
                if term == "Índice":
                    flush()
                    return entries
                flush()
                current_term = term
            elif started and current_term:
                current_lines.append(line)
    flush()
    return entries


def article_xhtml(entry):
    content = []
    for class_name, paragraph in entry["paragraphs"]:
        class_attr = f' class="{class_name}"' if class_name else ""
        content.append(f"<p{class_attr}>{html.escape(paragraph)}</p>")
    return (
        f'<article id="{entry["fragment"]}" class="ministerium-dictionary-entry">'
        f'<h1>{html.escape(entry["term"])}</h1>{"".join(content)}</article>'
    )


def xhtml_for(entries, title):
    return """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="es">
<head><meta charset="utf-8"/><title>{}</title>
<link rel="stylesheet" type="text/css" href="../Styles/dictionary.css"/></head>
<body>{}</body></html>
""".format(html.escape(title), "\n".join(article_xhtml(entry) for entry in entries))


def build_epub(entries, epub_path, index_path):
    chunk_size = 30
    chunks = [entries[index:index + chunk_size] for index in range(0, len(entries), chunk_size)]
    for number, entry in enumerate(entries):
        entry["file"] = f"OEBPS/Text/entries-{number // chunk_size:03d}.xhtml"
        entry["fragment"] = f"san-pablo-entry-{number + 1:04d}"

    index_lines = ["# Ministerium San Pablo biblical dictionary index v1"]
    index_lines.extend(
        f'{entry["term"]}\t{entry["file"]}\t{entry["fragment"]}' for entry in entries
    )
    index_path.write_text("\n".join(index_lines) + "\n", encoding="utf-8")

    manifest = []
    spine = []
    nav_points = []
    for number, chunk in enumerate(chunks):
        name = f"entries-{number:03d}.xhtml"
        manifest.append(f'<item id="entry{number}" href="Text/{name}" media-type="application/xhtml+xml"/>')
        spine.append(f'<itemref idref="entry{number}"/>')
        nav_points.append(
            f'<navPoint id="chunk{number}" playOrder="{number + 1}"><navLabel><text>'
            f'{html.escape(chunk[0]["term"])} - {html.escape(chunk[-1]["term"])}'
            f'</text></navLabel><content src="Text/{name}"/></navPoint>'
        )

    container = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""
    opf = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="BookID">ministerium-biblical-dictionary-san-pablo</dc:identifier>
<dc:title>Diccionario bíblico abreviado</dc:title><dc:creator>Equipo editorial San Pablo</dc:creator>
<dc:language>es</dc:language></metadata><manifest>
<item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
<item id="css" href="Styles/dictionary.css" media-type="text/css"/>{}</manifest>
<spine toc="ncx">{}</spine></package>""".format("".join(manifest), "".join(spine))
    toc = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<head><meta name="dtb:uid" content="ministerium-biblical-dictionary-san-pablo"/></head>
<docTitle><text>Diccionario bíblico abreviado</text></docTitle><navMap>{}</navMap></ncx>""".format(
        "".join(nav_points)
    )
    css = """body{font-family:serif;line-height:1.6;margin:0;padding:16px;background:#fffdf7;color:#2a2521}
.ministerium-dictionary-entry{margin:0 0 18px;padding:18px;border:1px solid #d8c9b5;border-left:4px solid #6e1d2a;border-radius:10px;background:#fff}
.ministerium-dictionary-entry h1{margin:0 0 12px;color:#6e1d2a}.dictionary-reference{font-style:italic;color:#6f665e}
"""

    epub_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(epub_path, "w") as archive:
        archive.writestr("mimetype", "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        archive.writestr("META-INF/container.xml", container)
        archive.writestr("OEBPS/content.opf", opf)
        archive.writestr("OEBPS/toc.ncx", toc)
        archive.writestr("OEBPS/Styles/dictionary.css", css)
        for number, chunk in enumerate(chunks):
            archive.writestr(
                f"OEBPS/Text/entries-{number:03d}.xhtml",
                xhtml_for(chunk, f"Diccionario bíblico abreviado {number + 1}"),
            )


def main():
    if len(sys.argv) != 4:
        raise SystemExit("Uso: build_san_pablo_dictionary.py <entrada.pdf> <salida.epub> <indice.tsv>")
    pdf_path, epub_path, index_path = map(Path, sys.argv[1:])
    entries = parse_entries(pdf_path)
    if len(entries) != 881:
        raise RuntimeError(f"Extracción incompleta: se esperaban 881 entradas y se obtuvieron {len(entries)}")
    if entries[0]["term"] != "Aarón" or entries[-1]["term"] != "Zorra y chacal":
        raise RuntimeError("Los límites alfabéticos del diccionario no coinciden")
    build_epub(entries, epub_path, index_path)
    print(f"Diccionario bíblico San Pablo: {len(entries)} entradas extraídas como texto")


if __name__ == "__main__":
    main()
