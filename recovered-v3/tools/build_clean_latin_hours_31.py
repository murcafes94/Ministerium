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


def clean(raw):
    soup = BeautifulSoup(raw.decode("utf-8", errors="replace"), "html.parser")
    for node in soup.find_all(["script", "style", "nav"]):
        node.decompose()
    for node in soup.find_all("link"):
        rel = " ".join(node.get("rel") or []).lower()
        if "stylesheet" in rel:
            node.decompose()
    for a in list(soup.find_all("a")):
        text = " ".join(a.stripped_strings).strip()
        if text in {"↑", "↓", "←", "→"}:
            a.decompose()
            continue
        for attr in ["style", "target", "onclick", "onmousedown", "onmouseup"]:
            a.attrs.pop(attr, None)
    # Add semantic anchors that the bilingual reader can pair with Spanish blocks.
    counts = {}
    patterns = [
        ("hymn", re.compile(r"^HYMNUS\b", re.I)),
        ("psalmody", re.compile(r"^PSALMODIA\b", re.I)),
        ("antiphon", re.compile(r"^ANT(?:IPHONA)?\.?\s*[123]?\b", re.I)),
        ("psalm", re.compile(r"^PSALMUS\s+\d+", re.I)),
        ("canticle", re.compile(r"^CANTICUM\b", re.I)),
        ("reading", re.compile(r"^LECTIO BREVIS\b", re.I)),
        ("responsory", re.compile(r"^RESPONSORIUM BREVE\b", re.I)),
        ("gospel", re.compile(r"^CANTICUM EVANGELICUM\b", re.I)),
        ("intercessions", re.compile(r"^PRECES\b", re.I)),
        ("prayer", re.compile(r"^ORATIO\b", re.I)),
    ]
    for node in soup.find_all(["p", "h1", "h2", "h3", "h4", "h5", "h6"]):
        text = re.sub(r"\s+", " ", " ".join(node.stripped_strings)).strip()
        for kind, pattern in patterns:
            if pattern.search(text):
                counts[kind] = counts.get(kind, 0) + 1
                node["data-ministerium-block"] = f"{kind}:{counts[kind]}"
                node["data-semantic-id"] = f"hours-la:{kind}:{counts[kind]}"
                break
    if soup.html is not None:
        if soup.head is None:
            head = soup.new_tag("head")
            soup.html.insert(0, head)
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
        "schema": 1,
        "year": 2026,
        "language": "la",
        "source": "epubs/Liturgia-horarum-2026-latin.epub",
        "sourceSha256": sha256(SOURCE),
        "runtimeUsesBundledEpub": False,
        "htmlFiles": len(written),
        "dateFiles": len(dates),
        "generatedBy": "build_clean_latin_hours_31.py",
    }
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Latin 2026 clean package: {len(written)} HTML / {len(dates)} dates")


if __name__ == "__main__":
    main()
