#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import shutil
import zipfile
from pathlib import Path
from urllib.parse import urljoin
from xml.etree import ElementTree as ET

try:
    from bs4 import BeautifulSoup
except Exception as exc:
    raise SystemExit("beautifulsoup4 is required: pip install beautifulsoup4") from exc

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT = ASSETS / "hours-clean"
VOLUMES = {
    "advent": "epubs/LH - 1. ADVIENTO.epub",
    "christmas": "epubs/LH - 2. NAVIDAD.epub",
    "lent": "epubs/LH - 3. CUARESMA.epub",
    "easter": "epubs/LH - 4. PASCUA.epub",
    "ordinary": "epubs/LH - 5. TIEMPO ORDINARIO.epub",
    "sanctoral": "epubs/LH - 6. SANTORAL.epub",
}
NAV_LABELS = {"1V", "2V", "V", "C", "C1", "C2", "IN", "O", "L", "M"}
NAV_TOKEN = re.compile(r"^\s*\[?\s*(?:1V|2V|V|C|C1|C2|IN|O|L|M)\s*\]?\s*$", re.I)
NAV_WORD = re.compile(r"^\s*(?:anterior|siguiente|índice|indice|inicio|volver)\s*$", re.I)
NAV_GARBAGE = re.compile(r"^[\s\[\](){}|·•/\\,.;:_-]*$")
HTML_EXT = {".html", ".htm", ".xhtml"}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def normpath(value: str) -> str:
    parts = []
    for part in value.replace("\\", "/").split("/"):
        if not part or part == ".":
            continue
        if part == "..":
            if parts:
                parts.pop()
            continue
        parts.append(part)
    return "/".join(parts)


def find_toc(zf: zipfile.ZipFile):
    candidates = [n for n in zf.namelist() if n.lower().endswith("toc.ncx")]
    if not candidates:
        raise RuntimeError("EPUB without toc.ncx")
    toc_path = candidates[0]
    base = toc_path.rsplit("/", 1)[0] + "/" if "/" in toc_path else ""
    xml = ET.fromstring(zf.read(toc_path))
    rows = []

    def walk(node, depth):
        for child in list(node):
            tag = child.tag.split("}")[-1]
            if tag == "navPoint":
                title = ""
                src = ""
                for sub in list(child):
                    name = sub.tag.split("}")[-1]
                    if name == "navLabel":
                        for label_node in sub.iter():
                            if label_node.tag.split("}")[-1] == "text":
                                title = "".join(label_node.itertext()).strip()
                                break
                    elif name == "content":
                        src = (sub.attrib.get("src") or "").strip()
                if title and src:
                    path, _, frag = src.partition("#")
                    rows.append((title, normpath(base + path), frag, depth))
                walk(child, depth + 1)
            else:
                walk(child, depth)

    walk(xml, 0)
    if not rows:
        raise RuntimeError(f"EPUB TOC is empty: {toc_path}")
    return rows


def extract_hour_navigation(raw: bytes, source_path: str):
    soup = BeautifulSoup(raw.decode("utf-8", errors="replace"), "html.parser")
    rows = []
    base_dir = source_path.rsplit("/", 1)[0] + "/" if "/" in source_path else ""
    for anchor in soup.find_all("a"):
        label = re.sub(r"\s+", "", " ".join(anchor.stripped_strings)).strip("[](){} ").upper()
        if label not in NAV_LABELS:
            continue
        href = (anchor.get("href") or "").strip()
        if not href or href.lower().startswith(("http:", "https:", "mailto:", "javascript:")):
            continue
        target, _, fragment = href.partition("#")
        if target:
            resolved = normpath(base_dir + target)
        else:
            resolved = source_path
        rows.append((source_path, label, resolved, fragment))
    return rows


def remove_epub_navigation(soup: BeautifulSoup):
    for tag in soup.find_all(["script", "style", "nav"]):
        tag.decompose()
    for tag in soup.find_all("link"):
        rel = " ".join(tag.get("rel") or []).lower()
        if "stylesheet" in rel:
            tag.decompose()
    for a in list(soup.find_all("a")):
        text = " ".join(a.stripped_strings)
        if NAV_TOKEN.fullmatch(text) or NAV_WORD.fullmatch(text):
            a.decompose()
            continue
        for attr in ["target", "style", "onclick", "onmousedown", "onmouseup"]:
            a.attrs.pop(attr, None)
    # After removing the EPUB navigation links, discard wrappers that contain
    # only the orphan brackets/punctuation left by constructs such as [O] [L].
    for node in list(soup.find_all(["p", "div", "td", "tr", "span"])):
        if not node.parent:
            continue
        if node.find(["p", "div", "table", "h1", "h2", "h3", "h4"]):
            continue
        text = " ".join(node.stripped_strings).strip()
        if (not text or NAV_GARBAGE.fullmatch(text)) and not node.find(["img", "audio"]):
            node.decompose()


def semantic_markup(soup: BeautifulSoup):
    counters = {}
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
        text = " ".join(node.stripped_strings).strip()
        if not text:
            continue
        normalized = re.sub(r"\s+", " ", text)
        for kind, pattern in patterns:
            if pattern.search(normalized):
                counters[kind] = counters.get(kind, 0) + 1
                node["data-ministerium-block"] = f"{kind}:{counters[kind]}"
                node["data-semantic-id"] = f"hours:{kind}:{counters[kind]}"
                break


def clean_html(raw: bytes) -> str:
    text = raw.decode("utf-8", errors="replace")
    soup = BeautifulSoup(text, "html.parser")
    remove_epub_navigation(soup)
    semantic_markup(soup)
    if soup.html is None:
        wrapper = BeautifulSoup("<!doctype html><html><head></head><body></body></html>", "html.parser")
        wrapper.body.append(soup)
        soup = wrapper
    if soup.head is None:
        head = soup.new_tag("head")
        soup.html.insert(0, head)
    meta = soup.new_tag("meta")
    meta["name"] = "viewport"
    meta["content"] = "width=device-width,initial-scale=1"
    soup.head.insert(0, meta)
    marker = soup.new_tag("meta")
    marker["name"] = "ministerium-source"
    marker["content"] = "clean-hours-3.1.1"
    soup.head.append(marker)
    return str(soup)


def build_volume(volume_id: str, epub_rel: str):
    epub = ASSETS / epub_rel
    if not epub.is_file():
        raise FileNotFoundError(epub)
    target_root = OUT / volume_id
    if target_root.exists():
        shutil.rmtree(target_root)
    target_root.mkdir(parents=True, exist_ok=True)
    navigation = []
    with zipfile.ZipFile(epub) as zf:
        toc = find_toc(zf)
        referenced = {row[1] for row in toc}
        html_files = {normpath(n) for n in zf.namelist() if Path(n).suffix.lower() in HTML_EXT}
        paths = sorted(referenced | html_files)
        written = []
        for internal in paths:
            try:
                raw = zf.read(internal)
            except KeyError:
                continue
            navigation.extend(extract_hour_navigation(raw, internal))
            output = target_root / internal
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(clean_html(raw), encoding="utf-8")
            written.append(internal)
        with (target_root / "toc.tsv").open("w", encoding="utf-8") as fh:
            fh.write("#title\tpath\tfragment\tdepth\n")
            for title, path, frag, depth in toc:
                safe_title = re.sub(r"[\t\r\n]+", " ", title).strip()
                fh.write(f"{safe_title}\t{path}\t{frag}\t{depth}\n")
        with (target_root / "navigation.tsv").open("w", encoding="utf-8") as fh:
            fh.write("#source\tlabel\ttarget\tfragment\n")
            seen = set()
            for row in navigation:
                if row in seen:
                    continue
                seen.add(row)
                fh.write("\t".join(row) + "\n")
        (target_root / "files.txt").write_text("\n".join(written) + "\n", encoding="utf-8")
        info = {
            "id": volume_id,
            "source": epub_rel,
            "sourceSha256": sha256(epub),
            "tocEntries": len(toc),
            "htmlFiles": len(written),
            "navigationTargets": len(navigation),
            "runtimeUsesEpub": False,
            "generatedBy": "build_clean_hours_31.py",
        }
        (target_root / "manifest.json").write_text(json.dumps(info, ensure_ascii=False, indent=2), encoding="utf-8")
        return info


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--volume", choices=list(VOLUMES) + ["all"], default="all")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    selected = VOLUMES.items() if args.volume == "all" else [(args.volume, VOLUMES[args.volume])]
    results = [build_volume(k, v) for k, v in selected]
    if args.volume == "all":
        overall = {
            "schema": 2,
            "version": "3.1.1",
            "sourceMode": "EPUB build input → clean runtime package",
            "runtimeUsesEpub": False,
            "volumes": results,
        }
        (OUT / "manifest.json").write_text(json.dumps(overall, ensure_ascii=False, indent=2), encoding="utf-8")
    for item in results:
        print(f"{item['id']}: {item['tocEntries']} TOC / {item['htmlFiles']} HTML / {item['navigationTargets']} nav")


if __name__ == "__main__":
    main()
