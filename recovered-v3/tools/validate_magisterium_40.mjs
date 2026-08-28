import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const expect = (condition, message) => {
  if (!condition) throw new Error(message);
};

const builder = read('tools/build_magisterium_index_40.py');
const repository = read('app/src/main/java/com/fabri/ministerium/MagisteriumIndexRepository.java');
const result = read('app/src/main/java/com/fabri/ministerium/SearchResult.java');
const search = read('app/src/main/java/com/fabri/ministerium/SearchActivity.java');
const magisterium = read('app/src/main/java/com/fabri/ministerium/MagisteriumActivity.java');
const contentRepository = read('app/src/main/java/com/fabri/ministerium/ContentRepository.java');
const reader = read('app/src/main/java/com/fabri/ministerium/HoursReaderActivity.java');
const indexPath = path.join(root, 'app/src/main/assets/magisterium-index.tsv');

for (const source of [
  'Concilio-Vaticano-II.epub',
  'Catecismo-Iglesia-Catolica.epub',
  'Compendio-Catecismo.epub',
  'Compendio-Doctrina-Social.epub',
]) {
  expect(builder.includes(source), `Missing Magisterium source in builder: ${source}`);
}
expect(builder.includes('META-INF/container.xml') && builder.includes('reading_order'),
  'Magisterium builder must follow the EPUB package and reading order.');
expect(repository.includes('normalizedReference') && repository.includes('matchedTerms'),
  'Magisterium search must rank full-text and stable-reference matches.');
expect(result.includes('directFilePath') && result.includes('isMagisterium()'),
  'SearchResult must carry precise Magisterium navigation data.');
expect(search.includes('SCOPE_MAGISTERIUM')
    && search.includes('MagisteriumIndexRepository.search')
    && search.includes('EXTRA_FIND_TEXT'),
  'Search screen is not wired to precise Magisterium search.');
expect(contentRepository.includes('MagisteriumIndexRepository.search'),
  'Global search must include the Magisterium full-text index.');
expect(reader.includes('HoursRepository.isReference(volume)')
    && reader.includes('"Magisterio · " + volume.title'),
  'Direct Magisterium hits must be identified correctly in the reader.');
expect(magisterium.includes('SECTION_LITURGY')
    && magisterium.includes('Ordenacion%20Lecturas%20Misa.pdf')
    && magisterium.includes('Buscar en todo el Magisterio'),
  'Magisterium hierarchy or OLM source is incomplete.');
expect(fs.existsSync(indexPath), 'Missing Magisterium index asset placeholder.');

const rows = read('app/src/main/assets/magisterium-index.tsv')
  .split(/\r?\n/).filter(line => line && !line.startsWith('#'));
if (rows.length > 0) {
  expect(rows.length >= 20, `Generated Magisterium index is too small: ${rows.length}`);
  for (const [position, line] of rows.slice(0, 50).entries()) {
    expect(line.split('\t').length === 6,
      `Malformed Magisterium index row ${position + 1}`);
  }
}

console.log(`Magisterium 4.0 contract OK: ${rows.length || 'placeholder'} indexed chunks.`);
