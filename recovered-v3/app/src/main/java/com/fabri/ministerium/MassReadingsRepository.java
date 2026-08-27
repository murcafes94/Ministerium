package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;

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
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descarga una vez al mes las páginas textuales del Misal de Guadalajara,
 * conserva las lecturas y reutiliza el mismo HTML para guardar los propios de
 * la Misa. El lector diario funciona después sin red.
 */
public final class MassReadingsRepository {
    public interface ProgressListener {
        void onProgress(int completed, int total);
    }

    public static final class SyncResult {
        public final int saved;
        public final int total;

        SyncResult(int saved, int total) {
            this.saved = saved;
            this.total = total;
        }
    }

    private static final String SOURCE =
            "https://arquidiocesisgdl.org/lectura_dia%d.php";
    private static final String PREFS = "mass_readings_content";
    private static final String[] MONTHS = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };
    private static final Pattern BRACKETED = Pattern.compile("\\[([^\\]]{8,240})\\]");
    private static final Pattern SCRIPTURE_REFERENCE = Pattern.compile(
            "\\b\\d{1,3}\\s*,\\s*\\d+[a-z]?(?:\\s*[-–—]\\s*\\d+[a-z]?"
                    + "(?:\\s*,\\s*\\d+[a-z]?)?)?(?:\\s*[.;]\\s*\\d+[a-z]?"
                    + "(?:\\s*[-–—]\\s*\\d+[a-z]?)?)*",
            Pattern.CASE_INSENSITIVE);
    private static final String FORMAT_MARKER = "data-format=\"4\"";

    private MassReadingsRepository() {}

    public static boolean has(Context context, Calendar date) {
        return fileFor(context, date).isFile();
    }

    public static boolean needsFormattingUpdate(Context context, Calendar date) {
        File file = fileFor(context, date);
        if (!file.isFile()) return false;
        try {
            return !readFile(file).contains(FORMAT_MARKER);
        } catch (Exception ignored) {
            return true;
        }
    }

    public static boolean monthNeedsFormattingUpdate(Context context, Calendar date) {
        File[] files = monthRoot(context, date).listFiles(
                (directory, name) -> name.endsWith(".html"));
        if (files == null) return false;
        for (File file : files) try {
            if (!readFile(file).contains(FORMAT_MARKER)) return true;
        } catch (Exception ignored) {
            return true;
        }
        return false;
    }

    public static File fileFor(Context context, Calendar date) {
        return new File(monthRoot(context, date), String.format(Locale.US, "%02d.html",
                date.get(Calendar.DAY_OF_MONTH)));
    }

    public static String gospelSummary(Context context, Calendar date) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("gospel_" + key(date), "");
    }

    public static String sourceUrl(Calendar date) {
        return String.format(Locale.US, SOURCE, date.get(Calendar.DAY_OF_MONTH));
    }

    public static boolean isCurrentMonth(Calendar date) {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == date.get(Calendar.MONTH);
    }

    public static int cachedDays(Context context, Calendar date) {
        File[] files = monthRoot(context, date).listFiles((dir, name) -> name.endsWith(".html"));
        return files == null ? 0 : files.length;
    }

    public static synchronized SyncResult syncCurrentMonth(Context context, Calendar month,
                                                            ProgressListener listener)
            throws Exception {
        if (!isCurrentMonth(month)) {
            throw new IllegalArgumentException(
                    "La fuente publica las páginas individuales del mes actual.");
        }
        Calendar cursor = (Calendar) month.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int total = cursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        int saved = 0;
        int consecutiveFailures = 0;
        File root = monthRoot(context, cursor);
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException("No se pudo preparar el mes de lecturas.");
        }

        for (int day = 1; day <= total; day++) {
            cursor.set(Calendar.DAY_OF_MONTH, day);
            try {
                syncDay(context, cursor);
                saved++;
                consecutiveFailures = 0;
            } catch (Exception ignored) {
                consecutiveFailures++;
            }
            if (listener != null) listener.onProgress(day, total);
            if (consecutiveFailures >= 3) break;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("count_" + monthKey(cursor), saved).apply();
        if (saved == 0) throw new IllegalStateException(
                "La fuente no entregó lecturas válidas para este mes.");
        return new SyncResult(saved, total);
    }

    public static synchronized void syncDay(Context context, Calendar date) throws Exception {
        if (!isCurrentMonth(date)) throw new IllegalArgumentException(
                "La fuente diaria sólo corresponde al mes actual.");
        String rawHtml = download(sourceUrl(date));
        ReadingPage page = extract(rawHtml, date);
        writeAtomic(fileFor(context, date), page.html);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("gospel_" + key(date), page.gospelSummary).apply();
        try {
            DailyMassProperRepository.cacheFromSourceHtml(context, date, rawHtml);
        } catch (Exception ignored) {
            // Una anomalía en los propios nunca invalida lecturas ya verificadas.
        }
    }

    public static String read(Context context, Calendar date) throws Exception {
        File file = fileFor(context, date);
        if (!file.isFile()) throw new IllegalStateException("La lectura no está guardada.");
        return readFile(file);
    }

    private static String readFile(File file) throws Exception {
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static ReadingPage extract(String rawHtml, Calendar date) throws Exception {
        String prepared = rawHtml
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "</p>\n")
                .replaceAll("(?i)</div>", "</div>\n")
                .replaceAll("(?i)</h[1-6]>", "$0\n");
        String plain = Html.fromHtml(prepared).toString()
                .replace('\u00a0', ' ')
                .replace("\r", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n");

        String marker = date.get(Calendar.DAY_OF_MONTH) + " de "
                + MONTHS[date.get(Calendar.MONTH)] + " del " + date.get(Calendar.YEAR);
        int dateStart = normalize(plain).indexOf(normalize(marker));
        if (dateStart < 0) throw new IllegalStateException("La fecha descargada no coincide.");
        String day = plain.substring(dateStart);

        int first = indexOf(day, "PRIMERA LECTURA", 0);
        int psalm = indexOf(day, "SALMO RESPONSORIAL", first + 1);
        int second = indexOf(day, "SEGUNDA LECTURA", psalm + 1);
        int acclamation = indexOf(day, "ACLAMACIÓN ANTES DEL EVANGELIO", psalm + 1);
        if (acclamation < 0) acclamation = indexOf(day,
                "ACLAMACION ANTES DEL EVANGELIO", psalm + 1);
        int gospelSearchStart = acclamation < 0 ? psalm + 1
                : acclamation + "ACLAMACIÓN ANTES DEL EVANGELIO".length();
        int gospel = indexOf(day, "EVANGELIO", gospelSearchStart);
        if (first < 0 || psalm < 0 || acclamation < 0 || gospel < 0) {
            throw new IllegalStateException("La página no contiene todas las lecturas.");
        }

        int end = firstPositive(
                indexOf(day, "REFLEXIÓN:", gospel + 1),
                indexOf(day, "REFLEXION:", gospel + 1),
                indexOf(day, "SE DICE CREDO", gospel + 1),
                indexOf(day, "SE PUEDE DECIR CREDO", gospel + 1),
                indexOf(day, "ORACIÓN DE LOS FIELES", gospel + 1),
                indexOf(day, "ORACION DE LOS FIELES", gospel + 1),
                indexOf(day, "PLEGARIA UNIVERSAL", gospel + 1),
                indexOf(day, "ORACIÓN SOBRE LAS OFRENDAS", gospel + 1),
                indexOf(day, "ORACION SOBRE LAS OFRENDAS", gospel + 1),
                indexOf(day, "ACTIVIDAD DIOCESANA", gospel + 1));
        if (end < 0) end = day.length();

        String header = clean(day.substring(0, first));
        String firstText = clean(day.substring(first + "PRIMERA LECTURA".length(), psalm));
        int psalmEnd = second >= 0 && second < acclamation ? second : acclamation;
        String psalmText = clean(day.substring(psalm + "SALMO RESPONSORIAL".length(), psalmEnd));
        String secondText = second >= 0 && second < acclamation
                ? clean(day.substring(second + "SEGUNDA LECTURA".length(), acclamation)) : "";
        String acclamationText = clean(day.substring(acclamation
                + "ACLAMACIÓN ANTES DEL EVANGELIO".length(), gospel));
        String gospelText = clean(day.substring(gospel + "EVANGELIO".length(), end));

        String gospelSummary = "";
        Matcher summary = BRACKETED.matcher(gospelText);
        if (summary.find()) gospelSummary = clean(summary.group(1));

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("</head><body><article ").append(FORMAT_MARKER).append(">")
                .append("<h1>").append(escape(longDate(date))).append("</h1>");
        String celebration = celebration(header, marker);
        if (!celebration.isEmpty()) html.append("<p class=\"celebration\">")
                .append(escape(celebration)).append("</p>");
        html.append("<p class=\"lectionary-label\">LECCIONARIO</p>");
        readingSection(html, "Primera lectura", firstText);
        psalmSection(html, psalmText);
        if (!secondText.isEmpty()) readingSection(html, "Segunda lectura", secondText);
        acclamationSection(html, acclamationText);
        readingSection(html, "Evangelio", gospelText);
        html.append("<p class=\"source\">Fuente: Arquidiócesis de Guadalajara · texto guardado para uso personal.</p>")
                .append("</article></body></html>");
        return new ReadingPage(html.toString(), gospelSummary);
    }

    private static String celebration(String header, String marker) {
        String value = header;
        int markerIndex = normalize(value).indexOf(normalize(marker));
        if (markerIndex >= 0) value = value.substring(Math.min(value.length(),
                markerIndex + marker.length()));
        String[] lines = value.split("\\n");
        for (String line : lines) {
            String clean = clean(line);
            String normalized = normalize(clean);
            if (clean.length() > 4 && clean.length() < 150
                    && (normalized.contains("memoria") || normalized.contains("fiesta")
                    || normalized.contains("solemnidad") || normalized.contains("feria")
                    || normalized.contains("domingo"))) return clean;
        }
        return "";
    }

    private static void readingSection(StringBuilder html, String title, String text) {
        ReadingParts parts = readingParts(text);
        html.append("<section class=\"reading-section\"><h2>")
                .append(escape(title)).append("</h2>");
        if (!parts.summary.isEmpty()) html.append("<p class=\"reading-summary\">")
                .append(escape(parts.summary)).append("</p>");
        if (!parts.reference.isEmpty()) html.append("<p class=\"reading-reference\">")
                .append(escape(parts.reference)).append("</p>");
        appendParagraphs(html, parts.body, "reading-body");
        html.append("</section>");
    }

    private static void psalmSection(StringBuilder html, String text) {
        String value = clean(text);
        int responseAt = responseIndex(value, 0);
        if (responseAt < 0) {
            html.append("<section class=\"reading-section psalm-section\"><h2>")
                    .append("Salmo responsorial</h2>");
            appendParagraphs(html, value, "psalm-stanza");
            html.append("</section>");
            return;
        }
        String reference = clean(value.substring(0, responseAt));
        String responseAndVerses = clean(value.substring(responseAt + 2));
        int responseEnd = sentenceEnd(responseAndVerses);
        String response = responseEnd < 0 ? responseAndVerses
                : clean(responseAndVerses.substring(0, responseEnd));
        String verses = responseEnd < 0 ? ""
                : clean(responseAndVerses.substring(responseEnd));

        html.append("<section class=\"reading-section psalm-section\"><h2>")
                .append("Salmo responsorial</h2>");
        if (!reference.isEmpty()) html.append("<p class=\"reading-reference\">")
                .append(escape(reference)).append("</p>");
        if (response.isEmpty()) {
            appendParagraphs(html, verses, "psalm-stanza");
            html.append("</section>");
            return;
        }
        appendResponse(html, response);
        String[] stanzas = verses.split("(?i)\\s+R\\.\\s*");
        for (String stanza : stanzas) {
            String cleanStanza = clean(stanza).replaceFirst("(?i)^R\\.\\s*", "")
                    .replaceFirst("(?i)\\s+R\\.$", "");
            if (cleanStanza.isEmpty()) continue;
            html.append("<p class=\"psalm-stanza\">")
                    .append(verseHtml(cleanStanza)).append("</p>");
            appendResponse(html, response);
        }
        html.append("</section>");
    }

    private static void acclamationSection(StringBuilder html, String text) {
        ReadingParts parts = readingParts(text);
        String body = parts.body;
        int first = responseIndex(body, 0);
        String opening = "";
        String verse = body;
        String closing = "";
        if (first >= 0) {
            String after = clean(body.substring(first + 2));
            int end = sentenceEnd(after);
            if (end >= 0) {
                opening = clean(after.substring(0, end));
                verse = clean(after.substring(end));
                int last = lastResponseIndex(verse);
                if (last >= 0) {
                    closing = clean(verse.substring(last + 2));
                    verse = clean(verse.substring(0, last));
                }
            }
        }
        html.append("<section class=\"reading-section acclamation-section\"><h2>")
                .append("Aclamación antes del Evangelio</h2>");
        if (!parts.reference.isEmpty()) html.append("<p class=\"reading-reference\">")
                .append(escape(parts.reference)).append("</p>");
        if (!opening.isEmpty()) appendResponse(html, opening);
        appendParagraphs(html, verse, "reading-body");
        if (!closing.isEmpty()) appendResponse(html, closing);
        html.append("</section>");
    }

    private static ReadingParts readingParts(String text) {
        String value = clean(text);
        String summary = "";
        Matcher bracketed = BRACKETED.matcher(value);
        if (bracketed.find() && bracketed.start() == 0) {
            summary = clean(bracketed.group(1));
            value = clean(value.substring(bracketed.end()));
        }
        String reference = "";
        Matcher scripture = SCRIPTURE_REFERENCE.matcher(value);
        if (scripture.find() && scripture.start() < 220) {
            reference = clean(value.substring(0, scripture.end()));
            value = clean(value.substring(scripture.end()));
        } else {
            int line = value.indexOf('\n');
            if (line > 0 && line < 220) {
                reference = clean(value.substring(0, line));
                value = clean(value.substring(line + 1));
            }
        }
        return new ReadingParts(summary, reference, value);
    }

    private static void appendParagraphs(StringBuilder html, String text, String cssClass) {
        for (String paragraph : clean(text).split("\\n+")) {
            String value = clean(paragraph);
            if (!value.isEmpty()) html.append("<p class=\"").append(cssClass)
                    .append("\">").append(escape(value)).append("</p>");
        }
    }

    private static void appendResponse(StringBuilder html, String response) {
        String value = clean(response).replaceFirst("(?i)^R\\.\\s*", "");
        if (!value.isEmpty()) html.append("<p class=\"psalm-response\"><b>R. ")
                .append(escape(value)).append("</b></p>");
    }

    private static String verseHtml(String stanza) {
        String escaped = escape(stanza).replace("\n", "<br>");
        return escaped.replaceAll("([.!?])\\s+(?=[A-ZÁÉÍÓÚÜÑ¿¡])", "$1<br>");
    }

    private static int responseIndex(String value, int from) {
        Matcher matcher = Pattern.compile("(?i)(?:^|\\s)(R\\.)\\s*").matcher(value);
        return matcher.find(Math.max(0, from)) ? matcher.start(1) : -1;
    }

    private static int lastResponseIndex(String value) {
        Matcher matcher = Pattern.compile("(?i)(?:^|\\s)(R\\.)\\s*").matcher(value);
        int result = -1;
        while (matcher.find()) result = matcher.start(1);
        return result;
    }

    private static int sentenceEnd(String value) {
        Matcher matcher = Pattern.compile("[.!?](?:\\s|$)").matcher(value);
        return matcher.find() ? matcher.end() : -1;
    }

    private static int indexOf(String text, String wanted, int from) {
        return normalize(text).indexOf(normalize(wanted), Math.max(0, from));
    }

    private static int firstPositive(int... values) {
        int result = -1;
        for (int value : values) if (value >= 0 && (result < 0 || value < result)) result = value;
        return result;
    }

    private static String clean(String value) {
        return value.replace('\u00a0', ' ').replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
    }

    private static String download(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Ministerium/4.0 (lector liturgico personal)");
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
                Matcher declared = Pattern.compile("(?i)charset\\s*=\\s*[\"']?([^;\"']+)")
                        .matcher(contentType);
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

    private static void writeAtomic(File target, String content) throws Exception {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("No se pudo crear la carpeta de lecturas.");
        }
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        if (target.exists() && !target.delete()) throw new IllegalStateException(
                "No se pudo reemplazar la lectura anterior.");
        if (!temporary.renameTo(target)) throw new IllegalStateException(
                "No se pudo activar la lectura descargada.");
    }

    private static File monthRoot(Context context, Calendar date) {
        return new File(context.getFilesDir(), "mass_readings/" + monthKey(date));
    }

    private static String monthKey(Calendar date) {
        return String.format(Locale.US, "%04d-%02d", date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1);
    }

    private static String key(Calendar date) {
        return String.format(Locale.US, "%04d%02d%02d", date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
    }

    private static String longDate(Calendar date) {
        String value = new java.text.SimpleDateFormat(
                "EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "EC"))
                .format(date.getTime());
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final class ReadingPage {
        final String html;
        final String gospelSummary;

        ReadingPage(String html, String gospelSummary) {
            this.html = html;
            this.gospelSummary = gospelSummary;
        }
    }

    private static final class ReadingParts {
        final String summary;
        final String reference;
        final String body;

        ReadingParts(String summary, String reference, String body) {
            this.summary = summary;
            this.reference = reference;
            this.body = body;
        }
    }
}
