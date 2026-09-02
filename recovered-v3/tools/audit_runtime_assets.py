#!/usr/bin/env python3
"""Report runtime asset weight and exact duplicate files without modifying content."""
from __future__ import annotations

import hashlib
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT = ROOT / "build" / "reports" / "runtime-assets.json"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def human(size: int) -> str:
    units = ["B", "KiB", "MiB", "GiB"]
    value = float(size)
    for unit in units:
        if value < 1024 or unit == units[-1]:
            return f"{value:.1f} {unit}"
        value /= 1024
    return f"{size} B"


def main() -> None:
    files = [p for p in ASSETS.rglob("*") if p.is_file()]
    rows = []
    by_hash: dict[str, list[dict]] = defaultdict(list)
    by_top: dict[str, int] = defaultdict(int)
    total = 0

    for path in files:
        rel = path.relative_to(ASSETS).as_posix()
        size = path.stat().st_size
        digest = sha256(path)
        row = {"path": rel, "bytes": size, "sha256": digest}
        rows.append(row)
        by_hash[digest].append(row)
        by_top[rel.split("/", 1)[0]] += size
        total += size

    duplicates = []
    reclaimable = 0
    for digest, group in by_hash.items():
        if len(group) < 2:
            continue
        size = group[0]["bytes"]
        reclaimable += size * (len(group) - 1)
        duplicates.append({"sha256": digest, "bytes_each": size,
                           "paths": [x["path"] for x in group]})

    rows.sort(key=lambda x: x["bytes"], reverse=True)
    duplicates.sort(key=lambda x: x["bytes_each"] * (len(x["paths"]) - 1), reverse=True)
    report = {
        "asset_count": len(rows),
        "total_bytes": total,
        "exact_duplicate_reclaimable_bytes": reclaimable,
        "largest_assets": rows[:80],
        "top_level_bytes": dict(sorted(by_top.items(), key=lambda kv: kv[1], reverse=True)),
        "exact_duplicates": duplicates[:100],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Runtime assets: {len(rows)} files · {human(total)}")
    print(f"Exact duplicate potential: {human(reclaimable)}")
    for name, size in sorted(by_top.items(), key=lambda kv: kv[1], reverse=True)[:12]:
        print(f"  {name:30s} {human(size):>10s}")
    print("Largest runtime assets:")
    for row in rows[:15]:
        print(f"  {human(row['bytes']):>10s}  {row['path']}")
    print(f"Report: {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
