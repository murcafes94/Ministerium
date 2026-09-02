import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const expect = (condition, message) => { if (!condition) throw new Error(message); };

const gradle = read('app/build.gradle');
expect(gradle.includes('versionCode 41') && gradle.includes("versionName '4.1.0'"),
  'Version must remain 4.1.0/41.');
expect(gradle.includes('compileSdkVersion 30') && gradle.includes('targetSdkVersion 30'),
  'SDK 30 toolchain drifted.');

const readerChrome = read('app/src/main/java/com/fabri/ministerium/ReaderChrome.java');
const textViewChrome = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
expect(readerChrome.includes('keepHeaderStatic') && readerChrome.includes('setOnScrollChangeListener(null)')
    && !readerChrome.includes('translationY(-height)'), 'WebView header must remain static.');
expect(textViewChrome.includes('setOnScrollChangeListener(null)')
    && !textViewChrome.includes('final boolean[] hidden'), 'TextView header must remain static.');

const hoursReader = read('app/src/main/java/com/fabri/ministerium/HoursReaderActivity.java');
expect(hoursReader.includes('Benedictus') && hoursReader.includes('Magníficat')
    && hoursReader.includes('Bendito sea el Señor, Dios de Israel')
    && hoursReader.includes('Proclama mi alma la grandeza del Señor'),
  'Spanish Gospel canticles must remain complete.');

const latinHours = read('app/src/main/java/com/fabri/ministerium/LatinHoursActivity.java');
const latinReader = read('app/src/main/java/com/fabri/ministerium/LatinHoursReaderActivity.java');
expect(latinHours.includes('LatinHoursReaderActivity.class') && latinHours.includes('Liturgia Horarum'),
  'Latin Hours runtime is not wired.');
expect(latinReader.includes('ReaderPreferences.apply') && latinReader.includes('LiturgicalWebStyle.apply'),
  'Latin Hours must share reader styling.');

const ritualFormatter = read('app/src/main/java/com/fabri/ministerium/RitualTextFormatter.java');
const ritualRepository = read('app/src/main/java/com/fabri/ministerium/RitualRepository.java');
expect(ritualRepository.includes('COMMON_BLESSINGS_ID = "blessings"')
    && ritualRepository.includes('Bendicional') && ritualRepository.includes('blessing_family.txt'),
  'Bendicional is not routed through the structured ritual repository.');
expect(ritualFormatter.includes('isMinisterSpeech') && ritualFormatter.includes('isResponse')
    && ritualFormatter.includes('isRubric') && ritualFormatter.includes('celebrantBg')
    && ritualFormatter.includes('responseBg') && ritualFormatter.includes('BackgroundColorSpan')
    && ritualFormatter.includes('ForegroundColorSpan') && ritualFormatter.includes('LeadingMarginSpan'),
  'Ritual/Bendicional must visually distinguish celebrant, assembly and rubrics.');
expect(ritualFormatter.includes('joinBrokenProseContinuations')
    && ritualFormatter.includes('previous.endsWith(",")')
    && ritualFormatter.includes('previous.endsWith(";")')
    && ritualFormatter.includes('previous.endsWith(":")'),
  'Ritual/Bendicional prose continuation normalization is missing.');

const bibleReader = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
const markerUtils = read('app/src/main/java/com/fabri/ministerium/ReadingMarkerUtils.java');
expect(bibleReader.includes('ReadingMarkerUtils.injectHighlights')
    && bibleReader.includes('UniversalSelectionMenu.restoreHighlights'),
  'Bible study marker restore path is incomplete.');
expect(markerUtils.includes('StudyEntry.NOTE') && markerUtils.includes('StudyEntry.BOOKMARK')
    && markerUtils.includes('anchorText'), 'Tolerant Bible note/bookmark anchors are missing.');

const missalReader = read('app/src/main/java/com/fabri/ministerium/MissalSectionReaderActivity.java');
const missalDocument = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const missalGuard = read('app/src/main/java/com/fabri/ministerium/MissalLanguageGuard.java');
expect(missalReader.includes('LiturgicalDayCache.prepare')
    && missalReader.includes('LiturgicalDayCache.prefetch'),
  'Missal day cache/prefetch architecture is missing.');
expect(missalDocument.includes('String lang = "la".equals(language) ? "la" : "es"')
    && !missalDocument.includes('parallel-unit'), 'Missal must remain single-language.');
expect(missalReader.includes('MercabaMissalFallback.apply')
    && missalGuard.includes('Spanish proper as a fallback'),
  'Controlled Missal fallback policy is missing.');

const sourceResolver = read('app/src/main/java/com/fabri/ministerium/LiturgicalSourceResolver.java');
const dayCache = read('app/src/main/java/com/fabri/ministerium/LiturgicalDayCache.java');
expect(sourceResolver.includes('SOURCE_LOCAL_ECUADOR')
    && sourceResolver.includes('SOURCE_MERCABA_VERIFIED')
    && sourceResolver.includes('SOURCE_GUADALAJARA'),
  'Central liturgical source hierarchy is incomplete.');
expect(dayCache.includes('LinkedHashMap') && dayCache.includes('prefetch'),
  'Liturgical day cache is incomplete.');

const runtime = fs.readdirSync('app/src/main/java/com/fabri/ministerium')
  .filter(name => name.endsWith('.java'))
  .map(name => read(`app/src/main/java/com/fabri/ministerium/${name}`)).join('\n');
for (const forbidden of ['AnythingLLM', 'WhisperModel', 'whisper.cpp', 'SpeechRecognizer.startListening']) {
  expect(!runtime.includes(forbidden), `4.1 must not implement AI/dictation yet: ${forbidden}`);
}

await import('./validate_liturgical_integrity_41.mjs');
console.log('Ministerium 4.1 release contract OK');
