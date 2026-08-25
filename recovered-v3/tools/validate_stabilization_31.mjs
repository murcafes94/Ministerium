import fs from 'node:fs';
import path from 'node:path';

const read = (p) => fs.readFileSync(p, 'utf8');
const failures = [];
const requireText = (source, text, label) => { if (!source.includes(text)) failures.push(`Falta ${label}: ${text}`); };
const forbidText = (source, text, label) => { if (source.includes(text)) failures.push(`No debe existir ${label}: ${text}`); };
const requireFile = (p, label = p) => { if (!fs.existsSync(p)) failures.push(`Falta ${label}: ${p}`); };

function textFiles(root, extensions = new Set(['.java', '.gradle', '.xml', '.json'])) {
  const out = [];
  if (!fs.existsSync(root)) return out;
  for (const name of fs.readdirSync(root)) {
    const p = path.join(root, name);
    const stat = fs.statSync(p);
    if (stat.isDirectory()) out.push(...textFiles(p, extensions));
    else if (extensions.has(path.extname(name))) out.push(p);
  }
  return out;
}

const build = read('app/build.gradle');
const activity = read('app/src/main/java/com/fabri/ministerium/CombinedMassActivity.java');
const composer = read('app/src/main/java/com/fabri/ministerium/CombinedMassComposer31.java');
const papal = read('app/src/main/java/com/fabri/ministerium/LiturgiaPapalMissalRepository.java');
const missalActivity = read('app/src/main/java/com/fabri/ministerium/MissalActivity.java');
const missalReader = read('app/src/main/java/com/fabri/ministerium/MissalSectionReaderActivity.java');
const missalDocument = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const missalCompact = read('app/src/main/java/com/fabri/ministerium/MissalCompactView.java');
const manifest = read('app/src/main/AndroidManifest.xml');
const packageManifest = read('app/src/main/assets/package-manifest.json');
const ritualRepository = read('app/src/main/java/com/fabri/ministerium/RitualRepository.java');
const ritualCatalog = read('app/src/main/java/com/fabri/ministerium/RitualCatalogActivity.java');
const backup = read('app/src/main/java/com/fabri/ministerium/BackupActivity.java');
const backupManager = read('app/src/main/java/com/fabri/ministerium/BackupManager.java');
const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');
const latinReader = read('app/src/main/java/com/fabri/ministerium/LatinHoursReaderActivity.java');
const selectionChrome = read('app/src/main/java/com/fabri/ministerium/MinisteriumWebView.java');
const selectionMenu = read('app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java');
const textSelection = read('app/src/main/java/com/fabri/ministerium/TextViewReaderChrome.java');
const readerPrefs = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
const readerSettings = read('app/src/main/java/com/fabri/ministerium/ReaderSettingsActivity.java');
const readerSettingsLayout = read('app/src/main/res/layout/activity_reader_settings.xml');
const readerContext = read('app/src/main/java/com/fabri/ministerium/ReaderContext.java');
const bible = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
const bibleSearch = read('app/src/main/java/com/fabri/ministerium/BibleSearchActivity.java');
const bibleSearchRepo = read('app/src/main/java/com/fabri/ministerium/BibleSearchRepository.java');
const dictionary = read('app/src/main/java/com/fabri/ministerium/BibleDictionaryRepository.java');
const epubEntryReader = read('app/src/main/java/com/fabri/ministerium/EpubEntryReader.java');
const epubUtils = read('app/src/main/java/com/fabri/ministerium/EpubUtils.java');
const epubNavigation = read('app/src/main/java/com/fabri/ministerium/EpubNavigation.java');
const rae = read('app/src/main/java/com/fabri/ministerium/RaeOnlineRepository.java');
const overlay = read('app/src/main/java/com/fabri/ministerium/ReaderOverlayDialog.java');
const markers = read('app/src/main/java/com/fabri/ministerium/MarkersActivity.java');
const studyEntry = read('app/src/main/java/com/fabri/ministerium/StudyEntry.java');
const studyStore = read('app/src/main/java/com/fabri/ministerium/StudyStore.java');
const studyEditor = read('app/src/main/java/com/fabri/ministerium/StudyEditorActivity.java');
const studyDesk = read('app/src/main/java/com/fabri/ministerium/StudyDeskActivity.java');
const studyExport = read('app/src/main/java/com/fabri/ministerium/StudyExport.java');
const contentReference = read('app/src/main/java/com/fabri/ministerium/ContentReference.java');
const liturgicalIdentity = read('app/src/main/java/com/fabri/ministerium/LiturgicalIdentity.java');
const liturgicalDay = read('app/src/main/java/com/fabri/ministerium/LiturgicalDay.java');
const complineRepo = read('app/src/main/java/com/fabri/ministerium/ComplineContentRepository.java');
const complineReader = read('app/src/main/java/com/fabri/ministerium/ComplineReaderActivity.java');
const cleanHours = read('app/src/main/java/com/fabri/ministerium/CleanHoursAssets.java');
const dailyHours = read('app/src/main/java/com/fabri/ministerium/DailyHoursRepository.java');
const hoursBuilder = read('tools/build_clean_hours_31.py');
const latinHoursBuilder = read('tools/build_clean_latin_hours_31.py');
const hoursToday = read('app/src/main/java/com/fabri/ministerium/HoursTodayActivity.java');
const updateCenter = read('app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java');
const mainActivity = read('app/src/main/java/com/fabri/ministerium/MainActivity.java');
const readingsActivity = read('app/src/main/java/com/fabri/ministerium/MassReadingsActivity.java');
const liturgicalResolver = read('app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java');
const liturgicalStyle = read('app/src/main/java/com/fabri/ministerium/LiturgicalWebStyle.java');
const layout = read('app/src/main/res/layout/activity_combined_mass.xml');
const audit = read('docs/REPOSITORY_INTEGRATION_AUDIT_3_1.md');

// Build/version and continuous combined celebration.
requireText(build, 'versionCode 32', 'versionCode 3.1.1');
requireText(build, "versionName '3.1.1'", 'versionName 3.1.1');
requireText(layout, 'combinedMassWebView', 'lector continuo');
requireText(activity, 'CombinedMassComposer31.compose', 'compositor 3.1');
forbidText(activity, 'CombinedMassPolisher.compose', 'compositor heredado');
forbidText(activity, 'startActivity(', 'salida a otra Activity desde la celebración');
for (const required of ['MassReadingsRepository.read', 'LiturgiaPapalMissalRepository', 'Cántico evangélico de ', 'Oración después de la Comunión'])
  requireText(composer, required, `contrato combinado ${required}`);
for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub', 'MissalProperRepository', 'CombinedMassComposer.compose', 'CombinedMassPolisher'])
  forbidText(composer, forbidden, `fallback de Misal EPUB (${forbidden})`);
for (const source of [missalActivity, missalDocument])
  for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub', 'MissalProperRepository'])
    forbidText(source, forbidden, `Misal autónomo heredado (${forbidden})`);
requireText(missalActivity, 'MissalSectionReaderActivity.class', 'lector autónomo Liturgia Papal');
requireText(missalReader, 'LiturgicalWebStyle.apply', 'línea gráfica común del Misal');
requireText(missalReader, 'ReaderPreferences.apply(', 'fuente global del Misal');
requireText(missalReader, 'MissalInteractiveOptions.inject', 'Credo/Padre Nuestro también en bilingüe');
forbidText(missalReader, 'if ("es".equals(language))', 'restricción de opciones litúrgicas solo a español');
requireText(missalDocument, 'LiturgiaPapalMissalRepository', 'fuente Liturgia Papal');
requireText(missalDocument, 'parallel-unit', 'alineación ES/LAT por unidades');
requireText(missalDocument, 'setPrayer(n)', 'una Plegaria eucarística visible por selector');
forbidText(missalDocument, 'MassReadingsRepository.syncDay', 'descarga implícita al abrir Misal');
forbidText(composer, 'MassReadingsRepository.syncDay', 'descarga implícita al abrir Misa unida');
requireText(missalCompact, 'ministerium-technical-note', 'ocultación de notas técnicas');
requireText(manifest, '.MissalSectionReaderActivity', 'Activity del nuevo Misal');
requireText(papal, 'liturgia-papal-mexico', 'trazabilidad Liturgia Papal México');
requireText(liturgicalStyle, '.reading-section', 'estilo compartido con lecturas');

// Ritual/Bendicional and explicit updates.
forbidText(ritualRepository, 'argentina', 'fuente ritual argentina heredada');
requireText(ritualRepository, 'rituals/liturgiapapal/', 'ruta de rituales Liturgia Papal');
requireText(ritualCatalog, 'COMMON_BLESSINGS_ID', 'entrada de Bendicional');
requireText(ritualCatalog, 'showBlessings()', 'catálogo de Bendicional');
requireText(backup, 'createDriveBackup()', 'acción de Google Drive');
requireText(backup, 'com.google.android.apps.docs', 'proveedor Google Drive');
requireText(backupManager, 'ministerium-study-v3.json', 'Mi estudio dentro del backup completo');
forbidText(mainActivity, 'LiturgicalCalendarRepository.ensureCurrentYear', 'actualización automática del calendario al iniciar');
requireText(updateCenter, 'LiturgicalCalendarRepository.updateYear', 'actualización explícita del calendario desde Ajustes');
requireText(readingsActivity, 'Sincronizar desde Ajustes', 'Leccionario pasivo al abrir');
forbidText(readingsActivity, 'syncDay(', 'sincronización al entrar en Lecturas');
requireText(liturgicalResolver, '!event.isOptionalMemorial()', 'feria separada de memoria libre');

// Calibre-inspired annotation contract, implemented natively.
for (const required of ['contentId', 'anchorText', 'prefix', 'suffix', 'anchorVersion', 'tags'])
  requireText(studyEntry, required, `campo de anotación robusta ${required}`);
requireText(studyEntry, 'CURRENT_ANCHOR_VERSION = 2', 'versión de ancla v2');
requireText(selectionMenu, 'anchorText', 'captura exacta de selección');
requireText(selectionMenu, 'prefix', 'contexto previo de selección');
requireText(selectionMenu, 'suffix', 'contexto posterior de selección');
requireText(selectionMenu, 'data-ministerium-align-key', 'contrato semántico compartido en selección/lectura');
requireText(selectionMenu, 'context.allowTts', 'TTS condicionado por contexto');
requireText(studyStore, 'forContentId', 'consulta por ID canónico');
requireText(studyStore, 'upgradeEntries', 'migración no destructiva de anotaciones');
requireText(studyEditor, 'EXTRA_CONTENT_ID', 'editor enlazado a ID canónico');
requireText(studyEditor, 'inputStudyTags', 'etiquetas en editor');
requireText(studyDesk, 'StudyExport.markdown', 'exportación Markdown desde Mesa de estudio');
requireText(studyDesk, 'StudyExport.json', 'exportación JSON desde Mesa de estudio');
requireText(studyExport, 'ministerium-study-export', 'formato portable Mi estudio');
for (const action of ['Subrayar', 'Nota', 'Reflexión', 'Diccionario', 'Traducir'])
  requireText(selectionMenu, `\"${action}\"`, `acción contextual ${action}`);
requireText(selectionMenu, 'addSubMenu', 'acciones secundarias dentro de Más');
requireText(selectionMenu, 'RaeOnlineRepository.actionCard', 'RAE opcional desde selección');

// Lokus/Missale Meum: stable internal identity without inventing external IDs.
requireText(readerContext, 'contentId', 'ID canónico en ReaderContext');
requireText(readerContext, 'ContentReference.infer', 'migración automática de sourceKey');
for (const required of ['bible(', 'canon(', 'magisterium(', 'hours(', 'mass(', 'missal(', 'combined(', 'infer('])
  requireText(contentReference, required, `ContentReference.${required}`);
requireText(liturgicalIdentity, 'celebrationId', 'celebrationId');
requireText(liturgicalIdentity, 'missalFormId', 'missalFormId');
requireText(liturgicalIdentity, 'cledrId', 'campo CLEDR opcional');
requireText(liturgicalIdentity, 'clbdrId', 'campo CLBDR opcional');
requireText(liturgicalIdentity, '"",\n                ""', 'IDs externos vacíos mientras no estén verificados');
requireText(liturgicalDay, 'LiturgicalIdentity.internal', 'celebración enlazada al formulario');

// Divinum Officium-inspired bilingual alignment.
requireText(hoursBuilder, 'data-ministerium-align-key', 'claves de alineación ES');
requireText(latinHoursBuilder, 'data-ministerium-align-key', 'claves de alineación LAT');
requireText(hoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación ES');
requireText(latinHoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación LAT');
requireText(cleanHours, '.ready-3.1.1-align4', 'refresco de paquete alineado');
requireText(bilingual, 'semanticSynchronize', 'scroll por ancla semántica');
requireText(bilingual, 'sourceY / (float) sourceRange', 'fallback proporcional bilingüe');
requireText(bilingual, 'data-ministerium-align-key', 'búsqueda de ancla equivalente');
requireText(bilingual, '"Liturgia", true, false', 'TTS latino desactivado en bilingüe');
requireText(latinReader, '"Liturgia", true, false', 'TTS latino desactivado en lector LAT');
requireText(readerPrefs, 'data-ministerium-align-key', 'estilo de bloques paralelos');

// Readium/FolioReader-inspired EPUB robustness plus KOReader/Readest UX.
requireText(epubUtils, 'EpubNavigation.navTableOfContents', 'fallback EPUB3 NAV');
requireText(epubNavigation, 'META-INF/container.xml', 'container.xml EPUB3');
requireText(epubNavigation, 'properties', 'manifest nav EPUB3');
requireText(epubNavigation, 'data', 'parser de navegación EPUB3');
requireText(selectionChrome, 'super.startActionMode(wrap(callback), type)', 'toolbar contextual nativo');
forbidText(selectionChrome, 'resolvedType = ActionMode.TYPE_FLOATING', 'forzado del popup fuera del texto');
requireText(textSelection, 'SHOW_AS_ACTION_ALWAYS', 'toolbar equivalente TextView');
requireText(readerPrefs, 'PALATINO', 'opción Palatino global');
requireText(readerSettingsLayout, 'readerPalatino', 'opción visual Palatino');
requireText(readerSettings, 'ReaderPreferences.PALATINO', 'selección Palatino');

// Bible/search/dictionaries.
requireText(bible, 'ReaderPreferences.apply(BibleReaderActivity.this, webView', 'preferencias globales en Biblia');
requireText(bibleSearch, 'ReferenceParser.parse(this, query)', 'apertura directa de referencias bíblicas');
requireText(bibleSearch, 'EXTRA_SCROLL_VERSE', 'salto al versículo buscado');
requireText(bibleSearchRepo, 'bible-search-index.tsv', 'índice bíblico rápido');
forbidText(bibleSearchRepo, 'BibleChapterDocument.from', 'reconstrucción completa del EPUB en cada búsqueda');
requireText(dictionary, 'ENTRY_INDEX_CACHE', 'índice directo de diccionario');
requireText(dictionary, 'EpubEntryReader.read', 'lectura de una sola entrada del diccionario');
requireText(epubEntryReader, 'ZipInputStream', 'lector parcial de EPUB');
requireText(rae, 'https://rae-api.com/api/words/', 'RAE API opcional');
requireText(rae, 'dictionary_cache/rae', 'caché local RAE');
requireText(overlay, '"rae".equals(uri.getHost())', 'consulta RAE dentro del overlay');
requireText(markers, 'StudyStore.ofType(this, StudyEntry.HIGHLIGHT)', 'subrayados unificados con Mi estudio');

// Hours and Compline.
requireText(complineRepo, 'liturgicalVolume', 'criterio de tomos para Completas');
requireText(complineRepo, 'ordinaryWeek >= 18', 'Tomo IV desde semana XVIII');
requireText(complineReader, 'ComplineMarianLanguage.inject', 'antífonas marianas ES/LAT en Completas');
requireText(hoursToday, 'Semana " + roman(ordinaryWeek) + " del Tiempo Ordinario', 'semana ordinaria visible');
requireText(hoursToday, 'Salterio " + currentDay.psalterWeek', 'semana del salterio separada');
requireText(cleanHours, 'hours-clean/', 'paquete limpio de Horas');
requireText(dailyHours, 'links.get("IN")', 'Invitatorio desde navegación limpia');
requireText(dailyHours, 'targetFromToc', 'fallback de tarjetas de Horas sin EPUB');
requireText(hoursBuilder, 'NAV_INLINE', 'limpieza de tokens editoriales sueltos');
requireText(epubUtils, 'CleanHoursAssets.ensureExtracted', 'Horas españolas sin EPUB en runtime');

// Audit must stay present and must explicitly exclude the recent repository batch.
for (const name of ['Calibre', 'Lokus', 'Divinum Officium', 'Missale Meum', 'Readium / FolioReader', 'Bitwarden Android', 'PDFBox / Stirling-PDF', 'Qkor/MissaleRomanumApp', 'jgcrunden/missalette', 'NewOpenBible/NewOpenBible'])
  requireText(audit, name, `auditoría ${name}`);
for (const excluded of ['Turbo Editor', 'All-In-One-Python-Projects', 'auth-flow-kit', 'LeafPic', 'LizardFS', 'TeknoMW3', 'SELinuxModeChanger'])
  requireText(audit, excluded, `exclusión explícita ${excluded}`);

// No accidental external reader runtime/dependency and no production-style secrets in APK sources.
const appSources = textFiles('app/src/main');
const runtimeText = appSources.map((p) => read(p)).join('\n');
for (const forbidden of ['implementation \'org.readium', 'implementation("org.readium', 'import calibre.', 'import org.readium.', 'import com.folioreader.', 'import lokus.'])
  forbidText(build + '\n' + runtimeText, forbidden, `dependencia externa no autorizada (${forbidden})`);
for (const secret of ['github_pat_', 'ghp_', 'client_secret=', 'client_secret\"', 'Authorization: Bearer '])
  forbidText(runtimeText, secret, `secreto de producción en APK (${secret})`);

for (const asset of [
  'app/src/main/assets/hours-clean/manifest.json',
  'app/src/main/assets/hours-clean/ordinary/toc.tsv',
  'app/src/main/assets/missal/es/initial.txt',
  'app/src/main/assets/missal/es/word.txt',
  'app/src/main/assets/missal/es/proper_ordinary.txt',
  'app/src/main/assets/missal/la/initial.txt',
  'app/src/main/assets/rituals/liturgiapapal/baptism_one_child.txt',
  'app/src/main/assets/rituals/liturgiapapal/unction.txt',
  'app/src/main/assets/rituals/liturgiapapal/funeral_typical.txt',
  'app/src/main/assets/rituals/liturgiapapal/blessing_house.txt',
  'app/src/main/assets/bible-search-index.tsv'
]) {
  if (!fs.existsSync(asset) || fs.statSync(asset).size < 40) failures.push(`Falta asset generado: ${asset}`);
}
requireFile('tools/build_clean_hours_31.py', 'extractor limpio ES');
requireFile('tools/build_clean_latin_hours_31.py', 'extractor limpio LAT');
requireFile('app/src/main/java/com/fabri/ministerium/EpubNavigation.java', 'navegación EPUB3');
requireFile('app/src/main/java/com/fabri/ministerium/StudyExport.java', 'exportador de Mi estudio');
requireFile('docs/REPOSITORY_INTEGRATION_AUDIT_3_1.md', 'auditoría de repositorios');
requireText(packageManifest, '"hours-es-clean"', 'manifiesto de Horas limpias');
requireText(packageManifest, '"delivery": "bundled"', 'contenido incluido en APK');
forbidText(packageManifest, 'LH - 5. TIEMPO ORDINARIO.epub', 'EPUB de Horas como paquete runtime');

if (failures.length) {
  console.error('Ministerium 3.1.1 stabilization: FALLÓ');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log('Ministerium 3.1.1 final fixes + repository integrations: OK');
