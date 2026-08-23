import fs from 'node:fs';

// Contrato de regresión: Misa + Laudes/Vísperas debe seguir siendo una sola pantalla.
const activity = fs.readFileSync(
  'app/src/main/java/com/fabri/ministerium/CombinedMassActivity.java', 'utf8');
const composer = fs.readFileSync(
  'app/src/main/java/com/fabri/ministerium/CombinedMassComposer.java', 'utf8');
const layout = fs.readFileSync(
  'app/src/main/res/layout/activity_combined_mass.xml', 'utf8');

const failures = [];
const requireText = (source, text, label) => {
  if (!source.includes(text)) failures.push(`Falta ${label}: ${text}`);
};
const forbidText = (source, text, label) => {
  if (source.includes(text)) failures.push(`No debe existir ${label}: ${text}`);
};

// Contrato principal: una sola pantalla, no menú de navegación entre módulos.
requireText(layout, 'combinedMassWebView', 'el WebView único de celebración');
forbidText(layout, 'btnCombinedReadings', 'botón externo de Lecturas');
forbidText(layout, 'btnCombinedCollect', 'botón externo de Colecta');
forbidText(layout, 'btnCombinedPsalmody', 'botón externo de Salmodia');
forbidText(layout, 'groupCombinedStart', 'selector antiguo de inicios separados');
forbidText(activity, 'startActivity(', 'navegación a otra Activity desde la celebración combinada');
requireText(activity, 'CombinedMassComposer.compose', 'compositor continuo');
requireText(activity, 'loadDataWithBaseURL', 'carga del documento continuo');

// Orden/contenido mínimo acordado.
for (const required of [
  'Inicio de la celebración',
  'Salmodia de ',
  'Santa Misa',
  'Oración colecta',
  'Liturgia de la Palabra',
  'Homilía',
  'Profesión de fe',
  'Preces de ',
  'Liturgia eucarística',
  'Plegaria eucarística',
  'Rito de la Comunión',
  'Padre nuestro',
  'Cántico evangélico de ',
  'Oración después de la Comunión',
  'Rito de conclusión'
]) requireText(composer, required, `bloque ${required}`);

// Selectores inline requeridos.
for (const required of [
  'Niceno-constantinopolitano',
  'Apostólico',
  'setCreed(',
  'setPrayer(1)',
  'setPrayer(2)',
  'setPrayer(3)',
  'setPrayer(4)',
  'cycleBlockLanguage',
  'ESP/LAT'
]) requireText(composer, required, `selector ${required}`);

// La lectura diaria debe venir del repositorio del Leccionario y permanecer inline.
requireText(composer, 'MassReadingsRepository.read', 'lecturas del Leccionario inline');
forbidText(composer, 'MassReadingsActivity.class', 'apertura externa del Leccionario');
forbidText(composer, 'MissalActivity.class', 'apertura externa del Misal');
forbidText(composer, 'HoursReaderActivity.class', 'apertura externa de la Liturgia de las Horas');

if (failures.length) {
  console.error('Validación Misa + Hora: FALLÓ');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log('Validación Misa + Hora: OK — celebración integrada en una sola pantalla.');
