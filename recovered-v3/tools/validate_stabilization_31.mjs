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
const missalActivity = read('app/src/main/java/com/fabri/ministerium/MissalActivity.java');
const missalReader = read('app/src/main/java/com/fabri/ministerium/MissalSectionReaderActivity.java');
const missalDocument = read('app/src/main/java/com/fabri/ministerium/MissalDocument31.java');
const ritualRepository = read('app/src/main/java/com/fabri/ministerium/RitualRepository.java');
const backup = read('app/src/main/java/com/fabri/ministerium/BackupActivity.java');
const backupManager = read('app/src/main/java/com/fabri/ministerium/BackupManager.java');
const bilingual = read('app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java');
const latinReader = read('app/src/main/java/com/fabri/ministerium/LatinHoursReaderActivity.java');
const selectionMenu = read('app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java');
const readerPrefs = read('app/src/main/java/com/fabri/ministerium/ReaderPreferences.java');
const readerContext = read('app/src/main/java/com/fabri/ministerium/ReaderContext.java');
const bible = read('app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java');
const bibleSearch = read('app/src/main/java/com/fabri/ministerium/BibleSearchActivity.java');
const bibleSearchRepo = read('app/src/main/java/com/fabri/ministerium/BibleSearchRepository.java');
const dictionary = read('app/src/main/java/com/fabri/ministerium/BibleDictionaryRepository.java');
const epubUtils = read('app/src/main/java/com/fabri/ministerium/EpubUtils.java');
const epubNavigation = read('app/src/main/java/com/fabri/ministerium/EpubNavigation.java');
const studyEntry = read('app/src/main/java/com/fabri/ministerium/StudyEntry.java');
const studyStore = read('app/src/main/java/com/fabri/ministerium/StudyStore.java');
const studyEditor = read('app/src/main/java/com/fabri/ministerium/StudyEditorActivity.java');
const studyDesk = read('app/src/main/java/com/fabri/ministerium/StudyDeskActivity.java');
const studyExport = read('app/src/main/java/com/fabri/ministerium/StudyExport.java');
const contentReference = read('app/src/main/java/com/fabri/ministerium/ContentReference.java');
const liturgicalIdentity = read('app/src/main/java/com/fabri/ministerium/LiturgicalIdentity.java');
const liturgicalDay = read('app/src/main/java/com/fabri/ministerium/LiturgicalDay.java');
const cleanHours = read('app/src/main/java/com/fabri/ministerium/CleanHoursAssets.java');
const dailyHours = read('app/src/main/java/com/fabri/ministerium/DailyHoursRepository.java');
const hoursBuilder = read('tools/build_clean_hours_31.py');
const latinHoursBuilder = read('tools/build_clean_latin_hours_31.py');
const updateCenter = read('app/src/main/java/com/fabri/ministerium/UpdateCenterActivity.java');
const mainActivity = read('app/src/main/java/com/fabri/ministerium/MainActivity.java');
const readingsActivity = read('app/src/main/java/com/fabri/ministerium/MassReadingsActivity.java');
const liturgicalResolver = read('app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java');
const packageManifest = read('app/src/main/assets/package-manifest.json');
const audit = read('docs/REPOSITORY_INTEGRATION_AUDIT_3_1.md');

// Baseline and continuous Mass + Hour.
requireText(build, 'versionCode 32', 'versionCode 3.1.1');
requireText(build, "versionName '3.1.1'", 'versionName 3.1.1');
requireText(activity, 'CombinedMassComposer31.compose', 'compositor combinado 3.1');
forbidText(activity, 'startActivity(', 'salida a otra Activity desde la celebración unida');
for (const required of ['MassReadingsRepository.read', 'LiturgiaPapalMissalRepository', 'Cántico evangélico de ', 'Oración después de la Comunión'])
  requireText(composer, required, `contrato combinado ${required}`);
for (const forbidden of ['HoursRepository.ROMAN_MISSAL', 'Misal-Diario-Romano.epub', 'MissalProperRepository', 'CombinedMassPolisher'])
  forbidText(composer + missalActivity + missalDocument, forbidden, `fallback antiguo de Misal (${forbidden})`);

// Final Missal contract: ES and Latin are separate full-width documents.
requireText(missalActivity, 'String[] languages = {"Español", "Latín"}', 'selector ES/LAT separado');
requireText(missalReader, 'MissalAlternativeOptions31.inject', 'alternativas compactas del Misal');
requireText(missalDocument, 'String lang = "la".equals(language) ? "la" : "es"', 'documento monolingüe por idioma');
requireText(missalDocument, 'professionOfFaithHtml(context, lang)', 'Credo en el idioma del Misal');
requireText(missalDocument, 'Lecturas del día', 'orden de Liturgia de la Palabra');
forbidText(missalDocument, 'parallel-unit', 'Misal ES/LAT en columnas paralelas');
forbidText(missalDocument + composer, 'MassReadingsRepository.syncDay', 'descarga implícita dentro del compositor');

// Liturgia Papal/Rituals and explicit update contract.
requireText(ritualRepository, 'rituals/liturgiapapal/', 'Rituales Liturgia Papal');
forbidText(ritualRepository, 'argentina', 'fuente ritual argentina heredada');
requireText(backup, 'Intent.ACTION_CREATE_DOCUMENT', 'backup mediante Storage Access Framework');
requireText(backup, 'Intent.CATEGORY_OPENABLE', 'selector de documentos del sistema');
forbidText(backup, 'com.google.android.apps.docs', 'dependencia rígida del paquete Google Drive');
requireText(backupManager, 'ministerium-study-v3.json', 'Mi estudio dentro del backup');
forbidText(mainActivity, 'LiturgicalCalendarRepository.ensureCurrentYear', 'actualización del calendario al iniciar');
requireText(updateCenter, 'LiturgicalCalendarRepository.updateYear', 'actualización explícita del calendario');
requireText(readingsActivity, 'Sincronizar desde Ajustes', 'Leccionario pasivo al abrir');
forbidText(readingsActivity, 'syncDay(', 'sincronización del Leccionario al entrar');
requireText(liturgicalResolver, '!event.isOptionalMemorial()', 'feria separada de memoria libre');

// Annotation contract v3: robust anchors + editable highlights/notes/bookmarks.
for (const required of ['contentId', 'anchorText', 'prefix', 'suffix', 'anchorVersion', 'tags', 'style', 'icon'])
  requireText(studyEntry, required, `campo robusto de anotación ${required}`);
requireText(studyEntry, 'CURRENT_ANCHOR_VERSION = 3', 'ancla v3');
for (const required of ['anchorText', 'prefix', 'suffix', 'semanticUnitId', 'startOffset', 'endOffset', 'openEntry'])
  requireText(selectionMenu, required, `captura/restauración ${required}`);
requireText(selectionMenu, 'context.allowTts', 'TTS condicionado por contexto');
requireText(studyStore, 'forContentId', 'consulta por contentId');
requireText(studyStore, 'upgradeEntries', 'migración no destructiva de anotaciones');
requireText(studyEditor, 'EXTRA_CONTENT_ID', 'editor con ID canónico');
requireText(studyEditor, 'inputStudyTags', 'etiquetas de nota/reflexión');
requireText(studyDesk, 'StudyExport.markdown', 'exportación Markdown');
requireText(studyDesk, 'StudyExport.json', 'exportación JSON');
requireText(studyExport, 'ministerium-study-export', 'formato portable Mi estudio');
for (const action of ['Resaltar', 'Nota', 'Marcador', 'Reflexión', 'Diccionario', 'Traducir'])
  requireText(selectionMenu, `\"${action}\"`, `acción contextual ${action}`);
requireText(selectionMenu, 'Cambiar estilo', 'edición de resaltado existente');
requireText(selectionMenu, 'Ampliar al párrafo o versículo', 'ampliación de resaltado');

// Lokus/Missale Meum/CLEDR-CLBDR internal identity contract.
requireText(readerContext, 'contentId', 'contentId en ReaderContext');
requireText(readerContext, 'ContentReference.infer', 'compatibilidad sourceKey→contentId');
for (const required of ['bible(', 'canon(', 'magisterium(', 'hours(', 'mass(', 'missal(', 'combined(', 'infer('])
  requireText(contentReference, required, `ContentReference.${required}`);
requireText(liturgicalIdentity, 'celebrationId', 'celebrationId');
requireText(liturgicalIdentity, 'missalFormId', 'missalFormId');
requireText(liturgicalIdentity, 'cledrId', 'campo CLEDR opcional');
requireText(liturgicalIdentity, 'clbdrId', 'campo CLBDR opcional');
requireText(liturgicalIdentity, 'return new LiturgicalIdentity(', 'constructor de identidad interna');
requireText(liturgicalDay, 'LiturgicalIdentity.internal', 'día enlazado a identidad/formulario');

// Divinum Officium-inspired alignment, final paragraph-card strategy.
requireText(hoursBuilder, 'data-ministerium-align-key', 'claves de alineación ES');
requireText(latinHoursBuilder, 'data-ministerium-align-key', 'claves de alineación LAT');
requireText(hoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación ES');
requireText(latinHoursBuilder, 'shared semantic keys v1', 'manifiesto de alineación LAT');
requireText(cleanHours, '.ready-3.1.1-align4', 'refresco de paquete ES alineado');
requireText(bilingual, 'semanticSynchronize', 'sincronización por ancla');
requireText(bilingual, 'data-ministerium-align-key', 'búsqueda de ancla equivalente');
requireText(bilingual, 'applyParagraphCards', 'alineación visual por párrafos');
requireText(bilingual, 'ministerium-align-card', 'tarjetas de párrafos bilingües');
forbidText(bilingual, 'sourceY / (float) sourceRange', 'fallback porcentual antiguo');
requireText(bilingual, '"Liturgia", true, false', 'TTS LAT desactivado en bilingüe');
requireText(latinReader, '"Liturgia", true, false', 'TTS LAT desactivado');
requireText(readerPrefs, 'data-ministerium-align-key', 'estilos de alineación semántica');

// Readium/FolioReader-style EPUB robustness and current Bible/index contract.
requireText(epubUtils, 'EpubNavigation.navTableOfContents', 'fallback EPUB3 NAV');
for (const required of ['META-INF/container.xml', 'rootfile(', 'navHref(', 'parseNav('])
  requireText(epubNavigation, required, `EPUB3 ${required}`);
requireText(bible, 'ReaderPreferences.apply(BibleReaderActivity.this, webView', 'preferencias globales Biblia');
requireText(bibleSearch, 'ReferenceParser.parse(this, query)', 'parser de referencias bíblicas');
requireText(bibleSearchRepo, 'bible-search-index.tsv', 'índice bíblico rápido');
forbidText(bibleSearchRepo, 'BibleChapterDocument.from', 'reconstrucción completa del EPUB en cada búsqueda');
requireText(dictionary, 'EpubEntryReader.read', 'lectura puntual de diccionario');

// Clean Hours contract.
requireText(cleanHours, 'hours-clean/', 'paquete limpio de Horas');
requireText(dailyHours, 'links.get("IN")', 'Invitatorio desde navegación limpia');
requireText(dailyHours, 'targetFromToc', 'fallback de tarjetas de Horas');
requireText(hoursBuilder, 'NAV_INLINE', 'limpieza de tokens editoriales');
requireText(epubUtils, 'CleanHoursAssets.ensureExtracted', 'Horas ES sin EPUB en runtime');
requireText(packageManifest, '"hours-es-clean"', 'manifiesto de Horas limpias');
forbidText(packageManifest, 'LH - 5. TIEMPO ORDINARIO.epub', 'EPUB de Horas como paquete runtime');

// Audit/documentation and explicit exclusion of the newer batch.
for (const name of ['Calibre', 'Lokus', 'Divinum Officium', 'Missale Meum', 'Readium / FolioReader', 'Bitwarden Android', 'PDFBox / Stirling-PDF', 'Qkor/MissaleRomanumApp', 'jgcrunden/missalette', 'NewOpenBible/NewOpenBible'])
  requireText(audit, name, `auditoría ${name}`);
for (const excluded of ['Turbo Editor', 'All-In-One-Python-Projects', 'auth-flow-kit', 'LeafPic', 'LizardFS', 'TeknoMW3', 'SELinuxModeChanger'])
  requireText(audit, excluded, `exclusión ${excluded}`);

// No accidental runtime import from reference projects and no production-style secrets in APK sources.
const runtimeText = textFiles('app/src/main').map((p) => read(p)).join('\n');
for (const forbidden of ['implementation \'org.readium', 'implementation("org.readium', 'import calibre.', 'import org.readium.', 'import com.folioreader.', 'import lokus.'])
  forbidText(build + '\n' + runtimeText, forbidden, `dependencia externa no autorizada (${forbidden})`);
for (const secret of ['github_pat_', 'ghp_', 'client_secret=', 'client_secret\"', 'Authorization: Bearer '])
  forbidText(runtimeText, secret, `secreto de producción en APK (${secret})`);

for (const p of [
  'tools/build_clean_hours_31.py',
  'tools/build_clean_latin_hours_31.py',
  'app/src/main/java/com/fabri/ministerium/EpubNavigation.java',
  'app/src/main/java/com/fabri/ministerium/StudyExport.java',
  'app/src/main/java/com/fabri/ministerium/ContentReference.java',
  'app/src/main/java/com/fabri/ministerium/LiturgicalIdentity.java',
  'app/src/main/java/com/fabri/ministerium/DailyMassProperRepository.java',
  'app/src/main/java/com/fabri/ministerium/MissalAlternativeOptions31.java',
  'docs/REPOSITORY_INTEGRATION_AUDIT_3_1.md'
]) requireFile(p);

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

if (failures.length) {
  console.error('Ministerium 3.1.1 stabilization: FALLÓ');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log('Ministerium 3.1.1 final test contract: OK');