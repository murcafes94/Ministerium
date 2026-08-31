#!/usr/bin/env python3
"""Cross-check Ministerium temporal calendar arithmetic against LiturgicalCalendarAPI.

Developer/build check only: Ministerium never depends on the external API at
runtime. Ecuadorian proper celebrations remain local. Temporary API outages are
reported distinctly so they cannot masquerade as liturgical conflicts.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Dict, List

API_BASE = "https://litcal.johnromanodorazio.com:443/api/v5/calendar"


class ExternalCalendarUnavailable(RuntimeError):
    pass


def easter_sunday(year: int) -> dt.date:
    a = year % 19; b = year // 100; c = year % 100; d = b // 4; e = b % 4
    f = (b + 8) // 25; g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30; i = c // 4; k = c % 4
    l = (32 + 2 * e + 2 * i - h - k) % 7; m = (a + 11 * h + 22 * l) // 451
    month = (h + l - 7 * m + 114) // 31
    day = (h + l - 7 * m + 114) % 31 + 1
    return dt.date(year, month, day)


def advent_start(year: int) -> dt.date:
    date = dt.date(year, 12, 3)
    while date.weekday() != 6:
        date -= dt.timedelta(days=1)
    return date


def epiphany_sunday(year: int) -> dt.date:
    date = dt.date(year, 1, 2)
    while date.weekday() != 6:
        date += dt.timedelta(days=1)
    return date


def baptism_transferred_epiphany(year: int) -> dt.date:
    epiphany = epiphany_sunday(year)
    return epiphany + dt.timedelta(days=1 if epiphany.day in (7, 8) else 7)


def expected_temporal(year: int, transferred_epiphany: bool) -> Dict[str, dt.date]:
    easter = easter_sunday(year)
    advent = advent_start(year)
    baptism = baptism_transferred_epiphany(year) if transferred_epiphany else dt.date(year, 1, 13)
    expected: Dict[str, dt.date] = {
        "AshWednesday": easter - dt.timedelta(days=46),
        "Easter": easter,
        "Pentecost": easter + dt.timedelta(days=49),
        "ChristKing": advent - dt.timedelta(days=7),
        "BaptismLord": baptism,
    }
    first_ord_sunday = baptism
    while first_ord_sunday.weekday() != 6:
        first_ord_sunday += dt.timedelta(days=1)
    if first_ord_sunday == baptism:
        first_ord_sunday += dt.timedelta(days=7)
    ash = expected["AshWednesday"]
    n, date = 2, first_ord_sunday
    while date < ash and n <= 34:
        expected[f"OrdSunday{n}"] = date
        n += 1; date += dt.timedelta(days=7)
    christ_king = expected["ChristKing"]
    for week in range(34, 1, -1):
        date = christ_king - dt.timedelta(days=(34 - week) * 7)
        if date > expected["Pentecost"]:
            expected[f"OrdSunday{week}"] = date
    return expected


def fetch_litcal(year: int, epiphany: str, ascension: str, corpus: str, locale: str) -> Dict[str, dt.date]:
    params = urllib.parse.urlencode({
        "return_type": "JSON", "locale": locale, "epiphany": epiphany,
        "ascension": ascension, "corpus_christi": corpus, "year_type": "CIVIL",
    })
    url = f"{API_BASE}/{year}?{params}"
    request = urllib.request.Request(url, headers={
        "Accept": "application/json",
        "User-Agent": "Ministerium-calendar-validator/3.1",
    })
    last_error = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                data = json.load(response)
            break
        except urllib.error.HTTPError as error:
            last_error = error
            if error.code < 500 and error.code != 429:
                raise
        except urllib.error.URLError as error:
            last_error = error
        if attempt < 2:
            time.sleep(2 ** attempt)
    else:
        raise ExternalCalendarUnavailable(
            f"LitCal unavailable for {year} after 3 attempts: {last_error}"
        )

    result: Dict[str, dt.date] = {}
    for event in data.get("litcal", []):
        key = event.get("event_key") or event.get("eventKey") or event.get("key")
        raw_date = event.get("date")
        if not key or not raw_date:
            continue
        try:
            result[key] = dt.date.fromisoformat(str(raw_date)[:10])
        except ValueError:
            continue
    return result


def compare_year(year: int, remote: Dict[str, dt.date], transferred_epiphany: bool) -> List[str]:
    failures: List[str] = []
    for key, local_date in sorted(expected_temporal(year, transferred_epiphany).items()):
        remote_date = remote.get(key)
        if remote_date is None:
            if not key.startswith("OrdSunday"):
                failures.append(f"{year} {key}: missing in LitCal response")
            continue
        if remote_date != local_date:
            failures.append(f"{year} {key}: Ministerium={local_date.isoformat()} LitCal={remote_date.isoformat()}")
    return failures


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", type=int, default=2020)
    parser.add_argument("--end", type=int, default=2040)
    parser.add_argument("--locale", default="es_ES")
    parser.add_argument("--epiphany", choices=["JAN6", "SUNDAY_JAN2_JAN8"], default="SUNDAY_JAN2_JAN8")
    parser.add_argument("--ascension", choices=["THURSDAY", "SUNDAY"], default="THURSDAY")
    parser.add_argument("--corpus", choices=["THURSDAY", "SUNDAY"], default="THURSDAY")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.start < 1970 or args.end < args.start:
        raise SystemExit("Invalid validation year range")
    failures: List[str] = []
    try:
        for year in range(args.start, args.end + 1):
            remote = fetch_litcal(year, args.epiphany, args.ascension, args.corpus, args.locale)
            failures.extend(compare_year(year, remote, args.epiphany == "SUNDAY_JAN2_JAN8"))
            print(f"{year}: checked {len(remote)} LitCal events")
    except ExternalCalendarUnavailable as error:
        print(f"External validation deferred: {error}", file=sys.stderr)
        return 75
    if failures:
        print("\nCalendar conflicts:", file=sys.stderr)
        for failure in failures:
            print(" - " + failure, file=sys.stderr)
        return 1
    print(f"LitCal temporal validation OK for {args.start}-{args.end}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
