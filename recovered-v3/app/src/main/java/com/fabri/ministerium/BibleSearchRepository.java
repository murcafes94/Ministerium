package com.fabri.ministerium;

import android.content.Context;
import android.text.Html;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private BibleSearchRepository() {}

    public static List<Hit> search(Context context, String query,
                                   boolean flexible, int limit) throws Exception {
        String wanted = normalize(query);
        String[] tokens = wanted.split(" ");
        List<Hit> result = new ArrayList<>();
        List<BibleRepository.Book> books = BibleRepository.books(context);
        File root = EpubUtils.ensureExtracted(context, HoursRepository.BIBLE);
        for (int b = 0; b < books.size() && result.size() < limit; b++) {
            BibleRepository.Book book = books.get(b);
            for (int c = 0; c < book.chapters.size() && result.size() < limit; c++) {
                BibleRepository.Chapter chapter = book.chapters.get(c);
                BibleRepository.Chapter next = c + 1 < book.chapters.size()
                        ? book.chapters.get(c + 1) : null;
                String html = BibleChapterDocument.from(root, chapter.file, chapter.fragment,
                        chapter.number, next == null ? null : next.file,
                        next == null ? null : next.fragment,
                        next == null ? -1 : next.number);
                String plain = Html.fromHtml(html.replaceAll("(?i)<sup[^>]*>.*?</sup>", " "))
                        .toString().replaceAll("\\s+", " ").trim();
                String normalized = normalize(plain);
                if (!matches(normalized, wanted, tokens, flexible)) continue;
                int at = normalized.indexOf(flexible ? firstToken(tokens) : wanted);
                int originalAt = proportionalIndex(plain, normalized, Math.max(0, at));
                int start = Math.max(0, originalAt - 90);
                int end = Math.min(plain.length(), originalAt + query.length() + 150);
                String snippet = (start > 0 ? "…" : "")
                        + plain.substring(start, end).trim() + (end < plain.length() ? "…" : "");
                result.add(new Hit(b, c, BibleRepository.citationAbbreviation(book)
                        + " " + chapter.number, snippet));
            }
        }
        return result;
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
