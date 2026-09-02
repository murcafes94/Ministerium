package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ContentRepository {
    public static final String BENDICIONAL = "bendicional";
    public static final String DEVOCIONARIO = "devocionario";

    private static final Map<String, DocumentInfo> DOCUMENTS = new LinkedHashMap<>();
    private static final Map<String, String[]> PAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, String[]>(4, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > 2;
                }
            });

    private ContentRepository() {}

    public static DocumentInfo document(String id) {
        return DOCUMENTS.get(id);
    }

    public static List<DocumentInfo> availableDocuments() {
        return new ArrayList<>(DOCUMENTS.values());
    }

    public static String[] pages(Context context, DocumentInfo document) throws IOException {
        String[] cached = PAGE_CACHE.get(document.id);
        if (cached != null) return cached;

        try (InputStream input = context.getAssets().open(document.textAsset);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            String content = output.toString(StandardCharsets.UTF_8.name());
            String[] raw = content.split("\\f", -1);
            String[] exact = raw.length == document.pageCount
                    ? raw
                    : Arrays.copyOf(raw, document.pageCount);
            for (int i = 0; i < exact.length; i++) {
                if (exact[i] == null) exact[i] = "";
            }
            PAGE_CACHE.put(document.id, exact);
            return exact;
        }
    }

    public static List<SearchResult> search(Context context, String query, String documentId,
                                            int maximumResults) throws IOException {
        String normalizedQuery = normalize(query.trim());
        if (normalizedQuery.length() < 2) return Collections.emptyList();

        List<SearchResult> results = new ArrayList<>();
        if (documentId == null) {
            for (PrayerEntry prayer : PrayerRepository.search(context, query)) {
                results.add(new SearchResult(prayer,
                        prayer.category + " · " + prayer.description));
                if (results.size() >= maximumResults) return results;
            }
            results.addAll(RitualRepository.search(
                    context, query, maximumResults - results.size()));
            if (results.size() >= maximumResults) return results;
            for (LiturgicalDateHit hit : LiturgicalCalendarRepository.search(
                    context, query, maximumResults - results.size())) {
                results.add(new SearchResult(hit));
                if (results.size() >= maximumResults) return results;
            }
            try {
                results.addAll(CanonSearchRepository.search(
                        context, query, maximumResults - results.size()));
            } catch (Exception ignored) {
                // Derecho Canónico: solo texto de los cánones; los comentarios se abren desde el canon.
            }
            if (results.size() >= maximumResults) return results;
            try {
                results.addAll(MagisteriumIndexRepository.search(
                        context, query, maximumResults - results.size()));
            } catch (Exception ignored) {
                // Una falla aislada del índice magisterial no debe impedir la búsqueda global.
            }
            if (results.size() >= maximumResults) return results;
            try {
                results.addAll(HoursRepository.search(
                        context, query, maximumResults - results.size()));
            } catch (Exception ignored) {
                // Una falla aislada del índice EPUB no debe impedir buscar los demás contenidos.
            }
            if (results.size() >= maximumResults) return results;
        }
        List<DocumentInfo> sources = documentId == null
                ? availableDocuments()
                : Collections.singletonList(document(documentId));

        for (DocumentInfo document : sources) {
            if (document == null) continue;
            String[] sourcePages = pages(context, document);
            for (int i = 0; i < sourcePages.length; i++) {
                String original = sourcePages[i];
                String normalized = normalize(original);
                int match = normalized.indexOf(normalizedQuery);
                if (match >= 0) {
                    String title = firstUsefulLine(original);
                    String snippet = snippet(original, match, normalizedQuery.length());
                    results.add(new SearchResult(document, i, title, snippet));
                    if (results.size() >= maximumResults) return results;
                }
            }
        }
        return results;
    }

    public static String firstUsefulLine(String page) {
        String[] lines = page.replace('\r', '\n').split("\\n+");
        for (String line : lines) {
            String cleaned = line.trim().replaceAll("\\s+", " ");
            if (cleaned.length() >= 4 && !cleaned.matches("^[0-9]+$")) {
                return cleaned.length() > 90 ? cleaned.substring(0, 90) + "…" : cleaned;
            }
        }
        return "Contenido de la página";
    }

    private static String snippet(String original, int approximateIndex, int queryLength) {
        String compact = original.replaceAll("\\s+", " ").trim();
        if (compact.isEmpty()) return "Página sin texto extraíble; consulta la vista original.";

        String normalizedCompact = normalize(compact);
        int normalizedIndex = Math.max(0, normalizedCompact.indexOf(
                normalize(original.substring(Math.max(0, Math.min(approximateIndex, original.length())),
                        Math.max(0, Math.min(original.length(), approximateIndex + queryLength))))));
        int center = normalizedIndex >= 0 ? normalizedIndex : Math.min(approximateIndex, compact.length());
        int start = Math.max(0, center - 85);
        int end = Math.min(compact.length(), center + queryLength + 125);
        return (start > 0 ? "…" : "") + compact.substring(start, end) + (end < compact.length() ? "…" : "");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('­', '-');
    }

    public static List<CatalogEntry> catalog(String documentId) {
        return BENDICIONAL.equals(documentId) ? bendicionalCatalog() : devocionarioCatalog();
    }

    public static String catalogSectionText(Context context, DocumentInfo document,
                                            CatalogEntry entry, int fallbackLastPage)
            throws IOException {
        if (!DEVOCIONARIO.equals(document.id)) {
            return sectionText(context, document, entry.pdfPageIndex, fallbackLastPage);
        }

        List<CatalogEntry> allEntries = devocionarioAllCatalog();
        int originalPosition = -1;
        for (int i = 0; i < allEntries.size(); i++) {
            CatalogEntry candidate = allEntries.get(i);
            if (candidate.title.equals(entry.title)
                    && candidate.pdfPageIndex == entry.pdfPageIndex) {
                originalPosition = i;
                break;
            }
        }
        if (originalPosition < 0) {
            return sectionText(context, document, entry.pdfPageIndex, fallbackLastPage);
        }

        CatalogEntry next = originalPosition + 1 < allEntries.size()
                ? allEntries.get(originalPosition + 1) : null;
        int lastPage = next == null
                ? document.pageCount - 1
                : Math.max(entry.pdfPageIndex, next.pdfPageIndex);
        String block = sectionText(context, document, entry.pdfPageIndex, lastPage);
        String lowerBlock = block.toLowerCase(Locale.ROOT);
        int start = lowerBlock.indexOf(entry.sourceTitle.toLowerCase(Locale.ROOT));
        int end = next == null ? -1
                : lowerBlock.indexOf(next.sourceTitle.toLowerCase(Locale.ROOT), Math.max(0, start + 1));

        if (start < 0 && end < 0) {
            return sectionText(context, document, entry.pdfPageIndex, fallbackLastPage);
        }
        int safeStart = Math.max(0, start);
        int safeEnd = end > safeStart ? end : block.length();
        return block.substring(safeStart, safeEnd).trim();
    }

    public static String sectionText(Context context, DocumentInfo document,
                                     int firstPage, int lastPage) throws IOException {
        String[] source = pages(context, document);
        int start = Math.max(0, Math.min(firstPage, source.length - 1));
        int end = Math.max(start, Math.min(lastPage, source.length - 1));
        StringBuilder result = new StringBuilder();
        for (int i = start; i <= end; i++) {
            String cleaned = cleanExtractedPage(source[i]);
            if (cleaned.isEmpty()) continue;
            if (result.length() > 0) result.append("\n\n");
            result.append(cleaned);
        }
        return result.toString().trim();
    }

    private static String cleanExtractedPage(String page) {
        if (page == null) return "";
        String normalized = page.replace("\u00ad", "").replace('\r', '\n');
        String[] lines = normalized.split("\\n+");
        StringBuilder output = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim().replaceAll("\\s+", " ");
            if (line.isEmpty()) continue;
            if (line.matches("^[0-9]{1,4}$")) continue;
            if (line.toLowerCase(Locale.ROOT).contains("liturgiapapal.org")) continue;
            if (line.matches("^\\|?\\s*(BENDICIONAL|RITUAL DEL BAUTISMO.*)\\s*\\|?$")) continue;

            if (output.length() > 0) {
                int length = output.length();
                char previous = output.charAt(length - 1);
                boolean joinHyphen = previous == '-' && !line.isEmpty()
                        && Character.isLowerCase(line.charAt(0));
                if (joinHyphen) {
                    output.deleteCharAt(length - 1);
                } else {
                    output.append('\n');
                }
            }
            output.append(line);
        }
        return output.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static CatalogEntry b(String title, String section, int printedPage) {
        return new CatalogEntry(title, section, Math.max(0, printedPage - 3), printedPage);
    }

    private static CatalogEntry d(String title, String section, int printedPage) {
        return new CatalogEntry(title, section, printedPage + 1, printedPage);
    }

    private static CatalogEntry d(String title, String sourceTitle,
                                  String section, int printedPage) {
        return new CatalogEntry(title, sourceTitle, section, printedPage + 1, printedPage);
    }

    private static List<CatalogEntry> bendicionalCatalog() {
        List<CatalogEntry> list = new ArrayList<>();
        list.add(b("Orientaciones generales", "Introducción", 11));
        list.add(b("PRIMERA PARTE: Bendiciones que se refieren directamente a las personas", "Parte", 27));
        list.add(b("I. Bendición de las familias y de sus miembros", "Primera parte", 29));
        list.add(b("II. Bendición de los enfermos", "Primera parte", 132));
        list.add(b("III. Bendición de los enviados a anunciar el Evangelio", "Primera parte", 146));
        list.add(b("IV. Bendiciones relativas a la catequesis y la oración en común", "Primera parte", 163));
        list.add(b("V. Bendición para diversos ministerios eclesiásticos", "Primera parte", 177));
        list.add(b("VI. Bendición de asociaciones de ayuda en necesidades públicas", "Primera parte", 196));
        list.add(b("VII. Bendición de los peregrinos", "Primera parte", 206));
        list.add(b("VIII. Bendición de quienes emprenden un viaje", "Primera parte", 219));
        list.add(b("SEGUNDA PARTE: Construcciones y actividades de los cristianos", "Parte", 229));
        list.add(b("IX. Trabajos para la estructura de un nuevo edificio", "Segunda parte", 232));
        list.add(b("X. Bendición de una nueva casa", "Segunda parte", 238));
        list.add(b("XI. Bendición de un nuevo seminario", "Segunda parte", 249));
        list.add(b("XII. Bendición de una nueva casa religiosa", "Segunda parte", 261));
        list.add(b("XIII. Bendición de una nueva escuela o universidad", "Segunda parte", 272));
        list.add(b("XIV. Bendición de una nueva biblioteca", "Segunda parte", 284));
        list.add(b("XV. Bendición de un nuevo hospital o centro de salud", "Segunda parte", 292));
        list.add(b("XVI. Bendición de un laboratorio, taller o comercio", "Segunda parte", 300));
        list.add(b("XVII. Locales destinados a medios de comunicación social", "Segunda parte", 309));
        list.add(b("XVIII. Gimnasios e instalaciones deportivas", "Segunda parte", 317));
        list.add(b("XIX. Desplazamientos humanos", "Segunda parte", 324));
        list.add(b("XX. Bendición de instrumentos técnicos", "Segunda parte", 337));
        list.add(b("XXI. Bendición de instrumentos de trabajo", "Segunda parte", 347));
        list.add(b("XXII. Bendición de una bandera", "Segunda parte", 355));
        list.add(b("XXIII. Bendición de los animales", "Segunda parte", 360));
        list.add(b("XXIV. Campos, tierras de cultivo y terrenos de pasto", "Segunda parte", 370));
        list.add(b("XXV. Términos de una población", "Segunda parte", 378));
        list.add(b("XXVI. Presentación de los nuevos frutos", "Segunda parte", 389));
        list.add(b("XXVII. Bendición de la mesa", "Segunda parte", 396));
        list.add(b("TERCERA PARTE: Cosas destinadas al uso litúrgico o la devoción", "Parte", 419));
        list.add(b("XXVIII. Bendición del baptisterio o nueva pila bautismal", "Tercera parte", 422));
        list.add(b("XXIX. Cátedra, ambón, sagrario o sede de la penitencia", "Tercera parte", 443));
        list.add(b("XXX. Bendición de una nueva puerta de la iglesia", "Tercera parte", 470));
        list.add(b("XXXI. Bendición de una nueva cruz", "Tercera parte", 479));
        list.add(b("XXXII. Bendición de imágenes para la veneración pública", "Tercera parte", 491));
        list.add(b("XXXIII. Bendición de una campana", "Tercera parte", 517));
        list.add(b("XXXIV. Bendición de un órgano", "Tercera parte", 526));
        list.add(b("XXXV. Objetos usados en celebraciones litúrgicas", "Tercera parte", 533));
        list.add(b("XXXVI. Bendición del agua fuera de la misa", "Tercera parte", 548));
        list.add(b("XXXVII. Bendición de la corona de Adviento", "Tercera parte", 553));
        list.add(b("XXXVIII. Bendición del belén navideño", "Tercera parte", 557));
        list.add(b("XXXIX. Bendición del árbol de Navidad", "Tercera parte", 569));
        list.add(b("XL. Bendición de las estaciones del vía crucis", "Tercera parte", 571));
        list.add(b("XLI. Bendición de un cementerio", "Tercera parte", 580));
        list.add(b("CUARTA PARTE: Objetos de devoción del pueblo cristiano", "Parte", 595));
        list.add(b("XLII. Bebidas, comestibles u otras cosas por devoción", "Cuarta parte", 598));
        list.add(b("XLIII. Objetos destinados a ejercitar la piedad", "Cuarta parte", 609));
        list.add(b("XLIV. Bendición de los rosarios", "Cuarta parte", 617));
        list.add(b("XLV. Bendición e imposición del escapulario", "Cuarta parte", 628));
        list.add(b("XLVI. Bendición de un hábito", "Cuarta parte", 637));
        list.add(b("QUINTA PARTE: Bendiciones para diversas circunstancias", "Parte", 639));
        list.add(b("XLVII. Acción de gracias por los beneficios recibidos", "Quinta parte", 642));
        list.add(b("XLVIII. Bendición para diversas ocasiones", "Quinta parte", 650));
        list.add(b("Índices bíblico y analítico", "Índices", 661));
        return list;
    }

    private static List<CatalogEntry> devocionarioCatalog() {
        List<CatalogEntry> visible = new ArrayList<>();
        for (CatalogEntry entry : devocionarioAllCatalog()) {
            String title = entry.title;
            if (title.equals("Rezo del Ángelus")
                    || title.equals("Rezo del Santo Rosario")
                    || title.equals("Bendita sea tu pureza")
                    || title.equals("Salve")
                    || title.equals("Salve Regina")
                    || title.equals("Regina Cæli")) {
                continue;
            }
            visible.add(entry);
        }
        return visible;
    }

    private static List<CatalogEntry> devocionarioAllCatalog() {
        List<CatalogEntry> list = new ArrayList<>();
        list.add(d("Rezo del Ángelus", "Oración básica", 3));
        list.add(d("Rezo del Santo Rosario", "Oración básica", 4));
        list.add(d("Letanías de la Santísima Virgen María",
                "Letanías a la Stma. Virgen María", "Letanías", 6));
        list.add(d("Rezo de la Corona Dolorosa", "Devoción", 9));
        list.add(d("Bendita sea tu pureza", "Oración", 11));
        list.add(d("Salve", "Oración básica", 11));
        list.add(d("Salve Regina", "Oración básica", 12));
        list.add(d("Regina Cæli", "Oración", 12));
        list.add(d("Magníficat", "Cántico", 13));
        list.add(d("Stabat Mater", "Himno", 14));
        list.add(d("Bendición de María Auxiliadora",
                "Bendición María Auxiliadora", "Bendición", 17));
        list.add(d("Novena propagada por san Juan Bosco",
                "Novena a María Auxiliadora", "Novena", 18));
        list.add(d("Novena de la confianza", "Novena", 19));
        list.add(d("Oración por la Asociación de María Auxiliadora",
                "Oración por la Asociación de", "Oración", 20));
        list.add(d("Oración compuesta por san Juan Bosco",
                "Oración compuesta por S. Juan", "Oración", 20));
        list.add(d("Madre de la Familia Salesiana",
                "Oración a María Auxiliadora,", "Oración", 21));
        list.add(d("Oración de los Salesianos a María Auxiliadora",
                "Oración de los Salesianos a María", "Oración", 22));
        list.add(d("Consagración a María Auxiliadora", "Consagración", 23));
        list.add(d("Plegaria de Don Bosco", "Plegaria", 24));
        list.add(d("Oración de la estampa en la cartera",
                "Soneto-Oración de la estampa en la", "Oración", 25));
        list.add(d("A la Virgen de mi cartera", "Oración", 26));
        list.add(d("Oración por la Iglesia y la juventud", "Oración", 26));
        list.add(d("Súplica en la enfermedad", "Oración", 27));
        list.add(d("Oración del enfermo hospitalizado", "Oración", 27));
        list.add(d("Oración por la familia", "Oración Familia", "Oración", 28));
        list.add(d("Oración por sí mismo", "Oración", 28));
        list.add(d("Maestra de oración", "Oración", 28));
        list.add(d("Oración por los hijos", "Oración", 29));
        list.add(d("Concédenos, Madre Auxiliadora", "Oración", 30));
        list.add(d("Te llamo Auxiliadora", "Oración", 30));
        list.add(d("Oración para llevar en el coche", "Oración", 31));
        list.add(d("Auxiliadora y Virgen de Caná", "Oración", 31));
        list.add(d("Quiero llegar a ti", "Oración", 32));
        list.add(d("Otras oraciones a María Auxiliadora", "Oraciones", 33));
        list.add(d("Auxiliadora de Sevilla", "Oración", 34));
        list.add(d("Madre Auxiliadora", "Oración", 35));
        list.add(d("Himno a María Auxiliadora", "Himno", 36));
        list.add(d("Visita domiciliaria de la capilla", "Celebración", 37));
        list.add(d("Entronización de María Auxiliadora", "Celebración", 38));
        list.add(d("Presentación de los niños", "Celebración", 40));
        list.add(d("Rendidos a tus plantas", "Himno", 41));
        return list;
    }
}
