#!/usr/bin/env python3
import hashlib
import json
import re
import shutil
import zipfile
from pathlib import Path

from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
SOURCE = ASSETS / "epubs" / "Liturgia-horarum-2026-latin.epub"
OUT = ASSETS / "hours-clean" / "latin" / "2026"
HTML_EXT = {".htm", ".html", ".xhtml"}

# Keep the Latin build-time cleaning contract equivalent to the Spanish one:
# source EPUB is only an input; runtime receives clean HTML without EPUB UI.
NAV_LABELS = {"1V", "2V", "V", "C", "C1", "C2", "IN", "O", "L", "M"}
NAV_TOKEN = re.compile(r"^\s*\[?\s*(?:1V|2V|V|C|C1|C2|IN|O|L|M)\s*\]?\s*$", re.I)
NAV_WORD = re.compile(
    r"^\s*(?:anterior|siguiente|índice|indice|inicio|volver|previous|next|index|home|back)\s*$",
    re.I,
)
NAV_GARBAGE = re.compile(r"^[\s\[\](){}|·•/\\,.;:_-]*$")


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def normalize_path(value):
    parts = []
    for part in value.replace("\\", "/").split("/"):
        if not part or part == ".":
            continue
        if part == "..":
            if parts:
                parts.pop()
        else:
            parts.append(part)
    return "/".join(parts)


def remove_epub_navigation(soup):
    for node in soup.find_all(["script", "style", "nav"]):
        node.decompose()
    for node in soup.find_all("link"):
        rel = " ".join(node.get("rel") or []).lower()
        if "stylesheet" in rel:
            node.decompose()
    for a in list(soup.find_all("a")):
        text = " ".join(a.stripped_strings).strip()
        compact = re.sub(r"\s+", "", text).strip("[](){} ").upper()
        if text in {"↑", "↓", "←", "→"} or compact in NAV_LABELS \
                or NAV_TOKEN.fullmatch(text) or NAV_WORD.fullmatch(text):
            a.decompose()
            continue
        for attr in ["style", "target", "onclick", "onmousedown", "onmouseup"]:
            a.attrs.pop(attr, None)

    # Same cleanup used by the Spanish package: after removing EPUB navigation,
    # discard empty wrappers and orphan punctuation such as "[ ]" or "|".
    for node in list(soup.find_all(["p", "div", "td", "tr", "span"])):
        if not node.parent:
            continue
        if node.find(["p", "div", "table", "h1", "h2", "h3", "h4"]):
            continue
        text = " ".join(node.stripped_strings).strip()
        if (not text or NAV_GARBAGE.fullmatch(text)) and not node.find(["img", "audio"]):
            node.decompose()


def semantic_markup(soup):
    counts = {}
    patterns = [
        ("hymn", re.compile(r"^(HIMNO|HYMNUS)\b", re.I)),
        ("psalmody", re.compile(r"^SALMODIA\b", re.I)),
        ("antiphon", re.compile(r"^(ANT\.?|ANT[ÍI]FONA|ANTIPHONA)\s*[123]?\b", re.I)),
        ("psalm", re.compile(r"^(SALMO|PSALMUS)\s+\d+", re.I)),
        ("canticle", re.compile(r"^(C[ÁA]NTICO|CANTICUM)\b", re.I)),
        ("reading", re.compile(r"^(LECTURA BREVE|LECTIO BREVIS)\b", re.I)),
        ("responsory", re.compile(r"^(RESPONSORIO BREVE|RESPONSORIUM BREVE)\b", re.I)),
        ("gospel", re.compile(r"^C[ÁA]NTICO EVANG[ÉE]LICO\b|^CANTICUM EVANGELICUM\b", re.I)),
        ("intercessions", re.compile(r"^PRECES\b", re.I)),
        ("prayer", re.compile(r"^(ORACI[ÓO]N|ORATIO)\b", re.I)),
    ]
    for node in soup.find_all(["p", "h1", "h2", "h3", "h4", "h5", "h6"]):
        text = re.sub(r"\s+", " ", " ".join(node.stripped_strings)).strip()
        if not text:
            continue
        for kind, pattern in patterns:
            if pattern.search(text):
                counts[kind] = counts.get(kind, 0) + 1
                key = f"{kind}:{counts[kind]}"
                node["data-ministerium-block"] = key
                node["data-semantic-id"] = f"hours-la:{key}"
                break


def clean(raw):
    soup = BeautifulSoup(raw.decode("utf-8", errors="replace"), "html.parser")
    remove_epub_navigation(soup)
    semantic_markup(soup)

    if soup.html is None:
        wrapper = BeautifulSoup(
            "<!doctype html><html><head></head><body></body></html>", "html.parser"
        )
        wrapper.body.append(soup)
        soup = wrapper
    if soup.head is None:
        head = soup.new_tag("head")
        soup.html.insert(0, head)

    viewport = soup.find("meta", attrs={"name": "viewport"})
    if viewport is None:
        viewport = soup.new_tag("meta")
        viewport["name"] = "viewport"
        soup.head.insert(0, viewport)
    viewport["content"] = "width=device-width,initial-scale=1"

    marker = soup.new_tag("meta")
    marker["name"] = "ministerium-source"
    marker["content"] = "clean-latin-hours-3.1.1"
    soup.head.append(marker)
    return str(soup)


def main():
    if not SOURCE.is_file():
        raise SystemExit(f"Missing bundled Latin source: {SOURCE}")
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True, exist_ok=True)
    written = []
    with zipfile.ZipFile(SOURCE) as zf:
        for name in zf.namelist():
            normalized = normalize_path(name)
            if not normalized or Path(normalized).suffix.lower() not in HTML_EXT:
                continue
            target = OUT / normalized
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(clean(zf.read(name)), encoding="utf-8")
            written.append(normalized)

    # The current package is expected to include every 2026 day file.
    dates = [p for p in written if re.search(r"(?:^|/)26\d{4}\.htm$", p, re.I)]
    if len(dates) < 365:
        raise SystemExit(f"Latin package has only {len(dates)} date files")
    (OUT / "files.txt").write_text("\n".join(sorted(written)) + "\n", encoding="utf-8")
    manifest = {
        "schema": 2,
        "year": 2026,
        "language": "la",
        "source": "epubs/Liturgia-horarum-2026-latin.epub",
        "sourceSha256": sha256(SOURCE),
        "sourceMode": "EPUB build input → clean runtime package",
        "runtimeUsesBundledEpub": False,
        "htmlFiles": len(written),
        "dateFiles": len(dates),
        "generatedBy": "build_clean_latin_hours_31.py",
        "cleanupParity": "Spanish hours 3.1.1",
    }
    (OUT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"Latin 2026 clean package: {len(written)} HTML / {len(dates)} dates")


if __name__ == "__main__":
    main()
