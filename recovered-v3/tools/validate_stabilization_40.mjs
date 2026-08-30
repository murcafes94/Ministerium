import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Alias de transición: el compilador local heredado de 4.0 todavía invoca este
// nombre. En la rama 4.1 delega en el contrato de estabilización 4.1.
const tools = path.dirname(fileURLToPath(import.meta.url));
const result = spawnSync(process.execPath, [path.join(tools, 'validate_stabilization_41.mjs')], {
  cwd: path.resolve(tools, '..'),
  stdio: 'inherit',
});
if (result.error) throw result.error;
process.exit(result.status ?? 0);
