import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..");

const prayerDirectory = path.join(root, "app/src/main/assets/prayers");
const prayers = fs.readdirSync(prayerDirectory).filter((name) => name.endsWith(".txt"));
if (prayers.length !== 10) {
  throw new Error(`Se esperaban 10 oraciones básicas y se encontraron ${prayers.length}`);
}
for (const prayer of prayers) {
  const content = fs.readFileSync(path.join(prayerDirectory, prayer), "utf8").trim();
  if (content.length < 20) throw new Error(`Oración vacía o incompleta: ${prayer}`);
}
const rosary = fs.readFileSync(path.join(prayerDirectory, "santo_rosario.txt"), "utf8");
for (const marker of [
  "SALVE",
  "LETANÍAS DE LA VIRGEN",
  "Madre de la esperanza",
  "Consuelo de los migrantes",
  "Reina asunta a los Cielos",
  "Te rogamos nos concedas",
  "gozar de continua salud de alma y cuerpo",
]) {
  if (!rosary.includes(marker)) throw new Error(`Falta en el Rosario: ${marker}`);
}

const hoursLayout = fs.readFileSync(
  path.join(root, "app/src/main/res/layout/activity_hours_today.xml"), "utf8",
);
for (const marker of [
  "Los horarios son orientativos",
  "HORA ORIENTATIVA · 6:00",
  "HORA ORIENTATIVA · 18:00",
  "ANTES DE DORMIR · ≈ 21:00",
]) {
  if (!hoursLayout.includes(marker)) throw new Error(`Falta el horario visible: ${marker}`);
}

const memoryComposer = fs.readFileSync(
  path.join(root, "app/src/main/java/com/fabri/ministerium/SaintOfficeRepository.java"), "utf8",
);
for (const marker of [
  "hymnSection(properHour)",
  "properGospelAntiphon",
  "replaceGospelAntiphon",
  "composeInvitatory",
  "properInvitatoryAntiphon",
  "replaceInvitatoryAntiphon",
]) {
  if (!memoryComposer.includes(marker)) throw new Error(`Falta la composición de memorias: ${marker}`);
}

const ordinaryResolver = fs.readFileSync(
  path.join(root, "app/src/main/java/com/fabri/ministerium/OrdinaryReferenceResolver.java"), "utf8",
);
for (const marker of ["ordinaryWeek", "targetValue", "sectionById", "selectReadingsYear"]) {
  if (!ordinaryResolver.includes(marker)) {
    throw new Error(`Falta la resolución completa del Oficio: ${marker}`);
  }
}
if (!ordinaryResolver.includes("LECTURAS Y ORACION")) {
  throw new Error("Falta la resolución automática de llamadas del Tiempo Ordinario");
}

for (const [relative, markers] of [
  ["app/src/main/java/com/fabri/ministerium/ThemeUtils.java",
    ["theme_mode", "SYSTEM", "LIGHT", "DARK", "UI_MODE_NIGHT_MASK",
      "createConfigurationContext"]],
  ["app/src/main/java/com/fabri/ministerium/PrayerReminderScheduler.java",
    ["EXTRA_HOUR_INDEX", "HOUR_LABELS", "seven_reminders_migrated",
      "REQUEST_CODE_BASE + index", "setReminder", "scheduleNext(context, index)",
      "restore"]],
  ["app/src/main/java/com/fabri/ministerium/PrayerReminderReceiver.java",
    ["NotificationChannel", "EXTRA_HOUR_INDEX", "label(index)",
      "NOTIFICATION_ID_BASE + index", "HoursTodayActivity"]],
  ["app/src/main/java/com/fabri/ministerium/SettingsActivity.java",
    ["TimePickerDialog", "ROW_IDS", "switchReminderRow", "setReminder",
      "configureGospelReminder"]],
  ["app/src/main/java/com/fabri/ministerium/DailyHoursRepository.java",
    ["Text/Co1.html", "Text/Co2.html", "Después de las II Vísperas del domingo",
      "exactComplineIndex"]],
  ["app/src/main/java/com/fabri/ministerium/HoursTodayActivity.java",
    ["\"invitatory\".equals(entry.key)", "EXTRA_MEMORY_HOUR_KEY"]],
  ["app/src/main/res/layout/activity_settings.xml",
    ["themeSystem", "themeLight", "themeDark", "rowReminderOffice",
      "rowReminderLauds", "rowReminderVespers", "rowReminderCompline",
      "row_prayer_reminder"]],
  ["app/src/main/AndroidManifest.xml",
    ["INTERNET", "POST_NOTIFICATIONS", "RECEIVE_BOOT_COMPLETED",
      "PrayerReminderReceiver", "GospelReminderReceiver", "BootReceiver"]],
  ["app/src/main/java/com/fabri/ministerium/MassReadingsRepository.java",
    ["syncCurrentMonth", "PRIMERA LECTURA", "SALMO RESPONSORIAL",
      "ACLAMACIÓN ANTES DEL EVANGELIO", "writeAtomic", "gospelSummary",
      "readingSection", "psalmSection", "psalm-response", "FORMAT_MARKER"]],
  ["app/src/main/java/com/fabri/ministerium/LatinContentManager.java",
    ["DOWNLOAD_PAGE", "LITURGIA HORARUM", "OFFICIUM LECTIONIS",
      "validateExtracted", "findLatinEpub", "SLOVENSK"]],
  ["app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java",
    ["spanishWebView", "latinWebView", "screenWidthDp", "compose",
      "OrdinaryReferenceResolver.resolve", "configureSynchronizedScroll"]],
]) {
  const content = fs.readFileSync(path.join(root, relative), "utf8");
  for (const marker of markers) {
    if (!content.includes(marker)) throw new Error(`Falta ${marker} en ${relative}`);
  }
}

const navigationSources = [
  "MagisteriumActivity.java", "HoursTocActivity.java", "HoursReaderActivity.java",
  "MissalActivity.java", "MassReadingsActivity.java", "MassReadingReaderActivity.java",
  "CanonLawActivity.java",
].map((name) => fs.readFileSync(path.join(root,
  "app/src/main/java/com/fabri/ministerium", name), "utf8")).join("\n");
if (navigationSources.includes("EXTRA_EXIT_TO_HOME_ON_BACK")
    || navigationSources.includes("exitToHomeOnBack")) {
  throw new Error("La navegación del Misal y Magisterio no debe saltarse sus índices padres");
}
for (const reader of ["BibleReaderActivity.java", "HoursReaderActivity.java",
  "MassReadingReaderActivity.java", "LatinHoursReaderActivity.java",
  "CanonLawActivity.java"]) {
  const source = fs.readFileSync(path.join(root,
    "app/src/main/java/com/fabri/ministerium", reader), "utf8");
  if (!source.includes("padding-left:48px") || !source.includes("padding-right:64px")) {
    throw new Error(`Faltan márgenes adaptables en ${reader}`);
  }
}

for (const reader of [
  "app/src/main/java/com/fabri/ministerium/HoursReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/LatinHoursReaderActivity.java",
  "app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java",
]) {
  const content = fs.readFileSync(path.join(root, reader), "utf8");
  for (const marker of ["body,body *", "-webkit-text-fill-color"]) {
    if (!content.includes(marker)) throw new Error(`Falta contraste oscuro en ${reader}`);
  }
}

const settingsLayout = fs.readFileSync(
  path.join(root, "app/src/main/res/layout/activity_settings.xml"), "utf8",
);
for (const obsolete of ["spinnerReminderHour", "timeReminder", "btnSaveSettings"]) {
  if (settingsLayout.includes(obsolete)) {
    throw new Error(`Aún queda el recordatorio único anterior: ${obsolete}`);
  }
}
if (settingsLayout.includes("rowReminderInvitatory")) {
  throw new Error("El Invitatorio no debe tener un recordatorio separado");
}
if ((settingsLayout.match(/layout="@layout\/row_prayer_reminder"/g) || []).length !== 8) {
  throw new Error("La configuración debe contener siete Horas y el aviso del Evangelio");
}
const reminderScheduler = fs.readFileSync(
  path.join(root, "app/src/main/java/com/fabri/ministerium/PrayerReminderScheduler.java"), "utf8",
);
const labels = reminderScheduler.split("HOUR_LABELS", 2)[1].split("};", 1)[0];
if (labels.includes("Invitatorio") || !labels.includes("Oficio de lecturas")) {
  throw new Error("Los siete recordatorios de las Horas no están configurados correctamente");
}

const themeUtils = fs.readFileSync(
  path.join(root, "app/src/main/java/com/fabri/ministerium/ThemeUtils.java"), "utf8",
);
if (themeUtils.includes("applyOverrideConfiguration")) {
  throw new Error("El tema no debe aplicarse tarde con applyOverrideConfiguration");
}
const themedActivity = fs.readFileSync(
  path.join(root, "app/src/main/java/com/fabri/ministerium/ThemedActivity.java"), "utf8",
);
if (!themedActivity.includes("attachBaseContext") || !themedActivity.includes("ThemeUtils.wrap")) {
  throw new Error("Falta aplicar el tema antes de crear las pantallas");
}
process.stdout.write(`Oraciones básicas: ${prayers.length} textos independientes\n`);

const epubDirectory = path.join(root, "app/src/main/assets/epubs");
const epubs = fs.readdirSync(epubDirectory).filter((name) => name.endsWith(".epub"));
if (epubs.length !== 19) throw new Error(`Se esperaban 19 EPUB y se encontraron ${epubs.length}`);
for (const epub of epubs) {
  const size = fs.statSync(path.join(epubDirectory, epub)).size;
  if (size < 100_000) throw new Error(`EPUB incompleto: ${epub}`);
}
if (!epubs.includes("Devocionario-Opus-Dei.epub")) {
  throw new Error("Falta el Devocionario EPUB de Opus Dei");
}
for (const required of ["Liturgia-horarum-2026-latin.epub", "Misal-Diario-Romano.epub"]) {
  if (!epubs.includes(required)) throw new Error(`Falta el libro integrado: ${required}`);
}
for (const required of ["Concilio-Vaticano-II.epub", "Catecismo-Iglesia-Catolica.epub",
  "Compendio-Catecismo.epub", "Compendio-Doctrina-Social.epub"]) {
  if (!epubs.includes(required)) throw new Error(`Falta el documento de Magisterio: ${required}`);
}
if (!epubs.includes("Biblia-de-Jerusalen.epub")) throw new Error("Falta la Biblia de Jerusalén offline");
if (!epubs.includes("Via-Crucis-Joseph-Ratzinger.epub")) {
  throw new Error("Falta el Viacrucis offline de Joseph Ratzinger");
}
for (const required of ["Diccionario-Biblico-Texto.epub",
  "Diccionario-Biblico-Abreviado-San-Pablo.epub",
  "Diccionario-Teologia-Eunsa-2006.epub", "Diccionario-RAE-15-edicion.epub"]) {
  if (!epubs.includes(required)) throw new Error(`Falta el diccionario integrado: ${required}`);
}
const bibleIndex = JSON.parse(fs.readFileSync(
  path.join(root, "app/src/main/assets/bible-index.json"), "utf8",
));
if (!Array.isArray(bibleIndex.books) || bibleIndex.books.length !== 73) {
  throw new Error("El índice bíblico no contiene los 73 libros");
}
const bibleChapters = bibleIndex.books.reduce((sum, book) => sum + book.chapters.length, 0);
if (bibleChapters !== 1328) throw new Error(`El índice bíblico tiene ${bibleChapters} capítulos`);
process.stdout.write("Libros: 6 tomos, Liturgia latina, Misal, Devocionario, Biblia y 4 documentos de Magisterio\n");

for (const [relative, markers] of [
  ["app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java",
    ["showIntegratedNote", "extractBlock", "UniversalSelectionMenu.restoreHighlights",
      "ReaderChrome.attach", "BibleHistoryStore.record", "EXTRA_PLAN_ID",
      "ContinueReadingStore.save", "btnBack).setOnClickListener(v -> finish())"]],
  ["app/src/main/java/com/fabri/ministerium/BibleActivity.java",
    ["RECENT", "MarkersActivity", "BiblePlansActivity", "BibleSearchActivity"]],
  ["app/src/main/java/com/fabri/ministerium/BibleDictionaryActivity.java",
    ["EXTRA_SOURCE_ID", "Diccionarios",
      "Toca para abrir la entrada en un recuadro de lectura"]],
  ["app/src/main/java/com/fabri/ministerium/BibleDictionaryRepository.java",
    ["dictionary-biblical-index.tsv", "dictionary-biblical-san-pablo-index.tsv",
      "dictionary-theology-index.tsv", "dictionary-rae-index.tsv", "2.843 voces",
      "881 voces", "Diccionario de Teología EUNSA"]],
  ["app/src/main/java/com/fabri/ministerium/HoursReaderActivity.java",
    ["prepareDictionaryLayout", "ministerium-dictionary-entry",
      "ministerium-dictionary-card", "isBiblicalDictionary"]],
  ["app/src/main/java/com/fabri/ministerium/ReadingMarkerStore.java",
    ["reading_markers", "forSource", "delete"]],
  ["app/src/main/java/com/fabri/ministerium/BiblePlanRepository.java",
    ["bible_365", "gospels_89", "book_", "bookIndex", "DayReading"]],
  ["app/src/main/java/com/fabri/ministerium/BiblePlansActivity.java",
    ["btnPlanBook", "chooseBook", "Elige el libro que deseas completar",
      "cancelPlan", "Cancelar plan"]],
  ["app/src/main/java/com/fabri/ministerium/BiblePlanReminderScheduler.java",
    ["setAndAllowWhileIdle", "restore", "BiblePlanReminderReceiver"]],
  ["app/src/main/java/com/fabri/ministerium/MissalActivity.java",
    ["Ordinario de la Misa", "Oración colecta", "Misas por diversas necesidades",
      "RitoComunión", "RitoConclusión", "openDay", "MissalProperRepository.Part.COLLECT",
      "openProperTarget", "exitToHome", "onBackPressed"]],
  ["app/src/main/java/com/fabri/ministerium/MissalProperRepository.java",
    ["agosto", "Colecta", "Ofrendas", "AntifonaComunion", "DespuesComunion"]],
  ["app/src/main/java/com/fabri/ministerium/DictionarySelectionHelper.java",
    ["showDictionary", "showTranslator", "quickLookup", "ReaderOverlayDialog.show"]],
  ["app/src/main/java/com/fabri/ministerium/CanonLawActivity.java",
    ["canon-text/canons-", "CanonCommentaryRepository", "ReaderOverlayDialog.show",
      "UniversalSelectionMenu.attach", "CanonTextRepository.find",
      "Advertencia histórica", "private void back()", "padding-left:48px"]],
  ["app/src/main/java/com/fabri/ministerium/MagisteriumActivity.java",
    ["La flecha Atrás vuelve siempre al índice inmediatamente anterior",
      "openEpub", "exitToHome"]],
  ["app/src/main/java/com/fabri/ministerium/LiturgicalCalendarRepository.java",
    ["gcatholic-", "updateYear", "ensureCurrentYear", "%d-es-EC.ics"]],
  ["app/src/main/java/com/fabri/ministerium/SaintOfficeRepository.java",
    ["properPrayer(saintSection)", "expandReferencedPsalmody", "psalms[0]",
      "psalms[1]", "psalms[2]", "filepos2256303", "filepos2257391",
      "filepos2260088", "isMaryQueen"]],
  ["app/src/main/java/com/fabri/ministerium/MassReadingReaderActivity.java",
    ["lectionary-label", "reading-reference", "UniversalSelectionMenu.attach",
      "ReaderChrome.attach"]],
  ["app/src/main/java/com/fabri/ministerium/ReflectionStore.java",
    ["personal_reflections", "forSource", "delete"]],
  ["app/src/main/java/com/fabri/ministerium/PersonalPrayersActivity.java",
    ["Nueva oración", "PersonalPrayerStore.save", "Eliminar oración"]],
  ["app/src/main/java/com/fabri/ministerium/DevotionalHubActivity.java",
    ["Viacrucis de Joseph Ratzinger", "RATZINGER_WAY_OF_CROSS"]],
]) {
  const content = fs.readFileSync(path.join(root, relative), "utf8");
  for (const marker of markers) if (!content.includes(marker)) {
    throw new Error(`Falta ${marker} en ${relative}`);
  }
}

const dictionary = JSON.parse(fs.readFileSync(
  path.join(root, "app/src/main/assets/bible-dictionary.json"), "utf8",
));
if (dictionary.format !== 3 || !Array.isArray(dictionary.sources)
    || dictionary.sources.length !== 4) {
  throw new Error("El centro de diccionarios no contiene las cuatro fuentes");
}
const expectedDictionaryEntries = new Map([
  ["biblical_pdf", 2843], ["biblical_san_pablo", 881],
  ["theology_eunsa", 94], ["rae_15", 85811],
]);
for (const source of dictionary.sources) {
  if (expectedDictionaryEntries.get(source.id) !== source.entryCount) {
    throw new Error(`Cantidad incorrecta en el diccionario ${source.id}`);
  }
  const lines = fs.readFileSync(path.join(root, "app/src/main/assets", source.index), "utf8")
    .split(/\r?\n/).filter((line) => line && !line.startsWith("#"));
  if (lines.length !== source.entryCount) {
    throw new Error(`Índice incompleto en ${source.id}: ${lines.length} entradas`);
  }
}
if (dictionary.sources.filter((source) => source.id.startsWith("biblical_"))
    .some((source) => source.presentation !== "text-card")) {
  throw new Error("Los diccionarios bíblicos deben presentarse como texto en recuadros");
}
process.stdout.write("Diccionarios: 2.843 voces bíblicas, 881 voces San Pablo, 94 artículos teológicos y 85.811 voces RAE\n");

const ritualSources = [
  ["app/src/main/assets/rituals/bendicional_comun.txt", 25_000, "BENDICIÓN DE TODO LO RELACIONADO CON LOS DESPLAZAMIENTOS HUMANOS"],
  ["app/src/main/assets/rituals/bautismo_ninos.txt", 9_000, "BAUTISMO DE UN SOLO NIÑO"],
  ["app/src/main/assets/rituals/ritual_enfermos.txt", 150_000, "ASISTENCIA A LOS MORIBUNDOS"],
  ["app/src/main/assets/rituals/enfermos_difuntos.txt", 9_000, "PLEGARIA POR UN DIFUNTO"],
];
for (const [relative, minimumSize, finalMarker] of ritualSources) {
  const content = fs.readFileSync(path.join(root, relative), "utf8");
  if (content.length < minimumSize || !content.includes(finalMarker)) {
    throw new Error(`Fuente pastoral incompleta: ${relative}`);
  }
}
process.stdout.write("Bendicional y Atención pastoral: cuatro fuentes completas\n");

const calendar = fs.readFileSync(
  path.join(root, "app/src/main/assets/calendar/gcatholic-2026-es-EC.ics"),
  "utf8",
);
if (!calendar.includes("X-WR-CALNAME:Calendário Litúrgico 2026 (Ecuador)")
    || !calendar.includes("DTSTART;VALUE=DATE:20260819")) {
  throw new Error("El calendario litúrgico de Ecuador 2026 está incompleto");
}
process.stdout.write("Calendario litúrgico: Ecuador 2026 completo\n");

const assetRoot = path.join(root, "app/src/main/assets");
const assetFiles = [];
const visit = (directory) => {
  for (const name of fs.readdirSync(directory)) {
    const absolute = path.join(directory, name);
    if (fs.statSync(absolute).isDirectory()) visit(absolute);
    else assetFiles.push(absolute);
  }
};
visit(assetRoot);
const canonTextIndex = fs.readFileSync(
  path.join(assetRoot, "canon-text-index.tsv"), "utf8",
).split(/\r?\n/).filter((line) => line && !line.startsWith("#"));
if (canonTextIndex.length !== 1752) {
  throw new Error(`Se esperaban 1.752 cánones y se encontraron ${canonTextIndex.length}`);
}
const canonCommentIndex = fs.readFileSync(
  path.join(assetRoot, "canon-commentary-index.tsv"), "utf8",
).split(/\r?\n/).filter((line) => line && !line.startsWith("#"));
if (canonCommentIndex.length < 1600) {
  throw new Error("El índice de comentarios canónicos está incompleto");
}
const canonDocuments = assetFiles.filter((name) => /canon-text\/canons-\d+\.html$/.test(name));
const commentaryDocuments = assetFiles.filter(
  (name) => /canon-comments\/canon-comments-\d+\.html$/.test(name),
);
if (canonDocuments.length !== 18 || commentaryDocuments.length !== 18) {
  throw new Error("Los cánones o sus comentarios no están divididos en los 18 tomos esperados");
}
if (canonDocuments.some((name) => fs.readFileSync(name, "utf8").includes("§ §"))) {
  throw new Error("El texto canónico conserva signos de sección espurios del PDF");
}
const canonHtml = canonDocuments.map((name) => fs.readFileSync(name, "utf8")).join("\n");
if ((canonHtml.match(/class="canon"/g) || []).length !== 1752
    || (canonHtml.match(/class="reform-note"/g) || []).length !== 144
    || (canonHtml.match(/class="complementary-note"/g) || []).length !== 2) {
  throw new Error("El Código consolidado o sus avisos jurídicos están incompletos");
}
const canonBase = JSON.parse(fs.readFileSync(
  path.join(root, "tools/data/canon-vatican-base.json"), "utf8",
));
const overrides = JSON.parse(fs.readFileSync(
  path.join(assetRoot, "canon-official-overrides.json"), "utf8",
));
if (Object.keys(canonBase.es || {}).length !== 1752
    || Object.keys(canonBase.la || {}).length !== 1752
    || Object.keys(overrides.canons || {}).length !== 144) {
  throw new Error("La base oficial bilingüe o la consolidación de reformas está incompleta");
}
process.stdout.write("Derecho canónico: 1.752 pares oficiales, 144 reformas consolidadas y comentarios históricos locales\n");
