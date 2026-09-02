package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CanonCommentaryRepository {
    public static final class Entry {
        public final int canon;
        public final String asset;
        public final String fragment;
        public final String commentedCanons;

        Entry(int canon, String asset, String fragment, String commentedCanons) {
            this.canon = canon;
            this.asset = asset;
            this.fragment = fragment;
            this.commentedCanons = commentedCanons;
        }
    }

    private static volatile Map<Integer, Entry> cache;
    private static final Map<String, String> ARTICLE_CHUNK_CACHE =
            new LinkedHashMap<String, String>(6, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > 8;
                }
            };

    private CanonCommentaryRepository() {}

    public static Entry find(Context context, int canon) throws Exception {
        return entries(context).get(canon);
    }

    public static String article(Context context, Entry entry) throws Exception {
        String html = read(context, entry.asset);
        int marker = html.indexOf("id=\"" + entry.fragment + "\"");
        if (marker < 0) return "";
        int start = html.lastIndexOf("<article", marker);
        int end = html.indexOf("</article>", marker);
        if (start < 0 || end < 0) return "";
        return html.substring(start, end + "</article>".length());
    }

    private static Map<Integer, Entry> entries(Context context) throws Exception {
        Map<Integer, Entry> current = cache;
        if (current != null) return current;
        synchronized (CanonCommentaryRepository.class) {
            if (cache != null) return cache;
            Map<Integer, Entry> loaded = new HashMap<>();
            try (InputStream input = context.getAssets().open("canon-commentary-index.tsv");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') continue;
                    String[] parts = line.split("\\t", -1);
                    if (parts.length < 4) continue;
                    int canon = Integer.parseInt(parts[0]);
                    loaded.put(canon, new Entry(canon, parts[1], parts[2], parts[3]));
                }
            }
            cache = loaded;
            return loaded;
        }
    }

    private static String read(Context context, String asset) throws Exception {
        synchronized (ARTICLE_CHUNK_CACHE) {
            String cached = ARTICLE_CHUNK_CACHE.get(asset);
            if (cached != null) return cached;
        }
        try (InputStream input = context.getAssets().open(asset);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String value = new String(output.toByteArray(), StandardCharsets.UTF_8);
            synchronized (ARTICLE_CHUNK_CACHE) { ARTICLE_CHUNK_CACHE.put(asset, value); }
            return value;
        }
    }
}
