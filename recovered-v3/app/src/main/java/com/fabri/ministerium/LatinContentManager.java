package com.fabri.ministerium;

import android.content.Context;
import android.text.Html;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Instala, valida e indexa los EPUB latinos anuales de breviar.sk. */
public final class LatinContentManager {
    public interface ProgressListener {
        void onProgress(String message);
    }

    public static final String DOWNLOAD_PAGE = "https://breviar.sk/download/main.htm";
    private static final String BUNDLED_2026 = "epubs/Liturgia-horarum-2026-latin.epub";
    private static final Pattern LINK = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final long MAX_EPUB_BYTES = 180L * 1024L * 1024L;
    private static final long MAX_EXTRACTED_BYTES = 450L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 30000;

    public static final class LatinDay {
        public final int year;
        public final String dateTitle;
        public final String celebration;
        public final String rank;
        public final Map<String, String> hours;

        LatinDay(int year, String dateTitle, String celebration, String rank,
                 Map<String, String> hours) {
            this.year = year;
            this.dateTitle = dateTitle;
            this.celebration = celebration;
            this.rank = rank;
            this.hours = hours;
        }
    }

    private LatinContentManager() {}

    public static synchronized File ensureYear(Context context, int year) throws Exception {
        File root = rootFor(context, year);
        if (new File(root, ".ready").isFile()) return root;
        File epub = epubFor(context, year);
        if (epub.isFile()) {
            validate(epub, year);
            extract(epub, root);
            validateExtracted(root, year);
            markReady(root, year);
            return root;
        }
        if (year == 2026) {
            try (InputStream input = context.getAssets().open(BUNDLED_2026)) {
                install(context, input, year);
            }
            return rootFor(context, year);
        }
        throw new IllegalStateException("El año latino " + year + " aún no está instalado.");
    }

    public static boolean isAvailable(Context context, int year) {
        return new File(rootFor(context, year), ".ready").isFile()
                || epubFor(context, year).isFile() || year == 2026;
    }

    public static String status(Context context, int year) {
        if (new File(rootFor(context, year), ".ready").isFile()) {
            return "Liturgia Horarum " + year + " · latín verificado · disponible sin conexión";
        }
        if (year == 2026) return "Liturgia Horarum 2026 incluida · se preparará al abrirla";
        return "El paquete latino de " + year + " aún no está instalado";
    }

    public static synchronized void install(Context context, InputStream input, int year)
            throws Exception {
        File base = base(context);
        if (!base.exists() && !base.mkdirs()) throw new IllegalStateException(
                "No se pudo preparar la Liturgia latina.");
        File temporary = new File(base, "latin_" + year + ".download");
        copy(input, temporary);
        validate(temporary, year);

        File staged = new File(base, year + ".staged");
        deleteTree(staged);
        extract(temporary, staged);
        validateExtracted(staged, year);

        File targetEpub = epubFor(context, year);
        File targetRoot = rootFor(context, year);
        if (targetEpub.exists() && !targetEpub.delete()) throw new IllegalStateException(
                "No se pudo reemplazar el EPUB latino anterior.");
        if (!temporary.renameTo(targetEpub)) throw new IllegalStateException(
                "No se pudo guardar el EPUB latino.");
        deleteTree(targetRoot);
        if (!staged.renameTo(targetRoot)) throw new IllegalStateException(
                "No se pudo activar el año latino.");
        markReady(targetRoot, year);
        context.getSharedPreferences("latin_content", Context.MODE_PRIVATE).edit()
                .putInt("installed_" + year, year).apply();
    }

    public static void updateFromOfficial(Context context, int year,
                                          ProgressListener listener) throws Exception {
        if (listener != null) listener.onProgress("Buscando los textos latinos de " + year + "…");
        String page = downloadText(DOWNLOAD_PAGE);
        List<String> links = findLatinEpubCandidates(page, year);
        if (links.isEmpty()) throw new IllegalStateException(
                "Breviar.sk aún no publica un EPUB latino identificable para " + year + ".");
        Exception last = null;
        for (int index = 0; index < links.size(); index++) {
            URL resolved = new URL(new URL(DOWNLOAD_PAGE), links.get(index));
            if (listener != null) listener.onProgress(
                    "Descargando y verificando el paquete latino " + (index + 1)
                            + " de " + links.size() + "…");
            HttpURLConnection connection = null;
            try {
                connection = open(resolved.toString());
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    install(context, input, year);
                }
                if (listener != null) listener.onProgress("Año latino verificado e instalado");
                return;
            } catch (Exception error) {
                last = error;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        throw new IllegalStateException(
                "Los archivos publicados para " + year
                        + " no superaron la verificación de latín y año.", last);
    }

    public static LatinDay day(Context context, Calendar date) throws Exception {
        int year = date.get(Calendar.YEAR);
        File root = ensureYear(context, year);
        String name = String.format(Locale.US, "%02d%02d%02d.htm", year % 100,
                date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
        File dayFile = new File(root, name);
        if (!dayFile.isFile()) throw new IllegalStateException(
                "No se encontró esta fecha en el año latino.");
        String html = read(dayFile);

        String heading = between(html, "<!--BEGIN:heading-->", "<!--END:heading-->");
        String dateTitle = plain(heading);
        if (dateTitle.isEmpty()) {
            dateTitle = extractTag(html, "title").replaceFirst("(?i)^LA\\s*\\|\\s*", "");
        }
        String proper = between(html, "<!--BEGIN:_global_string-->",
                "<!--END:_global_string-->");
        String properText = plain(proper);
        String rank = "";
        Matcher rankMatcher = Pattern.compile("class=[\\\"']redtitle[\\\"'][^>]*>(.*?)<",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(proper);
        if (rankMatcher.find()) rank = plain(rankMatcher.group(1));
        String celebration = properText.replace(rank, "").replaceAll("\\s+", " ").trim();

        Map<String, String> hours = hourLinks(root, name, html);
        if (hours.size() < 8) throw new IllegalStateException(
                "La fecha latina no contiene todas las Horas.");
        return new LatinDay(year, dateTitle, celebration, rank, hours);
    }

    public static File hourFile(Context context, int year, String relativePath)
            throws Exception {
        File root = ensureYear(context, year);
        File target = new File(root, relativePath);
        String canonicalRoot = root.getCanonicalPath() + File.separator;
        if (!target.getCanonicalPath().startsWith(canonicalRoot) || !target.isFile()) {
            throw new IllegalStateException("Ruta latina no válida.");
        }
        return target;
    }

    public static File rootFor(Context context, int year) {
        return new File(base(context), String.valueOf(year));
    }

    private static List<String> findLatinEpubCandidates(String html, int year) {
        Matcher matcher = LINK.matcher(html);
        List<Candidate> candidates = new ArrayList<>();
        while (matcher.find()) {
            String href = matcher.group(1).trim();
            String label = plain(matcher.group(2));
            int start = Math.max(0, matcher.start() - 1200);
            int end = Math.min(html.length(), matcher.end() + 600);
            String neighborhood = normalize(plain(html.substring(start, end)));
            String own = normalize(label + " " + href);
            String combined = normalize(own + " " + neighborhood);
            if (!combined.contains(String.valueOf(year))) continue;
            if (!own.contains("EPUB") && !normalize(href).contains("EPUB")) continue;
            int score = 0;
            if (own.contains("LATIN")) score += 30;
            if (own.contains("LITURGIA HORARUM")) score += 20;
            if (combined.contains("LATIN")) score += 8;
            if (combined.contains("LITURGIA HORARUM")) score += 8;
            if (own.contains("SLOVENSK") || own.contains("SLOVAK")) score -= 40;
            candidates.add(new Candidate(href, score));
        }
        Collections.sort(candidates, new Comparator<Candidate>() {
            @Override public int compare(Candidate left, Candidate right) {
                return Integer.compare(right.score, left.score);
            }
        });
        List<String> result = new ArrayList<>();
        for (Candidate candidate : candidates) if (!result.contains(candidate.href)) {
            result.add(candidate.href);
        }
        return result;
    }

    private static void validate(File epub, int year) throws Exception {
        String opf = "";
        String day = "";
        String lauds = "";
        int dateFiles = 0;
        String prefix = String.format(Locale.US, "%02d", year % 100);
        Pattern dateFile = Pattern.compile("(?:.*/)?" + prefix + "\\d{4}\\.htm",
                Pattern.CASE_INSENSITIVE);
        try (InputStream input = new FileInputStream(epub);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) continue;
                if (dateFile.matcher(name).matches()) dateFiles++;
                if (name.endsWith("content.opf")) opf = readEntry(zip);
                else if (name.endsWith(prefix + "0101.htm")) day = readEntry(zip);
                else if (lauds.isEmpty() && name.matches("(?:.*/)?" + prefix
                        + "\\d{4}_[01]r\\.htm")) lauds = readEntry(zip);
            }
        }
        int expected = isLeap(year) ? 366 : 365;
        String normalizedOpf = normalize(plain(opf));
        String normalizedDay = normalize(plain(day));
        String normalizedLauds = normalize(plain(lauds));
        if (!normalizedOpf.contains("LITURGIA HORARUM")
                || !normalizedOpf.contains(String.valueOf(year))) {
            throw new IllegalStateException("El EPUB no se identifica como Liturgia Horarum "
                    + year + ".");
        }
        if (!(day.contains("lang=\"la\"") || day.contains("xml:lang=\"la\""))
                || !normalizedDay.contains("INVITATORIUM")
                || !normalizedDay.contains("OFFICIUM LECTIONIS")
                || !normalizedDay.contains("LAUDES MATUTIN")) {
            throw new IllegalStateException("El EPUB descargado no contiene textos latinos.");
        }
        if (!normalizedLauds.contains("HYMNUS") || !normalizedLauds.contains("PSALMODIA")
                || !normalizedLauds.contains("ORATIO")) {
            throw new IllegalStateException("La estructura interna del oficio latino no es válida.");
        }
        if (dateFiles < expected) throw new IllegalStateException(
                "El EPUB latino no contiene todos los días de " + year + ".");
    }

    private static void validateExtracted(File root, int year) throws Exception {
        Calendar cursor = Calendar.getInstance();
        cursor.clear();
        cursor.set(year, Calendar.JANUARY, 1);
        int total = isLeap(year) ? 366 : 365;
        for (int index = 0; index < total; index++) {
            String name = String.format(Locale.US, "%02d%02d%02d.htm", year % 100,
                    cursor.get(Calendar.MONTH) + 1, cursor.get(Calendar.DAY_OF_MONTH));
            File day = new File(root, name);
            if (!day.isFile()) throw new IllegalStateException(
                    "Falta la fecha latina " + name + ".");
            if (hourLinks(root, name, read(day)).size() < 8) {
                throw new IllegalStateException(
                        "La fecha latina " + name + " no contiene las ocho Horas.");
            }
            cursor.add(Calendar.DATE, 1);
        }
    }

    private static Map<String, String> hourLinks(File root, String dayPath, String html)
            throws Exception {
        Map<String, String> hours = new LinkedHashMap<>();
        Matcher links = LINK.matcher(html);
        while (links.find()) {
            String label = normalize(plain(links.group(2)));
            String key = hourKey(label);
            if (key.isEmpty() || hours.containsKey(key)) continue;
            String href = links.group(1).trim().split("#", 2)[0];
            URI baseUri = new URI(null, null, dayPath, null);
            String path = baseUri.resolve(href).normalize().getPath();
            while (path.startsWith("/")) path = path.substring(1);
            File target = new File(root, path);
            String canonicalRoot = root.getCanonicalPath() + File.separator;
            if (target.getCanonicalPath().startsWith(canonicalRoot) && target.isFile()) {
                hours.put(key, path);
            }
        }
        return hours;
    }

    private static void extract(File epub, File root) throws Exception {
        deleteTree(root);
        if (!root.mkdirs()) throw new IllegalStateException(
                "No se pudo preparar el año latino.");
        String rootPath = root.getCanonicalPath() + File.separator;
        try (InputStream input = new FileInputStream(epub);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            int entries = 0;
            long extracted = 0;
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ENTRIES) throw new IllegalStateException(
                        "El EPUB contiene demasiados archivos.");
                File target = new File(root, entry.getName());
                if (!target.getCanonicalPath().startsWith(rootPath)) {
                    throw new IllegalStateException("El EPUB contiene una ruta no válida.");
                }
                if (entry.isDirectory()) {
                    if (!target.exists() && !target.mkdirs()) throw new IllegalStateException(
                            "No se pudo extraer el EPUB latino.");
                    continue;
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("No se pudo crear una carpeta latina.");
                }
                try (FileOutputStream output = new FileOutputStream(target)) {
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        extracted += count;
                        if (extracted > MAX_EXTRACTED_BYTES) throw new IllegalStateException(
                                "El EPUB excede el tamaño permitido al extraerse.");
                        output.write(buffer, 0, count);
                    }
                }
            }
        }
    }

    private static String hourKey(String label) {
        if (label.startsWith("INVITATORIUM")) return "invitatory";
        if (label.startsWith("OFFICIUM LECTIONIS")) return "office";
        if (label.startsWith("LAUDES MATUTIN")) return "lauds";
        if (label.equals("TERTIA")) return "terce";
        if (label.equals("SEXTA")) return "sext";
        if (label.equals("NONA")) return "none";
        if (label.startsWith("VESPER")) return "vespers";
        if (label.startsWith("COMPLETORIUM")) return "compline";
        return "";
    }

    private static HttpURLConnection open(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(18000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Ministerium/2.0");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("La fuente respondió con el código " + status + ".");
        }
        return connection;
    }

    private static String downloadText(String address) throws Exception {
        HttpURLConnection connection = open(address);
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
            if (normalize(text).contains("VERIFICACION DE SEGURIDAD")
                    || text.contains("cf-chl")) throw new IllegalStateException(
                    "Breviar.sk solicita verificación; usa Importar EPUB como respaldo.");
            return text;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(File file) throws Exception {
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String readEntry(ZipInputStream zip) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void copy(InputStream input, File target) throws Exception {
        try (FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16384];
            int count;
            long total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_EPUB_BYTES) throw new IllegalStateException(
                        "El EPUB supera el tamaño máximo permitido.");
                output.write(buffer, 0, count);
            }
        }
    }

    private static String extractTag(String html, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + "[^>]*>(.*?)</" + tag + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? plain(matcher.group(1)) : "";
    }

    private static String between(String value, String start, String end) {
        int from = value.indexOf(start);
        if (from < 0) return "";
        int to = value.indexOf(end, from + start.length());
        return to < 0 ? "" : value.substring(from + start.length(), to);
    }

    private static String plain(String html) {
        return Html.fromHtml(html == null ? "" : html).toString()
                .replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT)
                .replace("Æ", "AE").replace("Œ", "OE")
                .replaceAll("[^A-Z0-9]+", " ").trim();
    }

    private static boolean isLeap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private static File base(Context context) {
        return new File(context.getFilesDir(), "latin_liturgy");
    }

    private static File epubFor(Context context, int year) {
        return new File(base(context), "latin_" + year + ".epub");
    }

    private static void markReady(File root, int year) throws Exception {
        try (FileOutputStream marker = new FileOutputStream(new File(root, ".ready"))) {
            marker.write(("latin-" + year).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteTree(File file) throws Exception {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        if (!file.delete()) throw new IllegalStateException(
                "No se pudo reemplazar un contenido latino anterior.");
    }

    private static final class Candidate {
        final String href;
        final int score;

        Candidate(String href, int score) {
            this.href = href;
            this.score = score;
        }
    }
}
