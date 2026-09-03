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
];

for (const [before, after] of replacements) {
  if (!source.includes(before)) throw new Error(`Falta contrato base: ${before}`);
  source = source.replace(before, after);
}

// 4.1 deliberately removed the old bilingual Hours reader and its visual
// alignment system. Keep semantic keys inside generated content as harmless
// metadata, but do not require synchronised readers, alignment cards or spacer
// CSS at runtime. Spanish and Latin Hours are independent readers now.
const obsoleteStart = source.indexOf('// Divinum Officium-inspired alignment, final paragraph-card strategy.');
const obsoleteEnd = source.indexOf('// Readium/FolioReader-style EPUB robustness and current Bible/index contract.');
if (obsoleteStart < 0 || obsoleteEnd < 0 || obsoleteEnd <= obsoleteStart) {
  throw new Error('No se encontró el bloque heredado de alineación bilingüe 3.1');
}
const independentHoursContract = `// 4.1 independent Hours readers.\nrequireText(mainActivity, 'HoursTodayActivity.class', 'entrada Liturgia de las Horas ES');\nrequireText(mainActivity, 'LatinHoursActivity.class', 'entrada Liturgia Horarum LAT');\nrequireText(latinReader, '\"Liturgia\", true, false', 'TTS LAT desactivado');\nforbidText(readerPrefs, 'ministerium-align-spacer', 'espaciador visual bilingüe heredado');\nforbidText(readerPrefs, 'data-ministerium-align-key', 'estilos runtime de alineación bilingüe');\n\n`;
source = source.slice(0, obsoleteStart) + independentHoursContract + source.slice(obsoleteEnd);

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
