#!/usr/bin/env python3
"""Genera el índice compacto de la Biblia de Jerusalén incluida en la app."""

import html
import json
import re
import sys
import zipfile
from pathlib import Path


BOOKS = [
    ("Gn", "Génesis", "Antiguo Testamento"), ("Ex", "Éxodo", "Antiguo Testamento"),
    ("Lv", "Levítico", "Antiguo Testamento"), ("Nm", "Números", "Antiguo Testamento"),
    ("Dt", "Deuteronomio", "Antiguo Testamento"), ("Jos", "Josué", "Antiguo Testamento"),
    ("Jc", "Jueces", "Antiguo Testamento"), ("Rt", "Rut", "Antiguo Testamento"),
    ("1 S", "Primer libro de Samuel", "Antiguo Testamento"),
    ("2 S", "Segundo libro de Samuel", "Antiguo Testamento"),
    ("1 R", "Primer libro de los Reyes", "Antiguo Testamento"),
    ("2 R", "Segundo libro de los Reyes", "Antiguo Testamento"),
    ("1 Cro", "Primer libro de las Crónicas", "Antiguo Testamento"),
    ("2 Cro", "Segundo libro de las Crónicas", "Antiguo Testamento"),
    ("Esd", "Esdras", "Antiguo Testamento"), ("Ne", "Nehemías", "Antiguo Testamento"),
    ("Tb", "Tobías", "Antiguo Testamento"), ("Jdt", "Judit", "Antiguo Testamento"),
    ("Est", "Ester", "Antiguo Testamento"), ("1 M", "Primer libro de los Macabeos", "Antiguo Testamento"),
    ("2 M", "Segundo libro de los Macabeos", "Antiguo Testamento"),
    ("Sal", "Salmos", "Antiguo Testamento"), ("Ct", "Cantar de los Cantares", "Antiguo Testamento"),
    ("Lm", "Lamentaciones", "Antiguo Testamento"), ("Jb", "Job", "Antiguo Testamento"),
    ("Pr", "Proverbios", "Antiguo Testamento"), ("Qo", "Eclesiastés", "Antiguo Testamento"),
    ("Sb", "Sabiduría", "Antiguo Testamento"), ("Si", "Eclesiástico", "Antiguo Testamento"),
    ("Is", "Isaías", "Antiguo Testamento"), ("Jr", "Jeremías", "Antiguo Testamento"),
    ("Ba", "Baruc", "Antiguo Testamento"), ("Ez", "Ezequiel", "Antiguo Testamento"),
    ("Dn", "Daniel", "Antiguo Testamento"), ("Os", "Oseas", "Antiguo Testamento"),
    ("Jl", "Joel", "Antiguo Testamento"), ("Am", "Amós", "Antiguo Testamento"),
    ("Ab", "Abdías", "Antiguo Testamento"), ("Jon", "Jonás", "Antiguo Testamento"),
    ("Mi", "Miqueas", "Antiguo Testamento"), ("Na", "Nahúm", "Antiguo Testamento"),
    ("Ha", "Habacuc", "Antiguo Testamento"), ("So", "Sofonías", "Antiguo Testamento"),
    ("Ag", "Ageo", "Antiguo Testamento"), ("Za", "Zacarías", "Antiguo Testamento"),
    ("Ml", "Malaquías", "Antiguo Testamento"),
    ("Mt", "Evangelio según san Mateo", "Nuevo Testamento"),
    ("Mc", "Evangelio según san Marcos", "Nuevo Testamento"),
    ("Lc", "Evangelio según san Lucas", "Nuevo Testamento"),
    ("Jn", "Evangelio según san Juan", "Nuevo Testamento"),
    ("Hch", "Hechos de los Apóstoles", "Nuevo Testamento"),
    ("Rm", "Romanos", "Nuevo Testamento"), ("1 Co", "Primera carta a los Corintios", "Nuevo Testamento"),
    ("2 Co", "Segunda carta a los Corintios", "Nuevo Testamento"),
    ("Ga", "Gálatas", "Nuevo Testamento"), ("Ef", "Efesios", "Nuevo Testamento"),
    ("Flp", "Filipenses", "Nuevo Testamento"), ("Col", "Colosenses", "Nuevo Testamento"),
    ("1 Ts", "Primera carta a los Tesalonicenses", "Nuevo Testamento"),
    ("2 Ts", "Segunda carta a los Tesalonicenses", "Nuevo Testamento"),
    ("1 Tm", "Primera carta a Timoteo", "Nuevo Testamento"),
    ("2 Tm", "Segunda carta a Timoteo", "Nuevo Testamento"),
    ("Tt", "Tito", "Nuevo Testamento"), ("Flm", "Filemón", "Nuevo Testamento"),
    ("Hb", "Hebreos", "Nuevo Testamento"), ("St", "Santiago", "Nuevo Testamento"),
    ("1 P", "Primera carta de san Pedro", "Nuevo Testamento"),
    ("2 P", "Segunda carta de san Pedro", "Nuevo Testamento"),
    ("1 Jn", "Primera carta de san Juan", "Nuevo Testamento"),
    ("2 Jn", "Segunda carta de san Juan", "Nuevo Testamento"),
    ("3 Jn", "Tercera carta de san Juan", "Nuevo Testamento"),
    ("Judas", "Judas", "Nuevo Testamento"), ("Ap", "Apocalipsis", "Nuevo Testamento"),
]

ONE_CHAPTER = {
    "Ab": "text/part0070.html", "Flm": "text/part0105.html",
    "2 Jn": "text/part0114.html", "3 Jn": "text/part0115.html",
    "Judas": "text/part0116.html",
}


def first_verse(source: str, start: int = 0) -> str:
    match = re.search(r'<sup[^>]+id="([^"]+)"', source[start:], re.I)
    return match.group(1) if match else ""


def main(epub_path: Path, output_path: Path) -> None:
    found = {abbr: {} for abbr, _, _ in BOOKS}
    with zipfile.ZipFile(epub_path) as archive:
        for name in sorted(archive.namelist()):
            if not name.startswith("text/part") or not name.endswith(".html"):
                continue
            source = archive.read(name).decode("utf-8", "ignore")
            for match in re.finditer(
                    r'<span class="(?:capital|salmocapital1)">\s*([^<]*?)\s+(\d+)\s*</span>',
                    source, re.I):
                abbreviation = html.unescape(match.group(1)).strip()
                chapter = int(match.group(2))
                if abbreviation in found and chapter not in found[abbreviation]:
                    found[abbreviation][chapter] = {
                        "number": chapter, "file": name,
                        "fragment": first_verse(source, match.end()),
                    }
            for match in re.finditer(r'<h3[^>]+id="([^"]+)"[^>]*>\s*SALMO\s+(\d+)', source, re.I):
                chapter = int(match.group(2))
                found["Sal"][chapter] = {
                    "number": chapter, "file": name, "fragment": match.group(1),
                }

        for abbreviation, name in ONE_CHAPTER.items():
            source = archive.read(name).decode("utf-8", "ignore")
            found[abbreviation][1] = {
                "number": 1, "file": name, "fragment": first_verse(source),
            }

        # Esta edición reúne los salmos 9-10 y 114-115 bajo un solo encabezado.
        # Se conservan ambos números en el selector y ambos abren el texto reunido.
        found["Sal"][10] = dict(found["Sal"][9], number=10)
        found["Sal"][43] = dict(found["Sal"][42], number=43)
        found["Sal"][115] = dict(found["Sal"][114], number=115)

    books = []
    for abbreviation, title, testament in BOOKS:
        chapters = [found[abbreviation][number] for number in sorted(found[abbreviation])]
        if not chapters:
            raise SystemExit(f"No se encontraron capítulos para {title} ({abbreviation})")
        books.append({
            "abbreviation": abbreviation, "title": title,
            "testament": testament, "chapters": chapters,
        })

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps({"books": books}, ensure_ascii=False, separators=(",", ":")),
                           encoding="utf-8")
    print(f"{len(books)} libros y {sum(len(book['chapters']) for book in books)} capítulos")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Uso: generate_bible_index.py BIBLIA.epub bible-index.json")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
