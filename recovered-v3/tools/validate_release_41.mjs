import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const expect = (condition, message) => { if (!condition) throw new Error(message); };

// 4.1 release contract: preserve the stabilized app architecture and the
// intentional Spanish/Latin separation while allowing Spanish fallback text
// where a Latin proper is not locally available.
const main = read('app/src/main/java/com/fabri/ministerium/MainActivity.java');
expect(main.includes('HoursTodayActivity.class') && main.includes('LatinHoursActivity.class')
    && !main.includes('BilingualHoursActivity.class'),
  'Spanish and Latin Hours must remain separate entry points.');

const readerPrefs = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
expect(!readerPrefs.includes('ministerium-align-spacer')
    && !readerPrefs.includes('[data-ministerium-align-key]'),
  'Obsolete bilingual alignment styling must not return.');

const webChrome = read('app/src/main/java/com/fabri/ministerium/ReaderChrome.java');
const textChrome = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
expect(webChrome.includes('keepHeaderStatic')
    && webChrome.includes('header.setVisibility(View.VISIBLE)')
    && webChrome.includes('header.setAlpha(1f)')
    && webChrome.includes('header.setTranslationY(0f)')
    && webChrome.includes('webView.setOnScrollChangeListener(null)')
    && !webChrome.includes('attachAutoHideHeader'),
  'Web readers must keep the top header static.');
expect(textChrome.includes('header.setVisibility(View.VISIBLE)')
    && textChrome.includes('scroll.setOnScrollChangeListener(null)'),
  'Text readers must keep the top header static.');

const missalActivity = read('app/src/main/java/com/fabri/ministerium/MissalActivity.java');
const missalReader = read('app/src/main/java/com/fabri/ministerium/MissalSectionReaderActivity.java');
const missalDocument = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const missalAlternatives = read('app/src/main/java/com/fabri/ministerium/MissalAlternativeOptions31.java');
expect(missalActivity.includes('String[] languages = {"Español", "Latín"}')
    && missalDocument.includes('String lang = "la".equals(language) ? "la" : "es"')
    && !missalDocument.includes('parallel-unit'),
  'Missal must remain a single Spanish OR Latin document, never side-by-side.');
expect(missalReader.includes('MissalAlternativeOptions31.inject')
    && missalAlternatives.includes('ministerium-alt-button'),
  'Missal alternatives must remain selectable.');
expect(missalDocument.includes('Propio del día · fuente española'),
  'Latin Missal must retain the accepted Spanish fallback for missing daily propers.');

const ritualFormatter = read('app/src/main/java/com/fabri/ministerium/RitualTextFormatter.java');
const ritualRepository = read('app/src/main/java/com/fabri/ministerium/RitualRepository.java');
expect(ritualRepository.includes('COMMON_BLESSINGS_ID = "blessings"')
    && ritualRepository.includes('Bendicional')
    && ritualRepository.includes('blessing_family.txt'),
  'Bendicional is not routed through the structured ritual repository.');
expect(ritualFormatter.includes('isMinisterSpeech')
    && ritualFormatter.includes('isResponse')
    && ritualFormatter.includes('isRubric')
    && ritualFormatter.includes('celebrantBg')
    && ritualFormatter.includes('responseBg')
    && ritualFormatter.includes('BackgroundColorSpan(responseBg)')
    && ritualFormatter.includes('BackgroundColorSpan(celebrantBg)')
    && ritualFormatter.includes('StyleSpan(Typeface.ITALIC)'),
  'Ritual/Bendicional must visually distinguish celebrant, assembly and rubrics.');

const ritualReader = read('app/src/main/java/com/fabri/ministerium/RitualReaderActivity.java');
expect(ritualReader.includes('RitualTextFormatter.format'),
  'Ritual reader must apply the structured formatter.');

console.log('Ministerium 4.1 release contract OK');
