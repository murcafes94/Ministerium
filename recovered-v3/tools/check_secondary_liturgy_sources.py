#!/usr/bin/env python3
"""Best-effort external cross-checks for Ministerium liturgical metadata.

These sources never decide the app calendar and never make the Android build
fail. They only help detect obvious drift in external reference sites.
"""

from __future__ import annotations

import re
import unicodedata
import urllib.request

SOURCES = [
    ("Ciudad Redonda", "https://www.ciudadredonda.org/calendario/"),
    ("Liturgia Papal", "https://liturgiapapal.org/index.php/recursos-lit%C3%BArgicos/libros-lit%C3%BArgicos-2/1583-calendario-lit%C3%BArgico-2026-2027.html"),
]


def fold(value: str) -> str:
    value = "".join(c for c in unicodedata.normalize("NFD", value or "")
                    if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", value).lower()


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={
        "User-Agent": "Ministerium-Calendar-Crosscheck/3.1.1",
        "Accept": "text/html,*/*;q=0.8",
    })
    with urllib.request.urlopen(request, timeout=25) as response:
        return response.read().decode("utf-8", errors="replace")


def main() -> int:
    for name, url in SOURCES:
        try:
            content = fold(fetch(url))
            bart = "bartolome" in content and ("24 de agosto" in content or "24 agosto" in content)
            print(f"{name}: reachable; 24 Aug / San Bartolome cross-check={'OK' if bart else 'not found'}")
        except Exception as error:
            print(f"WARNING {name}: external cross-check unavailable: {error}")
    print("Secondary sources are informational only; local Ecuador calendar remains authoritative.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
