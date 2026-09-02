import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const expect = (ok, msg) => { if (!ok) throw new Error(msg); };

const saint = read('app/src/main/java/com/fabri/ministerium/SaintOfficeRepository.java');
expect(saint.includes('EpubTocEntry saintEntry = tocEntry(context, saint)')
    && saint.includes('saintSection(context, saint, saintEntry'),
  'Santoral must be delimited from the selected TOC entry.');
expect(saint.includes('properPrayer(saintSection)')
    && saint.includes('properGospelAntiphon(saintSection'),
  'Saint propers must be read from the isolated saint section.');
expect(!saint.includes('sigil_toc_id'),
  'Santoral must not fall back to generic Sigil ids that can cross saint boundaries.');

const calendar = read('app/src/main/assets/calendar/gcatholic-2026-es-EC.ics');
expect(calendar.includes('DTSTART;VALUE=DATE:20260828')
    && calendar.includes('San Agustín\\, obispo y doctor de la Iglesia'),
  'Ecuador calendar regression: San Agustín 28 Aug 2026 is missing.');

const resolver = read('app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java');
expect(resolver.includes('primaryEvent') && resolver.includes('isOptionalMemorial')
    && resolver.includes('easterSunday') && resolver.includes('adventStart')
    && resolver.includes('baptismOfLord'),
  'Liturgical date precedence/season resolution contract is incomplete.');

const missal = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const sourceResolver = read('app/src/main/java/com/fabri/ministerium/LiturgicalSourceResolver.java');
expect(missal.includes('creedRequired') && missal.includes('hasRequiredSaint'),
  'Missal solemnity/saint decision hooks are missing.');
expect(sourceResolver.includes('SOURCE_LOCAL_ECUADOR')
    && sourceResolver.includes('SOURCE_MERCABA_VERIFIED')
    && sourceResolver.includes('SOURCE_GUADALAJARA'),
  'Missal source priority contract is incomplete.');

const bible = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
expect(bible.includes('onWindowFocusChanged(boolean hasFocus)')
    && bible.includes('ReadingMarkerUtils.injectHighlights(this, webView, sourceKey())'),
  'Bible must refresh tolerant note/bookmark anchors immediately after dialogs/editors.');

console.log('Liturgical/Bible regression integrity checks OK');
