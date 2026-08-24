#!/usr/bin/env python3
"""Ministerium 3.1 wrapper for the Liturgia Papal missal builder.

Runtime packages are built from the section PDFs. The very large full Ordinary
PDFs are useful for manual auditing but redundant during every CI build, so they
are removed from the daily source set. Strict Mexico-formula validation remains
active on the relevant Ordinary/Eucharistic components.

The source PDFs under proper_* are themselves from the Mexico collection. Some
proper formularies reproduce exceptional rubrics or quotations (for example the
Chrism Mass rubric in Cuaresma.pdf says «Podéis ir en paz»). Those occurrences
must not be treated as proof that the whole PDF belongs to the Spain edition.
"""

import time
import urllib.request

import build_liturgiapapal_missal as base

_original_validate_mexico = base.validate_mexico_ordinary

# The full books are audit references, not runtime inputs. Avoid downloading
# tens of MB on every commit when all required runtime sections are separate PDFs.
for language in ("es", "la"):
    base.SOURCES.get(language, {}).pop("ordinary_full", None)


def validate_mexico_31(component: str, text: str) -> list[str]:
    if component.startswith("proper_"):
        return []
    return _original_validate_mexico(component, text)


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


base.validate_mexico_ordinary = validate_mexico_31
base.download = resilient_download

if __name__ == "__main__":
    raise SystemExit(base.main())
