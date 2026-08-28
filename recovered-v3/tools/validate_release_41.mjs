import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const expect = (condition, message) => { if (!condition) throw new Error(message); };

const gradle = read('app/build.gradle');
expect(gradle.includes('versionCode 41'), 'Version code must be 41.');
expect(gradle.includes("versionName '4.1.0'"), 'Version name must be 4.1.0.');
expect(gradle.includes('compileSdkVersion 30') && gradle.includes('targetSdkVersion 30'),
  'The constrained SDK 30 toolchain must not drift.');

const manifest = JSON.parse(read('app/src/main/assets/package-manifest.json'));
expect(manifest.app.versionName === '4.1.0' && manifest.app.versionCode === 41,
  'Package manifest app version is not 4.1.0/41.');

const updates = read('app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java');
expect(updates.includes('Ministerium 4.1.0') && updates.includes('changelog-4.1.0.txt'),
  'Update center must show the 4.1.0 changelog.');
expect(fs.existsSync(path.join(root, 'app/src/main/assets/changelog-4.1.0.txt')),
  'Missing in-app 4.1.0 changelog.');

const exportSource = read('app/src/main/java/com/fabri/ministerium/StudyExport.java');
const desk = read('app/src/main/java/com/fabri/ministerium/StudyDeskActivity.java');
expect(exportSource.includes('public static byte[] obsidian')
    && exportSource.includes('app_version:')
    && exportSource.includes('ministerium-anchor:'),
  'Obsidian export contract is incomplete.');
expect(desk.includes('Obsidian (.md)') && desk.includes('StudyExport.obsidian'),
  'Mi estudio does not expose the Obsidian export.');

const runtime = fs.readdirSync('app/src/main/java/com/fabri/ministerium')
  .filter(name => name.endsWith('.java'))
  .map(name => read(`app/src/main/java/com/fabri/ministerium/${name}`)).join('\n');
for (const forbidden of ['AnythingLLM', 'WhisperModel', 'whisper.cpp', 'SpeechRecognizer.startListening']) {
  expect(!runtime.includes(forbidden), `4.1 must not implement AI/dictation yet: ${forbidden}`);
}

const workflow = read('../.github/workflows/android-debug.yml');
expect(workflow.includes('feature/ministerium-4.1')
    && workflow.includes('Ministerium-4.1.0-debug')
    && workflow.includes('validate_stabilization_41.mjs')
    && workflow.includes('validate_release_41.mjs'),
  '4.1 debug workflow is incomplete.');

const local = read('tools/build_local_windows.ps1');
expect(local.includes('Ministerium 4.1 - compilacion local Windows')
    && local.includes('validate_stabilization_41.mjs')
    && local.includes('validate_release_41.mjs'),
  'Windows 4.1 build contract is incomplete.');

console.log('Ministerium 4.1 initial release contract OK');
