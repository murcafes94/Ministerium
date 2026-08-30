package com.fabri.ministerium;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrdinaryReferenceResolver {
    private static final Pattern PARAGRAPH = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private OrdinaryReferenceResolver() {}

    public static String resolve(File root, String sourcePath, int ordinaryWeek,
                                 String cycle, int readingsYear) throws Exception {
        if (ordinaryWeek < 1 || ordinaryWeek > 34) {
            return read(new File(root, sourcePath));
        }
        String html = read(new File(root, sourcePath));
        Matcher paragraphs = PARAGRAPH.matcher(html);
        StringBuffer result = new StringBuffer();
        while (paragraphs.find()) {
            String paragraph = paragraphs.group();
            Matcher anchors = ANCHOR.matcher(paragraph);
            int numberedLinks = 0;
            String selectedHref = "";
            while (anchors.find()) {
                String label = plainText(anchors.group(2));
                if (!label.matches("\\d{1,2}")) continue;
                numberedLinks++;
                int number;
                try {
                    number = Integer.parseInt(label);
                } catch (NumberFormatException error) {
                    continue;
                }
                if (number == ordinaryWeek) selectedHref = anchors.group(1).trim();
            }
            if (numberedLinks >= 2 && !selectedHref.isEmpty()) {
                boolean officeReadings = isOfficeReadingsReference(
                        html.substring(0, paragraphs.start()));
                String replacement = resolvedContent(root, sourcePath, selectedHref,
                        paragraph, cycle, readingsYear, officeReadings);
                paragraphs.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                paragraphs.appendReplacement(result, Matcher.quoteReplacement(paragraph));
            }
        }
        paragraphs.appendTail(result);
        return result.toString();
    }

    private static String resolvedContent(File root, String sourcePath, String href,
                                          String sourceParagraph, String cycle,
                                          int readingsYear, boolean officeReadings)
            throws Exception {
        String[] parts = href.split("#", 2);
        if (parts.length < 2 || parts[1].isEmpty()) return sourceParagraph;
        URI base = new URI(null, null, sourcePath, null);
        String targetPath = parts[0].isEmpty()
                ? sourcePath : base.resolve(parts[0]).normalize().getPath();
        while (targetPath.startsWith("/")) targetPath = targetPath.substring(1);
        File target = new File(root, targetPath).getCanonicalFile();
        String rootPath = root.getCanonicalPath() + File.separator;
        if (!target.getPath().startsWith(rootPath) || !target.exists()) {
            return sourceParagraph;
        }
        String targetHtml = read(target);
        if (officeReadings) {
            String section = sectionById(targetHtml, parts[1]);
            if (section.isEmpty()) return sourceParagraph;
            return selectReadingsYear(section, readingsYear);
        }
        String targetParagraph = paragraphById(targetHtml, parts[1]);
        if (targetParagraph.isEmpty()) return sourceParagraph;
        String value = targetValue(targetParagraph, cycle);
        if (value.isEmpty()) return sourceParagraph;

        String sourceText = plainText(sourceParagraph).toUpperCase(Locale.ROOT);
        if (sourceText.startsWith("ANT.")) {
            return "<p class=\"ministerium-resolved-reference\"><span class=\"rojo\">Ant.</span> "
                    + value + "</p>";
        }
        return "<p class=\"ministerium-resolved-reference\">" + value + "</p>";
    }

    private static boolean isOfficeReadingsReference(String prefix) {
        int start = Math.max(0, prefix.length() - 500);
        String text = plainText(prefix.substring(start)).toUpperCase(Locale.ROOT)
                .replace('Ó', 'O');
        return text.endsWith("LECTURAS Y ORACION:")
                || text.endsWith("LECTURAS Y ORACION");
    }

    private static String sectionById(String html, String fragment) {
        int id = idPosition(html, fragment);
        if (id < 0) return "";
        int start = html.lastIndexOf("<p", id);
        if (start < 0) return "";
        Pattern nextSection = Pattern.compile("<p\\b[^>]*\\bid=[\\\"'][^\\\"']+[\\\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
        Matcher next = nextSection.matcher(html);
        int end = next.find(id + fragment.length()) ? next.start() : bodyEnd(html);
        return html.substring(start, Math.max(start, end));
    }

    private static String selectReadingsYear(String section, int readingsYear) {
        if (readingsYear != 1 && readingsYear != 2) return section;
        int first = markerParagraph(section, "Año I:");
        int second = markerParagraph(section, "Año II:");
        int common = markerParagraph(section, "SEGUNDA LECTURA");
        if (first < 0 || second < 0 || common < 0 || !(first < second && second < common)) {
            return section;
        }
        if (readingsYear == 1) {
            return section.substring(0, first) + section.substring(first, second)
                    + section.substring(common);
        }
        return section.substring(0, first) + section.substring(second, common)
                + section.substring(common);
    }

    private static int markerParagraph(String html, String marker) {
        int at = plainSearch(html, marker);
        return at < 0 ? -1 : html.lastIndexOf("<p", at);
    }

    private static int plainSearch(String html, String marker) {
        String normalizedMarker = marker.toUpperCase(Locale.ROOT);
        Matcher paragraphs = PARAGRAPH.matcher(html);
        while (paragraphs.find()) {
            if (plainText(paragraphs.group()).toUpperCase(Locale.ROOT)
                    .startsWith(normalizedMarker)) return paragraphs.start();
        }
        return -1;
    }

    private static int idPosition(String html, String fragment) {
        int result = html.indexOf("id=\"" + fragment + "\"");
        if (result < 0) result = html.indexOf("id='" + fragment + "'");
        return result;
    }

    private static int bodyEnd(String html) {
        int result = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return result < 0 ? html.length() : result;
    }

    private static String targetValue(String paragraph, String cycle) {
        int open = paragraph.indexOf('>');
        int close = paragraph.toLowerCase(Locale.ROOT).lastIndexOf("</p>");
        if (open < 0 || close <= open) return "";
        String inner = paragraph.substring(open + 1, close).trim();
        String selectedCycle = "A".equals(cycle) || "B".equals(cycle)
                || "C".equals(cycle) ? cycle : "A";
        Pattern year = Pattern.compile(
                "A(?:ñ|&ntilde;)o\\s+" + selectedCycle
                        + "\\s*:\\s*</span>\\s*(.*?)(?=<br\\b[^>]*?/?>\\s*"
                        + "<span\\b[^>]*>\\s*A(?:ñ|&ntilde;)o\\s+[ABC]\\s*:|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher yearMatcher = year.matcher(inner);
        if (yearMatcher.find()) return yearMatcher.group(1).trim();

        inner = inner.replaceFirst(
                "(?is)^\\s*<span\\b[^>]*>\\s*Oraci[oó]n\\s*</span>\\s*"
                        + "<br\\b[^>]*?/?>\\s*", "");
        inner = inner.replaceFirst(
                "(?is)^\\s*<span\\b[^>]*class=[\\\"'][^\\\"']*bold[^\\\"']*"
                        + "[\\\"'][^>]*>.*?</span>\\s*<br\\b[^>]*?/?>\\s*", "");
        return inner.trim();
    }

    private static String paragraphById(String html, String fragment) {
        int id = html.indexOf("id=\"" + fragment + "\"");
        if (id < 0) id = html.indexOf("id='" + fragment + "'");
        if (id < 0) return "";
        int start = html.lastIndexOf("<p", id);
        if (start < 0) return "";
        int end = html.toLowerCase(Locale.ROOT).indexOf("</p>", id);
        if (end < 0) return "";
        return html.substring(start, end + 4);
    }

    private static String plainText(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replaceAll("\\s+", " ").trim();
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
}
