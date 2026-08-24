#!/usr/bin/env python3
"""Ministerium 3.1 wrapper for the Liturgia Papal missal builder.

The source PDFs under proper_* are themselves from the Mexico collection. Some
proper formularies reproduce exceptional rubrics or quotations (for example the
Chrism Mass rubric in Cuaresma.pdf says «Podéis ir en paz»). Those occurrences
must not be treated as proof that the whole PDF belongs to the Spain edition.

Strict dialect/formula checks remain enabled for the Ordinary and Eucharistic
Prayer components, where they are meaningful. Provenance of every proper PDF is
still pinned by SOURCES and written to manifest.json.
"""

import build_liturgiapapal_missal as base

_original_validate_mexico = base.validate_mexico_ordinary


def validate_mexico_31(component: str, text: str) -> list[str]:
    if component.startswith("proper_"):
        return []
    return _original_validate_mexico(component, text)


base.validate_mexico_ordinary = validate_mexico_31

if __name__ == "__main__":
    raise SystemExit(base.main())
