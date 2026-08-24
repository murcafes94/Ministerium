package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Acceso semántico al paquete del Misal generado desde Liturgia Papal.
 *
 * El APK no interpreta los PDF ni depende de su paginación. El preprocesador
 * tools/build_liturgiapapal_missal.py produce texto limpio en assets/missal/ y
 * esta clase lo resuelve por componente y por unidad litúrgica.
 *
 * Español = edición de México de Liturgia Papal. Latín = Missale Romanum.
 */
public final class LiturgiaPapalMissalRepository {
    public static final String ENTRANCE = "entrance";
    public static final String COLLECT = "collect";
    public static final String OFFERINGS = "offerings";
    public static final String COMMUNION_ANTIPHON = "communion_antiphon";
    public static final String POST_COMMUNION = "post_communion";

    private static final String BASE = "missal/";
    private static final Pattern ORDINARY_HEADING = Pattern.compile(
            "^[IVXLCDM]+\\s+(?:DOMINGO|SEMANA)\\s+DEL\\s+TIEMPO\\s+ORDINARIO$",
            Pattern.CASE_INSENSITIVE);

    private LiturgiaPapalMissalRepository() {}

    public static boolean isAvailable(Context context, String language) {
        try {
            String lang = normalizeLanguage(language);
            String[] files = context.getAssets().list(BASE + lang);
            if (files == null || files.length == 0) return false;
            boolean initial = false;
            boolean communion = false;
            for (String file : files) {
                if ("initial.txt".equals(file)) initial = true;
                if ("communion.txt".equals(file)) communion = true;
            }
            return initial && communion;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String component(Context context, String language, String id) throws Exception {
        return readAsset(context, BASE + normalizeLanguage(language) + "/" + id + ".txt");
    }

    /** Kyrie + Gloria de la edición elegida, sin el acto penitencial. */
    public static String kyrieAndGloriaHtml(Context context, String language) throws Exception {
        String text = component(context, language, "initial");
        String start = "es".equals(normalizeLanguage(language))
                ? "V/. Señor, ten piedad." : "V/. Kýrie, eléison.";
        String end = "es".equals(normalizeLanguage(language))
                ? "Acabado el himno" : "Quo hymno expleto";
        return render(extract(text, start, end));
    }

    /** Preparación de los dones hasta antes del diálogo del prefacio. */
    public static String preparationHtml(Context context, String language) throws Exception {
        String text = component(context, language, "eucharistic_liturgy");
        String end = "es".equals(normalizeLanguage(language))
                ? "PLEGARIA EUCARÍSTICA" : "PREX EUCHARISTICA";
        return render(extract(text, null, end));
    }

    /** Diálogo inicial del prefacio; el texto concreto del prefacio se resuelve aparte. */
    public static String prefaceDialogueHtml(Context context, String language) throws Exception {
        String text = component(context, language, "eucharistic_liturgy");
        if ("es".equals(normalizeLanguage(language))) {
            return render(extract(text,
                    "El sacerdote comienza la plegaria eucarística con el prefacio.",
                    "El sacerdote prosigue el Prefacio"));
        }
        return render(extract(text, "Dóminus vobíscum.", "Sacerdos prosequitur præfationem"));
    }

    /** Cada plegaria procede de su PDF independiente: no puede invadir la siguiente. */
    public static String eucharisticPrayerHtml(Context context, String language, int number)
            throws Exception {
        if (number < 1 || number > 4) throw new IllegalArgumentException("Plegaria inválida");
        return render(component(context, language, "eucharistic_prayer_" + number));
    }

    /** Rito de la Comunión sin la rúbrica de la oración poscomunión. */
    public static String communionHtml(Context context, String language) throws Exception {
        String text = component(context, language, "communion");
        String end = "es".equals(normalizeLanguage(language))
                ? "Luego, de pie en la sede" : "Deinde sacerdos";
        return render(extract(text, null, end));
    }

    public static String conclusionHtml(Context context, String language) throws Exception {
        return render(component(context, language, "conclusion"));
    }

    /**
     * Resuelve el propio del Tiempo Ordinario por número de semana/domingo.
     * Es la primera migración del propio porque evita el fallo III/XXI del EPUB.
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
            String result = labeledPartAnyStart(form,
                    new String[]{"Antífona de la comunión", "Antífona de comunión"},
                    new String[]{"Oración después de la comunión"});
            return result;
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
            String trimmed = lines[i].trim();
            if (ORDINARY_HEADING.matcher(stripAccents(trimmed)).matches()) break;
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
        StringBuilder text = new StringBuilder(lines[begin].trim()).append('\n');
        for (int i = begin + 1; i < finish; i++) text.append(lines[i]).append('\n');
        return render(text.toString().trim());
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
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) result.append(lines[i]).append('\n');
        return result.toString().trim();
    }

    private static int findStartsWith(String[] lines, String marker, int from) {
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wanted) || value.startsWith(wanted)) return i;
        }
        return -1;
    }

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder("<div class=\"liturgia-papal\" data-missal-source=\"liturgia-papal\">");
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
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
