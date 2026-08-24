package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Acceso semántico al paquete del Misal generado desde Liturgia Papal.
 *
 * El APK no interpreta PDF ni depende de páginas, tablas o anchors editoriales.
 * tools/build_liturgiapapal_missal.py genera texto limpio en assets/missal/ y
 * esta clase lo resuelve por componente y por unidad litúrgica.
 *
 * Español = Misal Romano, versión de México publicada por Liturgia Papal.
 * Latín = Missale Romanum publicado por Liturgia Papal.
 */
public final class LiturgiaPapalMissalRepository {
    public static final String ENTRANCE = "entrance";
    public static final String COLLECT = "collect";
    public static final String OFFERINGS = "offerings";
    public static final String COMMUNION_ANTIPHON = "communion_antiphon";
    public static final String POST_COMMUNION = "post_communion";

    private static final String BASE = "missal/";
    private static final Pattern ORDINARY_HEADING = Pattern.compile(
            "^[ivxlcdm]+ (?:domingo|semana) del tiempo ordinario$");
    private static final Pattern PDF_PAGE_HEADER = Pattern.compile(
            "(?i)\\b(?:PLEGARIA EUCAR[IÍ]STICA|PREX EUCHARISTICA)(?:\\s+[IVXLCDM]+)?\\s+\\d{1,3}\\b");

    private LiturgiaPapalMissalRepository() {}

    public static boolean isAvailable(Context context, String language) {
        try {
            String lang = normalizeLanguage(language);
            String[] files = context.getAssets().list(BASE + lang);
            if (files == null || files.length == 0) return false;
            boolean initial = false;
            boolean communion = false;
            boolean prayers = false;
            for (String file : files) {
                if ("initial.txt".equals(file)) initial = true;
                if ("communion.txt".equals(file)) communion = true;
                if ("eucharistic_prayer_1.txt".equals(file)) prayers = true;
            }
            return initial && communion && prayers;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String component(Context context, String language, String id) throws Exception {
        return sanitize(readAsset(context,
                BASE + normalizeLanguage(language) + "/" + id + ".txt"));
    }

    /**
     * Kyrie y, cuando corresponde, Gloria. Se toma del Ordinario mexicano y no
     * del EPUB antiguo. La búsqueda se hace por contenido y no por posición de página.
     */
    public static String initialMassHtml(Context context, boolean includeGloria) throws Exception {
        String[] lines = component(context, "es", "initial").split("\\r?\\n");
        int gloria = findContains(lines, "gloria a dios en el cielo", 0);
        if (gloria < 0) return "";

        int lastLord = findLastContains(lines, "senor ten piedad", gloria - 1);
        int firstLord = lastLord < 0 ? -1
                : findLastContains(lines, "senor ten piedad", lastLord - 1);
        int kyrieStart = firstLord >= 0 && lastLord - firstLord <= 16
                ? firstLord : Math.max(0, lastLord - 6);
        if (lastLord < 0) return "";

        StringBuilder result = new StringBuilder();
        result.append(render(join(lines, kyrieStart, gloria)));
        if (includeGloria) {
            int gloriaEnd = findContains(lines, "acabado el himno", gloria + 1);
            if (gloriaEnd < 0) gloriaEnd = findContains(lines, "oremos", gloria + 1);
            if (gloriaEnd < 0) gloriaEnd = Math.min(lines.length, gloria + 40);
            result.append(render(join(lines, gloria, gloriaEnd)));
        }
        return result.toString();
    }

    /** Preparación de los dones, antes del diálogo del prefacio. */
    public static String preparationHtml(Context context, String language) throws Exception {
        String text = component(context, language, "eucharistic_liturgy");
        String end = "es".equals(normalizeLanguage(language))
                ? "PLEGARIA EUCARÍSTICA" : "PREX EUCHARISTICA";
        return render(extract(text, null, end));
    }

    /** Diálogo inicial del prefacio. */
    public static String prefaceDialogueHtml(Context context, String language) throws Exception {
        String text = component(context, language, "eucharistic_liturgy");
        if ("es".equals(normalizeLanguage(language))) {
            return render(extract(text,
                    "El sacerdote comienza la plegaria eucarística con el prefacio.",
                    "El sacerdote prosigue el Prefacio"));
        }
        return render(extract(text, "Dóminus vobíscum.",
                "Sacerdos prosequitur præfationem"));
    }

    /**
     * Selector de los diez prefacios dominicales del Tiempo Ordinario de la
     * edición mexicana. El I queda visible inicialmente; el usuario puede elegir.
     */
    public static String ordinarySundayPrefacesHtml(Context context) throws Exception {
        String[] lines = component(context, "es", "prefaces").split("\\r?\\n");
        StringBuilder options = new StringBuilder();
        StringBuilder bodies = new StringBuilder();
        int count = 0;
        for (int number = 1; number <= 10; number++) {
            String marker = "prefacio " + roman(number) + " para los domingos";
            int start = findStartsWith(lines, marker, 0);
            if (start < 0) continue;
            int end = findNextPrefaceHeading(lines, start + 1);
            if (end < 0) end = lines.length;
            String value = render(join(lines, start, end));
            if (value.isEmpty()) continue;
            count++;
            options.append("<button type=\"button\" id=\"lpPrefaceButton")
                    .append(number).append("\" onclick=\"setLpPreface(")
                    .append(number).append(")\"")
                    .append(number == 1 ? " class=\"selected\" aria-pressed=\"true\""
                            : " aria-pressed=\"false\"")
                    .append(">").append(roman(number)).append("</button>");
            bodies.append("<div id=\"lpPreface").append(number)
                    .append("\" class=\"lp-preface")
                    .append(number == 1 ? "\"" : " hidden\"")
                    .append(">").append(value).append("</div>");
        }
        if (count == 0) return "";
        return "<div class=\"lp-prefaces\"><h4>Prefacio dominical</h4>"
                + "<div class=\"choicebar lp-preface-choicebar\">" + options + "</div>"
                + bodies
                + "<script>function setLpPreface(n){for(var i=1;i<=10;i++){"
                + "var p=document.getElementById('lpPreface'+i),b=document.getElementById('lpPrefaceButton'+i);"
                + "var a=i===n;if(p)p.classList.toggle('hidden',!a);if(b){b.classList.toggle('selected',a);"
                + "b.setAttribute('aria-pressed',a?'true':'false');}}}</script></div>";
    }

    /** Cada Plegaria procede de un PDF independiente; una no puede invadir a otra. */
    public static String eucharisticPrayerHtml(Context context, String language, int number)
            throws Exception {
        if (number < 1 || number > 4) throw new IllegalArgumentException("Plegaria inválida");
        return render(component(context, language, "eucharistic_prayer_" + number));
    }

    /** Selector I–IV compatible con setPrayer() del documento combinado. */
    public static String eucharisticPrayersHtml(Context context,
                                                boolean properPrefaceRequired) throws Exception {
        String fourthButton = properPrefaceRequired
                ? "<button id=\"prayerButton4\" disabled aria-disabled=\"true\" "
                + "title=\"No disponible cuando la celebración exige prefacio propio\">IV</button>"
                : "<button onclick=\"setPrayer(4)\" id=\"prayerButton4\" aria-pressed=\"false\">IV</button>";
        String restriction = properPrefaceRequired
                ? "<p class=\"rubric small prayer-restriction\">La Plegaria IV no se usa aquí porque posee prefacio propio e invariable.</p>"
                : "<p class=\"rubric small\">La Plegaria IV incluye su propio prefacio; al elegirla se oculta el prefacio anterior.</p>";

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"eucharistic-prayers\" data-missal-source=\"liturgia-papal-mexico\">"
                + "<h3>Plegaria eucarística</h3><div class=\"choicebar prayer-choicebar\">"
                + "<button onclick=\"setPrayer(1)\" id=\"prayerButton1\" aria-pressed=\"false\">I</button>"
                + "<button onclick=\"setPrayer(2)\" id=\"prayerButton2\" class=\"selected\" aria-pressed=\"true\">II</button>"
                + "<button onclick=\"setPrayer(3)\" id=\"prayerButton3\" aria-pressed=\"false\">III</button>"
                + fourthButton + "</div>" + restriction);
        for (int number = 1; number <= 4; number++) {
            html.append("<div id=\"prayer").append(number)
                    .append("\" class=\"eucharistic-prayer")
                    .append(number == 2 ? "\"" : " hidden\"")
                    .append("><h4>Plegaria Eucarística ").append(roman(number)).append("</h4>")
                    .append(eucharisticPrayerHtml(context, "es", number)).append("</div>");
        }
        return html.append("</div>").toString();
    }

    /**
     * Rito de la Comunión. La antífona propia se inserta donde el Ordinario
     * indica el comienzo del canto de comunión, no al final del rito.
     */
    public static String communionHtml(Context context, String language,
                                       String communionAntiphonHtml) throws Exception {
        String text = component(context, language, "communion");
        String endMarker = "es".equals(normalizeLanguage(language))
                ? "Luego, de pie en la sede" : "Deinde sacerdos";
        String rite = extract(text, null, endMarker);
        if (rite.isEmpty()) return "";
        String[] lines = rite.split("\\r?\\n");
        String marker = "es".equals(normalizeLanguage(language))
                ? "comienza el canto de comunión" : "cantus ad Communionem";
        int position = findContains(lines, marker, 0);
        if (position < 0 || communionAntiphonHtml == null
                || communionAntiphonHtml.trim().isEmpty()) {
            return render(rite);
        }
        return render(join(lines, 0, position + 1))
                + "<div class=\"proper-communion-antiphon\">" + communionAntiphonHtml + "</div>"
                + render(join(lines, position + 1, lines.length));
    }

    public static String conclusionHtml(Context context, String language) throws Exception {
        return render(component(context, language, "conclusion"));
    }

    /**
     * Resuelve el propio del Tiempo Ordinario por el número litúrgico exacto.
     * No hace coincidencias difusas: XXI jamás puede resolverse como III.
     */
    public static String ordinaryProperPartHtml(Context context, Calendar date, String part)
            throws Exception {
        int week = LiturgicalResolver.ordinaryWeekNumber(date);
        if (week < 1 || week > 34) return "";
        String text = component(context, "es", "proper_ordinary");
        String form = ordinaryForm(text, week, date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
        if (form.isEmpty()) return "";

        if (ENTRANCE.equals(part)) {
            return labeledPart(form, "Antífona de entrada", "Oración colecta");
        }
        if (COLLECT.equals(part)) {
            return labeledPart(form, "Oración colecta", "Oración sobre las ofrendas");
        }
        if (OFFERINGS.equals(part)) {
            return labeledPartAnyEnd(form, "Oración sobre las ofrendas",
                    new String[]{"Prefacio", "Antífona de la comunión", "Antífona de comunión"});
        }
        if (COMMUNION_ANTIPHON.equals(part)) {
            return labeledPartAnyStart(form,
                    new String[]{"Antífona de la comunión", "Antífona de comunión"},
                    new String[]{"Oración después de la comunión"});
        }
        if (POST_COMMUNION.equals(part)) {
            return labeledPart(form, "Oración después de la comunión", null);
        }
        return "";
    }

    private static String ordinaryForm(String text, int week, boolean sunday) {
        String roman = roman(week);
        String wantedSunday = normalize(roman + " DOMINGO DEL TIEMPO ORDINARIO");
        String wantedWeek = normalize(roman + " SEMANA DEL TIEMPO ORDINARIO");
        String[] lines = text.split("\\r?\\n");
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wantedSunday) || (!sunday && value.equals(wantedWeek))) {
                start = i;
                break;
            }
        }
        if (start < 0 && !sunday) {
            for (int i = 0; i < lines.length; i++) {
                if (normalize(lines[i]).equals(wantedSunday)) {
                    start = i;
                    break;
                }
            }
        }
        if (start < 0) return "";
        StringBuilder result = new StringBuilder();
        for (int i = start + 1; i < lines.length; i++) {
            if (ORDINARY_HEADING.matcher(normalize(lines[i])).matches()) break;
            result.append(lines[i]).append('\n');
        }
        return result.toString().trim();
    }

    private static String labeledPart(String form, String start, String end) {
        return labeledPartAnyEnd(form, start, end == null ? new String[0] : new String[]{end});
    }

    private static String labeledPartAnyEnd(String form, String start, String[] ends) {
        return labeledPartAnyStart(form, new String[]{start}, ends);
    }

    private static String labeledPartAnyStart(String form, String[] starts, String[] ends) {
        String[] lines = form.split("\\r?\\n");
        int begin = findLabel(lines, starts, 0);
        if (begin < 0) return "";
        int finish = ends == null || ends.length == 0 ? lines.length
                : findLabel(lines, ends, begin + 1);
        if (finish < 0) finish = lines.length;
        return render(join(lines, begin, finish));
    }

    private static int findLabel(String[] lines, String[] labels, int from) {
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            for (String label : labels) {
                String wanted = normalize(label);
                if (value.equals(wanted) || value.startsWith(wanted + " ")) return i;
            }
        }
        return -1;
    }

    private static String extract(String text, String startMarker, String endMarker) {
        String[] lines = text.split("\\r?\\n");
        int start = 0;
        if (startMarker != null && !startMarker.isEmpty()) {
            int found = findStartsWith(lines, startMarker, 0);
            if (found < 0) return "";
            start = found;
        }
        int end = lines.length;
        if (endMarker != null && !endMarker.isEmpty()) {
            int found = findStartsWith(lines, endMarker, start + 1);
            if (found >= 0) end = found;
        }
        return join(lines, start, end);
    }

    private static int findStartsWith(String[] lines, String marker, int from) {
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wanted) || value.startsWith(wanted)) return i;
        }
        return -1;
    }

    private static int findContains(String[] lines, String marker, int from) {
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            if (normalize(lines[i]).contains(wanted)) return i;
        }
        return -1;
    }

    private static int findLastContains(String[] lines, String marker, int before) {
        String wanted = normalize(marker);
        for (int i = Math.min(before, lines.length - 1); i >= 0; i--) {
            if (normalize(lines[i]).contains(wanted)) return i;
        }
        return -1;
    }

    private static int findNextPrefaceHeading(String[] lines, int from) {
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.startsWith("PREFACIO ") && normalize(raw).startsWith("prefacio ")) return i;
        }
        return -1;
    }

    private static String join(String[] lines, int start, int end) {
        StringBuilder result = new StringBuilder();
        for (int i = Math.max(0, start); i < Math.min(lines.length, end); i++) {
            result.append(lines[i]).append('\n');
        }
        return result.toString().trim();
    }

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder(
                "<div class=\"liturgia-papal\" data-missal-source=\"liturgia-papal-mexico\">");
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        String value = PDF_PAGE_HEADER.matcher(text).replaceAll("");
        value = value.replaceAll("(?im)^\\s*(?:www\\.)?liturgiapapal\\.org\\s*$", "");
        value = value.replaceAll("(?m)[ \\t]+$", "");
        return value.replaceAll("\\n{3,}", "\\n\\n").trim() + "\\n";
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String normalizeLanguage(String language) {
        return "la".equalsIgnoreCase(language) ? "la" : "es";
    }

    private static String normalize(String value) {
        return stripAccents(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String roman(int number) {
        int[] values = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                result.append(symbols[i]);
                remaining -= values[i];
            }
        }
        return result.toString();
    }
}
