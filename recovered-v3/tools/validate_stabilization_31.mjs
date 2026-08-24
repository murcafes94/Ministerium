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
const backup = read('app/src/main/java/com/fabri/ministerium/BackupActivity.java');
const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');
const selectionChrome = read('app/src/main/java/com/fabri/ministerium/MinisteriumWebView.java');
const selectionMenu = read('app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java');
const textSelection = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
const readerPrefs = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
const readerSettings = read('app/src/main/java/com/fabri/ministerium/ReaderSettingsActivity.java');
const readerSettingsLayout = read('app/src/main/res/layout/activity_reader_settings.xml');
const bible = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
const layout = read('app/src/main/res/layout/activity_combined_mass.xml');

requireText(build, 'versionCode 31', 'versionCode 3.1');
requireText(build, "versionName '3.1.0'", 'versionName 3.1.0');

requireText(layout, 'combinedMassWebView', 'lector continuo');
requireText(activity, 'CombinedMassComposer31.compose', 'compositor 3.1');
forbidText(activity, 'CombinedMassPolisher.compose', 'compositor heredado');
forbidText(activity, 'startActivity(', 'salida a otra Activity desde la celebración');

for (const required of [
  'Inicio de la celebración', 'Salmodia de ', 'Kyrie y Gloria',
  'Oración colecta', 'Liturgia de la Palabra', 'Homilía',
  'Profesión de fe', 'Preces de ', 'Liturgia eucarística',
  'Rito de la Comunión', 'Cántico evangélico de ',
  'Oración después de la Comunión', 'Rito de conclusión',
  'MassReadingsRepository.read', 'LiturgiaPapalMissalRepository'
]) requireText(composer, required, `contrato combinado ${required}`);

for (const forbidden of [
  'HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub',
  'MissalProperRepository', 'CombinedMassComposer.compose',
  'CombinedMassPolisher'
]) forbidText(composer, forbidden, `fallback de Misal EPUB (${forbidden})`);

for (const source of [missalActivity, missalDocument]) {
  for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub', 'MissalProperRepository']) {
    forbidText(source, forbidden, `Misal autónomo heredado (${forbidden})`);
  }
}
requireText(missalActivity, 'MissalSectionReaderActivity.class', 'lector autónomo Liturgia Papal');
requireText(missalDocument, 'LiturgiaPapalMissalRepository', 'fuente Liturgia Papal del Misal autónomo');
requireText(manifest, '.MissalSectionReaderActivity', 'Activity del nuevo Misal');

requireText(papal, 'liturgia-papal-mexico', 'trazabilidad Liturgia Papal México');
for (const asset of [
  'app/src/main/assets/missal/es/initial.txt',
  'app/src/main/assets/missal/es/word.txt',
  'app/src/main/assets/missal/es/eucharistic_prayer_1.txt',
  'app/src/main/assets/missal/es/proper_ordinary.txt',
  'app/src/main/assets/missal/la/initial.txt'
]) {
  if (!fs.existsSync(asset) || fs.statSync(asset).size < 40) failures.push(`Falta paquete Liturgia Papal generado: ${asset}`);
}

forbidText(ritualRepository, 'argentina', 'fuente ritual argentina heredada');
forbidText(ritualRepository, 'ritual_enfermos.txt', 'ritual de enfermos heredado');
requireText(ritualRepository, 'rituals/liturgiapapal/', 'ruta de rituales Liturgia Papal');
for (const asset of [
  'app/src/main/assets/rituals/liturgiapapal/baptism_one_child.txt',
  'app/src/main/assets/rituals/liturgiapapal/baptism_danger.txt',
  'app/src/main/assets/rituals/liturgiapapal/unction.txt',
  'app/src/main/assets/rituals/liturgiapapal/funeral_typical.txt',
  'app/src/main/assets/rituals/liturgiapapal/funeral_ashes.txt',
  'app/src/main/assets/rituals/liturgiapapal/blessing_house.txt',
  'app/src/main/assets/rituals/liturgiapapal/blessing_water.txt',
  'app/src/main/assets/rituals/liturgiapapal/blessing_rosaries.txt'
]) {
  if (!fs.existsSync(asset) || fs.statSync(asset).size < 100) failures.push(`Falta ritual Liturgia Papal generado: ${asset}`);
}

requireText(backup, 'createDriveBackup()', 'acción de Google Drive');
requireText(backup, 'com.google.android.apps.docs', 'proveedor Google Drive');
requireText(bilingual, 'sourceY / (float) sourceRange', 'scroll bilingüe proporcional');
requireText(bilingual, 'progress * targetRange', 'scroll bilingüe proporcional destino');

for (const action of ['Subrayar', 'Nota', 'Reflexión', 'Diccionario', 'Traducir', 'Leer']) {
  requireText(selectionMenu, `\"${action}\"`, `acción contextual ${action}`);
}
requireText(selectionChrome, 'ActionMode.TYPE_FLOATING', 'toolbar contextual flotante');
for (const action of ['Subrayar', 'Nota', 'Diccionario']) {
  requireText(selectionChrome, `\"${action}\"`, `acción prioritaria ${action}`);
}
requireText(selectionChrome, 'SHOW_AS_ACTION_ALWAYS', 'acciones principales visibles');
requireText(textSelection, 'SHOW_AS_ACTION_ALWAYS', 'toolbar equivalente en lectores TextView');

requireText(readerPrefs, 'public static String family(Context context)', 'preferencia global de tipografía');
requireText(readerPrefs, 'PALATINO', 'opción Palatino global');
forbidText(readerPrefs, 'Biblia y Misal se fuerzan a Palatino', 'excepción tipográfica antigua');
requireText(readerPrefs, 'ignoredLegacyPreserveTypeface', 'compatibilidad de llamada antigua sin excepción tipográfica');
requireText(readerPrefs, 'applyInternal(context, webView, cssFamily(context))', 'familia global aplicada por WebView');
requireText(readerSettingsLayout, 'readerPalatino', 'opción visual Palatino');
requireText(readerSettings, 'ReaderPreferences.PALATINO', 'selección Palatino en ajustes');
requireText(bible, 'ReaderPreferences.apply(BibleReaderActivity.this, webView', 'preferencias globales aplicadas a Biblia');

if (failures.length) {
  console.error('Ministerium 3.1 stabilization: FALLÓ');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('Ministerium 3.1 stabilization: OK');
