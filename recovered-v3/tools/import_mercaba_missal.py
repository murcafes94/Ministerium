#!/usr/bin/env python3
"""Build a structured, offline Mercabá Missal package for Ministerium.

This importer intentionally keeps scraping/build-time concerns outside the APK.
It discovers the public Mercabá Misal index, downloads only linked Misal pages,
normalizes legacy encodings/HTML, and emits semantic JSON that Ministerium can
consume as a controlled fallback.

Source priority is NOT decided here. Runtime code must keep the Ecuador-approved
package authoritative and may use imported Mercabá content only when the entry
is explicitly enabled/verified.
"""

from __future__ import annotations

import argparse
import hashlib
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import time
from typing import Iterable
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen

INDEX_URL = "https://www.mercaba.org/LITURGIA/Misal/MISALE/cartel_misal_romano.htm"
USER_AGENT = "Ministerium-Missal-Importer/4.1 (+offline build tool)"

PART_ALIASES = {
    "antifona de entrada": "entrance",
    "antífona de entrada": "entrance",
    "oracion colecta": "collect",
    "oración colecta": "collect",
    "oracion sobre las ofrendas": "offerings",
    "oración sobre las ofrendas": "offerings",
    "antifona de la comunion": "communion_antiphon",
    "antífona de la comunión": "communion_antiphon",
    "antifona de comunion": "communion_antiphon",
    "antífona de comunión": "communion_antiphon",
    "oracion despues de la comunion": "post_communion",
    "oración después de la comunión": "post_communion",
    "oracion postcomunion": "post_communion",
    "oración postcomunión": "post_communion",
    "prefacio": "preface",
}

SECTION_LABELS = {
    "adviento": "temporal/advent",
    "cuaresma": "temporal/lent",
    "semana santa": "temporal/holy-week",
    "pascual": "temporal/easter",
    "ordinario": "temporal/ordinary",
    "solemnidades del señor": "temporal/lord-solemnities",
    "ordinario de la misa": "ordinary",
    "propio de los santos": "sanctoral",
    "comun de los santos": "commons",
    "común de los santos": "commons",
    "misas rituales": "ritual",
    "diversas": "needs",
    "votivas": "votive",
    "difuntos": "dead",
}


def fold(value: str) -> str:
    import unicodedata
    value = unicodedata.normalize("NFD", value or "")
    value = "".join(ch for ch in value if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", value).strip().lower()


class LinkCollector(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.links: list[tuple[str, str]] = []
        self._href: str | None = None
        self._text: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() == "a":
            self._href = dict(attrs).get("href")
            self._text = []

    def handle_data(self, data: str) -> None:
        if self._href is not None:
            self._text.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() == "a" and self._href:
            self.links.append((self._href, " ".join(self._text).strip()))
            self._href = None
            self._text = []


class TextExtractor(HTMLParser):
    BLOCKS = {"p", "div", "br", "li", "h1", "h2", "h3", "h4", "h5", "tr", "td"}
    SKIP = {"script", "style", "noscript"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.out: list[str] = []
        self.skip_depth = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        if tag in self.SKIP:
            self.skip_depth += 1
        elif not self.skip_depth and tag in self.BLOCKS:
            self.out.append("\n")

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in self.SKIP and self.skip_depth:
            self.skip_depth -= 1
        elif not self.skip_depth and tag in self.BLOCKS:
            self.out.append("\n")

    def handle_data(self, data: str) -> None:
        if not self.skip_depth:
            self.out.append(data)

    def text(self) -> str:
        value = html.unescape("".join(self.out)).replace("\xa0", " ")
        value = value.replace("\r", "")
        lines = [re.sub(r"[ \t]+", " ", line).strip() for line in value.split("\n")]
        compact: list[str] = []
        for line in lines:
            if not line:
                if compact and compact[-1] != "":
                    compact.append("")
                continue
            compact.append(line)
        return "\n".join(compact).strip()


def fetch(url: str, timeout: int = 30) -> tuple[str, str]:
    req = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "text/html,*/*;q=0.8"})
    with urlopen(req, timeout=timeout) as response:
        raw = response.read()
        declared = response.headers.get_content_charset()
    candidates = [declared, "utf-8", "windows-1252", "iso-8859-1"]
    for encoding in candidates:
        if not encoding:
            continue
        try:
            text = raw.decode(encoding)
            if "�" not in text:
                return text, encoding
        except (LookupError, UnicodeDecodeError):
            pass
    return raw.decode("windows-1252", errors="replace"), "windows-1252-replace"


def discover_pages(index_html: str) -> list[dict]:
    parser = LinkCollector()
    parser.feed(index_html)
    pages: list[dict] = []
    seen: set[str] = set()
    for href, label in parser.links:
        absolute = urljoin(INDEX_URL, href)
        parsed = urlparse(absolute)
        if parsed.netloc.lower() not in {"www.mercaba.org", "mercaba.org"}:
            continue
        if "/LITURGIA/Misal/MISALE/".lower() not in parsed.path.lower():
            continue
        if absolute == INDEX_URL or absolute in seen:
            continue
        seen.add(absolute)
        section = classify_section(label, parsed.path)
        pages.append({"url": absolute, "label": label or Path(parsed.path).stem, "section": section})
    return pages


def classify_section(label: str, path: str) -> str:
    probe = fold(label + " " + path.replace("_", " "))
    for needle, section in SECTION_LABELS.items():
        if fold(needle) in probe:
            return section
    return "reference"


def normalize_lines(text: str) -> list[str]:
    result: list[str] = []
    for raw in text.splitlines():
        line = re.sub(r"\s+", " ", raw).strip()
        if not line:
            if result and result[-1] != "":
                result.append("")
            continue
        # Ignore common navigation/noise without altering liturgical prose.
        if fold(line) in {"el misal romano", "mercaba", "volver", "inicio"}:
            continue
        result.append(line)
    while result and result[-1] == "":
        result.pop()
    return result


def is_part_heading(line: str) -> str | None:
    key = fold(line).strip(" .:-")
    for label, part in PART_ALIASES.items():
        if key == fold(label):
            return part
    return None


def looks_like_formulary_heading(line: str) -> bool:
    if len(line) < 4 or len(line) > 160:
        return False
    letters = [c for c in line if c.isalpha()]
    if len(letters) < 3:
        return False
    upper = sum(c.isupper() for c in letters)
    return upper / len(letters) >= 0.72


def join_broken_prose(lines: Iterable[str]) -> list[str]:
    """Join only obvious extraction line-wraps, never liturgical block boundaries."""
    out: list[str] = []
    for line in lines:
        if not line:
            if out and out[-1] != "":
                out.append("")
            continue
        if out and out[-1] and re.search(r"[,;:]$", out[-1]) and re.match(r"^[a-záéíóúüñ]", line):
            out[-1] += " " + line
        else:
            out.append(line)
    return out


def parse_page(text: str, page: dict) -> list[dict]:
    extractor = TextExtractor()
    extractor.feed(text)
    lines = join_broken_prose(normalize_lines(extractor.text()))
    entries: list[dict] = []
    current_title = page["label"]
    current_parts: dict[str, list[str]] = {}
    current_part: str | None = None

    def flush() -> None:
        nonlocal current_parts
        parts = {key: "\n\n".join(chunk for chunk in chunks if chunk).strip()
                 for key, chunks in current_parts.items()}
        parts = {k: v for k, v in parts.items() if v}
        if parts:
            entries.append({
                "id": stable_id(page["section"], current_title),
                "title": current_title,
                "section": page["section"],
                "sourceUrl": page["url"],
                "sourceEdition": "Mercabá · traducción castellana de Editio typica tertia (2002)",
                "verifiedForEcuador": False,
                "parts": parts,
            })
        current_parts = {}

    for line in lines:
        part = is_part_heading(line)
        if part:
            current_part = part
            current_parts.setdefault(part, [])
            continue
        if looks_like_formulary_heading(line) and not is_part_heading(line):
            # New formulary only after at least one recognized proper part was captured.
            if current_parts:
                flush()
            current_title = line.title()
            current_part = None
            continue
        if current_part:
            if line == "":
                if current_parts[current_part] and current_parts[current_part][-1] != "":
                    current_parts[current_part].append("")
            else:
                current_parts[current_part].append(line)
    flush()
    return entries


def stable_id(section: str, title: str) -> str:
    base = re.sub(r"[^a-z0-9]+", "-", fold(title)).strip("-")[:72] or "formulary"
    digest = hashlib.sha1((section + "|" + fold(title)).encode("utf-8")).hexdigest()[:8]
    return f"{section.replace('/', '-')}-{base}-{digest}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="app/src/main/assets/missal/mercaba/index.json")
    parser.add_argument("--delay", type=float, default=0.35, help="polite delay between requests")
    parser.add_argument("--limit", type=int, default=0, help="debug: import only N linked pages")
    parser.add_argument("--verify-id", action="append", default=[],
                        help="mark an imported formulary ID as manually verified for Ecuador")
    args = parser.parse_args()

    index_html, index_encoding = fetch(INDEX_URL)
    pages = discover_pages(index_html)
    if args.limit > 0:
        pages = pages[:args.limit]

    entries: list[dict] = []
    sources: list[dict] = []
    for pos, page in enumerate(pages, 1):
        try:
            body, encoding = fetch(page["url"])
            parsed = parse_page(body, page)
            entries.extend(parsed)
            sources.append({"url": page["url"], "encoding": encoding,
                            "section": page["section"], "entries": len(parsed)})
            print(f"[{pos}/{len(pages)}] {page['section']}: {len(parsed)} formularios")
        except Exception as exc:  # keep the build auditable rather than silently dropping failures
            sources.append({"url": page["url"], "section": page["section"], "error": str(exc)})
            print(f"[{pos}/{len(pages)}] ERROR {page['url']}: {exc}")
        if pos < len(pages) and args.delay:
            time.sleep(args.delay)

    verified = set(args.verify_id)
    for entry in entries:
        if entry["id"] in verified:
            entry["verifiedForEcuador"] = True

    package = {
        "schemaVersion": 1,
        "source": "Mercabá",
        "indexUrl": INDEX_URL,
        "indexEncoding": index_encoding,
        "authorityPolicy": "ecuador-first; mercaba-only-when-verified; never-silent-language-mixing",
        "entries": entries,
        "sources": sources,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(package, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Escritos {len(entries)} formularios en {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
