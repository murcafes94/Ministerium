#!/usr/bin/env python3
"""Ministerium 3.1 Liturgia Papal package builder.

The complete Ordinary PDFs remain recorded as canonical audit references, but
CI builds runtime assets only from the smaller section PDFs. This avoids making
an app build depend on repeatedly downloading very large PDFs.
"""

import json
import time
import unicodedata
import urllib.request

import build_liturgiapapal_missal as base

_original_validate_mexico = base.validate_mexico_ordinary
_original_validate_cleaned = base.validate_cleaned
CANONICAL_ES = base.SOURCES["es"]["ordinary_full"]
CANONICAL_LA = base.SOURCES["la"]["ordinary_full"]


def validate_mexico_31(component: str, text: str) -> list[str]:
    # Proper PDFs can legitimately quote exceptional formulas; provenance is
    # pinned by URL. Dialect checks remain strict on Ordinary/PE components.
    if component.startswith("proper_"):
        return []
    return _original_validate_mexico(component, text)


def fold_diacritics(value: str) -> str:
    return "".join(
        ch for ch in unicodedata.normalize("NFD", value or "")
        if unicodedata.category(ch) != "Mn"
    )


def validate_cleaned_31(language: str, component: str, text: str) -> list[str]:
    problems = _original_validate_cleaned(language, component, text)
    if language == "la" and component in {"ordinary_full", "initial"}:
        # Liturgia Papal preserves Latin stress marks, e.g. «In nómine Patris».
        # The base validator historically searched the unaccented spelling.
        problems = [
            problem for problem in problems
            if problem != "no se encontró el comienzo del Ordo latino"
        ]
        if "in nomine patris" not in fold_diacritics(text).lower():
            problems.append("no se encontró el comienzo del Ordo latino")
    return problems


def resilient_download(url, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists() and destination.stat().st_size > 1024:
        return
    last_error = None
    for attempt in range(1, 4):
        try:
            request = urllib.request.Request(url, headers={
                "User-Agent": "Ministerium-Missal-Builder/3.1 (+GitHub Actions)",
                "Accept": "application/pdf,*/*;q=0.8",
            })
            with urllib.request.urlopen(request, timeout=180) as response:
                data = response.read()
            if not data.startswith(b"%PDF"):
                raise RuntimeError(f"La URL no devolvió un PDF válido: {url}")
            destination.write_bytes(data)
            return
        except Exception as error:
            last_error = error
            if attempt < 3:
                time.sleep(3 * attempt)
    raise last_error


def build31(output, languages, force):
    output.mkdir(parents=True, exist_ok=True)
    manifest = {
        "schema": 5,
        "provider": "Liturgia Papal",
        "editions": {
            "es": "Misal Romano - versión de México",
            "la": "Missale Romanum",
        },
        "canonical_es_ordinary": CANONICAL_ES,
        "canonical_la_ordinary": CANONICAL_LA,
        "notes": "PDF completos conservados como referencias de auditoría; assets runtime generados desde PDFs por secciones. Español: versión de México.",
        "languages": {},
    }

    for language in languages:
        lang_dir = output / language
        lang_dir.mkdir(parents=True, exist_ok=True)
        generated = []
        for component, url in base.SOURCES[language].items():
            if component == "ordinary_full":
                continue
            pdf = base.CACHE / language / f"{component}.pdf"
            destination = lang_dir / f"{component}.txt"
            if force and pdf.exists():
                pdf.unlink()
            print(f"[{language}] {component}")
            resilient_download(url, pdf)
            cleaned = base.clean_text(base.pdf_text(pdf))
            problems = base.validate_cleaned(language, component, cleaned)
            if problems:
                raise RuntimeError(f"{language}/{component}: " + "; ".join(problems))
            destination.write_text(cleaned, encoding="utf-8", newline="\n")
            generated.append({
                "id": component,
                "asset": f"missal/{language}/{component}.txt",
                "source": url,
            })
        manifest["languages"][language] = generated

    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8", newline="\n")
    print(f"OK: {output}")


base.validate_mexico_ordinary = validate_mexico_31
base.validate_cleaned = validate_cleaned_31
base.download = resilient_download
base.build = build31

if __name__ == "__main__":
    raise SystemExit(base.main())
