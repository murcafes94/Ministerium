import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const tools = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(tools, '..');
const sourcePath = path.join(tools, 'validate_stabilization_31.mjs');
const generatedPath = path.join(tools, '.validate_stabilization_41.generated.mjs');

let source = fs.readFileSync(sourcePath, 'utf8');
const replacements = [
  ["requireText(build, 'versionCode 32', 'versionCode 3.1.1');",
   "requireText(build, 'versionCode 41', 'versionCode 4.1.0');"],
  ["requireText(build, \"versionName '3.1.1'\", 'versionName 3.1.1');",
   "requireText(build, \"versionName '4.1.0'\", 'versionName 4.1.0');"],
  ["const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');\n", ""],
  [`// Divinum Officium-inspired alignment, final paragraph-card strategy.\nrequireText(hoursBuilder, 'data-ministerium-align-key', 'claves de alineación ES');\nrequireText(latinHoursBuilder, 'data-ministerium-align-key', 'claves de alineación LAT');\nrequireText(hoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación ES');\nrequireText(latinHoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación LAT');\nrequireText(cleanHours, '.ready-3.1.1-align4', 'refresco de paquete ES alineado');\nrequireText(bilingual, 'semanticSynchronize', 'sincronización por ancla');\nrequireText(bilingual, 'data-ministerium-align-key', 'búsqueda de ancla equivalente');\nrequireText(bilingual, 'applyParagraphCards', 'alineación visual por párrafos');\nrequireText(bilingual, 'ministerium-align-card', 'tarjetas de párrafos bilingües');\nforbidText(bilingual, 'sourceY / (float) sourceRange', 'fallback porcentual antiguo');\nrequireText(bilingual, '\"Liturgia\", true, false', 'TTS LAT desactivado en bilingüe');\nrequireText(latinReader, '\"Liturgia\", true, false', 'TTS LAT desactivado');\nrequireText(readerPrefs, 'data-ministerium-align-key', 'estilos de alineación semántica');`,
`// Hours 4.1 contract: Spanish and Latin are separate navigation/readers.\nrequireText(hoursBuilder, 'data-ministerium-align-key', 'claves semánticas ES');\nrequireText(latinHoursBuilder, 'data-ministerium-align-key', 'claves semánticas LAT');\nrequireText(hoursBuilder, 'shared semantic keys v1', 'manifiesto ES');\nrequireText(latinHoursBuilder, 'shared semantic keys v1', 'manifiesto LAT');\nrequireText(cleanHours, '.ready-3.1.1-align4', 'refresco de paquete ES');\nrequireText(mainActivity, 'new Intent(this, LatinHoursActivity.class)', 'entrada independiente a Liturgia Horarum');\nrequireText(mainActivity, 'HoursTodayActivity.class', 'entrada independiente a Liturgia de las Horas ES');\nforbidText(mainActivity, 'BilingualHoursActivity.class', 'entrada bilingüe obsoleta');\nrequireText(latinReader, '\"Liturgia\", true, false', 'TTS LAT desactivado');\nrequireText(readerPrefs, 'data-ministerium-align-key', 'estilos semánticos compartidos');\nfor (const obsolete of [\n  'app/src/main/java/com/fabri/ministerium/BilingualHoursActivity.java',\n  'app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java',\n  'app/src/main/res/layout/activity_bilingual_hours.xml',\n  'app/src/main/res/layout/activity_bilingual_reader.xml'\n]) { if (fs.existsSync(obsolete)) failures.push('No debe existir arquitectura bilingüe obsoleta: ' + obsolete); }\nconst manifest41 = read('app/src/main/AndroidManifest.xml');\nforbidText(manifest41, '.BilingualHoursActivity', 'Activity bilingüe en manifest');\nforbidText(manifest41, '.BilingualHoursReaderActivity', 'lector bilingüe en manifest');`]
];

for (const [before, after] of replacements) {
  if (!source.includes(before)) throw new Error(`Falta contrato base: ${before}`);
  source = source.replace(before, after);
}

fs.writeFileSync(generatedPath, source, 'utf8');
try {
  const result = spawnSync(process.execPath, [generatedPath], {
    cwd: root,
    stdio: 'inherit',
  });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status ?? 1);
  console.log('Ministerium 4.1 stabilization baseline OK');
} finally {
  try { fs.unlinkSync(generatedPath); } catch {}
}
