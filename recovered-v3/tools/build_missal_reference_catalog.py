#!/usr/bin/env python3
"""Enrich Ministerium's metadata-only Missal reference catalog.

Curas.com.ar is used ONLY to infer structure (date, rank, common, Gloria,
Credo, preface and solemn blessing). Prayer text is deliberately discarded.
Liturgia Papal/Ecuador remain authoritative for runtime liturgical decisions.

Network failure is non-fatal: the committed seed catalog remains usable.
"""

from __future__ import annotations

import json
import re
import unicodedata
import urllib.request
from pathlib import Path

from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app" / "src" / "main" / "assets" / "missal-reference-catalog.json"
BASE = "https://www.curas.com.ar/Misal3/Misas3/Msantos3.{:02d}.htm"
MONTH_NAMES = {
    1: "enero", 2: "febrero", 3: "marzo", 4: "abril", 5: "mayo", 6: "junio",
    7: "julio", 8: "agosto", 9: "septiembre", 10: "octubre", 11: "noviembre", 12: "diciembre",
}
DATE_RE = re.compile(r"^(\d{1,2})\s+de\s+([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)$", re.I)
RANKS = {
    "solemnidad": "Solemnidad",
    "fiesta": "Fiesta",
    "memoria obligatoria": "Memoria obligatoria",
    "memoria libre": "Memoria libre",
    "memoria": "Memoria",
}
TITLE_START = re.compile(
    r"^(?:SAN(?:TA|TOS|TAS)?\b|BEAT[OA]S?\b|BIENAVENTURAD[OA]\b|LA\s+|EL\s+|"
    r"NACIMIENTO\b|MARTIRIO\b|CONVERSI[ÓO]N\b|PRESENTACI[ÓO]N\b|VISITACI[ÓO]N\b|"
    r"TRANSFIGURACI[ÓO]N\b|EXALTACI[ÓO]N\b|DEDICACI[ÓO]N\b|TODOS LOS SANTOS\b)", re.I)
DESCRIPTOR_WORDS = (
    "apóstol", "apóstoles", "evangelista", "mártir", "mártires", "papa", "obispo",
    "obispos", "presbítero", "presbíteros", "diácono", "virgen", "religiosa", "religioso",
    "abad", "abadesa", "doctor de la iglesia", "doctores de la iglesia", "monje", "monja",
)


def fold(value: str) -> str:
    return "".join(
        ch for ch in unicodedata.normalize("NFD", value or "")
        if unicodedata.category(ch) != "Mn"
    ).lower().strip()


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={
        "User-Agent": "Ministerium-Missal-Reference/3.1.1 (+metadata-only)",
        "Accept": "text/html,*/*;q=0.8",
    })
    with urllib.request.urlopen(req, timeout=35) as response:
        return response.read().decode("utf-8", errors="replace")


def exact_rank(line: str) -> str | None:
    key = fold(line)
    return RANKS.get(key)


def looks_descriptor(line: str) -> bool:
    value = fold(line).strip(" ,.;")
    if not value or len(value) > 80:
        return False
    return any(value == fold(word) or value.endswith(" y " + fold(word)) for word in DESCRIPTOR_WORDS)


def title_before(lines: list[str], rank_index: int) -> tuple[str, str]:
    descriptor: list[str] = []
    for i in range(rank_index - 1, max(-1, rank_index - 6), -1):
        line = lines[i].strip()
        if DATE_RE.match(line):
            break
        if looks_descriptor(line):
            descriptor.insert(0, line)
            continue
        if TITLE_START.match(line):
            return line, " · ".join(descriptor)
    return "", " · ".join(descriptor)


def common_ids(segment: list[str]) -> list[str]:
    text = " ".join(fold(x) for x in segment if "comun" in fold(x))
    result: list[str] = []
    mapping = [
        ("apostol", "Apóstoles"), ("martir", "Mártires"), ("pastor", "Pastores"),
        ("papa", "Pastores"), ("obispo", "Pastores"), ("doctor", "Doctores de la Iglesia"),
        ("virgen", "Vírgenes"), ("religios", "Santos y santas"), ("monj", "Santos y santas"),
        ("misericordia", "Santos y santas"), ("educador", "Santos y santas"),
        ("santo", "Santos y santas"), ("santa maria", "Bienaventurada Virgen María"),
    ]
    for needle, label in mapping:
        if needle in text and label not in result:
            result.append(label)
    return result


def parse_month(month: int) -> list[dict]:
    html = fetch(BASE.format(month))
    soup = BeautifulSoup(html, "html.parser")
    lines = [re.sub(r"\s+", " ", x).strip() for x in soup.stripped_strings]
    lines = [x for x in lines if x]
    current_day = None
    day_for_index: dict[int, int] = {}
    for i, line in enumerate(lines):
        match = DATE_RE.match(line)
        if match and fold(match.group(2)) == MONTH_NAMES[month]:
            current_day = int(match.group(1))
        if current_day is not None:
            day_for_index[i] = current_day

    rank_indexes = [(i, exact_rank(line)) for i, line in enumerate(lines)]
    rank_indexes = [(i, rank) for i, rank in rank_indexes if rank]
    entries: list[dict] = []
    for pos, (idx, rank) in enumerate(rank_indexes):
        day = day_for_index.get(idx)
        if not day:
            continue
        title, descriptor = title_before(lines, idx)
        if not title:
            continue
        end = rank_indexes[pos + 1][0] if pos + 1 < len(rank_indexes) else len(lines)
        # Do not read past the next date heading.
        for j in range(idx + 1, min(end, len(lines))):
            if DATE_RE.match(lines[j]):
                end = j
                break
        segment = lines[idx + 1:end]
        folded = [fold(x) for x in segment]
        preface = ""
        for line in segment:
            if fold(line).startswith("prefacio"):
                # Keep only the short structural label, never the prayer text.
                preface = re.sub(r"\s+", " ", line).strip().rstrip(" .")
                if len(preface) > 120:
                    preface = "Prefacio indicado por el formulario"
                break
        entry = {
            "month": month,
            "day": day,
            "title": title.title() if title.isupper() else title,
            "descriptor": descriptor,
            "rank": rank,
            "gloria": any("se dice gloria" in x for x in folded),
            "creed": any("se dice credo" in x for x in folded),
            "preface": preface,
            "solemnBlessing": "indicada" if any("bendicion solemne" in x for x in folded) else "",
            "commons": common_ids(segment),
            "structuralSource": BASE.format(month),
        }
        entries.append(entry)
    return entries


def dedupe(entries: list[dict]) -> list[dict]:
    found: dict[tuple[int, int, str], dict] = {}
    for item in entries:
        key = (int(item.get("month", 0)), int(item.get("day", 0)), fold(item.get("title", "")))
        if not key[2]:
            continue
        old = found.get(key)
        if old is None or len(json.dumps(item, ensure_ascii=False)) > len(json.dumps(old, ensure_ascii=False)):
            found[key] = item
    return sorted(found.values(), key=lambda x: (x.get("month", 0), x.get("day", 0), fold(x.get("title", ""))))


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    seed = list(catalog.get("saints", []))
    enriched: list[dict] = []
    failures: list[str] = []
    for month in range(1, 13):
        try:
            parsed = parse_month(month)
            enriched.extend(parsed)
            print(f"Curas metadata month {month:02d}: {len(parsed)} entries")
        except Exception as error:
            failures.append(f"{month:02d}: {error}")
            print(f"WARNING month {month:02d}: {error}")

    combined = dedupe(seed + enriched)
    catalog["saints"] = combined
    catalog["metadataRefresh"] = {
        "source": "https://www.curas.com.ar/Misal3/Misas3/Msantos3.htm",
        "mode": "metadata-only",
        "entries": len(combined),
        "networkFailures": failures,
    }
    CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    bart = [x for x in combined if x.get("month") == 8 and x.get("day") == 24
            and "bartolome" in fold(x.get("title", ""))]
    if not bart:
        raise SystemExit("Reference catalog lost the 24 Aug San Bartolome seed")
    print(f"OK: metadata-only catalog with {len(combined)} saint entries")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
