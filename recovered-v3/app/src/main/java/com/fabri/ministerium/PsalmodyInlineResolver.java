package com.fabri.ministerium;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expande remisiones de salmodia que un propio/común deja como enlaces.
 *
 * Ejemplo típico: «Los salmos y el cántico se toman del domingo I del
 * Salterio» seguido de Salmo 62 · Daniel 3 · Salmo 149. El lector no debe
 * mostrar esos enlaces como sustituto de la salmodia: resuelve sus href reales
 * e inserta los tres textos completos entre las antífonas propias.
 */
public final class PsalmodyInlineResolver {
    private static final Pattern PARAGRAPH = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private PsalmodyInlineResolver() {}

    public static String resolve(String html, String baseUrl) {
        try {
            if (html == null || html.isEmpty() || baseUrl == null || baseUrl.isEmpty()) {
                return html;
            }

            int ant1 = paragraphStart(html, "ANT. 1.");
            int ant2 = paragraphStart(html, "ANT. 2.");
            int ant3 = paragraphStart(html, "ANT. 3.");
            int reading = firstParagraphStart(html, ant3,
                    "LECTURA BREVE", "PRIMERA LECTURA", "SEGUNDA LECTURA");
            if (ant1 < 0 || ant2 < 0 || ant3 < 0 || reading < 0
                    || !(ant1 < ant2 && ant2 < ant3 && ant3 < reading)) {
                return html;
            }

            String currentPsalmody = html.substring(ant1, reading);
            if (alreadyContainsFullPsalmody(currentPsalmody)) return html;

            List<Target> targets = referencedPsalmTargets(currentPsalmody, baseUrl);
            if (targets.size() != 3) return html;

            String[] psalms = new String[3];
            for (int i = 0; i < targets.size(); i++) {
                Target current = targets.get(i);
                String targetHtml = read(current.file);
                String endFragment = null;
                for (int j = i + 1; j < targets.size(); j++) {
                    Target candidate = targets.get(j);
                    if (sameFile(current.file, candidate.file)) {
                        endFragment = candidate.fragment;
                        break;
                    }
                }
                psalms[i] = extractPsalm(targetHtml, current.fragment, endFragment);
                if (psalms[i].isEmpty()) return html;
            }

            String antiphon1 = paragraph(html, ant1);
            String antiphon2 = paragraph(html, ant2);
            String antiphon3 = paragraph(html, ant3);
            if (antiphon1.isEmpty() || antiphon2.isEmpty() || antiphon3.isEmpty()) {
                return html;
            }

            int ant1End = ant1 + antiphon1.length();
            return html.substring(0, ant1End)
                    + psalms[0] + antiphon1
                    + antiphon2 + psalms[1] + antiphon2
                    + antiphon3 + psalms[2] + antiphon3
                    + html.substring(reading);
        } catch (Exception ignored) {
            // Una remisión que no pueda resolverse nunca debe impedir abrir la Hora.
            return html;
        }
    }

    private static List<Target> referencedPsalmTargets(String section, String baseUrl)
            throws Exception {
        List<Target> result = new ArrayList<>();
        Matcher matcher = ANCHOR.matcher(section);
        while (matcher.find() && result.size() < 3) {
            String label = normalize(plain(matcher.group(2)));
            if (!isPsalmReference(label)) continue;
            Target target = target(baseUrl, matcher.group(1));
            if (target != null) result.add(target);
        }
        return result;
    }

    private static boolean isPsalmReference(String label) {
        return label.startsWith("SALMO ")
                || label.startsWith("CANTICO ")
                || label.startsWith("DANIEL ")
                || label.startsWith("DN ");
    }

    private static Target target(String baseUrl, String href) throws Exception {
        if (href == null || href.trim().isEmpty()) return null;
        String decoded = URLDecoder.decode(href.trim(), "UTF-8");
        String[] parts = decoded.split("#", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) return null;

        URI base = URI.create(baseUrl);
        URI resolved = base.resolve(parts[0].isEmpty() ? "" : parts[0]);
        if (!"file".equalsIgnoreCase(resolved.getScheme())) return null;
        File file = new File(resolved).getCanonicalFile();
        if (!file.isFile()) return null;
        return new Target(file, parts[1]);
    }

    private static String extractPsalm(String html, String startFragment,
                                       String endFragment) {
        int startId = idPosition(html, startFragment);
        if (startId < 0) return "";
        int start = tagStart(html, startId);

        int end = -1;
        if (endFragment != null && !endFragment.isEmpty()) {
            int next = idPosition(html, endFragment);
            if (next > start) end = tagStart(html, next);
        }
        if (end < 0) {
            end = firstParagraphStart(html, start,
                    "LECTURA BREVE", "PRIMERA LECTURA", "SEGUNDA LECTURA",
                    "RESPONSORIO BREVE", "CÁNTICO EVANGÉLICO", "CANTICO EVANGELICO");
        }
        if (end < 0) end = bodyEnd(html);
        if (end <= start) return "";

        String value = html.substring(start, end);
        // La antífona del salterio de origen no debe sustituir la antífona
        // propia de la celebración. Si el fragmento comienza con una Ant., se
        // descarta antes de insertar el salmo.
        Matcher first = PARAGRAPH.matcher(value);
        if (first.find()) {
            String text = normalize(plain(first.group()));
            if (text.startsWith("ANT ") || text.equals("ANT")) {
                value = value.substring(first.end());
            }
        }
        return value;
    }

    private static boolean alreadyContainsFullPsalmody(String value) {
        String text = normalize(plain(value));
        int headings = count(text, "SALMO ") + count(text, "CANTICO ");
        return headings >= 3 && text.length() > 700;
    }

    private static int paragraphStart(String html, String marker) {
        String wanted = normalize(marker);
        Matcher matcher = PARAGRAPH.matcher(html);
        while (matcher.find()) {
            String text = normalize(plain(matcher.group()));
            if (text.equals(wanted) || text.startsWith(wanted + " ")) return matcher.start();
        }
        return -1;
    }

    private static int firstParagraphStart(String html, int from, String... markers) {
        Matcher matcher = PARAGRAPH.matcher(html);
        if (from > 0) matcher.region(Math.min(from, html.length()), html.length());
        while (matcher.find()) {
            String text = normalize(plain(matcher.group()));
            for (String marker : markers) {
                String wanted = normalize(marker);
                if (text.equals(wanted) || text.startsWith(wanted + " ")) {
                    return matcher.start();
                }
            }
        }
        return -1;
    }

    private static String paragraph(String html, int start) {
        if (start < 0 || start >= html.length()) return "";
        int end = html.toLowerCase(Locale.ROOT).indexOf("</p>", start);
        return end < 0 ? "" : html.substring(start, end + 4);
    }

    private static int idPosition(String html, String fragment) {
        if (fragment == null || fragment.isEmpty()) return -1;
        Pattern pattern = Pattern.compile("(?i)id\\s*=\\s*[\\\"']"
                + Pattern.quote(fragment) + "[\\\"']");
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.start() : -1;
    }

    private static int tagStart(String html, int from) {
        int result = html.lastIndexOf('<', Math.max(0, from));
        return result < 0 ? Math.max(0, from) : result;
    }

    private static int bodyEnd(String html) {
        int result = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return result < 0 ? html.length() : result;
    }

    private static boolean sameFile(File a, File b) throws Exception {
        return a.getCanonicalPath().equals(b.getCanonicalPath());
    }

    private static int count(String value, String token) {
        int result = 0;
        int at = 0;
        while ((at = value.indexOf(token, at)) >= 0) {
            result++;
            at += token.length();
        }
        return result;
    }

    private static String plain(String html) {
        return html == null ? "" : html.replaceAll("(?is)<br\\b[^>]*>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
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

    private static final class Target {
        final File file;
        final String fragment;

        Target(File file, String fragment) {
            this.file = file;
            this.fragment = fragment;
        }
    }
}
