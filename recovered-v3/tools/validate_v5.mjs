import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const expect = (condition, message) => { if (!condition) throw new Error(message); };

const manifest = read('app/src/main/AndroidManifest.xml');
const mainV5 = read('app/src/main/java/com/fabri/ministerium/MainActivityV5.kt');
const hoursV5 = read('app/src/main/java/com/fabri/ministerium/HoursV5Activity.java');
const missalV5 = read('app/src/main/java/com/fabri/ministerium/MissalV5SectionActivity.java');
const missalRules = read('app/src/main/java/com/fabri/ministerium/MissalDisplayRules.kt');
const themed = read('app/src/main/java/com/fabri/ministerium/ThemedActivity.java');
const epub = read('app/src/main/java/com/fabri/ministerium/EpubUtils.java');
const magisterium = read('app/src/main/java/com/fabri/ministerium/MagisteriumActivity.java');
const updateCenter = read('app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java');
const build = read('app/build.gradle');
const packageManifest = JSON.parse(read('app/src/main/assets/package-manifest.json'));

expect(mainV5.includes('class MainActivityV5'), 'MainActivityV5 is missing.');
expect(/android:name="\.MainActivityV5"[\s\S]*android.intent.action.MAIN[\s\S]*android.intent.category.LAUNCHER/.test(manifest),
  'MainActivityV5 must remain the launcher.');
expect(!/android:name="\.MainActivity"[\s\S]{0,300}android.intent.category.LAUNCHER/.test(manifest),
  'Legacy MainActivity must not regain the launcher intent filter.');

const code = Number((build.match(/^\s*versionCode\s+(\d+)\s*$/m) || [])[1] || 0);
const version = (build.match(/^\s*versionName\s+'([^']+)'\s*$/m) || [])[1] || '';
expect(code >= 50 && /^5\./.test(version), 'Ministerium 5 preview version metadata is not configured.');
expect(packageManifest.app?.versionCode === code && packageManifest.app?.versionName === version,
  'Gradle and package-manifest app versions must match.');
expect(fs.existsSync(path.join(root, `app/src/main/assets/changelog-${version}.txt`)),
  'The current V5 changelog asset is missing.');
expect(updateCenter.includes('"changelog-" + assetVersion + ".txt"'),
  'Update Center must resolve the changelog from the current build version.');

expect(mainV5.includes('PrayerFocusController.recoverStaleSession')
    && mainV5.includes('PrayerReminderScheduler.restore')
    && mainV5.includes('GospelReminderScheduler.restore')
    && mainV5.includes('BiblePlanReminderScheduler.restore'),
  'V5 launcher must restore focus and reminder state.');
for (const required of [
  'SearchActivity', 'FavoritesActivity', 'DevotionalHubActivity', 'BasicPrayersActivity',
  'MyStudyActivity', 'LatinHoursActivity', 'MassReadingsActivity', 'LiturgicalCalendarActivity',
  'RitualCatalogActivity', 'ContinueReadingStore'
]) {
  expect(mainV5.includes(required), `V5 launcher lost access to ${required}.`);
}

expect(hoursV5.includes('HoursV5CommonPolicy.filter'),
  'V5 Hours must filter incompatible common-office choices before displaying them.');
expect(missalV5.includes('MissalDisplayRules.resolve(this, selectedDate, celebration)'),
  'V5 Missal must use the calendar-backed display rules.');
expect(missalV5.includes('getAllowEucharisticPrayerIV()') && !missalV5.includes('Contenido propio pendiente'),
  'V5 Missal still has simulated content or unrestricted Eucharistic Prayer IV.');
expect(missalRules.includes('LiturgicalCalendarRepository.eventsFor')
    && missalRules.includes('allowEucharisticPrayerIV'),
  'V5 Missal rules are not backed by the liturgical calendar.');

expect(epub.includes('CleanHoursAssets.isAvailable')
    && epub.includes('CleanHoursAssets.ensureExtracted'),
  'Runtime Hours must support the clean package after source EPUB removal.');
expect(themed.includes('HoursV5ReaderActivity')
    && themed.includes('MissalV5SectionActivity')
    && themed.includes('HoursV5ComplineActivity'),
  'Prayer focus is not wired to all V5 liturgical readers.');
expect(!magisterium.includes('new Intent(this, MainActivity.class)'),
  'Magisterium must not jump from V5 back into the legacy launcher.');

console.log('Ministerium 5 runtime contract OK');
