import fs from 'node:fs';

const read = (path) => fs.readFileSync(path, 'utf8');
const failures = [];
const requireText = (source, text, label) => {
  if (!source.includes(text)) failures.push(`Falta ${label}: ${text}`);
};
const forbidText = (source, text, label) => {
  if (source.includes(text)) failures.push(`No debe existir ${label}: ${text}`);
};

const build = read('app/build.gradle');
const activity = read('app/src/main/java/com/fabri/ministerium/CombinedMassActivity.java');
const composer = read('app/src/main/java/com/fabri/ministerium/CombinedMassComposer31.java');
const papal = read('app/src/main/java/com/fabri/ministerium/LiturgiaPapalMissalRepository.java');
const missalActivity = read('app/src/main/java/com/fabri/ministerium/MissalActivity.java');
const missalDocument = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const manifest = read('app/src/main/AndroidManifest.xml');
const ritualRepository = read('app/src/main/java/com/fabri/ministerium/RitualRepository.java');
const ritualCatalog = read('app/src/main/java/com/fabri/ministerium/RitualCatalogActivity.java');
const backup = read('app/src/main/java/com/fabri/ministerium/BackupActivity.java');
const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');
const selectionChrome = read('app/src/main/java/com/fabri/ministerium/MinisteriumWebView.java');
const selectionMenu = read('app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java');
const textSelection = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
const readerPrefs = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
const readerSettings = read('app/src/main/java/com/fabri/ministerium/ReaderSettingsActivity.java');
const readerSettingsLayout = read('app/src/main/res/layout/activity_reader_settings.xml');
const bible = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
const bibleSearch = read('app/src/main/java/com/fabri/ministerium/BibleSearchActivity.java');
const bibleSearchRepo = read('app/src/main/java/com/fabri/ministerium/BibleSearchRepository.java');
const dictionary = read('app/src/main/java/com/fabri/ministerium/BibleDictionaryRepository.java');
const markers = read('app/src/main/java/com/fabri/ministerium/MarkersActivity.java');
const complineRepo = read('app/src/main/java/com/fabri/ministerium/ComplineContentRepository.java');
const complineReader = read('app/src/main/java/com/fabri/ministerium/ComplineReaderActivity.java');
const layout = read('app/src/main/res/layout/activity_combined_mass.xml');

requireText(build, 'versionCode 32', 'versionCode 3.1.1');
requireText(build, "versionName '3.1.1'", 'versionName 3.1.1');

requireText(layout, 'combinedMassWebView', 'lector continuo');
requireText(activity, 'CombinedMassComposer31.compose', 'compositor 3.1');
forbidText(activity, 'CombinedMassPolisher.compose', 'compositor heredado');
forbidText(activity, 'startActivity(', 'salida a otra Activity desde la celebración');
for (const required of ['MassReadingsRepository.read', 'LiturgiaPapalMissalRepository',
  'Cántico evangélico de ', 'Oración después de la Comunión']) {
  requireText(composer, required, `contrato combinado ${required}`);
}
for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub',
  'MissalProperRepository', 'CombinedMassComposer.compose', 'CombinedMassPolisher']) {
  forbidText(composer, forbidden, `fallback de Misal EPUB (${forbidden})`);
}
for (const source of [missalActivity, missalDocument]) {
  for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub', 'MissalProperRepository'])
    forbidText(source, forbidden, `Misal autónomo heredado (${forbidden})`);
}
requireText(missalActivity, 'MissalSectionReaderActivity.class', 'lector autónomo Liturgia Papal');
requireText(missalDocument, 'LiturgiaPapalMissalRepository', 'fuente Liturgia Papal');
requireText(manifest, '.MissalSectionReaderActivity', 'Activity del nuevo Misal');
requireText(papal, 'liturgia-papal-mexico', 'trazabilidad Liturgia Papal México');

forbidText(ritualRepository, 'argentina', 'fuente ritual argentina heredada');
requireText(ritualRepository, 'rituals/liturgiapapal/', 'ruta de rituales Liturgia Papal');
requireText(ritualCatalog, 'COMMON_BLESSINGS_ID', 'entrada de Bendicional');
requireText(ritualCatalog, 'showBlessings()', 'catálogo de Bendicional');
requireText(ritualCatalog, '"Bendiciones".equalsIgnoreCase', 'filtro de bendiciones');

requireText(backup, 'createDriveBackup()', 'acción de Google Drive');
requireText(backup, 'com.google.android.apps.docs', 'proveedor Google Drive');
requireText(bilingual, 'sourceY / (float) sourceRange', 'scroll bilingüe proporcional base');

for (const action of ['Subrayar', 'Nota', 'Reflexión', 'Diccionario', 'Traducir', 'Leer'])
  requireText(selectionMenu, `\"${action}\"`, `acción contextual ${action}`);
requireText(selectionChrome, 'super.startActionMode(wrap(callback), type)', 'geometría nativa del toolbar contextual');
forbidText(selectionChrome, 'resolvedType = ActionMode.TYPE_FLOATING', 'forzado del popup fuera del texto');
requireText(selectionChrome, 'SHOW_AS_ACTION_ALWAYS', 'acciones principales visibles');
requireText(textSelection, 'SHOW_AS_ACTION_ALWAYS', 'toolbar equivalente TextView');

requireText(readerPrefs, 'PALATINO', 'opción Palatino global');
requireText(readerPrefs, 'applyInternal(context, webView, cssFamily(context))', 'familia global aplicada');
requireText(readerSettingsLayout, 'readerPalatino', 'opción visual Palatino');
requireText(readerSettings, 'ReaderPreferences.PALATINO', 'selección Palatino');
requireText(bible, 'ReaderPreferences.apply(BibleReaderActivity.this, webView', 'preferencias globales en Biblia');

requireText(bibleSearch, 'ReferenceParser.parse(this, query)', 'apertura directa de referencias bíblicas');
requireText(bibleSearch, 'EXTRA_SCROLL_VERSE', 'salto al versículo buscado');
requireText(bibleSearchRepo, 'bible-search-index.tsv', 'índice bíblico rápido');
forbidText(bibleSearchRepo, 'BibleChapterDocument.from', 'reconstrucción completa del EPUB en cada búsqueda');
requireText(dictionary, 'ENTRY_INDEX_CACHE', 'índice directo de diccionario');
requireText(dictionary, 'byTerm.get(candidate)', 'lookup O(1) de diccionario');
requireText(markers, 'StudyStore.ofType(this, StudyEntry.HIGHLIGHT)', 'subrayados unificados con Mi estudio');

requireText(complineRepo, 'liturgicalVolume', 'criterio de tomos para Completas');
requireText(complineRepo, 'ordinaryWeek >= 18', 'Tomo IV desde semana XVIII');
requireText(complineReader, 'Tomo " + volume', 'tomo visible/resuelto en Completas');

for (const asset of [
  'app/src/main/assets/missal/es/initial.txt',
  'app/src/main/assets/missal/es/word.txt',
  'app/src/main/assets/missal/es/proper_ordinary.txt',
  'app/src/main/assets/missal/la/initial.txt',
  'app/src/main/assets/rituals/liturgiapapal/baptism_one_child.txt',
  'app/src/main/assets/rituals/liturgiapapal/unction.txt',
  'app/src/main/assets/rituals/liturgiapapal/funeral_typical.txt',
  'app/src/main/assets/rituals/liturgiapapal/blessing_house.txt',
  'app/src/main/assets/bible-search-index.tsv'
]) {
  if (!fs.existsSync(asset) || fs.statSync(asset).size < 40) failures.push(`Falta asset generado: ${asset}`);
}

if (failures.length) {
  console.error('Ministerium 3.1.1 stabilization: FALLÓ');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log('Ministerium 3.1.1 stabilization: OK');
