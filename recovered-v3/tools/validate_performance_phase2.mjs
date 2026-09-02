import fs from 'node:fs';
import path from 'node:path';
const root = process.cwd();
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const expect = (x, m) => { if (!x) throw new Error(m); };

const epub = read('app/src/main/java/com/fabri/ministerium/EpubUtils.java');
expect(epub.includes('TOC_CACHE') && epub.includes('SAINTS_BY_MONTH_CACHE')
  && epub.includes('buildSaintMonth'), 'Santoral month cache missing.');

const canonSearch = read('app/src/main/java/com/fabri/ministerium/CanonSearchRepository.java');
const content = read('app/src/main/java/com/fabri/ministerium/ContentRepository.java');
const search = read('app/src/main/java/com/fabri/ministerium/SearchActivity.java');
expect(canonSearch.includes('Commentary is deliberately excluded')
  && canonSearch.includes('canon-text/canons-%02d.html'), 'Canon-only lazy search missing.');
expect(content.includes('CanonSearchRepository.search')
  && search.includes('result.isCanon()')
  && search.includes('CanonLawActivity.EXTRA_CANON'), 'Canon search is not wired to the canon reader.');
expect(!canonSearch.includes('canon-comments/'), 'Canon search must never index commentary assets.');

const cleanup = read('app/src/main/java/com/fabri/ministerium/WebViewCleanup.java');
expect(cleanup.includes('stopLoading') && cleanup.includes('about:blank')
  && cleanup.includes('setWebViewClient(null)') && cleanup.includes('destroy()'),
  'Shared WebView teardown is incomplete.');
for (const name of ['CanonLawActivity.java','HoursReaderActivity.java','LatinHoursReaderActivity.java',
  'MassReadingReaderActivity.java','ComplineReaderActivity.java']) {
  expect(read(`app/src/main/java/com/fabri/ministerium/${name}`).includes('WebViewCleanup.destroy'),
    `${name} does not use shared WebView cleanup.`);
}
console.log('Ministerium performance phase 2 contract OK');
