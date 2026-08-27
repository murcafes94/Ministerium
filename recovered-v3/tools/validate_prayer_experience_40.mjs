import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const expect = (condition, message) => {
  if (!condition) throw new Error(message);
};

const manifest = read('app/src/main/AndroidManifest.xml');
expect(manifest.includes('android:allowBackup="false"'),
  'Private prayer data must not enter automatic Android backups.');
expect(manifest.includes('android:fullBackupContent="false"'),
  'Full backup must remain explicitly disabled.');
expect(manifest.includes('android:usesCleartextTraffic="false"'),
  'Unencrypted HTTP traffic must remain disabled.');
expect(manifest.includes('<receiver android:name=".BootReceiver" android:exported="false">'),
  'BootReceiver must not be exported.');

const personal = read('app/src/main/java/com/fabri/ministerium/PersonalPrayersActivity.java');
const prayerReader = read('app/src/main/java/com/fabri/ministerium/PrayerReaderActivity.java');
const intentions = read('app/src/main/java/com/fabri/ministerium/PrayerIntentionsActivity.java');
const intentionsLayout = read('app/src/main/res/layout/activity_prayer_intentions.xml');
expect(personal.includes('open(prayers.get(position))')
    && personal.includes('Mantén pulsado para editar o eliminar'),
  'Personal prayer reading and editing modes must remain separate.');
expect(prayerReader.includes('EXTRA_PERSONAL_PRAYER_ID')
    && prayerReader.includes('EXTRA_DIRECT_TEXT')
    && prayerReader.includes('TextViewReaderChrome.bindMore'),
  'Private prayer sessions must use the complete reader experience.');
expect(intentions.includes('prayNow()')
    && intentions.includes('EXTRA_DIRECT_TEXT')
    && intentionsLayout.includes('android:id="@+id/btnPrayIntentions"'),
  'Intentions prayer mode is not fully wired.');

const settings = read('app/src/main/res/layout/activity_settings.xml');
expect(settings.includes('Tus datos permanecen contigo')
    && settings.includes('no los incluye en copias automáticas de Android'),
  'Settings must explain local-first privacy.');

const today = read('app/src/main/java/com/fabri/ministerium/HoursTodayActivity.java');
const todayLayout = read('app/src/main/res/layout/activity_hours_today.xml');
expect(today.includes('openMassReadings()')
    && today.includes('MassReadingsActivity.EXTRA_YEAR')
    && todayLayout.includes('android:id="@+id/btnMassReadings"'),
  'Today must preserve the selected date when opening Mass readings.');

const webChrome = read('app/src/main/java/com/fabri/ministerium/ReaderChrome.java');
const textChrome = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
const preferences = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
expect(webChrome.includes('attachAutoHideHeader')
    && webChrome.includes('final boolean[] hidden'),
  'Web readers must use stable auto-hide chrome.');
expect(textChrome.includes('final boolean[] hidden')
    && textChrome.includes('header.animate().cancel()'),
  'Text readers must suppress repeated header animations.');
expect(preferences.includes('maximumColumnWidthCss')
    && preferences.includes('return 780')
    && preferences.includes('return 880'),
  'Tablet reader measure must follow the selected margin.');

console.log('Ministerium 4.0 prayer, privacy, Today and reader contract OK');
