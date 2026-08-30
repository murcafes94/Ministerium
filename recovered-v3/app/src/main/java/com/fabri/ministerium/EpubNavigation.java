package com.fabri.ministerium;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Navegación EPUB3 mínima basada en el estándar container.xml → OPF → nav.
 * Complementa el lector NCX existente sin incorporar runtimes externos.
 */
public final class EpubNavigation {
    private EpubNavigation() {}

    public static List<EpubTocEntry> navTableOfContents(Context context, String assetPath)
            throws Exception {
        byte[] container = readEntry(context, assetPath, "META-INF/container.xml");
        if (container == null) return Collections.emptyList();
        String opfPath = rootfile(container);
        if (opfPath.isEmpty()) return Collections.emptyList();
        byte[] opf = readEntry(context, assetPath, opfPath);
        if (opf == null) return Collections.emptyList();
        String navHref = navHref(opf);
        if (navHref.isEmpty()) return Collections.emptyList();
        String navPath = resolve(dirname(opfPath), navHref);
        byte[] nav = readEntry(context, assetPath, navPath);
        if (nav == null) return Collections.emptyList();
        return parseNav(nav, navPath);
    }

    private static String rootfile(byte[] xml) throws Exception {
        XmlPullParser parser = parser(xml);
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "rootfile".equals(parser.getName())) {
                String value = parser.getAttributeValue(null, "full-path");
                return value == null ? "" : normalize(value);
            }
        }
        return "";
    }

    private static String navHref(byte[] xml) throws Exception {
        XmlPullParser parser = parser(xml);
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event != XmlPullParser.START_TAG || !"item".equals(parser.getName())) continue;
            String properties = parser.getAttributeValue(null, "properties");
            if (properties == null || !containsWord(properties, "nav")) continue;
            String href = parser.getAttributeValue(null, "href");
            if (href != null && !href.trim().isEmpty()) return href.trim();
        }
        return "";
    }

    private static List<EpubTocEntry> parseNav(byte[] xml, String navPath) throws Exception {
        XmlPullParser parser = parser(xml);
        List<EpubTocEntry> result = new ArrayList<>();
        int event;
        boolean inNav = false;
        boolean chosenNav = false;
        int listDepth = 0;
        String href = null;
        StringBuilder title = null;
        String base = dirname(navPath);
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("nav".equals(name) && !chosenNav) {
                    String type = attributeEnding(parser, "type");
                    String role = attributeEnding(parser, "role");
                    boolean toc = containsWord(type, "toc") || containsWord(role, "doc-toc");
                    // Muchos EPUB solo tienen un nav. Se acepta si no declara otro tipo.
                    inNav = toc || (type.isEmpty() && role.isEmpty());
                    if (inNav) chosenNav = true;
                } else if (inNav && "ol".equals(name)) {
                    listDepth++;
                } else if (inNav && "a".equals(name)) {
                    href = parser.getAttributeValue(null, "href");
                    title = new StringBuilder();
                }
            } else if (event == XmlPullParser.TEXT && inNav && title != null) {
                title.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                if (inNav && "a".equals(name) && title != null && href != null) {
                    String cleanTitle = title.toString().replaceAll("\\s+", " ").trim();
                    String cleanHref = href.trim();
                    if (!cleanTitle.isEmpty() && !cleanHref.isEmpty()
                            && !cleanHref.startsWith("http:") && !cleanHref.startsWith("https:")) {
                        String[] parts = cleanHref.split("#", 2);
                        result.add(new EpubTocEntry(cleanTitle,
                                resolve(base, parts[0]),
                                parts.length > 1 ? parts[1] : "",
                                Math.max(0, listDepth - 1)));
                    }
                    href = null;
                    title = null;
                } else if (inNav && "ol".equals(name)) {
                    listDepth = Math.max(0, listDepth - 1);
                } else if (inNav && "nav".equals(name)) {
                    inNav = false;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static XmlPullParser parser(byte[] xml) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(xml), StandardCharsets.UTF_8.name());
        return parser;
    }

    private static byte[] readEntry(Context context, String assetPath, String wanted)
            throws Exception {
        String normalizedWanted = normalize(wanted);
        try (InputStream input = context.getAssets().open(assetPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !normalize(entry.getName()).equals(normalizedWanted)) continue;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
                return output.toByteArray();
            }
        }
        return null;
    }

    private static String attributeEnding(XmlPullParser parser, String suffix) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if (suffix.equals(name) || (name != null && name.endsWith(":" + suffix))) {
                String value = parser.getAttributeValue(i);
                return value == null ? "" : value;
            }
        }
        return "";
    }

    private static boolean containsWord(String value, String word) {
        if (value == null || value.trim().isEmpty()) return false;
        for (String item : value.trim().split("\\s+")) if (word.equals(item)) return true;
        return false;
    }

    private static String dirname(String path) {
        String value = normalize(path);
        int slash = value.lastIndexOf('/');
        return slash < 0 ? "" : value.substring(0, slash + 1);
    }

    private static String resolve(String base, String relative) {
        if (relative == null || relative.isEmpty()) return normalize(base);
        if (relative.startsWith("/")) return normalize(relative.substring(1));
        return normalize((base == null ? "" : base) + relative);
    }

    private static String normalize(String path) {
        Deque<String> parts = new ArrayDeque<>();
        if (path == null) return "";
        for (String part : path.replace('\\', '/').split("/")) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (!parts.isEmpty()) parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) result.append('/');
            result.append(part);
        }
        return result.toString();
    }
}
