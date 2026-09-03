#!/usr/bin/env python3
"""Best-effort external cross-checks for Ministerium liturgical metadata.

These sources never decide the app calendar and never make the Android build
fail. They only help detect obvious drift in external reference sites.

4.1 adds a structured comparison against LiturgicalCalendarAPI. The bundled
Ecuador calendar remains the runtime source; the API is only a diagnostic
reference during build/update work.
"""

from __future__ import annotations

import datetime as dt
import json
import pathlib
import re
import unicodedata
import urllib.request

YEAR = dt.date.today().year
LOCAL_ICS = pathlib.Path(f"app/src/main/assets/calendar/gcatholic-{YEAR}-es-EC.ics")
LITCAL_API = f"https://litcal.johnromanodorazio.com/api/v5/calendar/roman/{YEAR}?locale=es"

SOURCES = [
    ("Ciudad Redonda", "https://www.ciudadredonda.org/calendario/"),
    ("Liturgia Papal", "https://liturgiapapal.org/index.php/recursos-lit%C3%BArgicos/libros-lit%C3%BArgicos-2/1583-calendario-lit%C3%BArgico-2026-2027.html"),
]

CORE_EVENTS = [
    ("Bautismo del Señor", ("bautismo del senor", "baptism of the lord", "baptismlord")),
    ("Pascua", ("domingo de pascua", "easter sunday", "easter")),
    ("Pentecostés", ("pentecostes", "pentecost")),
    ("San Bartolomé", ("bartolome", "bartholomew", "stbartholomew")),
]


def fold(value: str) -> str:
    value = "".join(c for c in unicodedata.normalize("NFD", value or "")
                    if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", value).lower().strip()


def fetch(url: str, accept: str = "text/html,*/*;q=0.8") -> bytes:
    request = urllib.request.Request(url, headers={
        "User-Agent": "Ministerium-Calendar-Crosscheck/4.1",
        "Accept": accept,
    })
    with urllib.request.urlopen(request, timeout=25) as response:
        return response.read()


def unfolded_ics(path: pathlib.Path) -> list[str]:
    if not path.is_file():
        return []
    physical = path.read_text(encoding="utf-8", errors="replace").splitlines()
    logical: list[str] = []
    for line in physical:
        if line.startswith((" ", "\t")) and logical:
            logical[-1] += line[1:]
        else:
            logical.append(line)
    return logical


def local_events(path: pathlib.Path) -> list[tuple[str, str]]:
    events: list[tuple[str, str]] = []
    date = ""
    summary = ""
    inside = False
    for line in unfolded_ics(path):
        if line == "BEGIN:VEVENT":
            inside = True
            date = ""
            summary = ""
        elif line == "END:VEVENT":
            if inside and date and summary:
                events.append((date, summary.replace("\\,", ",")))
            inside = False
        elif inside and line.startswith("DTSTART"):
            raw = line.split(":", 1)[-1].strip()
            if re.fullmatch(r"\d{8}", raw):
                date = f"{raw[:4]}-{raw[4:6]}-{raw[6:8]}"
        elif inside and line.startswith("SUMMARY:"):
            summary = line.split(":", 1)[1].strip()
    return events


def date_value(value) -> str:
    if isinstance(value, (int, float)):
        try:
            return dt.datetime.fromtimestamp(float(value), tz=dt.timezone.utc).date().isoformat()
        except Exception:
            return ""
    text = str(value or "").strip()
    match = re.search(r"\b(20\d{2})-(\d{2})-(\d{2})\b", text)
    if match:
        return match.group(0)
    compact = re.search(r"\b(20\d{6})\b", text)
    if compact:
        raw = compact.group(1)
        return f"{raw[:4]}-{raw[4:6]}-{raw[6:8]}"
    if re.fullmatch(r"\d{10}(?:\.\d+)?", text):
        try:
            return dt.datetime.fromtimestamp(float(text), tz=dt.timezone.utc).date().isoformat()
        except Exception:
            return ""
    return ""


def api_records(node):
    if isinstance(node, dict):
        date = ""
        for key in ("date", "date_time", "datetime", "timestamp", "start"):
            if key in node:
                date = date_value(node.get(key))
                if date:
                    break
        if date:
            searchable = fold(" ".join(str(node.get(key, "")) for key in (
                "name", "title", "event_key", "key", "event", "liturgical_event"
            )))
            if not searchable:
                searchable = fold(json.dumps(node, ensure_ascii=False))
            yield date, searchable
        for child in node.values():
            yield from api_records(child)
    elif isinstance(node, list):
        for child in node:
            yield from api_records(child)


def find_event(records, tokens: tuple[str, ...]) -> str:
    wanted = tuple(fold(token) for token in tokens)
    for date, text in records:
        if any(token in text for token in wanted):
            return date
    return ""


def find_local(records: list[tuple[str, str]], tokens: tuple[str, ...]) -> str:
    wanted = tuple(fold(token) for token in tokens)
    for date, summary in records:
        text = fold(summary)
        if any(token in text for token in wanted):
            return date
    return ""


def check_structured_calendar() -> None:
    local = local_events(LOCAL_ICS)
    if not local:
        print(f"WARNING LiturgicalCalendarAPI: local Ecuador ICS for {YEAR} not found; structured comparison skipped")
        return
    try:
        payload = json.loads(fetch(LITCAL_API, "application/json").decode("utf-8", errors="replace"))
        remote = list(api_records(payload))
        if not remote:
            print("WARNING LiturgicalCalendarAPI: response reachable but no dated event records were recognized")
            return
        print(f"LiturgicalCalendarAPI: reachable; {len(remote)} dated records recognized")
        compared = 0
        mismatches = 0
        for label, tokens in CORE_EVENTS:
            local_date = find_local(local, tokens)
            api_date = find_event(remote, tokens)
            if not local_date or not api_date:
                print(f"  INFO {label}: local={local_date or 'not found'} api={api_date or 'not found'}")
                continue
            compared += 1
            if local_date == api_date:
                print(f"  OK {label}: {local_date}")
            else:
                mismatches += 1
                print(f"  WARNING {label}: Ecuador local={local_date} API general={api_date}")
        print(f"LiturgicalCalendarAPI cross-check: {compared} comparable events; {mismatches} differences")
    except Exception as error:
        print(f"WARNING LiturgicalCalendarAPI: structured cross-check unavailable: {error}")


def main() -> int:
    for name, url in SOURCES:
        try:
            content = fold(fetch(url).decode("utf-8", errors="replace"))
            bart = "bartolome" in content and ("24 de agosto" in content or "24 agosto" in content)
            print(f"{name}: reachable; 24 Aug / San Bartolome cross-check={'OK' if bart else 'not found'}")
        except Exception as error:
            print(f"WARNING {name}: external cross-check unavailable: {error}")

    check_structured_calendar()
    print("Secondary sources are informational only; local Ecuador calendar remains authoritative.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
