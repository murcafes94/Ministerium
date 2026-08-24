#!/usr/bin/env python3
"""Preprocesa los PDFs del Misal de Liturgia Papal para Ministerium.

Objetivo:
    PDF fuente -> texto limpio -> bloques compactos en assets/missal/.

Para español se usa deliberadamente la VERSIÓN DE MÉXICO publicada por
Liturgia Papal. Esta es la base latinoamericana de Ministerium; no debe
sustituirse por la edición de España mediante reemplazos mecánicos.

El Ordinario completo de México se descarga también como fuente canónica de
control. Los archivos parciales del mismo artículo se usan para producir bloques
más pequeños, pero deben concordar con ese Ordinario completo.

NO conserva:
- números de página;
- encabezados/pies repetidos;
- marcas del sitio;
- líneas separadoras de maquetación;
- numeración editorial de párrafos (1., 2., 21., etc.).

SÍ conserva:
- títulos litúrgicos útiles;
- rúbricas necesarias;
- texto pronunciado por ministro/pueblo;
- alternativas legítimas ("O bien");
- referencias bíblicas de antífonas;
- signos ℣/℟ y cruces cuando aparecen en el PDF.

El script descarga las fuentes en una caché local ignorada por git. Los PDFs no
se incorporan al APK. La salida está pensada para ser consumida por un repositorio
semántico, no para mostrar el PDF en un WebView.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

try:
    from pypdf import PdfReader
except ImportError:  # pragma: no cover - mensaje para ejecución local
    PdfReader = None


ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "tools" / "cache" / "missal-pdf"
DEFAULT_OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "missal"

SOURCES = {
    "es": {
        # Liturgia Papal: Misal Romano, VERSIÓN DE MÉXICO (article/1030).
        # Fuente canónica de control para todo el Ordinario.
        "ordinary_full": "https://liturgiapapal.org/attachments/article/1030/Ordinario%20de%20la%20Misa%20Me%CC%81xico.pdf",
        # Bloques del mismo Ordinario, más cómodos para el runtime semántico.
        "initial": "https://liturgiapapal.org/attachments/article/1030/Iniciales.pdf",
        "word": "https://liturgiapapal.org/attachments/article/1030/Palabra.pdf",
        "eucharistic_liturgy": "https://liturgiapapal.org/attachments/article/1030/LiturgiaEucaristica.pdf",
        "prefaces": "https://liturgiapapal.org/attachments/article/1030/Prefacios.pdf",
        "eucharistic_prayer_1": "https://liturgiapapal.org/attachments/article/1030/PEI.pdf",
        "eucharistic_prayer_2": "https://liturgiapapal.org/attachments/article/1030/PEII.pdf",
        "eucharistic_prayer_3": "https://liturgiapapal.org/attachments/article/1030/PEIII.pdf",
        "eucharistic_prayer_4": "https://liturgiapapal.org/attachments/article/1030/PEIV.pdf",
        "communion": "https://liturgiapapal.org/attachments/article/1030/Comunion.pdf",
        "conclusion": "https://liturgiapapal.org/attachments/article/1030/Conclusion.pdf",
        # Propio del tiempo: URLs canónicas facilitadas/verificadas para México.
        "proper_advent": "https://liturgiapapal.org/attachments/article/1030/Adviento.pdf",
        "proper_christmas": "https://liturgiapapal.org/attachments/article/1030/Navidad.pdf",
        "proper_lent": "https://liturgiapapal.org/attachments/article/1030/Cuaresma.pdf",
        "proper_triduum": "https://liturgiapapal.org/attachments/article/1030/Triduo.pdf",
        "proper_easter": "https://liturgiapapal.org/attachments/article/1030/Pascua.pdf",
        "proper_ordinary": "https://liturgiapapal.org/attachments/article/1030/Ordinario.pdf",
    },
    "la": {
        "initial": "https://liturgiapapal.org/attachments/article/744/Ritus%20initiales.pdf",
        "word": "https://liturgiapapal.org/attachments/article/744/Liturgia%20verbi.pdf",
        "eucharistic_liturgy": "https://liturgiapapal.org/attachments/article/744/Liturgia%20eucharistica.pdf",
        "prefaces": "https://liturgiapapal.org/attachments/article/744/Praefatios.pdf",
        "eucharistic_prayer_1": "https://liturgiapapal.org/attachments/article/744/Prex%20I.pdf",
        "eucharistic_prayer_2": "https://liturgiapapal.org/attachments/article/744/Prex%20II.pdf",
        "eucharistic_prayer_3": "https://liturgiapapal.org/attachments/article/744/Prex%20III.pdf",
        "eucharistic_prayer_4": "https://liturgiapapal.org/attachments/article/744/Prex%20IV.pdf",
        "communion": "https://liturgiapapal.org/attachments/article/744/Communionis.pdf",
        "conclusion": "https://liturgiapapal.org/attachments/article/744/Conclusionis.pdf",
    },
}

# Encabezados/pies que se pegan al final o principio de una línea al extraer PDF.
PAGE_LABELS = [
    "ORDINARIO DE LA MISA",
    "RITOS INICIALES",
    "LITURGIA DE LA PALABRA",
    "LITURGIA EUCARÍSTICA",
    "RITO DE LA COMUNIÓN",
    "RITO DE CONCLUSIÓN",
    "ORDO MISSÆ",
    "RITUS INITIALES",
    "LITURGIA VERBI",
    "LITURGIA EUCHARISTICA",
    "RITUS COMMUNIONIS",
    "RITUS CONCLUSIONIS",
]

PAGE_LABEL_ALT = "|".join(re.escape(x) for x in PAGE_LABELS)
INLINE_HEADER_RE = re.compile(
    rf"(?:\b\d+\s+(?:{PAGE_LABEL_ALT})\b|\b(?:{PAGE_LABEL_ALT})\s+\d+\b)",
    re.IGNORECASE,
)
ONLY_HEADER_RE = re.compile(
    rf"^(?:\d+\s+)?(?:{PAGE_LABEL_ALT})(?:\s+\d+)?$", re.IGNORECASE
)
PAGE_ONLY_RE = re.compile(r"^\s*\d{1,3}\s*$")
EDITORIAL_NUMBER_RE = re.compile(r"^\s*\d{1,3}\.\s+(?=[A-ZÁÉÍÓÚÜÑ])")
SEPARATOR_RE = re.compile(r"^[\s_\-—–·.]{8,}$")
SITE_RE = re.compile(r"^\s*(?:www\.)?liturgiapapal\.org\s*$", re.IGNORECASE)

# Pies del Propio del Tiempo que suelen salir unidos a una línea de guiones bajos.
PROPER_FOOTER_RE = re.compile(
    r"^\s*(?:Adviento|Navidad|Cuaresma|Pascua|Propio del tiempo|Tiempo ordinario)\s*[_\-—–]{5,}\s*$",
    re.IGNORECASE,
)


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists() and destination.stat().st_size > 1024:
        return
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Ministerium-Missal-Builder/3.1 (+local preprocessing)",
            "Accept": "application/pdf,*/*;q=0.8",
        },
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if not data.startswith(b"%PDF"):
        raise RuntimeError(f"La URL no devolvió un PDF válido: {url}")
    destination.write_bytes(data)


def pdf_text(path: Path) -> str:
    if PdfReader is None:
        raise RuntimeError(
            "Falta pypdf. Instálalo una vez con:  py -3.11 -m pip install pypdf"
        )
    reader = PdfReader(str(path))
    pages: list[str] = []
    for page in reader.pages:
        value = page.extract_text() or ""
        pages.append(value)
    return "\n".join(pages)


def normalize_chars(value: str) -> str:
    value = unicodedata.normalize("NFC", value)
    return (
        value.replace("\u00a0", " ")
        .replace("\ufeff", "")
        .replace("ﬁ", "fi")
        .replace("ﬂ", "fl")
    )


def clean_line(line: str) -> str:
    line = normalize_chars(line).strip()
    if not line:
        return ""
    if SITE_RE.match(line) or PAGE_ONLY_RE.match(line) or SEPARATOR_RE.match(line):
        return ""
    if ONLY_HEADER_RE.match(line) or PROPER_FOOTER_RE.match(line):
        return ""

    line = INLINE_HEADER_RE.sub("", line)
    line = re.sub(
        r"(?:Adviento|Navidad|Cuaresma|Pascua|Propio del tiempo|Tiempo ordinario)\s*[_\-—–]{5,}.*$",
        "",
        line,
        flags=re.IGNORECASE,
    ).strip()
    line = EDITORIAL_NUMBER_RE.sub("", line)
    line = re.sub(r"[ \t]+", " ", line).strip()
    return line


def clean_text(raw: str) -> str:
    cleaned: list[str] = []
    previous_blank = True
    for raw_line in raw.splitlines():
        line = clean_line(raw_line)
        if not line:
            if not previous_blank:
                cleaned.append("")
            previous_blank = True
            continue
        cleaned.append(line)
        previous_blank = False

    while cleaned and not cleaned[0]:
        cleaned.pop(0)
    while cleaned and not cleaned[-1]:
        cleaned.pop()
    return "\n".join(cleaned).strip() + "\n"


def validate_mexico_ordinary(component: str, text: str) -> list[str]:
    """Impide que una fuente española de España vuelva a contaminar el paquete ES."""
    errors: list[str] = []
    normalized = unicodedata.normalize("NFC", text)
    lower = normalized.lower()

    # Señales positivas de la edición mexicana.
    if component in {"ordinary_full", "initial"} and "el señor esté con ustedes" not in lower:
        errors.append("no aparece la fórmula mexicana «El Señor esté con ustedes»")
    if component in {"ordinary_full", "eucharistic_prayer_2"}:
        if "por ustedes" not in lower:
            errors.append("no aparece «por ustedes» en la Plegaria II mexicana")

    # Señales negativas inequívocas de la edición española en estos bloques.
    forbidden = (
        "el señor esté con vosotros",
        "tomad y comed",
        "tomad y bebed",
        "será entregado por vosotros",
        "será derramada por vosotros",
        "podéis ir en paz",
    )
    for phrase in forbidden:
        if phrase in lower:
            errors.append(f"se detectó fórmula de España: «{phrase}»")
    return errors


def validate_cleaned(language: str, component: str, text: str) -> list[str]:
    errors: list[str] = []
    if len(text.strip()) < 40:
        errors.append("salida demasiado corta")
    if re.search(r"(?m)^\s*\d{1,3}\s*$", text):
        errors.append("quedó un número de página aislado")
    if re.search(r"(?im)^\s*(?:www\.)?liturgiapapal\.org\s*$", text):
        errors.append("quedó el pie liturgiapapal.org")
    if re.search(rf"(?im)^(?:\d+\s+)?(?:{PAGE_LABEL_ALT})\s+\d+\s*$", text):
        errors.append("quedó un encabezado con número de página")
    if language == "es":
        errors.extend(validate_mexico_ordinary(component, text))
        if component == "initial" and "En el nombre del Padre" not in text:
            errors.append("no se encontró el comienzo del Ordinario español")
    if language == "la" and component == "initial" and "In nomine Patris" not in text:
        errors.append("no se encontró el comienzo del Ordo latino")
    return errors


def build(output: Path, languages: list[str], force: bool) -> None:
    output.mkdir(parents=True, exist_ok=True)
    manifest = {
        "schema": 3,
        "provider": "Liturgia Papal",
        "editions": {
            "es": "Misal Romano - versión de México",
            "la": "Missale Romanum",
        },
        "canonical_es_ordinary": SOURCES["es"]["ordinary_full"],
        "notes": "PDFs preprocesados: sin paginación, encabezados ni pies; solo contenido litúrgico útil. Español basado exclusivamente en la versión de México.",
        "languages": {},
    }

    for language in languages:
        lang_dir = output / language
        lang_dir.mkdir(parents=True, exist_ok=True)
        generated = []
        for component, url in SOURCES[language].items():
            pdf = CACHE / language / f"{component}.pdf"
            destination = lang_dir / f"{component}.txt"
            if force and pdf.exists():
                pdf.unlink()
            print(f"[{language}] {component}")
            download(url, pdf)
            cleaned = clean_text(pdf_text(pdf))
            problems = validate_cleaned(language, component, cleaned)
            if problems:
                raise RuntimeError(
                    f"{language}/{component}: " + "; ".join(problems)
                )
            destination.write_text(cleaned, encoding="utf-8", newline="\n")
            generated.append(
                {
                    "id": component,
                    "asset": f"missal/{language}/{component}.txt",
                    "source": url,
                }
            )
        manifest["languages"][language] = generated

    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"OK: {output}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--language",
        choices=("es", "la", "all"),
        default="all",
        help="Fuente a generar (por defecto ambas).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Carpeta destino de assets/missal.",
    )
    parser.add_argument(
        "--force-download",
        action="store_true",
        help="Vuelve a descargar los PDF aunque estén en caché.",
    )
    args = parser.parse_args()
    languages = ["es", "la"] if args.language == "all" else [args.language]
    try:
        build(args.output, languages, args.force_download)
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
