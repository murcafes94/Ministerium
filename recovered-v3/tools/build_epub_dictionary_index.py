#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


def main():
    if len(sys.argv) != 3:
        raise SystemExit("Uso: build_epub_dictionary_index.py <entrada.epub> <indice.tsv>")
    epub_path, index_path = map(Path, sys.argv[1:])
    with zipfile.ZipFile(epub_path) as archive:
        root = ET.fromstring(archive.read("toc.ncx"))
    namespace = {"n": "http://www.daisy.org/z3986/2005/ncx/"}
    nav_map = root.find("n:navMap", namespace)
    ignored = {"presentación", "colaboradores", "abreviaturas"}
    rows = ["# Ministerium theology dictionary index v1"]
    for point in nav_map.findall("n:navPoint", namespace):
        title = "".join(point.find("n:navLabel/n:text", namespace).itertext()).strip().rstrip(".")
        if title.lower() in ignored:
            continue
        source = point.find("n:content", namespace).get("src")
        parts = source.split("#", 1)
        fragment = parts[1] if len(parts) > 1 else ""
        rows.append(f"{title}\t{parts[0]}\t{fragment}")
    if len(rows) < 90:
        raise RuntimeError(f"Índice teológico incompleto: {len(rows) - 1} entradas")
    index_path.write_text("\n".join(rows) + "\n", encoding="utf-8")
    print(f"Diccionario de Teología: {len(rows) - 1} voces indexadas")


if __name__ == "__main__":
    main()
