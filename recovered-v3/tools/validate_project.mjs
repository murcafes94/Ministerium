import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..");

const required = [
  "settings.gradle",
  "build.gradle",
  "gradle.properties",
  "gradlew",
  "gradlew.bat",
  "gradle/wrapper/gradle-wrapper.jar",
  "gradle/wrapper/gradle-wrapper.properties",
  "GUIA-INICIO-ANDROID-STUDIO.md",
  "app/build.gradle",
  "app/src/main/AndroidManifest.xml",
  "app/src/main/java/com/fabri/ministerium/MainActivity.java",
  "app/src/main/java/com/fabri/ministerium/BasicPrayersActivity.java",
  "app/src/main/java/com/fabri/ministerium/HoursActivity.java",
  "app/src/main/java/com/fabri/ministerium/HoursTodayActivity.java",
  "app/src/main/java/com/fabri/ministerium/DailyHoursRepository.java",
  "app/src/main/java/com/fabri/ministerium/SaintOfficeRepository.java",
  "app/src/main/java/com/fabri/ministerium/OrdinaryReferenceResolver.java",
  "app/src/main/java/com/fabri/ministerium/SettingsActivity.java",
  "app/src/main/java/com/fabri/ministerium/BilingualHoursActivity.java",
  "app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/LatinContentManager.java",
  "app/src/main/java/com/fabri/ministerium/LatinHoursActivity.java",
  "app/src/main/java/com/fabri/ministerium/LatinHoursReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/MassReadingsActivity.java",
  "app/src/main/java/com/fabri/ministerium/MassReadingsRepository.java",
  "app/src/main/java/com/fabri/ministerium/MassReadingReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/LiturgicalCalendarActivity.java",
  "app/src/main/java/com/fabri/ministerium/UsccbLinks.java",
  "app/src/main/java/com/fabri/ministerium/GospelReminderScheduler.java",
  "app/src/main/java/com/fabri/ministerium/GospelReminderReceiver.java",
  "app/src/main/java/com/fabri/ministerium/ThemedActivity.java",
  "app/src/main/java/com/fabri/ministerium/PrayerReminderScheduler.java",
  "app/src/main/java/com/fabri/ministerium/PrayerReminderReceiver.java",
  "app/src/main/java/com/fabri/ministerium/BootReceiver.java",
  "app/src/main/java/com/fabri/ministerium/PrayerIntentionsActivity.java",
  "app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java",
  "app/src/main/java/com/fabri/ministerium/LiturgicalCalendarRepository.java",
  "app/src/main/java/com/fabri/ministerium/PastoralActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleChaptersActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleChapterDocument.java",
  "app/src/main/java/com/fabri/ministerium/BibleHistoryStore.java",
  "app/src/main/java/com/fabri/ministerium/ReadingMarkerStore.java",
  "app/src/main/java/com/fabri/ministerium/MarkersActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleDictionaryActivity.java",
  "app/src/main/java/com/fabri/ministerium/BibleDictionaryRepository.java",
  "app/src/main/java/com/fabri/ministerium/DictionarySelectionHelper.java",
  "app/src/main/java/com/fabri/ministerium/ReaderOverlayDialog.java",
  "app/src/main/java/com/fabri/ministerium/BiblePlanRepository.java",
  "app/src/main/java/com/fabri/ministerium/BiblePlansActivity.java",
  "app/src/main/java/com/fabri/ministerium/BiblePlanReminderReceiver.java",
  "app/src/main/java/com/fabri/ministerium/MissalActivity.java",
  "app/src/main/java/com/fabri/ministerium/MissalProperRepository.java",
  "app/src/main/java/com/fabri/ministerium/ReflectionStore.java",
  "app/src/main/java/com/fabri/ministerium/ReflectionsActivity.java",
  "app/src/main/java/com/fabri/ministerium/PersonalPrayersActivity.java",
  "app/src/main/java/com/fabri/ministerium/MagisteriumActivity.java",
  "app/src/main/java/com/fabri/ministerium/CanonLawActivity.java",
  "app/src/main/java/com/fabri/ministerium/CanonTextRepository.java",
  "app/src/main/java/com/fabri/ministerium/CanonCommentaryRepository.java",
  "app/src/main/java/com/fabri/ministerium/AssetPdfReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/DevotionalHubActivity.java",
  "app/src/main/java/com/fabri/ministerium/ConscienceActivity.java",
  "app/src/main/java/com/fabri/ministerium/RitualRepository.java",
  "app/src/main/assets/calendar/gcatholic-2026-es-EC.ics",
  "app/src/main/assets/epubs/Devocionario-Opus-Dei.epub",
  "app/src/main/assets/epubs/LH - 1. ADVIENTO.epub",
  "app/src/main/assets/epubs/LH - 2. NAVIDAD.epub",
  "app/src/main/assets/epubs/LH - 3. CUARESMA.epub",
  "app/src/main/assets/epubs/LH - 4. PASCUA.epub",
  "app/src/main/assets/epubs/LH - 5. TIEMPO ORDINARIO.epub",
  "app/src/main/assets/epubs/LH - 6. SANTORAL.epub",
  "app/src/main/assets/epubs/Liturgia-horarum-2026-latin.epub",
  "app/src/main/assets/epubs/Misal-Diario-Romano.epub",
  "app/src/main/assets/epubs/Concilio-Vaticano-II.epub",
  "app/src/main/assets/epubs/Catecismo-Iglesia-Catolica.epub",
  "app/src/main/assets/epubs/Compendio-Catecismo.epub",
  "app/src/main/assets/epubs/Compendio-Doctrina-Social.epub",
  "app/src/main/assets/epubs/Biblia-de-Jerusalen.epub",
  "app/src/main/assets/epubs/Via-Crucis-Joseph-Ratzinger.epub",
  "app/src/main/assets/epubs/Diccionario-Biblico-Texto.epub",
  "app/src/main/assets/epubs/Diccionario-Biblico-Abreviado-San-Pablo.epub",
  "app/src/main/assets/epubs/Diccionario-Teologia-Eunsa-2006.epub",
  "app/src/main/assets/epubs/Diccionario-RAE-15-edicion.epub",
  "app/src/main/assets/bible-index.json",
  "app/src/main/assets/bible-dictionary.json",
  "app/src/main/assets/dictionary-biblical-index.tsv",
  "app/src/main/assets/dictionary-biblical-san-pablo-index.tsv",
  "app/src/main/assets/dictionary-theology-index.tsv",
  "app/src/main/assets/dictionary-rae-index.tsv",
  "app/src/main/assets/canon-text-index.tsv",
  "app/src/main/assets/canon-commentary-index.tsv",
  "app/src/main/assets/canon-official-overrides.json",
  "app/src/main/assets/canon-text/canons-01.html",
  "app/src/main/assets/canon-text/canons-18.html",
  "app/src/main/assets/canon-comments/canon-comments-01.html",
  "app/src/main/assets/canon-comments/canon-comments-18.html",
  "app/src/main/assets/rituals/bendicional_comun.txt",
  "app/src/main/assets/rituals/bautismo_ninos.txt",
  "app/src/main/assets/rituals/ritual_enfermos.txt",
  "app/src/main/assets/rituals/enfermos_difuntos.txt",
  "app/src/main/res/layout/activity_settings.xml",
  "app/src/main/res/layout/activity_bilingual_hours.xml",
  "app/src/main/res/layout/activity_bilingual_reader.xml",
  "app/src/main/res/layout/activity_latin_hours.xml",
  "app/src/main/res/layout/activity_mass_readings.xml",
  "app/src/main/res/layout/activity_mass_reading_reader.xml",
  "app/src/main/res/layout/activity_bible_reader.xml",
  "app/src/main/res/layout/activity_bible_library.xml",
  "app/src/main/res/layout/activity_bible_plans.xml",
  "app/src/main/res/layout/activity_missal.xml",
  "app/src/main/res/layout/activity_personal_prayers.xml",
  "app/src/main/res/layout/activity_liturgical_calendar.xml",
  "app/src/main/res/layout/row_prayer_reminder.xml",
  "NOVEDADES-1.7.0.md",
  "NOVEDADES-1.7.1.md",
  "NOVEDADES-1.8.0.md",
  "NOVEDADES-1.8.1.md",
  "NOVEDADES-1.9.0.md",
  "NOVEDADES-2.0.0.md",
  "NOVEDADES-2.0.1.md",
  "NOVEDADES-2.1.0.md",
  "NOVEDADES-2.2.0.md",
  "NOVEDADES-2.2.1.md",
  "NOVEDADES-2.3.0.md",
  "NOVEDADES-2.3.1.md",
  "NOVEDADES-2.3.2.md",
  "NOVEDADES-3.0.0.md",
  "app/src/main/java/com/fabri/ministerium/ReaderChrome.java",
  "app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java",
  "app/src/main/java/com/fabri/ministerium/StudyStore.java",
  "app/src/main/java/com/fabri/ministerium/ReferenceParser.java",
  "app/src/main/java/com/fabri/ministerium/BackupManager.java",
  "app/src/main/java/com/fabri/ministerium/CombinedHoursRepository.java",
  "app/src/main/java/com/fabri/ministerium/CombinedMassActivity.java",
  "app/src/main/java/com/fabri/ministerium/IntermediateHourResolver.java",
  "app/src/main/java/com/fabri/ministerium/TtsPlaybackService.java",
  "app/src/main/assets/package-manifest.json",
  "distribution/release-manifest.template.json",
  "distribution/content-packages.template.json",
  ".github/workflows/validate.yml",
  "tools/build_pdf_dictionary.py",
  "tools/build_san_pablo_dictionary.py",
  "tools/build_epub_dictionary_index.py",
  "tools/build_dictionary_index.mjs",
  "tools/build_canon_text.py",
  "tools/build_canon_commentary.py",
  "tools/build_canon_archive_base.py",
  "tools/build_canon_overrides.py",
  "tools/data/canon-vatican-base.json",
];

for (const relative of required) {
  const absolute = path.join(root, relative);
  if (!fs.existsSync(absolute) || fs.statSync(absolute).size === 0) {
    throw new Error(`Falta el archivo requerido: ${relative}`);
  }
}

const manifest = fs.readFileSync(path.join(root, "app/src/main/AndroidManifest.xml"), "utf8");
if (!/android\.permission\.INTERNET/.test(manifest)) {
  throw new Error("Falta Internet para actualizar lecturas y Liturgia latina");
}

const projectBuild = fs.readFileSync(path.join(root, "build.gradle"), "utf8");
if (!projectBuild.includes("com.android.tools.build:gradle:4.2.1")) {
  throw new Error("El proyecto no usa Android Gradle Plugin 4.2.1");
}

const wrapper = fs.readFileSync(path.join(root, "gradle/wrapper/gradle-wrapper.properties"), "utf8");
if (!wrapper.includes("gradle-6.7.1-bin.zip")) {
  throw new Error("La distribución Gradle 6.7.1 no está configurada");
}

const appBuild = fs.readFileSync(path.join(root, "app/build.gradle"), "utf8");
if (!appBuild.includes("compileSdkVersion 30") || !appBuild.includes("JavaVersion.VERSION_1_8")) {
  throw new Error("El módulo no está adaptado a Android Studio 4.2.1");
}
if (!appBuild.includes("versionCode 30") || !appBuild.includes("versionName '3.0.0'")) {
  throw new Error("La versión actualizada de la aplicación no está configurada");
}

const canonDirectory = path.join(root, "app/src/main/assets/canon-text");
const canonHtml = fs.readdirSync(canonDirectory)
  .filter((name) => name.endsWith(".html"))
  .map((name) => fs.readFileSync(path.join(canonDirectory, name), "utf8"))
  .join("\n");
const canonArticles = (canonHtml.match(/class="canon"/g) || []).length;
const spanishCanons = (canonHtml.match(/lang="es"/g) || []).length;
const latinCanons = (canonHtml.match(/lang="la"/g) || []).length;
if (canonArticles !== 1752 || spanishCanons !== 1752 || latinCanons !== 1752) {
  throw new Error("El Código canónico bilingüe no contiene los 1.752 pares esperados");
}
const canonBase = JSON.parse(fs.readFileSync(
  path.join(root, "tools/data/canon-vatican-base.json"), "utf8",
));
if (Object.keys(canonBase.es || {}).length !== 1752
    || Object.keys(canonBase.la || {}).length !== 1752) {
  throw new Error("La base oficial del Vaticano no contiene 1.752 cánones por idioma");
}
for (let canon = 1; canon <= 1752; canon += 1) {
  const es = (canonBase.es?.[canon] || []).join(" ").trim();
  const la = (canonBase.la?.[canon] || []).join(" ").trim();
  if (!es || !la || /aaaaaaaa|Indica que el texto corresponde|Redacción original/.test(es + la)) {
    throw new Error(`Texto oficial incompleto o contaminado en el canon ${canon}`);
  }
}
const canonOverrides = JSON.parse(fs.readFileSync(
  path.join(root, "app/src/main/assets/canon-official-overrides.json"), "utf8",
));
const amendedCanons = Object.entries(canonOverrides.canons || {});
if (amendedCanons.length !== 144 || canonOverrides.verified_through !== "2026-08-21") {
  throw new Error("La consolidación oficial del Código no cubre las 144 reformas verificadas");
}
for (const [canon, entry] of amendedCanons) {
  if (!(entry.es || []).join(" ").trim() || !(entry.la || []).join(" ").trim()
      || !entry.reform || !entry.date || !entry.source || !entry.spanish_status) {
    throw new Error(`Metadatos incompletos en la reforma del canon ${canon}`);
  }
}
if ((canonHtml.match(/class="reform-note"/g) || []).length !== 144
    || (canonHtml.match(/class="complementary-note"/g) || []).length !== 2) {
  throw new Error("Los avisos de reformas y normas complementarias no están completos");
}

const properties = fs.readFileSync(path.join(root, "gradle.properties"), "utf8");
for (const setting of [
  "org.gradle.jvmargs=-Xmx1536m",
  "org.gradle.parallel=false",
  "org.gradle.workers.max=2",
]) {
  if (!properties.includes(setting)) throw new Error(`Falta el ajuste liviano: ${setting}`);
}

const javaDirectory = path.join(root, "app/src/main/java/com/fabri/ministerium");
const javaFiles = fs.readdirSync(javaDirectory).filter((name) => name.endsWith(".java"));
if (javaFiles.length < 90) throw new Error("La aplicación contiene menos clases de las esperadas");

const layoutDirectory = path.join(root, "app/src/main/res/layout");
const layouts = fs.readdirSync(layoutDirectory).filter((name) => name.endsWith(".xml"));
if (layouts.length < 28) throw new Error("Faltan pantallas XML de la aplicación");

process.stdout.write(
  `Proyecto 3.0.0 válido para Android Studio 4.2.1: ${javaFiles.length} clases, ${layouts.length} diseños, diecinueve EPUB, cuatro diccionarios, Derecho canónico bilingüe, estudio, planes, TTS, Leccionario y Misal organizado\n`,
);
