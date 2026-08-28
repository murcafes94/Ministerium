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
