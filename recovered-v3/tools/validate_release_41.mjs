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

const pagination = read('app/src/main/java/com/fabri/ministerium/ReaderPagination.java');
const readerChrome = read('app/src/main/java/com/fabri/ministerium/ReaderChrome.java');
expect(pagination.includes('public static final String SCROLL = "scroll"')
    && pagination.includes('public static final String PAGE = "page"')
    && pagination.includes('column-width:calc(100vw')
    && pagination.includes('__ministeriumPageStep')
    && pagination.includes('category.contains("biblia")')
    && pagination.includes('category.contains("documentos")'),
  'Scroll/page reader engine is incomplete.');
expect(pagination.includes('localStorage.setItem(storageKey')
    && pagination.includes('localStorage.getItem(storageKey'),
  'Page mode does not persist and restore its per-document page index.');
expect(!pagination.includes('category.contains("liturgia")'),
  'Liturgical readers must remain outside 4.1 page mode.');
expect(readerChrome.includes('Modo de lectura · ')
    && readerChrome.includes('ReaderPagination.step')
    && readerChrome.includes('ReaderPagination.arm')
    && readerChrome.includes('setDomStorageEnabled(true)'),
  'Reader chrome does not expose, drive or persist page mode correctly.');

const main = read('app/src/main/java/com/fabri/ministerium/MainActivity.java');
const latinHours = read('app/src/main/java/com/fabri/ministerium/LatinHoursActivity.java');
expect(main.includes('new Intent(this, LatinHoursActivity.class)')
    && !main.includes('new Intent(this, BilingualHoursActivity.class)')
    && main.includes('Liturgia de las Horas en latín'),
  '4.1 must expose the Latin-only Hours entry and must not open the bilingual reader.');
expect(latinHours.includes('LatinHoursReaderActivity.class')
    && latinHours.includes('Liturgia Horarum'),
  'Latin Hours runtime is not wired correctly.');

const secondary = read('tools/check_secondary_liturgy_sources.py');
expect(secondary.includes('LiturgicalCalendarAPI')
    && secondary.includes('/api/v5/calendar/roman/')
    && secondary.includes('local Ecuador calendar remains authoritative'),
  'Structured secondary calendar validation is missing.');

const colors = read('app/src/main/res/values/colors.xml');
const darkColors = read('app/src/main/res/values-night/colors.xml');
const dimens = read('app/src/main/res/values/dimens.xml');
const tabletDimens = read('app/src/main/res/values-sw600dp/dimens.xml');
const styles = read('app/src/main/res/values/styles.xml');
const card = read('app/src/main/res/drawable/bg_card.xml');
const visualPalette = read('app/src/main/java/com/fabri/ministerium/ReaderVisualPalette.java');
const readerPreferences = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
for (const token of ['ministerium_surface', 'ministerium_text_primary',
  'ministerium_text_secondary', 'ministerium_accent', 'ministerium_divider']) {
  expect(colors.includes(`name="${token}"`) && darkColors.includes(`name="${token}"`),
    `Missing light/dark semantic color token: ${token}`);
}
for (const token of ['ministerium_card_radius', 'ministerium_card_horizontal_margin',
  'ministerium_card_padding']) {
  expect(dimens.includes(`name="${token}"`) && tabletDimens.includes(`name="${token}"`),
    `Missing phone/tablet dimension token: ${token}`);
}
expect(styles.includes('@dimen/ministerium_card_horizontal_margin')
    && styles.includes('@color/ministerium_text_primary')
    && card.includes('@dimen/ministerium_card_radius')
    && card.includes('@color/ministerium_surface'),
  'Shared card styles are not consuming the 4.1 visual tokens.');
expect(visualPalette.includes('ThemeUtils.SEPIA')
    && visualPalette.includes('public final String background')
    && visualPalette.includes('public final String accent')
    && readerPreferences.includes('ReaderVisualPalette.from(context)'),
  'WebView readers are not connected to the shared 4.1 palette including sepia.');

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

// El compilador local heredado de 4.0 puede seguir llamando temporalmente a los
// nombres validate_*_40; en esta rama esos archivos delegan en los contratos 4.1.
const local = read('tools/build_local_windows.ps1');
const stabilizationAlias = read('tools/validate_stabilization_40.mjs');
const releaseAlias = read('tools/validate_release_40.mjs');
expect(local.includes('validate_stabilization_40.mjs')
    && local.includes('validate_release_40.mjs')
    && stabilizationAlias.includes('validate_stabilization_41.mjs')
    && releaseAlias.includes('validate_release_41.mjs'),
  'Windows local build is not bridged to the 4.1 validators.');

console.log('Ministerium 4.1 initial release contract OK');
