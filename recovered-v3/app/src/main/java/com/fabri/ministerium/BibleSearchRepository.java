package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Fast offline full-text search over a chapter index generated from the bundled EPUB. */
public final class BibleSearchRepository {
    public static final class Hit {
        public final int bookIndex;
        public final int chapterIndex;
        public final String reference;
        public final String snippet;

        Hit(int bookIndex, int chapterIndex, String reference, String snippet) {
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.reference = reference;
            this.snippet = snippet;
        }
    }

    private static final class ChapterText {
        final int bookIndex;
        final int chapterIndex;
        final String reference;
        final String plain;
        final String normalized;

        ChapterText(int bookIndex, int chapterIndex, String reference, String plain) {
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.reference = reference;
            this.plain = plain;
            this.normalized = normalize(plain);
        }
    }

    private static volatile List<ChapterText> INDEX;

    private BibleSearchRepository() {}

    public static List<Hit> search(Context context, String query,
                                   boolean flexible, int limit) throws Exception {
        String wanted = normalize(query);
        if (wanted.length() < 2 || limit <= 0) return Collections.emptyList();
        String[] tokens = wanted.split(" ");
        List<Hit> result = new ArrayList<>();
        for (ChapterText chapter : index(context)) {
            if (result.size() >= limit) break;
            if (!matches(chapter.normalized, wanted, tokens, flexible)) continue;
            int normalizedAt = chapter.normalized.indexOf(flexible ? firstToken(tokens) : wanted);
            int originalAt = proportionalIndex(chapter.plain, chapter.normalized,
                    Math.max(0, normalizedAt));
            int start = Math.max(0, originalAt - 90);
            int end = Math.min(chapter.plain.length(), originalAt + query.length() + 170);
            String snippet = (start > 0 ? "…" : "")
                    + chapter.plain.substring(start, end).trim()
                    + (end < chapter.plain.length() ? "…" : "");
            result.add(new Hit(chapter.bookIndex, chapter.chapterIndex,
                    chapter.reference, snippet));
        }
        return result;
    }

    private static List<ChapterText> index(Context context) throws Exception {
        List<ChapterText> current = INDEX;
        if (current != null) return current;
        synchronized (BibleSearchRepository.class) {
            if (INDEX != null) return INDEX;
            List<ChapterText> loaded = new ArrayList<>(1300);
            try (InputStream input = context.getAssets().open("bible-search-index.tsv");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"), 32768)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t", 4);
                    if (parts.length != 4) continue;
                    try {
                        loaded.add(new ChapterText(Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]), parts[2], parts[3]));
                    } catch (NumberFormatException ignored) {}
                }
            }
            INDEX = Collections.unmodifiableList(loaded);
            return INDEX;
        }
    }

    private static boolean matches(String text, String wanted, String[] tokens,
                                   boolean flexible) {
        if (!flexible) return text.contains(wanted);
        for (String token : tokens) if (token.length() > 1 && !text.contains(token)) return false;
        return true;
    }

    private static String firstToken(String[] tokens) {
        for (String token : tokens) if (token.length() > 1) return token;
        return "";
    }

    private static int proportionalIndex(String original, String normalized, int at) {
        if (normalized.isEmpty()) return 0;
        return Math.min(original.length(), Math.round(at * original.length()
                / (float) normalized.length()));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
