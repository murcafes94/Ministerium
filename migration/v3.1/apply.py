#!/usr/bin/env python3
"""Apply the Ministerium 3.1 migration overlay to the reconstructed v3 project.

The large v3 Android project is temporarily reconstructed from bootstrap ZIP
parts in CI. This script keeps 3.1 changes reproducible until the full source
is normalized as regular repository files.
"""

from pathlib import Path
import re


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Migration point not found: {label} in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_version() -> None:
    path = Path("app/build.gradle")
    text = path.read_text(encoding="utf-8")
    text = text.replace("versionCode 30", "versionCode 31", 1)
    text = text.replace("versionName '3.0.0'", "versionName '3.1.0'", 1)
    if "versionCode 31" not in text or "versionName '3.1.0'" not in text:
        raise SystemExit("Unable to set Ministerium 3.1.0 version")
    path.write_text(text, encoding="utf-8")


def patch_project_validator() -> None:
    path = Path("tools/validate_project.mjs")
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    old = 'if (!appBuild.includes("versionCode 30") || !appBuild.includes("versionName \'3.0.0\'")) {'
    new = 'if (!appBuild.includes("versionCode 31") || !appBuild.includes("versionName \'3.1.0\'")) {'
    if new not in text:
        if old not in text:
            raise SystemExit("Migration point not found: validate_project application version")
        text = text.replace(old, new, 1)
    text = text.replace("Proyecto 3.0.0 válido para Android Studio 4.2.1:",
                        "Proyecto 3.1.0 válido para Android Studio 4.2.1:", 1)
    path.write_text(text, encoding="utf-8")


def patch_bible_reader() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java")
    old = """        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
        File target = new File(extractedRoot, chapter.file);
        try {
"""
    new = """        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());

        // Ministerium 3.1: prefer an installed semantic SQLite Bible package.
        // If none exists (or it is incompatible), preserve the exact 3.0 EPUB path.
        String semanticChapter = SemanticBibleCompat.chapterHtml(this, book, chapter.number);
        if (semanticChapter != null) {
            webView.loadDataWithBaseURL("https://ministerium.local/bible/", semanticChapter,
                    "text/html", "UTF-8", null);
            return;
        }

        File target = new File(extractedRoot, chapter.file);
        try {
"""
    replace_once(path, old, new, "semantic Bible fallback bridge")


def patch_reader_subtitle() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java")
    old = """        String subtitle = book.testament + " · notas integradas · sin conexión";
"""
    new = """        String semanticEdition = SemanticBibleCompat.installedEditionName(this);
        String subtitle = book.testament + " · notas integradas · sin conexión"
                + (semanticEdition == null ? "" : " · " + semanticEdition);
"""
    replace_once(path, old, new, "semantic Bible edition subtitle")


def patch_calendar_calculations() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java")
    text = path.read_text(encoding="utf-8")

    replacement = """    public static int ordinaryWeekNumber(Calendar selected) {
        return RomanCalendarMath.ordinaryWeekNumber(selected);
    }

    public static String lectionaryCycle"""
    if replacement not in text:
        pattern = re.compile(
            r"    public static int ordinaryWeekNumber\(Calendar selected\) \{.*?\n    \}\n\n"
            r"    public static String lectionaryCycle",
            re.DOTALL,
        )
        text, count = pattern.subn(replacement, text, count=1)
        if count != 1:
            raise SystemExit("Migration point not found: ordinaryWeekNumber in LiturgicalResolver")

    # The same Baptism boundary is also used when resolving the temporal office.
    text = text.replace(
        "Calendar baptism = addDays(epiphany, 7);",
        "Calendar baptism = RomanCalendarMath.baptismOfTheLord(year);",
        1,
    )

    # When the psalter week is not supplied, derive it from the same tested
    # Ordinary Time week source instead of repeating older arithmetic.
    old_psalter = """            int ordinaryWeek;
            if (date.before(ashWednesday)) {
                ordinaryWeek = daysBetween(addDays(baptism, 1), date) / 7 + 1;
            } else {
                Calendar christKing = addDays(advent, -7);
                int remaining = daysBetween(date, christKing);
                ordinaryWeek = 34 - ((Math.max(0, remaining) + 6) / 7);
            }
            psalter = ((ordinaryWeek - 1) % 4 + 4) % 4 + 1;
"""
    new_psalter = """            int ordinaryWeek = RomanCalendarMath.ordinaryWeekNumber(date);
            psalter = ((ordinaryWeek - 1) % 4 + 4) % 4 + 1;
"""
    if new_psalter not in text:
        if old_psalter not in text:
            raise SystemExit("Migration point not found: psalter Ordinary Time calculation")
        text = text.replace(old_psalter, new_psalter, 1)

    path.write_text(text, encoding="utf-8")


def main() -> None:
    patch_version()
    patch_project_validator()
    patch_bible_reader()
    patch_reader_subtitle()
    patch_calendar_calculations()
    print("Ministerium 3.1 migration overlay applied")


if __name__ == "__main__":
    main()
