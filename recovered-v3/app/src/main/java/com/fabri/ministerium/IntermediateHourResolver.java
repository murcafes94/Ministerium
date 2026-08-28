package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Convierte la Hora intermedia en un texto continuo y ofrece la salmodia complementaria. */
public final class IntermediateHourResolver {
    private static final Pattern PARAGRAPH = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private IntermediateHourResolver() {}

    public static String resolve(Context context, File root, String sourcePath,
                                 String sourceHtml, String hour,
                                 boolean sundayOrSolemnity) throws Exception {
        return resolve(context, root, sourcePath, sourceHtml, hour,
                sundayOrSolemnity, 0);
    }

    public static String resolve(Context context, File root, String sourcePath,
                                 String sourceHtml, String hour,
                                 boolean sundayOrSolemnity, int ordinaryWeek)
            throws Exception {
        String html = sourceHtml == null ? read(new File(root, sourcePath)) : sourceHtml;
        html = inlineHymn(root, sourcePath, html, hour, sundayOrSolemnity,
                ordinaryWeek);
        String complementary = complementaryPsalmody(context, hour);
        if (!complementary.isEmpty()) html = addComplementaryPsalmody(html, complementary);
        return html;
    }

    private static String inlineHymn(File root, String sourcePath, String html,
                                     String hour, boolean sundayOrSolemnity,
                                     int ordinaryWeek) throws Exception {
        List<Block> paragraphs = paragraphs(html);
        boolean afterHymn = false;
        for (Block block : paragraphs) {
            String text = plain(block.html);
            if ("HIMNO".equals(text)) {
                afterHymn = true;
                continue;
            }
            if (!afterHymn || "SALMODIA".equals(text)) break;
            Matcher anchor = ANCHOR.matcher(block.html);
            while (anchor.find()) {
                String linkLabel = plain(anchor.group(2));
                if (!hymnRangeMatches(linkLabel, ordinaryWeek)) continue;
                String href = anchor.group(1).trim();
                String[] parts = href.split("#", 2);
                URI base = new URI(null, null, sourcePath, null);
                String targetPath = parts[0].isEmpty() ? sourcePath
                        : base.resolve(parts[0]).normalize().getPath();
                while (targetPath.startsWith("/")) targetPath = targetPath.substring(1);
                File target = new File(root, targetPath).getCanonicalFile();
                String rootPath = root.getCanonicalPath() + File.separator;
                if (!target.getPath().startsWith(rootPath) || !target.isFile()) continue;
                String targetHtml = read(target);
                int fragmentAt = parts.length < 2 ? 0 : idPosition(targetHtml, parts[1]);
                if (fragmentAt < 0) fragmentAt = 0;
                String hymn = selectHymn(targetHtml.substring(fragmentAt), hour,
                        sundayOrSolemnity);
                if (hymn.isEmpty()) continue;
                String replacement = "<section class=\"ministerium-auto-hymn\">"
                        + "<p class=\"ministerium-source-note\">Himno elegido automáticamente para "
                        + escape(hour) + "</p>" + hymn + "</section>";
                return html.substring(0, block.start) + replacement + html.substring(block.end);
            }
        }
        return html;
    }

    /**
     * Los tomos dividen los himnos de la Hora intermedia en dos grupos:
     * semanas I-XVII y XVIII-XXXIV. Si el enlace no expresa un rango se acepta
     * como referencia genérica, preservando compatibilidad con otros tiempos.
     */
    private static boolean hymnRangeMatches(String label, int ordinaryWeek) {
        if (ordinaryWeek < 1 || ordinaryWeek > 34) return true;
        String value = plain(label);
        if (value.contains("XVIII") && value.contains("XXXIV")) {
            return ordinaryWeek >= 18;
        }
        if (value.contains("XVII") && !value.contains("XVIII")) {
            return ordinaryWeek <= 17;
        }
        return true;
    }

    private static String selectHymn(String html, String hour,
                                     boolean sundayOrSolemnity) {
        String wanted = plain(hour);
        String next = "TERCIA".equals(wanted) ? "SEXTA"
                : "SEXTA".equals(wanted) ? "NONA" : "";
        List<Block> values = paragraphs(html);
        int start = -1;
        int end = values.size();
        for (int i = 0; i < values.size(); i++) {
            String text = plain(values.get(i).html);
            if (start < 0 && wanted.equals(text)) start = i + 1;
            else if (start >= 0 && ((!next.isEmpty() && next.equals(text))
                    || (next.isEmpty() && isSectionHeading(text)))) {
                end = i;
                break;
            }
        }
        if (start < 0) return "";

        int chosen = start;
        if ("SEXTA".equals(wanted)) {
            String marker = sundayOrSolemnity ? "EN LOS DOMINGOS"
                    : "FUERA DE LOS DOMINGOS";
            for (int i = start; i < end; i++) {
                if (plain(values.get(i).html).contains(marker)) {
                    chosen = i + 1;
                    break;
                }
            }
        }
        StringBuilder hymn = new StringBuilder();
        for (int i = chosen; i < end; i++) {
            String text = plain(values.get(i).html);
            if (isRubric(text)) {
                if (hymn.length() > 0) break;
                continue;
            }
            if (!text.isEmpty()) hymn.append(values.get(i).html);
        }
        return hymn.toString();
    }

    private static String complementaryPsalmody(Context context, String hour)
            throws Exception {
        HoursVolume ordinary = HoursRepository.find("ordinary");
        if (ordinary == null) return "";
        File root = EpubUtils.ensureExtracted(context, ordinary);
        File source = findComplementarySource(root);
        if (source == null) return "";
        String wanted = "SALMODIA PARA " + plain(hour);
        String next = "TERCIA".equals(plain(hour)) ? "SALMODIA PARA SEXTA"
                : "SEXTA".equals(plain(hour)) ? "SALMODIA PARA NONA" : "";
        List<Block> values = paragraphs(read(source));
        int start = -1;
        StringBuilder result = new StringBuilder();
        for (Block value : values) {
            String text = plain(value.html);
            if (start < 0 && wanted.equals(text)) {
                start = 1;
                continue;
            }
            if (start >= 0 && ((!next.isEmpty() && next.equals(text))
                    || (next.isEmpty() && text.startsWith("SALMODIA PARA ")))) break;
            if (start >= 0) result.append(value.html);
        }
        return result.toString();
    }

    private static File findComplementarySource(File root) throws Exception {
        List<File> pending = new ArrayList<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            File current = pending.remove(pending.size() - 1);
            File[] children = current.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.isDirectory()) pending.add(child);
                else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
                    String value = read(child);
                    if (plain(value).contains("SALMODIA PARA TERCIA")
                            && plain(value).contains("SALMODIA PARA SEXTA")
                            && plain(value).contains("SALMODIA PARA NONA")) return child;
                }
            }
        }
        return null;
    }

    private static String addComplementaryPsalmody(String html, String psalmody) {
        for (Block block : paragraphs(html)) {
            if (!"SALMODIA".equals(plain(block.html))) continue;
            String details = "<details class=\"ministerium-complementary\"><summary>"
                    + "Usar salmodia complementaria</summary><p class=\"ministerium-source-note\">"
                    + "Opción litúrgica: sustituye la salmodia ordinaria de esta Hora.</p>"
                    + psalmody + "</details>";
            return html.substring(0, block.end) + details + html.substring(block.end);
        }
        return html;
    }

    private static boolean isRubric(String text) {
        return text.startsWith("O BIEN") || text.startsWith("EN LOS DOMINGOS")
                || text.startsWith("FUERA DE LOS DOMINGOS")
                || text.startsWith("TANTO EN LOS DOMINGOS");
    }

    private static boolean isSectionHeading(String text) {
        return text.startsWith("SEMANAS ") || text.startsWith("SEMANA ");
    }

    private static List<Block> paragraphs(String html) {
        List<Block> result = new ArrayList<>();
        Matcher matcher = PARAGRAPH.matcher(html);
        while (matcher.find()) result.add(new Block(matcher.start(), matcher.end(),
                matcher.group()));
        return result;
    }

    private static int idPosition(String html, String fragment) {
        int result = html.indexOf("id=\"" + fragment + "\"");
        if (result < 0) result = html.indexOf("id='" + fragment + "'");
        return result;
    }

    private static String plain(String html) {
        String value = html == null ? "" : html.replaceAll("(?is)<br\\b[^>]*>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&ntilde;", "ñ").replace("&Ntilde;", "Ñ");
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ")
                .trim().toUpperCase(Locale.ROOT);
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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class Block {
        final int start;
        final int end;
        final String html;
        Block(int start, int end, String html) {
            this.start = start;
            this.end = end;
            this.html = html;
        }
    }
}
