#!/usr/bin/env python3
"""Build native/offline ritual text packages from Liturgia Papal PDFs.

PDFs are build sources, never the final in-app reader. The generated text keeps
liturgical headings/rubrics/formulas while dropping obvious pagination/site
headers. Each output file keeps its source URL in manifest.json.
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

try:
    from pypdf import PdfReader
except ImportError:
    PdfReader = None

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "tools" / "cache" / "ritual-pdf"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "rituals" / "liturgiapapal"

SOURCES = {
    "baptism_one_child": {
        "title": "Ritual del Bautismo de niños — Bautismo de un solo niño",
        "url": "https://www.liturgiapapal.org/attachments/article/612/3%20Parte.pdf",
        "required": "BAUTISMO DE UN SOLO NIÑO",
    },
    "baptism_danger": {
        "title": "Ritual del Bautismo de niños — peligro de muerte",
        "url": "https://www.liturgiapapal.org/attachments/article/612/4%20Parte.pdf",
        "required": "PELIGRO DE MUERTE",
    },
    "baptism_received": {
        "title": "Ritual del Bautismo de niños — niño ya bautizado",
        "url": "https://www.liturgiapapal.org/attachments/article/612/5%20Parte.pdf",
        "required": "NIÑO YA BAUTIZADO",
    },
    "unction": {
        "title": "Ritual de la Unción y de la pastoral de enfermos",
        "url": "https://liturgiapapal.org/attachments/article/762/Ritual%20Uncio%CC%81n.pdf",
        "required": "UNCIÓN DEL ENFERMO",
    },
    "funeral_praenotanda": {
        "title": "Ritual de exequias — Praenotanda",
        "url": "https://www.liturgiapapal.org/attachments/article/755/Praenotanda.pdf",
        "required": "OBSERVACIONES GENERALES PREVIAS",
    },
    "funeral_preces": {
        "title": "Ritual de exequias — Preces para antes de las exequias",
        "url": "https://www.liturgiapapal.org/attachments/article/755/Preces%20para%20antes%20de%20las%20exequias.pdf",
        "required": "PRECES PARA ANTES DE LAS EXEQUIAS",
    },
    "funeral_typical": {
        "title": "Ritual de exequias — Forma típica con tres estaciones",
        "url": "https://www.liturgiapapal.org/attachments/article/755/Forma%20ti%CC%81pica%20de%20las%20exequias.pdf",
        "required": "EXEQUIAS",
    },
    "funeral_simplified": {
        "title": "Ritual de exequias — Rito simplificado",
        "url": "https://www.liturgiapapal.org/attachments/article/755/Rito%20simplificado%20de%20exequias.pdf",
        "required": "EXEQUIAS",
    },
    "funeral_ashes": {
        "title": "Ritual de exequias — Exequias ante la urna de las cenizas",
        "url": "https://www.liturgiapapal.org/attachments/article/755/Exequias%20ante%20cenizas.pdf",
        "required": "CENIZAS",
    },
}

PAGE_NUMBER = re.compile(r"^\s*\d{1,3}\s*$")
SITE_ONLY = re.compile(r"^\s*(?:www\.)?liturgiapapal\.org\s*$", re.I)
SEPARATOR = re.compile(r"^[\s_\-—–·.]{8,}$")
INLINE_SITE_HEADER = re.compile(
    r"(?i)(?:liturgiapapal\.org\s*\|?|\|\s*)"
    r"(?:RITUAL (?:DEL BAUTISMO DE NIÑOS|DE LA UNCIÓN Y DE LA PASTORAL DE ENFERMOS))?"
)


def download(url: str, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists() and target.stat().st_size > 1024:
        return
    request = urllib.request.Request(url, headers={
        "User-Agent": "Ministerium-Ritual-Builder/3.1",
        "Accept": "application/pdf,*/*;q=0.8",
    })
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    if not data.startswith(b"%PDF"):
        raise RuntimeError(f"La fuente no devolvió PDF: {url}")
    target.write_bytes(data)


def extract(pdf: Path) -> str:
    if PdfReader is None:
        raise RuntimeError("Falta pypdf")
    reader = PdfReader(str(pdf))
    return "\n".join((page.extract_text() or "") for page in reader.pages)


def clean(raw: str) -> str:
    raw = unicodedata.normalize("NFC", raw).replace("\u00a0", " ").replace("\ufeff", "")
    result: list[str] = []
    blank = True
    for raw_line in raw.splitlines():
        line = raw_line.strip()
        if SITE_ONLY.match(line) or PAGE_NUMBER.match(line) or SEPARATOR.match(line):
            line = ""
        line = INLINE_SITE_HEADER.sub("", line).strip(" |")
        line = re.sub(r"[ \t]+", " ", line).strip()
        if not line:
            if not blank:
                result.append("")
            blank = True
            continue
        result.append(line)
        blank = False
    while result and not result[0]: result.pop(0)
    while result and not result[-1]: result.pop()
    return "\n".join(result).strip() + "\n"


def normalized(value: str) -> str:
    return unicodedata.normalize("NFD", value).replace("\n", " ").lower().translate(
        str.maketrans("", "", "\u0300\u0301\u0302\u0303\u0304\u0305\u0306\u0307\u0308\u0309\u030a\u030b\u030c\u030d\u030e\u030f\u0310\u0311\u0312\u0313\u0314\u0315\u0316\u0317\u0318\u0319\u031a\u031b\u031c\u031d\u031e\u031f\u0320\u0321\u0322\u0323\u0324\u0325\u0326\u0327\u0328\u0329\u032a\u032b\u032c\u032d\u032e\u032f\u0330\u0331\u0332\u0333\u0334\u0335\u0336\u0337\u0338\u0339\u033a\u033b\u033c\u033d\u033e\u033f\u0340\u0341\u0342\u0343\u0344\u0345\u0346\u0347\u0348\u0349\u034a\u034b\u034c\u034d\u034e\u034f\u0350\u0351\u0352\u0353\u0354\u0355\u0356\u0357\u0358\u0359\u035a\u035b\u035c\u035d\u035e\u035f\u0360\u0361\u0362\u0363\u0364\u0365\u0366\u0367\u0368\u0369\u036a\u036b\u036c\u036d\u036e\u036f"))


def build() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    manifest = {"schema": 1, "provider": "Liturgia Papal", "documents": []}
    for key, source in SOURCES.items():
        print(f"[ritual] {key}")
        pdf = CACHE / f"{key}.pdf"
        download(source["url"], pdf)
        text = clean(extract(pdf))
        if len(text) < 200:
            raise RuntimeError(f"{key}: extracción demasiado corta")
        if normalized(source["required"]) not in normalized(text):
            raise RuntimeError(f"{key}: no aparece marcador esperado {source['required']}")
        target = OUTPUT / f"{key}.txt"
        target.write_text(text, encoding="utf-8", newline="\n")
        manifest["documents"].append({
            "id": key,
            "title": source["title"],
            "asset": f"rituals/liturgiapapal/{key}.txt",
            "source": source["url"],
        })
    (OUTPUT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8", newline="\n")
    print(f"OK: {OUTPUT}")


if __name__ == "__main__":
    try:
        build()
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
