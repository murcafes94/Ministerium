import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const expect = (condition, message) => {
  if (!condition) throw new Error(message);
};

const gradle = read('app/build.gradle');
expect(gradle.includes('versionCode 40'), 'Version code must be 40.');
expect(gradle.includes("versionName '4.0.0'"), 'Version name must be 4.0.0.');
expect(gradle.includes('compileSdkVersion 30')
    && gradle.includes('targetSdkVersion 30'),
  'The constrained SDK 30 toolchain must not drift.');

const packageManifest = JSON.parse(read('app/src/main/assets/package-manifest.json'));
expect(packageManifest.app.versionName === '4.0.0'
    && packageManifest.app.versionCode === 40,
  'Package manifest app version is not 4.0.0/40.');
const magisterium = packageManifest.packages.find(item => item.id === 'magisterium-search');
expect(magisterium
    && magisterium.asset === 'magisterium-index.tsv'
    && magisterium.verification === 'generated-build',
  'Magisterium search package is missing from the package manifest.');

const updates = read('app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java');
expect(updates.includes('Ministerium 4.0.0')
    && updates.includes('changelog-4.0.0.txt'),
  'Update center must show the 4.0.0 changelog.');
expect(fs.existsSync(path.join(root, 'app/src/main/assets/changelog-4.0.0.txt')),
  'Missing in-app 4.0.0 changelog.');

const workflow = read('../.github/workflows/android-debug.yml');
expect(workflow.includes('feature/ministerium-4.0')
    && workflow.includes('Ministerium-4.0.0-debug')
    && workflow.includes('build_magisterium_index_40.py')
    && workflow.includes('validate_release_40.mjs'),
  '4.0 debug workflow is incomplete.');
const local = read('tools/build_local_windows.ps1');
expect(local.includes('Ministerium 4.0 - compilacion local Windows')
    && local.includes('build_magisterium_index_40.py')
    && local.includes('validate_release_40.mjs'),
  'Windows 4.0 build contract is incomplete.');

const readme = read('README.md');
expect(readme.includes('# Ministerium 4.0')
    && readme.includes('Gradle 6.7.1')
    && readme.includes('JDK 11'),
  'Project README does not describe the 4.0 constrained build.');

console.log('Ministerium 4.0 release metadata and local build contract OK');
