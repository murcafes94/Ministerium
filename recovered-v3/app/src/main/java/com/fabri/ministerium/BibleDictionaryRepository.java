package com.fabri.ministerium;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public final class BibleDictionaryRepository {
    public static final class Source {
        public final String id;
        public final String title;
        public final String subtitle;
        public final String indexAsset;
        public final HoursVolume volume;

        Source(String id, String title, String subtitle, String indexAsset,
               HoursVolume volume) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.indexAsset = indexAsset;
            this.volume = volume;
        }
    }

    public static final class Entry {
        public final String term;
        public final String normalizedTerm;
        public final String filePath;
        public final String fragment;

        Entry(String term, String filePath, String fragment) {
            this.term = term;
            this.normalizedTerm = normalize(term);
            this.filePath = filePath;
            this.fragment = fragment;
        }
    }

    public static final class QuickResult {
        public final Source source;
        public final Entry entry;
        public final String html;

        QuickResult(Source source, Entry entry, String html) {
            this.source = source;
            this.entry = entry;
            this.html = html;
        }
    }

    private static final List<Source> SOURCES = Collections.unmodifiableList(Arrays.asList(
            new Source("biblical_pdf", "Diccionario bíblico",
                    "2.843 voces · texto extraído del PDF · sin conexión",
                    "dictionary-biblical-index.tsv", HoursRepository.BIBLICAL_DICTIONARY),
            new Source("biblical_san_pablo", "Diccionario bíblico abreviado",
                    "881 voces · Equipo editorial San Pablo · texto extraído · sin conexión",
                    "dictionary-biblical-san-pablo-index.tsv",
                    HoursRepository.SAN_PABLO_BIBLICAL_DICTIONARY),
            new Source("theology_eunsa", "Diccionario de Teología EUNSA",
                    "94 artículos · edición 2006 · sin conexión",
                    "dictionary-theology-index.tsv", HoursRepository.THEOLOGY_DICTIONARY),
            new Source("rae_15", "Diccionario de la lengua española",
                    "85.811 voces · 15.ª edición · sin conexión",
                    "dictionary-rae-index.tsv", HoursRepository.SPANISH_DICTIONARY)
    ));
    private static final Map<String, List<Entry>> ENTRY_CACHE = new HashMap<>();

    private BibleDictionaryRepository() {}

    public static List<Source> sources() {
        return SOURCES;
    }

    public static Source findSource(String id) {
        if (id == null) return null;
        for (Source source : SOURCES) if (source.id.equals(id)) return source;
        return null;
    }

    public static List<Entry> entries(Context context, Source source) throws Exception {
        synchronized (ENTRY_CACHE) {
            List<Entry> cached = ENTRY_CACHE.get(source.id);
            if (cached != null) return cached;
        }
        int capacity = "rae_15".equals(source.id) ? 86_000 : 3_000;
        List<Entry> result = new ArrayList<>(capacity);
        try (InputStream input = context.getAssets().open(source.indexAsset);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] parts = line.split("\\t", -1);
                if (parts.length < 2) continue;
                String term = parts[0].trim();
                String filePath = parts[1].trim();
                String fragment = parts.length > 2 ? parts[2].trim() : "";
                if (!term.isEmpty() && !filePath.isEmpty()) {
                    result.add(new Entry(term, filePath, fragment));
                }
            }
        }
        result = Collections.unmodifiableList(result);
        synchronized (ENTRY_CACHE) { ENTRY_CACHE.put(source.id, result); }
        return result;
    }

    public static List<QuickResult> quickLookup(Context context, String selectedWord)
            throws Exception {
        String word = selectedWord == null ? "" : selectedWord
                .replaceAll("^[^\\p{L}]+|[^\\p{L}]+$", "").trim();
        if (word.isEmpty() || word.contains(" ")) return Collections.emptyList();
        LinkedHashSet<String> candidates = candidates(word);
        List<QuickResult> results = new ArrayList<>();
        for (String sourceId : Arrays.asList("biblical_pdf", "biblical_san_pablo", "rae_15")) {
            Source source = findSource(sourceId);
            Entry match = bestMatch(context, source, candidates);
            if (match == null) continue;
            String article = article(context, source, match);
            if (article.isEmpty()) continue;
            String sourceName = "rae_15".equals(source.id)
                    ? "Diccionario de la lengua española (RAE)" : source.title;
            String card = "<article class=\"dictionary-card\"><h2>" + escape(sourceName)
                    + "</h2><p class=\"dictionary-source\">Resultado para «"
                    + escape(match.term) + "» · sin conexión</p>" + article + "</article>";
            results.add(new QuickResult(source, match, card));
        }
        return results;
    }

    private static Entry bestMatch(Context context, Source source,
                                   LinkedHashSet<String> candidates) throws Exception {
        for (String candidate : candidates) {
            for (Entry entry : entries(context, source)) {
                if (entry.normalizedTerm.equals(candidate)) return entry;
            }
        }
        return null;
    }

    private static LinkedHashSet<String> candidates(String word) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalized = normalize(word);
        result.add(normalized);
        if (normalized.endsWith("ces") && normalized.length() > 4) {
            result.add(normalized.substring(0, normalized.length() - 3) + "z");
        }
        if (normalized.endsWith("es") && normalized.length() > 4) {
            result.add(normalized.substring(0, normalized.length() - 2));
        }
        if (normalized.endsWith("s") && normalized.length() > 3) {
            result.add(normalized.substring(0, normalized.length() - 1));
        }
        return result;
    }

    private static String article(Context context, Source source, Entry entry) throws Exception {
        File root = EpubUtils.ensureExtracted(context, source.volume);
        String document = read(new File(root, entry.filePath));
        if (!entry.fragment.isEmpty()) {
            int marker = document.indexOf("id=\"" + entry.fragment + "\"");
            if (marker < 0) marker = document.indexOf("id='" + entry.fragment + "'");
            int start = marker < 0 ? -1 : document.lastIndexOf("<article", marker);
            int end = marker < 0 ? -1 : document.indexOf("</article>", marker);
            if (start < 0 || end < 0) return "";
            String article = document.substring(start, end + 10);
            int open = article.indexOf('>');
            return open < 0 ? article : article.substring(open + 1, article.length() - 10);
        }
        int position = 0;
        while ((position = document.indexOf("class=\"masnegrita\"", position)) >= 0) {
            int open = document.indexOf('>', position);
            int close = open < 0 ? -1 : document.indexOf("</span>", open);
            if (close < 0) break;
            String term = document.substring(open + 1, close).replaceAll("<[^>]+>", "")
                    .replaceAll("[.]$", "").trim();
            if (normalize(term).equals(entry.normalizedTerm)) {
                int start = document.lastIndexOf("<p", position);
                int end = document.indexOf("</p>", close);
                if (start >= 0 && end >= 0) {
                    return document.substring(start, end + 4).replaceFirst("-&gt;", "");
                }
            }
            position = close + 7;
        }
        return "";
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
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
