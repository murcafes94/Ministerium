package com.fabri.ministerium;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds a WebView document containing exactly one indexed Bible chapter. */
public final class BibleChapterDocument {
    private BibleChapterDocument() {}

    public static String from(File root, String chapterFile, String chapterFragment,
                              int chapterNumber, String nextFile, String nextFragment,
                              int nextNumber) throws Exception {
        List<File> files = chapterFiles(root, chapterFile, nextFile);
        String firstHtml = read(files.get(0));
        int marker = idPosition(firstHtml, chapterFragment);
        if (marker < 0) throw new IllegalArgumentException(
                "No se encontró el inicio del capítulo " + chapterFragment);

        int firstBlock = blockStart(firstHtml, marker);
        int chapterStart = includePrecedingHeadings(firstHtml, firstBlock);
        String syntheticHeading = "";
        String expectedFirstVerse = firstVerseId(chapterNumber);
        int expectedVerseAt = idPosition(firstHtml, expectedFirstVerse);
        String firstVerseAfterMarker = firstVerseIdAfter(firstHtml, marker);
        if (!chapterFragment.startsWith("v") && expectedVerseAt > marker
                && !expectedFirstVerse.equals(firstVerseAfterMarker)) {
            chapterStart = blockStart(firstHtml, expectedVerseAt);
            syntheticHeading = "<h3 class=\"ministerium-chapter-title\">CAPÍTULO "
                    + chapterNumber + "</h3>";
        }

        StringBuilder chapter = new StringBuilder(syntheticHeading);
        for (int i = 0; i < files.size(); i++) {
            String html = i == 0 ? firstHtml : read(files.get(i));
            int start = i == 0 ? chapterStart : bodyStart(html);
            int end = bodyEnd(html);
            if (i + 1 == files.size() && nextFragment != null && !nextFragment.isEmpty()) {
                String boundaryFragment = chapterFile.equals(nextFile)
                        && chapterFragment.equals(nextFragment)
                        ? firstVerseId(nextNumber) : nextFragment;
                int nextMarker = idPosition(html, boundaryFragment);
                if (nextMarker >= 0) end = includePrecedingHeadings(
                        html, blockStart(html, nextMarker));
            }
            if (end > start) chapter.append(html, start, end);
        }

        String content = chapter.toString();
        int withoutNextHeadings = precedingHeadingStart(content, content.length());
        if (withoutNextHeadings < content.length()) {
            content = content.substring(0, withoutNextHeadings).trim();
        }

        return "<!doctype html><html>" + head(firstHtml) + bodyOpeningTag(firstHtml)
                + "<main id=\"ministerium-chapter\">" + content
                + "</main></body></html>";
    }

    private static List<File> chapterFiles(File root, String chapterFile,
                                           String nextFile) {
        File first = new File(root, chapterFile);
        List<File> result = new ArrayList<>();
        result.add(first);
        if (nextFile == null || nextFile.isEmpty() || chapterFile.equals(nextFile)) {
            return result;
        }
        File last = new File(root, nextFile);
        if (!first.getParentFile().equals(last.getParentFile())) return result;
        File[] siblings = first.getParentFile().listFiles((directory, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".html"));
        if (siblings == null) return result;
        Arrays.sort(siblings, Comparator.comparing(File::getName));
        int firstAt = indexOf(siblings, first.getName());
        int lastAt = indexOf(siblings, last.getName());
        if (firstAt < 0 || lastAt <= firstAt) return result;
        result.clear();
        for (int i = firstAt; i <= lastAt; i++) result.add(siblings[i]);
        return result;
    }

    private static int indexOf(File[] files, String name) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(name)) return i;
        }
        return -1;
    }

    private static String firstVerseId(int chapterNumber) {
        return String.format(Locale.ROOT, "v%02d1", chapterNumber);
    }

    private static String firstVerseIdAfter(String html, int from) {
        Matcher matcher = Pattern.compile("(?i)<sup\\b[^>]*\\bid\\s*=\\s*([\"'])(v\\d+)\\1")
                .matcher(html);
        return matcher.find(from) ? matcher.group(2) : "";
    }

    private static int includePrecedingHeadings(String html, int paragraphStart) {
        int headingStart = precedingHeadingStart(html, paragraphStart);
        return headingStart < paragraphStart ? headingStart : paragraphStart;
    }

    private static int precedingHeadingStart(String html, int end) {
        String lower = html.toLowerCase(Locale.ROOT);
        int cursor = skipWhitespaceBackward(html, end);
        int earliest = end;
        boolean found = false;
        while (cursor > 0) {
            int level = 0;
            int closeStart = -1;
            for (int candidate = 1; candidate <= 6; candidate++) {
                String close = "</h" + candidate + ">";
                int possible = cursor - close.length();
                if (possible >= 0 && lower.regionMatches(possible, close, 0, close.length())) {
                    level = candidate;
                    closeStart = possible;
                    break;
                }
            }
            if (level == 0) break;
            int open = lower.lastIndexOf("<h" + level, closeStart);
            if (open < 0) break;
            int boundary = open + 3;
            if (boundary < lower.length()) {
                char value = lower.charAt(boundary);
                if (!(Character.isWhitespace(value) || value == '>')) break;
            }
            found = true;
            earliest = open;
            cursor = skipWhitespaceBackward(html, open);
        }
        return found ? earliest : end;
    }

    private static int skipWhitespaceBackward(String value, int from) {
        int cursor = Math.min(from, value.length());
        while (cursor > 0 && Character.isWhitespace(value.charAt(cursor - 1))) cursor--;
        return cursor;
    }

    private static int containingParagraphStart(String html, int marker) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.lastIndexOf("<p", marker);
        while (start >= 0) {
            int nameEnd = start + 2;
            char boundary = nameEnd < lower.length() ? lower.charAt(nameEnd) : 0;
            if ((Character.isWhitespace(boundary) || boundary == '>')
                    && lower.lastIndexOf("</p>", marker) < start) return start;
            start = lower.lastIndexOf("<p", start - 1);
        }
        return -1;
    }

    private static int blockStart(String html, int marker) {
        int paragraph = containingParagraphStart(html, marker);
        return paragraph >= 0 ? paragraph : tagStart(html, marker);
    }

    private static int idPosition(String html, String fragment) {
        if (fragment == null || fragment.isEmpty()) return -1;
        Matcher matcher = Pattern.compile("(?i)\\bid\\s*=\\s*([\"'])"
                + Pattern.quote(fragment) + "\\1").matcher(html);
        return matcher.find() ? matcher.start() : -1;
    }

    private static int tagStart(String html, int from) {
        int start = html.lastIndexOf('<', Math.max(0, from));
        return start < 0 ? Math.max(0, from) : start;
    }

    private static String head(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<head");
        int end = start < 0 ? -1 : lower.indexOf("</head>", start);
        return start < 0 || end < 0
                ? "<head><meta charset=\"utf-8\"></head>"
                : html.substring(start, end + 7);
    }

    private static String bodyOpeningTag(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        int end = start < 0 ? -1 : html.indexOf('>', start);
        return start < 0 || end < 0 ? "<body>" : html.substring(start, end + 1);
    }

    private static int bodyStart(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        int end = start < 0 ? -1 : html.indexOf('>', start);
        return end < 0 ? 0 : end + 1;
    }

    private static int bodyEnd(String html) {
        int end = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return end < 0 ? html.length() : end;
    }

    private static String read(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
