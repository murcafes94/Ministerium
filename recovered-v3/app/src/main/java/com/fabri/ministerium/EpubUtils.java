package com.fabri.ministerium;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EpubUtils {
    private static final Map<String, List<EpubTocEntry>> TOC_CACHE = new ConcurrentHashMap<>();
    private static final String[] MONTHS = {
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    };

    private EpubUtils() {}

    /**
     * For the six Spanish Liturgy of the Hours volumes, the EPUB is a build-time
     * input only. Runtime reads the generated clean TOC and HTML package.
     */
    public static List<EpubTocEntry> tableOfContents(Context context, HoursVolume volume)
            throws Exception {
        List<EpubTocEntry> cached = TOC_CACHE.get(volume.id);
        if (cached != null) return cached;

        if (isCleanHoursVolume(volume) && CleanHoursAssets.isAvailable(context, volume.id)) {
            List<EpubTocEntry> clean = CleanHoursAssets.tableOfContents(context, volume.id);
            TOC_CACHE.put(volume.id, clean);
            return clean;
        }

        byte[] tocBytes = null;
        String tocPath = null;
        try (InputStream input = context.getAssets().open(volume.assetPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith("toc.ncx")) {
                    tocPath = entry.getName();
                    tocBytes = readCurrentEntry(zip);
                    break;
                }
            }
        }
        if (tocBytes == null || tocPath == null) throw new IOException("El libro no contiene índice.");

        String base = tocPath.contains("/")
                ? tocPath.substring(0, tocPath.lastIndexOf('/') + 1) : "";
        List<EpubTocEntry> entries = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(tocBytes), StandardCharsets.UTF_8.name());
        int event;
        int depth = 0;
        String currentTitle = null;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("navPoint".equals(name)) {
                    depth++;
                    currentTitle = null;
                } else if ("text".equals(name) && depth > 0) {
                    currentTitle = parser.nextText().trim();
                } else if ("content".equals(name) && depth > 0 && currentTitle != null) {
                    String source = parser.getAttributeValue(null, "src");
                    if (source != null && !source.trim().isEmpty()) {
                        String[] parts = source.split("#", 2);
                        entries.add(new EpubTocEntry(currentTitle,
                                normalizePath(base + parts[0]),
                                parts.length > 1 ? parts[1] : "",
                                Math.max(0, depth - 1)));
                    }
                }
            } else if (event == XmlPullParser.END_TAG && "navPoint".equals(parser.getName())) {
                depth = Math.max(0, depth - 1);
            }
        }

        List<EpubTocEntry> result = Collections.unmodifiableList(entries);
        TOC_CACHE.put(volume.id, result);
        return result;
    }

    public static File ensureExtracted(Context context, HoursVolume volume) throws IOException {
        if (isCleanHoursVolume(volume) && CleanHoursAssets.isAvailable(context, volume.id)) {
            try {
                return CleanHoursAssets.ensureExtracted(context, volume.id);
            } catch (IOException error) {
                throw error;
            } catch (Exception error) {
                throw new IOException("No se pudo preparar el texto limpio de la Liturgia de las Horas.", error);
            }
        }

        File root = new File(context.getFilesDir(), "liturgy_hours/" + volume.id);
        File marker = new File(root, ".ready");
        if (marker.exists()) return root;
        if (!root.exists() && !root.mkdirs()) throw new IOException("No se pudo preparar el libro.");

        String rootPath = root.getCanonicalPath() + File.separator;
        try (InputStream input = context.getAssets().open(volume.assetPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[16 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                File target = new File(root, entry.getName());
                String targetPath = target.getCanonicalPath();
                if (!targetPath.startsWith(rootPath)) throw new IOException("Ruta del libro no válida.");
                if (entry.isDirectory()) {
                    if (!target.exists() && !target.mkdirs()) {
                        throw new IOException("No se pudo crear una carpeta del libro.");
                    }
                    continue;
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("No se pudo preparar una carpeta del libro.");
                }
                try (FileOutputStream output = new FileOutputStream(target)) {
                    int count;
                    while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
                }
            }
        }
        try (FileOutputStream ignored = new FileOutputStream(marker)) {
            ignored.write(1);
        }
        return root;
    }

    public static int findEntryIndex(Context context, HoursVolume volume,
                                     String... candidates) throws Exception {
        List<EpubTocEntry> entries = tableOfContents(context, volume);
        for (String candidate : candidates) {
            String wanted = normalizeTitle(candidate);
            for (int i = 0; i < entries.size(); i++) {
                if (normalizeTitle(entries.get(i).title).equals(wanted)) return i;
            }
        }
        for (String candidate : candidates) {
            String wanted = normalizeTitle(candidate);
            for (int i = 0; i < entries.size(); i++) {
                String actual = normalizeTitle(entries.get(i).title);
                if (actual.startsWith(wanted) || wanted.startsWith(actual)) return i;
            }
        }
        return -1;
    }

    public static List<HoursLink> saintsForDate(Context context, int month, int day)
            throws Exception {
        HoursVolume santoral = HoursRepository.find("sanctoral");
        List<EpubTocEntry> entries = tableOfContents(context, santoral);
        List<HoursLink> result = new ArrayList<>();
        String currentMonth = "";
        String wantedMonth = MONTHS[Math.max(0, Math.min(11, month))];
        String dayPrefix = String.valueOf(day);
        for (int i = 0; i < entries.size(); i++) {
            EpubTocEntry entry = entries.get(i);
            String title = entry.title.trim();
            if (entry.depth == 0 && isMonth(title)) currentMonth = title.toUpperCase(Locale.ROOT);
            if (!wantedMonth.equals(currentMonth) || entry.depth == 0) continue;
            String normalized = title.replaceAll("\\s+", " ");
            if (normalized.matches("^0?" + dayPrefix + "\\s*-.*")) {
                String cleanTitle = normalized.replaceFirst("^0?" + dayPrefix + "\\s*-\\s*", "");
                result.add(new HoursLink(santoral, i, cleanTitle,
                        "Propio del Santoral · " + wantedMonth));
            }
        }
        return result;
    }

    private static boolean isCleanHoursVolume(HoursVolume volume) {
        if (volume == null) return false;
        String id = volume.id;
        return "advent".equals(id) || "christmas".equals(id) || "lent".equals(id)
                || "easter".equals(id) || "ordinary".equals(id) || "sanctoral".equals(id);
    }

    private static byte[] readCurrentEntry(ZipInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static String normalizePath(String path) {
        String value = path.replace('\\', '/');
        while (value.contains("//")) value = value.replace("//", "/");
        while (value.startsWith("./")) value = value.substring(2);
        return value;
    }

    private static boolean isMonth(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        for (String month : MONTHS) if (month.equals(upper)) return true;
        return false;
    }

    static String normalizeTitle(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replace("Æ", "AE")
                .replace("Œ", "OE")
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }
}
