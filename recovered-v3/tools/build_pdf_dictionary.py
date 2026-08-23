#!/usr/bin/env python3
import html
import re
import subprocess
import sys
import zipfile
from pathlib import Path


def clean_line(value):
    return re.sub(r"\s+", " ", value.replace("\x00", " ")).strip()


def join_reflowed(lines):
    result = ""
    for raw in lines:
        line = clean_line(raw)
        if not line:
            continue
        if result.endswith("-") and line:
            before = result[-2:-1]
            first = line[:1]
            if before.isdigit() and first.isdigit():
                result += line
                continue
            if before.isalpha() and first.islower():
                result = result[:-1] + line
                continue
        result += (" " if result else "") + line
    return re.sub(r"\s+([,.;:!?])", r"\1", result).strip()


def split_heading(value):
    heading = clean_line(value).rstrip(".")
    match = re.match(
        r"^(.*?)\s*(?:\.|\()\s*(V[ée]ase|V[ée]anse|Ver)\s+(.+?)\)?$",
        heading,
        re.IGNORECASE,
    )
    if match:
        return match.group(1).strip(), f"Véase: {match.group(3).strip()}"
    return heading, ""


def parse_entries(pdf_path):
    raw = subprocess.check_output(
        ["pdftotext", "-raw", str(pdf_path), "-"],
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    marker = re.compile(r"^(?:nom|Nombre),\s*(.+)$", re.IGNORECASE)
    entries = []
    current_term = None
    current_lines = []
    heading_reference = ""

    def flush():
        nonlocal current_term, current_lines, heading_reference
        if not current_term:
            return
        lines = [clean_line(line) for line in current_lines]
        lines = [line for line in lines if line and not re.fullmatch(r"\d{1,4}", line)]
        references = [heading_reference] if heading_reference else []
        body = []
        reading_reference = False
        reading_tags = False
        for line in lines:
            lower = line.lower()
            if lower.startswith("tip,"):
                reading_tags = True
                continue
            if reading_tags and re.fullmatch(r"[A-ZÁÉÍÓÚÜÑ ]+", line):
                continue
            reading_tags = False
            if lower == "vet," or lower == "***":
                continue
            if lower.startswith("ver,"):
                references.append("Véase: " + line.split(",", 1)[1].strip())
                reading_reference = True
                continue
            if reading_reference and line == line.upper() and len(line) < 80:
                references[-1] += " " + line
                continue
            reading_reference = False
            if not re.fullmatch(r"[A-ZÑ]", line):
                body.append(line)
        definition = join_reflowed(body)
        entries.append({
            "term": current_term,
            "references": join_reflowed(references),
            "definition": definition,
        })
        current_term = None
        current_lines = []
        heading_reference = ""

    for raw_line in raw.replace("\f", "\n").splitlines():
        line = clean_line(raw_line)
        found = marker.match(line)
        if found:
            flush()
            current_term, heading_reference = split_heading(found.group(1))
        elif current_term:
            current_lines.append(line)
    flush()
    return entries


def xhtml_for(entries, title):
    articles = []
    for entry in entries:
        reference = ""
        if entry["references"]:
            reference = f'<p class="dictionary-reference">{html.escape(entry["references"])}</p>'
        definition = ""
        if entry["definition"]:
            definition = f'<p>{html.escape(entry["definition"])}</p>'
        articles.append(
            f'<article id="{entry["fragment"]}" class="ministerium-dictionary-entry">'
            f'<h1>{html.escape(entry["term"])}</h1>{reference}'
            f'{definition}</article>'
        )
    return """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="es">
<head><meta charset="utf-8"/><title>{}</title>
<link rel="stylesheet" type="text/css" href="../Styles/dictionary.css"/></head>
<body>{}</body></html>
""".format(html.escape(title), "\n".join(articles))


def build_epub(entries, epub_path, index_path):
    chunk_size = 40
    chunks = [entries[i:i + chunk_size] for i in range(0, len(entries), chunk_size)]
    for number, entry in enumerate(entries):
        entry["file"] = f"OEBPS/Text/entries-{number // chunk_size:03d}.xhtml"
        entry["fragment"] = f"entry-{number + 1:05d}"

    index_lines = ["# Ministerium biblical dictionary index v1"]
    for entry in entries:
        index_lines.append(f'{entry["term"]}\t{entry["file"]}\t{entry["fragment"]}')
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
<dc:identifier id="BookID">ministerium-biblical-dictionary</dc:identifier>
<dc:title>Diccionario bíblico</dc:title><dc:language>es</dc:language>
</metadata><manifest><item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
<item id="css" href="Styles/dictionary.css" media-type="text/css"/>{}</manifest>
<spine toc="ncx">{}</spine></package>""".format("".join(manifest), "".join(spine))
    toc = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<head><meta name="dtb:uid" content="ministerium-biblical-dictionary"/></head>
<docTitle><text>Diccionario bíblico</text></docTitle><navMap>{}</navMap></ncx>""".format(
        "".join(nav_points)
    )
    css = """body{font-family:serif;line-height:1.6;margin:0;padding:16px;background:#fffdf7;color:#2a2521}
.ministerium-dictionary-entry{margin:0 0 18px;padding:18px;border:1px solid #d8c9b5;border-radius:10px;background:#fff}
.ministerium-dictionary-entry h1{margin:0 0 12px;color:#6e1d2a}
.dictionary-reference{font-style:italic;color:#6f665e}
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
                xhtml_for(chunk, f"Diccionario bíblico {number + 1}"),
            )


def main():
    if len(sys.argv) != 4:
        raise SystemExit("Uso: build_pdf_dictionary.py <entrada.pdf> <salida.epub> <indice.tsv>")
    pdf_path, epub_path, index_path = map(Path, sys.argv[1:])
    entries = parse_entries(pdf_path)
    if len(entries) < 2_800:
        raise RuntimeError(f"Extracción incompleta: solo {len(entries)} entradas")
    build_epub(entries, epub_path, index_path)
    print(f"Diccionario bíblico: {len(entries)} entradas extraídas como texto")


if __name__ == "__main__":
    main()
