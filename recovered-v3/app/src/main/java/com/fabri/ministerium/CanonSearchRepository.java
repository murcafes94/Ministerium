package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lazy search over the normative canon text only.
 * Commentary is deliberately excluded: it remains accessible from its canon.
 */
public final class CanonSearchRepository {
    private static final Pattern CANON = Pattern.compile(
            "<article class=\\\"canon\\\" id=\\\"canon-(\\d+)\\\">.*?"
                    + "<section class=\\\"canon-language spanish\\\"[^>]*>.*?<p>(.*?)</p>"
                    + ".*?</article>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Map<Integer, List<Entry>> CHUNK_CACHE = new ConcurrentHashMap<>();

    public static final class Entry {
        public final int canon;
        public final String text;
        private final String normalized;

        Entry(int canon, String text) {
            this.canon = canon;
            this.text = text;
            this.normalized = normalize(text);
        }
    }

    private CanonSearchRepository() {}

    public static List<SearchResult> search(Context context, String query, int maximum) {
        String wanted = normalize(query);
        if (wanted.length() < 2 || maximum <= 0) return Collections.emptyList();
        List<SearchResult> out = new ArrayList<>();
        for (int chunk = 1; chunk <= 18 && out.size() < maximum; chunk++) {
            for (Entry entry : chunk(context, chunk)) {
                int at = entry.normalized.indexOf(wanted);
                if (at < 0 && !("canon " + entry.canon).contains(wanted)) continue;
                out.add(SearchResult.canon(entry.canon, snippet(entry.text, wanted)));
                if (out.size() >= maximum) break;
            }
        }
        return out;
    }

    private static List<Entry> chunk(Context context, int number) {
        List<Entry> cached = CHUNK_CACHE.get(number);
        if (cached != null) return cached;
        List<Entry> result = new ArrayList<>();
        String asset = String.format(Locale.US, "canon-text/canons-%02d.html", number);
        try (InputStream input = context.getAssets().open(asset);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String html = new String(output.toByteArray(), StandardCharsets.UTF_8);
            Matcher matcher = CANON.matcher(html);
            while (matcher.find()) {
                int canon = Integer.parseInt(matcher.group(1));
                String text = decode(matcher.group(2).replaceAll("<[^>]+>", " "));
                result.add(new Entry(canon, text.replaceAll("\\s+", " ").trim()));
            }
        } catch (Exception ignored) {}
        List<Entry> immutable = Collections.unmodifiableList(result);
        CHUNK_CACHE.put(number, immutable);
        return immutable;
    }

    private static String snippet(String text, String normalizedQuery) {
        String compact = text.replaceAll("\\s+", " ").trim();
        String normalized = normalize(compact);
        int at = normalized.indexOf(normalizedQuery);
        if (at < 0) return compact.length() > 210 ? compact.substring(0, 210) + "…" : compact;
        int start = Math.max(0, at - 70);
        int end = Math.min(compact.length(), at + normalizedQuery.length() + 120);
        return (start > 0 ? "…" : "") + compact.substring(start, end)
                + (end < compact.length() ? "…" : "");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }

    private static String decode(String value) {
        return value.replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">");
    }
}
