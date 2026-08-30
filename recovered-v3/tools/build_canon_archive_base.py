#!/usr/bin/env python3
"""Create a clean bilingual CIC base from the official Vatican HTML archive."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from lxml import html


def compact(value: str) -> str:
    value = re.sub(r"\s+", " ", value.replace("\u00a0", " ")).strip()
    value = value.replace("§1", "§ 1").replace("§2", "§ 2").replace("§3", "§ 3")
    value = value.replace("§4", "§ 4").replace("§5", "§ 5")
    value = re.sub(r"§\s+§(?=\s*\d)", "§§", value)
    return value.strip()


def paragraph_text(paragraph) -> str:
    # The archive marks revised wording with a superscript italic «n».  It is
    # editorial metadata rather than part of the canon and is presented by the
    # app in a separate reform notice.
    for marker in paragraph.xpath(".//sup"):
        if re.sub(r"\s+", "", marker.text_content()).lower() == "n":
            marker.drop_tree()
    return re.sub(r"\s+", " ", paragraph.text_content()).strip()


def remove_spanish_canon_marker(value: str, canon: int) -> str:
    return re.sub(rf"^\s*(?:Can\.\s*)?{canon}\b\s*(?:[—-]\s*)?", "", value).strip()


def clean_spanish_editorial(value: str) -> str:
    # Canon 588 § 2 contains an inline archive cross-reference to the 2022
    # rescript.  Keep the canon sentence and show that rescript separately.
    value = re.sub(
        r"\s*\(Cf\.\s*Rescriptum ex Audientia Ss\.mi.*?Congregación para los "
        r"Institutos de Vida Consagrada y las Sociedades de Vida Apostólica\),?\s*",
        ", ", value, flags=re.IGNORECASE)
    # The Spanish HTML archive represents enumerated points as plain leading
    # digits.  Restore the conventional legal marker without changing words.
    value = re.sub(r"^([1-9])\s+(?=[A-Za-zÁÉÍÓÚÜÑáéíóúüñ])", r"\1.º ", value)
    return compact(value)


def ignored_paragraph(node, value: str) -> bool:
    align = (node.get("align") or "").lower()
    if align == "center":
        return True
    if not value or value.startswith("_") or value.startswith("Carta apostólica"):
        return True
    if value in {"La Santa Sede", "Sancta Sedes", "FAQ", "Notas Legales", "Legal Notes"}:
        return True
    if re.fullmatch(r"[A-ZÁÉÍÓÚÜÑ\s.,;:()–—-]+", value) and len(value) < 180:
        return True
    return False


def parse_spanish(directory: Path) -> dict[int, list[str]]:
    result: dict[int, list[str]] = {}
    for file in sorted(directory.glob("*.html")):
        root = html.fromstring(file.read_bytes())
        current = 0
        for paragraph in root.xpath('//*[@id="corpo"]//p'):
            raw = paragraph_text(paragraph)
            if "Indica que el texto corresponde a la nueva versión" in raw:
                break
            bold = paragraph.xpath(".//b[1]")
            marker = re.fullmatch(r"\s*(\d{1,4})\s*", bold[0].text_content()) if bold else None
            raw_marker = re.match(r"^(\d{1,4})\b", raw)
            if raw_marker and (current == 0 or int(raw_marker.group(1)) > current):
                marker = raw_marker
            if marker:
                current = int(marker.group(1))
                if 1 <= current <= 1752:
                    result[current] = [clean_spanish_editorial(
                        remove_spanish_canon_marker(raw, current))]
                else:
                    current = 0
                continue
            if current and not ignored_paragraph(paragraph, raw):
                result[current].append(clean_spanish_editorial(raw))
    joined = {canon: [clean_spanish_editorial(" ".join(parts))]
              for canon, parts in result.items()}
    # The Vatican HTML places can. 1482 in the final paragraph of can. 1481.
    if 1482 not in joined and 1481 in joined and " 1482 " in joined[1481][0]:
        before, after = joined[1481][0].split(" 1482 ", 1)
        joined[1481] = [compact(before)]
        joined[1482] = [compact(after)]
    return joined


def parse_latin(directory: Path) -> dict[int, list[str]]:
    result: dict[int, list[str]] = {}
    for file in sorted(directory.glob("*.html")):
        root = html.fromstring(file.read_bytes())
        current = 0
        for paragraph in root.xpath('//*[@id="corpo"]//p'):
            raw = paragraph_text(paragraph)
            marker = re.match(r"Can\.\s*(\d{1,4})\s*(?:[—-]\s*)?(.*)", raw)
            if marker:
                current = int(marker.group(1))
                if 1 <= current <= 1752:
                    result[current] = [compact(
                        re.sub(r"^[\s:;—-]+", "", marker.group(2)))]
                else:
                    current = 0
                continue
            if current and not ignored_paragraph(paragraph, raw):
                result[current].append(compact(raw))
    joined = {canon: [compact(" ".join(parts))] for canon, parts in result.items()}
    # The Vatican HTML places can. 1667 in the same paragraph as can. 1666.
    if 1667 not in joined and 1666 in joined and "Can. 1667" in joined[1666][0]:
        before, after = re.split(r"\s*Can\.\s*1667\s*[—-]\s*", joined[1666][0], maxsplit=1)
        joined[1666] = [compact(before)]
        joined[1667] = [compact(after)]
    return joined


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("spanish_pages", type=Path)
    parser.add_argument("latin_books", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--overrides", type=Path,
                        help="Fill archive gaps from promulgated reform texts")
    args = parser.parse_args()
    spanish = parse_spanish(args.spanish_pages)
    latin = parse_latin(args.latin_books)
    if args.overrides:
        amendments = json.loads(args.overrides.read_text(encoding="utf-8"))["canons"]
        for canon, value in amendments.items():
            number = int(canon)
            if number not in spanish:
                spanish[number] = value["es"]
            if number not in latin:
                latin[number] = value["la"]
    expected = set(range(1, 1753))
    if set(spanish) != expected:
        raise RuntimeError(f"Spanish archive missing: {sorted(expected - set(spanish))}")
    if set(latin) != expected:
        raise RuntimeError(f"Latin archive missing: {sorted(expected - set(latin))}")
    payload = {
        "source_es": "https://www.vatican.va/archive/cod-iuris-canonici/cic_index_sp.html",
        "source_la": "https://www.vatican.va/archive/cod-iuris-canonici/cic_index_la.html",
        "es": spanish,
        "la": latin,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print("Official archive base: 1,752 Spanish and 1,752 Latin canons")


if __name__ == "__main__":
    main()
