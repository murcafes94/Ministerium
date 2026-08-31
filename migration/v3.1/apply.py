#!/usr/bin/env python3
"""Apply the Ministerium 3.1 migration overlay to the reconstructed v3 project."""

from pathlib import Path
import re
import shutil


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

        // 3.1 prefers an installed semantic SQLite package while retaining the
        // original EPUB as a zero-risk offline fallback.
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

    old_subtitle = '        String subtitle = book.testament + " · notas integradas · sin conexión";\n'
    new_subtitle = """        String semanticEdition = SemanticBibleCompat.installedEditionName(this);
        String subtitle = book.testament + " · notas integradas · sin conexión"
                + (semanticEdition == null ? "" : " · " + semanticEdition);
"""
    replace_once(path, old_subtitle, new_subtitle, "semantic Bible edition subtitle")


def patch_bible_visual_line() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java")
    text = path.read_text(encoding="utf-8")
    if "// Ministerium 3.1: preserve the Bible EPUB's own graphic line." in text:
        return
    replacement = r'''    private void applyStyle() {
        // Ministerium 3.1: preserve the Bible EPUB's own graphic line.
        // We only add safe reading margins, responsive media, dark-mode contrast
        // and Ministerium highlights; typography and hierarchy remain the EPUB's.
        boolean dark = ThemeUtils.isDark(this);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#E1C57A" : "#772233";
        String css = "html,body{background:" + bg + "!important;}"
                + "body{margin:0!important;padding:18px 20px!important;box-sizing:border-box;"
                + "max-width:100%!important;overflow-wrap:break-word;}"
                + "img,table{max-width:100%!important;height:auto!important;}"
                + ".ministerium-highlight{background:#F6E58D!important;color:#231F1B!important;"
                + "-webkit-text-fill-color:#231F1B!important;padding:1px 2px;border-radius:2px;}"
                + (dark ? "body,body p,body li,body span,body div{color:" + ink
                + "!important;-webkit-text-fill-color:" + ink + "!important;}"
                + "a,a *{color:" + accent + "!important;-webkit-text-fill-color:"
                + accent + "!important;}" : "")
                + "@media(min-width:700px){body{padding-left:42px!important;padding-right:42px!important}}";
        webView.evaluateJavascript("(function(){var s=document.getElementById('ministerium-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + org.json.JSONObject.quote(css) + ";})()", null);
    }

    private ReaderContext context()'''
    pattern = re.compile(r"    private void applyStyle\(\) \{.*?\n    \}\n\n    private ReaderContext context\(\)", re.DOTALL)
    patched, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit("Migration point not found: Bible applyStyle")
    path.write_text(patched, encoding="utf-8")


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
            r"    public static String lectionaryCycle", re.DOTALL)
        text, count = pattern.subn(replacement, text, count=1)
        if count != 1:
            raise SystemExit("Migration point not found: ordinaryWeekNumber in LiturgicalResolver")
    text = text.replace("Calendar baptism = addDays(epiphany, 7);",
                        "Calendar baptism = RomanCalendarMath.baptismOfTheLord(year);", 1)
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


def patch_selection_menu() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java")
    text = path.read_text(encoding="utf-8")
    old = ".setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);"
    new = ".setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);"
    if new not in text:
        if old not in text:
            raise SystemExit("Migration point not found: selection action visibility")
        text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")


def patch_drive_backup() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BackupActivity.java")
    text = path.read_text(encoding="utf-8")
    if "private static final int DRIVE_BACKUP = 83;" not in text:
        text = text.replace(
            "    private static final int RESTORE_BACKUP = 82;\n",
            "    private static final int RESTORE_BACKUP = 82;\n"
            "    private static final int DRIVE_BACKUP = 83;\n", 1)
    text = text.replace(
        "findViewById(R.id.btnDriveBackup).setOnClickListener(v -> createBackup());",
        "findViewById(R.id.btnDriveBackup).setOnClickListener(v -> createDriveBackup());", 1)
    if "private void createDriveBackup()" not in text:
        marker = """    private void chooseRestore() {
"""
        method = """    private void createDriveBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "ministerium-backup-current.json");
        intent.setPackage("com.google.android.apps.docs");
        try {
            startActivityForResult(intent, DRIVE_BACKUP);
        } catch (Exception driveUnavailable) {
            intent.setPackage(null);
            Toast.makeText(this,
                    "Google Drive no está disponible como proveedor. Elige Drive en el selector.",
                    Toast.LENGTH_LONG).show();
            startActivityForResult(intent, DRIVE_BACKUP);
        }
    }

"""
        if marker not in text:
            raise SystemExit("Migration point not found: BackupActivity restore method")
        text = text.replace(marker, method + marker, 1)
    text = text.replace("if (requestCode == CREATE_BACKUP) {",
                        "if (requestCode == CREATE_BACKUP || requestCode == DRIVE_BACKUP) {", 1)
    path.write_text(text, encoding="utf-8")

    layout = Path("app/src/main/res/layout/activity_backup.xml")
    xml = layout.read_text(encoding="utf-8")
    xml = xml.replace('android:text="Guardar en Drive (opcional)"',
                      'android:text="Guardar en Google Drive"', 1)
    xml = xml.replace(
        'android:text="Se abrirá el selector seguro de Android. Elige Drive solo si ya está disponible en tu dispositivo; Ministerium funciona sin vincularlo."',
        'android:text="Se abrirá Google Drive directamente. Si tu cuenta aún no está activa, Drive podrá pedirte iniciar sesión. Si Drive no está instalado, se usará el selector seguro de Android."',
        1)
    layout.write_text(xml, encoding="utf-8")


def patch_bilingual_reader() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java")
    text = path.read_text(encoding="utf-8")
    old_sync = """    private void synchronize(WebView source, WebView target, int sourceY) {
        if (syncingScroll || source.getVisibility() != View.VISIBLE
                || target.getVisibility() != View.VISIBLE) return;
        int targetRange = Math.round(target.getContentHeight() * target.getScale())
                - target.getHeight();
        if (targetRange <= 0) return;
        syncingScroll = true;
        target.scrollTo(target.getScrollX(), Math.max(0, Math.min(sourceY, targetRange)));
        target.postDelayed(() -> syncingScroll = false, 60L);
    }
"""
    new_sync = """    private void synchronize(WebView source, WebView target, int sourceY) {
        if (syncingScroll || source.getVisibility() != View.VISIBLE
                || target.getVisibility() != View.VISIBLE) return;
        int sourceRange = Math.round(source.getContentHeight() * source.getScale())
                - source.getHeight();
        int targetRange = Math.round(target.getContentHeight() * target.getScale())
                - target.getHeight();
        if (sourceRange <= 0 || targetRange <= 0) return;
        float progress = Math.max(0f, Math.min(1f, sourceY / (float) sourceRange));
        int targetY = Math.round(progress * targetRange);
        syncingScroll = true;
        target.scrollTo(target.getScrollX(), targetY);
        target.postDelayed(() -> syncingScroll = false, 60L);
    }
"""
    if new_sync not in text:
        if old_sync not in text:
            raise SystemExit("Migration point not found: bilingual synchronized scroll")
        text = text.replace(old_sync, new_sync, 1)
    text = text.replace(
        "panes.setOrientation(widthDp >= 700 ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);",
        "panes.setOrientation(LinearLayout.HORIZONTAL);", 1)
    old_alignment = """                if (Math.abs(difference) < 3 || Math.abs(difference) > 2400) continue;
                JSONObject spacer = new JSONObject();
                spacer.put("key", key);
                spacer.put("height", Math.round(Math.abs(difference)));
                if (difference < 0) {
                    spanishSpaces.put(spacer);
                    addedSpanish += Math.abs(difference);
                } else {
                    latinSpaces.put(spacer);
                    addedLatin += difference;
                }
"""
    new_alignment = """                if (Math.abs(difference) < 3 || Math.abs(difference) > 720) continue;
                double spacerHeight = Math.min(220d, Math.abs(difference));
                JSONObject spacer = new JSONObject();
                spacer.put("key", key);
                spacer.put("height", Math.round(spacerHeight));
                if (difference < 0) {
                    spanishSpaces.put(spacer);
                    addedSpanish += spacerHeight;
                } else {
                    latinSpaces.put(spacer);
                    addedLatin += spacerHeight;
                }
"""
    if new_alignment not in text:
        if old_alignment not in text:
            raise SystemExit("Migration point not found: bilingual alignment spacers")
        text = text.replace(old_alignment, new_alignment, 1)
    path.write_text(text, encoding="utf-8")


def patch_continuous_combined() -> None:
    source = Path("migration/v3.1/overlays/com/fabri/ministerium/CombinedMassActivity.java")
    target = Path("app/src/main/java/com/fabri/ministerium/CombinedMassActivity.java")
    if not source.is_file():
        raise SystemExit("Missing continuous combined celebration overlay")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)


def patch_release_labels() -> None:
    update = Path("app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java")
    if update.exists():
        text = update.read_text(encoding="utf-8")
        text = text.replace('setTitle("Ministerium 3.0.0")', 'setTitle("Ministerium 3.1.0")', 1)
        if Path("app/src/main/assets/changelog-3.1.0.txt").exists():
            text = text.replace('readAsset("changelog-3.0.0.txt")',
                                'readAsset("changelog-3.1.0.txt")', 1)
        update.write_text(text, encoding="utf-8")
    manifest = Path("app/src/main/assets/package-manifest.json")
    if manifest.exists():
        text = manifest.read_text(encoding="utf-8")
        text = text.replace('"versionName": "3.0.0"', '"versionName": "3.1.0"', 1)
        manifest.write_text(text, encoding="utf-8")


def main() -> None:
    patch_version()
    patch_project_validator()
    patch_bible_reader()
    patch_bible_visual_line()
    patch_calendar_calculations()
    patch_selection_menu()
    patch_drive_backup()
    patch_bilingual_reader()
    patch_continuous_combined()
    patch_release_labels()
    print("Ministerium 3.1 migration overlay applied")


if __name__ == "__main__":
    main()
