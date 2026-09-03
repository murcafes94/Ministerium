import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Alias de transición: build_local_windows.ps1 todavía invoca validate_release_40.mjs.
// En feature/ministerium-4.1 se delega al contrato real 4.1 para no romper la
// compilación local mientras se conserva compatibilidad con el script heredado.
const tools = path.dirname(fileURLToPath(import.meta.url));
const result = spawnSync(process.execPath, [path.join(tools, 'validate_release_41.mjs')], {
  cwd: path.resolve(tools, '..'),
  stdio: 'inherit',
});
if (result.error) throw result.error;
process.exit(result.status ?? 0);
