package com.fabri.ministerium;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Composición por bloques de Oficio de lecturas y Laudes según OGLH 99. */
public final class CombinedHoursRepository {
    private static final Pattern PARAGRAPH = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private CombinedHoursRepository() {}

    public static Result officeAndLauds(Context context, Calendar date,
                                        HoursLink selectedProper,
                                        CommonOfficeChoice common) throws Exception {
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        List<HourEntry> entries = DailyHoursRepository.hoursFor(
                context, day.temporalOffice, date);
        HourEntry invitatory = find(entries, "invitatory");
        HourEntry office = find(entries, "office");
        HourEntry lauds = find(entries, "lauds");
        if (office == null || lauds == null) throw new IllegalStateException(
                "No están disponibles el Oficio y Laudes para esta fecha.");

        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
        String cycle = LiturgicalResolver.lectionaryCycle(date);
        int readingsYear = date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1;
        EntryDocument invitatoryDocument = invitatory == null ? null
                : entryDocument(context, invitatory, selectedProper, common,
                ordinaryWeek, cycle, readingsYear);
        EntryDocument officeDocument = entryDocument(context, office, selectedProper, common,
                ordinaryWeek, cycle, readingsYear);
        EntryDocument laudsDocument = entryDocument(context, lauds, selectedProper, common,
                ordinaryWeek, cycle, readingsYear);
        String invitatoryHtml = invitatoryDocument == null ? "" : invitatoryDocument.html;
        String officeHtml = officeDocument.html;
        String laudsHtml = laudsDocument.html;

        File root = EpubUtils.ensureExtracted(context, office.volume);
        if (selectedProper == null) {
            if (invitatory != null) invitatoryHtml = inlineLink(root,
                    invitatory.filePath, invitatoryHtml, "INVITATORIO");
            officeHtml = inlineLink(root, office.filePath, officeHtml, "TE DEUM");
            laudsHtml = inlineLink(root, lauds.filePath, laudsHtml, "BENEDICTUS");
        }

        String invitatoryBody = body(invitatoryHtml);
        String officeBody = body(officeHtml);
        String laudsBody = body(laudsHtml);
        String laudsHymn = between(laudsBody, "HIMNO", "SALMODIA", true);
        String officeFromPsalmody = from(officeBody, "SALMODIA");
        officeFromPsalmody = beforeLast(officeFromPsalmody, "ORACION");
        String laudsFromPsalmody = from(laudsBody, "SALMODIA");

        String note = "<section class=\"ministerium-union-note\"><b>Unión litúrgica · OGLH 99</b>"
                + "<br>Se usa al comienzo el himno de Laudes; al final del Oficio se omiten "
                + "la oración y la conclusión; Laudes continúa directamente con su salmodia.</section>";
        StringBuilder document = new StringBuilder("<!doctype html><html><head>"
                + "<meta charset=\"utf-8\"></head><body>");
        document.append("<h1>Oficio de lecturas + Laudes</h1>").append(note);
        if (!invitatoryBody.isEmpty()) document.append("<section data-block=\"invitatory\">"
                + "<h2>Invitatorio</h2>").append(invitatoryBody).append("</section>");
        document.append("<section data-block=\"lauds-hymn\"><h2>Himno de Laudes</h2>")
                .append(laudsHymn).append("</section>")
                .append("<section data-block=\"office\"><h2>Oficio de lecturas</h2>")
                .append(officeFromPsalmody).append("</section>")
                .append("<section data-block=\"lauds\"><h2>Laudes</h2>")
                .append(laudsFromPsalmody).append("</section></body></html>");
        return new Result(document.toString(), officeDocument.baseUrl,
                day.celebration);
    }

    private static EntryDocument entryDocument(Context context, HourEntry entry,
                                               HoursLink saint,
                                               CommonOfficeChoice common,
                                               int ordinaryWeek, String cycle,
                                               int readingsYear) throws Exception {
        if (saint != null && ("invitatory".equals(entry.key) || "office".equals(entry.key)
                || "lauds".equals(entry.key))) {
            MemoryOffice result = SaintOfficeRepository.compose(context, entry, saint,
                    common, ordinaryWeek, cycle, readingsYear);
            if (result != null) return new EntryDocument(result.html, result.baseUrl);
        }
        File root = EpubUtils.ensureExtracted(context, entry.volume);
        File target = new File(root, entry.filePath);
        if ("ordinary".equals(entry.volume.id) && ordinaryWeek > 0) {
            return new EntryDocument(OrdinaryReferenceResolver.resolve(root, entry.filePath,
                    ordinaryWeek, cycle, readingsYear), Uri.fromFile(target).toString());
        }
        return new EntryDocument(read(target), Uri.fromFile(target).toString());
    }

    private static HourEntry find(List<HourEntry> entries, String key) {
        for (HourEntry entry : entries) if (key.equals(entry.key)) return entry;
        return null;
    }

    private static String inlineLink(File root, String sourcePath, String html,
                                     String label) throws Exception {
        Matcher paragraphs = PARAGRAPH.matcher(html);
        while (paragraphs.find()) {
            Matcher anchor = ANCHOR.matcher(paragraphs.group());
            if (!anchor.find() || !plain(anchor.group(2)).equals(label)) continue;
            String[] parts = anchor.group(1).trim().split("#", 2);
            URI base = new URI(null, null, sourcePath, null);
            String targetPath = parts[0].isEmpty() ? sourcePath
                    : base.resolve(parts[0]).normalize().getPath();
            while (targetPath.startsWith("/")) targetPath = targetPath.substring(1);
            File target = new File(root, targetPath).getCanonicalFile();
            if (!target.getPath().startsWith(root.getCanonicalPath() + File.separator)
                    || !target.isFile()) return html;
            String content = body(read(target));
            return html.substring(0, paragraphs.start()) + content
                    + html.substring(paragraphs.end());
        }
        return html;
    }

    private static String body(String html) {
        int start = html.toLowerCase(Locale.ROOT).indexOf("<body");
        if (start < 0) return html;
        start = html.indexOf('>', start);
        int end = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return start < 0 ? html : html.substring(start + 1,
                end < start ? html.length() : end);
    }

    private static String between(String html, String startLabel, String endLabel,
                                  boolean includeStart) {
        List<Block> blocks = blocks(html);
        int start = -1;
        int end = html.length();
        for (Block block : blocks) {
            String text = plain(block.html);
            if (start < 0 && text.equals(startLabel)) start = includeStart ? block.start : block.end;
            else if (start >= 0 && text.equals(endLabel)) {
                end = block.start;
                break;
            }
        }
        return start < 0 ? "" : html.substring(start, Math.max(start, end));
    }

    private static String from(String html, String label) {
        for (Block block : blocks(html)) {
            if (plain(block.html).equals(label)) return html.substring(block.start);
        }
        return html;
    }

    private static String beforeLast(String html, String label) {
        int position = -1;
        for (Block block : blocks(html)) {
            if (plain(block.html).equals(label)) position = block.start;
        }
        return position < 0 ? html : html.substring(0, position);
    }

    private static List<Block> blocks(String html) {
        List<Block> result = new ArrayList<>();
        Matcher matcher = PARAGRAPH.matcher(html);
        while (matcher.find()) result.add(new Block(matcher.start(), matcher.end(),
                matcher.group()));
        return result;
    }

    private static String plain(String value) {
        value = value == null ? "" : value.replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&");
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim()
                .toUpperCase(Locale.ROOT);
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

    public static final class Result {
        public final String html;
        public final String baseUrl;
        public final String celebration;
        Result(String html, String baseUrl, String celebration) {
            this.html = html;
            this.baseUrl = baseUrl;
            this.celebration = celebration;
        }
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

    private static final class EntryDocument {
        final String html;
        final String baseUrl;
        EntryDocument(String html, String baseUrl) {
            this.html = html;
            this.baseUrl = baseUrl;
        }
    }
}
