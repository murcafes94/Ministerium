package com.fabri.ministerium;

import android.content.Context;
import android.text.Html;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Propios diarios tomados de la misma página que Ministerium usa para el
 * Leccionario: Arquidiócesis de Guadalajara. La red solo se usa al preparar la
 * fecha; después los textos quedan en cache local junto a las lecturas.
 */
public final class DailyMassProperRepository {
    private static final int FORMAT = 2;
    private static final String[] MONTHS = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    public static final class ProperDay {
        public final String celebration;
        public final String entrance;
        public final String collect;
        public final String offerings;
        public final String communionAntiphon;
        public final String postCommunion;

        ProperDay(String celebration, String entrance, String collect, String offerings,
                  String communionAntiphon, String postCommunion) {
            this.celebration = value(celebration);
            this.entrance = value(entrance);
            this.collect = value(collect);
            this.offerings = value(offerings);
            this.communionAntiphon = value(communionAntiphon);
            this.postCommunion = value(postCommunion);
        }

        public boolean isComplete() {
            return !collect.isEmpty() && !offerings.isEmpty() && !postCommunion.isEmpty();
        }
    }

    private DailyMassProperRepository() {}

    /**
     * Returns a cached day, downloading it first only when the requested date
     * belongs to the currently published month. Call this from a worker thread.
     */
    public static synchronized ProperDay getOrSync(Context context, Calendar date) {
        ProperDay cached = cached(context, date);
        if (cached != null && cached.isComplete()) return cached;
        if (!MassReadingsRepository.isCurrentMonth(date)) return cached;
        try {
            ProperDay day = parse(download(MassReadingsRepository.sourceUrl(date)), date);
            write(context, date, day);
            return day;
        } catch (Exception ignored) {
            return cached;
        }
    }

    /** Reuses the exact HTML already downloaded by the Lectionary sync. */
    static synchronized void cacheFromSourceHtml(Context context, Calendar date,
                                                 String rawHtml) throws Exception {
        ProperDay day = parse(rawHtml, date);
        write(context, date, day);
    }

    public static ProperDay cached(Context context, Calendar date) {
        File file = fileFor(context, date);
        if (!file.isFile()) return null;
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONObject json = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            if (json.optInt("format", 0) != FORMAT) return null;
            return new ProperDay(json.optString("celebration"), json.optString("entrance"),
                    json.optString("collect"), json.optString("offerings"),
                    json.optString("communionAntiphon"), json.optString("postCommunion"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String render(String text) {
        String clean = value(text);
        if (clean.isEmpty()) return "";
        StringBuilder html = new StringBuilder("<div class=\"daily-proper\" data-missal-source=\"arquidiocesis-gdl\">");
        for (String paragraph : clean.split("\\n\\s*\\n")) {
            String p = paragraph.trim();
            if (!p.isEmpty()) html.append("<p>").append(escape(p).replace("\n", "<br>"))
                    .append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static ProperDay parse(String rawHtml, Calendar date) throws Exception {
        String prepared = rawHtml
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "</p>\n")
                .replaceAll("(?i)</div>", "</div>\n")
                .replaceAll("(?i)</h[1-6]>", "$0\n")
                .replaceAll("(?i)</li>", "$0\n");
        String plain = Html.fromHtml(prepared).toString()
                .replace('\u00a0', ' ')
                .replace("\r", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n");
        String markerA = date.get(Calendar.DAY_OF_MONTH) + " de "
                + MONTHS[date.get(Calendar.MONTH)] + " del " + date.get(Calendar.YEAR);
        String markerB = date.get(Calendar.DAY_OF_MONTH) + " de "
                + MONTHS[date.get(Calendar.MONTH)] + " de " + date.get(Calendar.YEAR);
        String[] lines = plain.split("\\n");
        int dateLine = findContains(lines, new String[]{markerA, markerB}, 0);
        if (dateLine < 0) throw new IllegalStateException("La fecha de los propios no coincide.");

        int entrance = findStarts(lines, new String[]{"ANTÍFONA DE ENTRADA", "ANTIFONA DE ENTRADA"}, dateLine);
        int collect = findStarts(lines, new String[]{"ORACIÓN COLECTA", "ORACION COLECTA"}, entrance + 1);
        int firstReading = findStarts(lines, new String[]{"PRIMERA LECTURA"}, collect + 1);
        int offerings = findStarts(lines,
                new String[]{"ORACIÓN SOBRE LAS OFRENDAS", "ORACION SOBRE LAS OFRENDAS"}, firstReading + 1);
        int communion = findStarts(lines,
                new String[]{"ANTÍFONA DE LA COMUNIÓN", "ANTIFONA DE LA COMUNION",
                        "ANTÍFONA DE COMUNIÓN", "ANTIFONA DE COMUNION"}, offerings + 1);
        int post = findStarts(lines,
                new String[]{"ORACIÓN DESPUÉS DE LA COMUNIÓN", "ORACION DESPUES DE LA COMUNION"}, communion + 1);
        if (entrance < 0 || collect < 0 || firstReading < 0 || offerings < 0
                || communion < 0 || post < 0) {
            throw new IllegalStateException("La página no contiene todos los propios de la Misa.");
        }

        String celebration = celebration(lines, dateLine, entrance);
        String entranceText = withHeadingReference(lines[entrance], "ANTÍFONA DE ENTRADA",
                cleanRange(lines, entrance + 1, collect));
        String collectText = cleanRange(lines, collect + 1, firstReading);
        String offeringsText = cleanRange(lines, offerings + 1, communion);
        String communionText = withHeadingReference(lines[communion], "ANTÍFONA DE LA COMUNIÓN",
                cleanRange(lines, communion + 1, post));
        String postText = cleanRange(lines, post + 1, properEnd(lines, post + 1));

        ProperDay day = new ProperDay(celebration, entranceText, collectText,
                offeringsText, communionText, postText);
        if (!day.isComplete()) throw new IllegalStateException("Los propios diarios están incompletos.");
        return day;
    }

    private static String celebration(String[] lines, int from, int to) {
        String best = "";
        for (int i = Math.max(0, from + 1); i < Math.min(lines.length, to); i++) {
            String line = lines[i].trim();
            String n = normalize(line);
            if (line.length() > 4 && line.length() < 180
                    && (n.contains("feria") || n.contains("san ") || n.contains("santa ")
                    || n.contains("domingo") || n.contains("solemnidad") || n.contains("fiesta")
                    || n.contains("memoria"))) best = line;
        }
        return best;
    }

    private static String withHeadingReference(String heading, String label, String body) {
        String cleanHeading = heading == null ? "" : heading.trim();
        String normalizedLabel = normalize(label);
        String normalizedHeading = normalize(cleanHeading);
        String reference = "";
        if (normalizedHeading.startsWith(normalizedLabel) && cleanHeading.length() > label.length()) {
            int offset = Math.min(cleanHeading.length(), label.length());
            reference = cleanHeading.substring(offset).trim();
        }
        if (reference.isEmpty()) return body;
        return reference + "\n\n" + body;
    }

    private static int properEnd(String[] lines, int from) {
        int max = Math.min(lines.length, from + 14);
        for (int i = from; i < max; i++) {
            String n = normalize(lines[i]);
            if (n.startsWith("actividad diocesana") || n.contains("biblia catecismo")
                    || n.startsWith("lectura de dia") || n.contains("noticias vaticano")
                    || n.contains("aviso de privacidad")
                    || n.contains("arquidiocesis de guadalajara")) return i;
        }
        return max;
    }

    private static int findStarts(String[] lines, String[] markers, int from) {
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            for (String marker : markers) if (value.startsWith(normalize(marker))) return i;
        }
        return -1;
    }

    private static int findContains(String[] lines, String[] markers, int from) {
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            for (String marker : markers) if (value.contains(normalize(marker))) return i;
        }
        return -1;
    }

    private static String cleanRange(String[] lines, int start, int end) {
        List<String> paragraphs = new ArrayList<>();
        for (int i = Math.max(0, start); i < Math.min(lines.length, end); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String n = normalize(line);
            if (n.startsWith("mr pp ") || n.startsWith("lecc ")) continue;
            paragraphs.add(line);
        }
        return joinParagraphs(paragraphs);
    }

    private static String joinParagraphs(List<String> lines) {
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (result.length() > 0) result.append("\n\n");
            result.append(line);
        }
        return result.toString().trim();
    }

    private static void write(Context context, Calendar date, ProperDay day) throws Exception {
        File file = fileFor(context, date);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("No se pudo preparar el cache de propios.");
        }
        JSONObject json = new JSONObject()
                .put("format", FORMAT)
                .put("celebration", day.celebration)
                .put("entrance", day.entrance)
                .put("collect", day.collect)
                .put("offerings", day.offerings)
                .put("communionAntiphon", day.communionAntiphon)
                .put("postCommunion", day.postCommunion);
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(json.toString().getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (file.exists() && !file.delete()) throw new IllegalStateException("No se pudo actualizar los propios.");
        if (!temporary.renameTo(file)) throw new IllegalStateException("No se pudo activar los propios.");
    }

    private static File fileFor(Context context, Calendar date) {
        File root = new File(context.getFilesDir(), "mass-propers/" + date.get(Calendar.YEAR)
                + "/" + String.format(Locale.US, "%02d", date.get(Calendar.MONTH) + 1));
        return new File(root, String.format(Locale.US, "%02d.json", date.get(Calendar.DAY_OF_MONTH)));
    }

    private static String download(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Ministerium/4.0 Android");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Respuesta HTTP " + status);
        }
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            Charset charset = StandardCharsets.UTF_8;
            String contentType = connection.getContentType();
            if (contentType != null) {
                Matcher declared = Pattern.compile(
                        "(?i)charset\\s*=\\s*[\"']?([^;\"']+)").matcher(contentType);
                if (declared.find()) try {
                    charset = Charset.forName(declared.group(1).trim());
                } catch (Exception ignored) {
                    charset = StandardCharsets.UTF_8;
                }
            }
            return new String(output.toByteArray(), charset);
        } finally {
            connection.disconnect();
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String value(String value) { return value == null ? "" : value.trim(); }
}
