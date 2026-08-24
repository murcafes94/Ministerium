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
const backup = read('app/src/main/java/com/fabri/ministerium/BackupActivity.java');
const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');
const selection = read('app/src/main/java/com/fabri/ministerium/MinisteriumWebView.java');
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

requireText(backup, 'createDriveBackup()', 'acción de Google Drive');
requireText(backup, 'com.google.android.apps.docs', 'proveedor Google Drive');
requireText(bilingual, 'sourceY / (float) sourceRange', 'scroll bilingüe proporcional');
requireText(bilingual, 'progress * targetRange', 'scroll bilingüe proporcional destino');
for (const action of ['Subrayar', 'Nota', 'Reflexión', 'Diccionario']) {
  requireText(selection, `\"${action}\"`, `acción flotante ${action}`);
}
requireText(selection, 'SHOW_AS_ACTION_ALWAYS', 'acciones principales visibles');

if (failures.length) {
  console.error('Ministerium 3.1 stabilization: FALLÓ');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('Ministerium 3.1 stabilization: OK');
