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
    && workflow.includes('validate_stabilization_40.mjs')
    && workflow.includes('validate_release_40.mjs'),
  '4.0 debug workflow is incomplete.');
expect(!workflow.includes('missal-reference-catalog.json\\n'),
  'Debug workflow contains a literal escaped newline in artifact paths.');
expect(workflow.includes("ElementTree.parse('app/src/main/AndroidManifest.xml')"),
  'Debug workflow must parse AndroidManifest.xml before Gradle.');

const local = read('tools/build_local_windows.ps1');
expect(local.includes('Ministerium 4.0 - compilacion local Windows')
    && local.includes('build_magisterium_index_40.py')
    && local.includes('validate_stabilization_40.mjs')
    && local.includes('validate_release_40.mjs'),
  'Windows 4.0 build contract is incomplete.');
expect(local.includes('Test-PythonImports')
    && local.includes('beautifulsoup4')
    && local.includes('Gradle/JVM 11: OK')
    && local.includes('android-studio-portable'),
  'Windows build must recover Python dependencies and support the tested portable SDK/JDK flow.');

expect(fs.existsSync(path.join(root, 'tools/validate_stabilization_40.mjs')),
  'Missing 4.0 adapter for the 3.1.1 stabilization baseline.');
const baseline40 = read('tools/validate_stabilization_40.mjs');
expect(baseline40.includes("versionCode 40")
    && baseline40.includes("versionName '4.0.0'"),
  '4.0 stabilization adapter does not rewrite the legacy version assertions.');

const main = read('app/src/main/java/com/fabri/ministerium/MainActivity.java');
const latinHours = read('app/src/main/java/com/fabri/ministerium/LatinHoursActivity.java');
const intermediate = read('app/src/main/java/com/fabri/ministerium/IntermediateHourResolver.java');
expect(main.includes('new Intent(this, LatinHoursActivity.class)')
    && !main.includes('new Intent(this, BilingualHoursActivity.class)')
    && main.includes('Liturgia de las Horas en latín'),
  'The secondary Hours entry must be Latin-only and must not open the bilingual reader.');
expect(latinHours.includes('LatinHoursReaderActivity.class')
    && latinHours.includes('Liturgia Horarum'),
  'Latin Hours runtime is not wired correctly.');
expect(intermediate.includes('hymnRangeMatches')
    && intermediate.includes('ordinaryWeek'),
  'Intermediate Hours must resolve the I-XVII / XVIII-XXXIV hymn range.');

const readings = read('app/src/main/java/com/fabri/ministerium/MassReadingsRepository.java');
const propers = read('app/src/main/java/com/fabri/ministerium/DailyMassProperRepository.java');
expect(readings.includes('DailyMassProperRepository.cacheFromSourceHtml')
    && readings.includes('String rawHtml = download(sourceUrl(date))'),
  'Lectionary sync must reuse its download to cache Mass propers.');
expect(propers.includes('cacheFromSourceHtml')
    && propers.includes('Charset.forName')
    && propers.includes('actividad diocesana'),
  'Daily Mass propers need charset-aware parsing and a safe page footer boundary.');

const readme = read('README.md');
expect(readme.includes('# Ministerium 4.0')
    && readme.includes('Gradle 6.7.1')
    && readme.includes('JDK 11'),
  'Project README does not describe the 4.0 constrained build.');

const localGuide = read('../ANDROID-STUDIO-LOCAL.md');
expect(localGuide.includes('feature/ministerium-4.0')
    && localGuide.includes('Ministerium-4.0.0-prueba.apk')
    && !localGuide.includes('feature/ministerium-3.1.1-final-fixes'),
  'Android Studio local guide is stale.');

const testing = JSON.parse(read('../distribution/manifests/testing.json'));
expect(testing.channel === 'testing'
    && testing.app.version === '4.0.0-test'
    && testing.app.downloadUrl === null
    && testing.app.sha256 === null,
  'Testing distribution manifest must describe 4.0.0-test without inventing an unpublished APK URL/hash.');

console.log('Ministerium 4.0 release metadata and local build contract OK');
