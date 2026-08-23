package com.fabri.ministerium;

import android.content.Context;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconoce referencias antes de iniciar una búsqueda textual. */
public final class ReferenceParser {
    public enum Kind { BIBLE, CANON, CATECHISM }

    public static final class Target {
        public final Kind kind;
        public final int number;
        public final int bookIndex;
        public final int chapterIndex;
        public final int verseStart;
        public final int verseEnd;

        Target(Kind kind, int number, int bookIndex, int chapterIndex,
               int verseStart, int verseEnd) {
            this.kind = kind;
            this.number = number;
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.verseStart = verseStart;
            this.verseEnd = verseEnd;
        }
    }

    private static final Pattern CANON = Pattern.compile(
            "(?i)^\\s*(?:c\\.?|can\\.?|canon)\\s*(\\d{1,4})\\s*$");
    private static final Pattern CATECHISM = Pattern.compile(
            "(?i)^\\s*(?:CEC\\.?|Cat\\.?|Catecismo)\\s*(\\d{1,4})\\s*$");
    private static final Pattern BIBLE = Pattern.compile(
            "^\\s*(.+?)\\s+(\\d{1,3})(?:\\s*[,.:]\\s*(\\d{1,3})"
                    + "(?:\\s*[-–—]\\s*(\\d{1,3}))?)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private ReferenceParser() {}

    public static Target parse(Context context, String query) {
        if (query == null) return null;
        Matcher canon = CANON.matcher(query);
        if (canon.matches()) {
            int value = integer(canon.group(1));
            return value >= 1 && value <= 1752
                    ? new Target(Kind.CANON, value, -1, -1, -1, -1) : null;
        }
        Matcher catechism = CATECHISM.matcher(query);
        if (catechism.matches()) {
            int value = integer(catechism.group(1));
            return value >= 1 && value <= 2865
                    ? new Target(Kind.CATECHISM, value, -1, -1, -1, -1) : null;
        }
        Matcher bible = BIBLE.matcher(query);
        if (!bible.matches()) return null;
        try {
            List<BibleRepository.Book> books = BibleRepository.books(context);
            int bookIndex = findBook(books, bible.group(1));
            int chapter = integer(bible.group(2));
            if (bookIndex < 0 || chapter < 1
                    || chapter > books.get(bookIndex).chapters.size()) return null;
            int start = bible.group(3) == null ? -1 : integer(bible.group(3));
            int end = bible.group(4) == null ? start : integer(bible.group(4));
            return new Target(Kind.BIBLE, -1, bookIndex, chapter - 1, start, end);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int findBook(List<BibleRepository.Book> books, String value) {
        String wanted = normalize(value).replaceFirst("^(libro|evangelio segun san|carta a los) ", "");
        if ("sal".equals(wanted) || "salmo".equals(wanted)) wanted = "salmos";
        if ("juan".equals(wanted)) wanted = "jn";
        if ("marcos".equals(wanted)) wanted = "mc";
        if ("mateo".equals(wanted)) wanted = "mt";
        if ("lucas".equals(wanted)) wanted = "lc";
        if ("1 cor".equals(wanted)) wanted = "1 co";
        if ("2 cor".equals(wanted)) wanted = "2 co";
        for (int i = 0; i < books.size(); i++) {
            BibleRepository.Book book = books.get(i);
            String title = normalize(book.title)
                    .replace("evangelio segun san ", "")
                    .replace("primera carta a los ", "1 ")
                    .replace("segunda carta a los ", "2 ");
            if (wanted.equals(normalize(book.abbreviation))
                    || wanted.equals(normalize(BibleRepository.citationAbbreviation(book)))
                    || wanted.equals(title) || title.endsWith(" " + wanted)) return i;
        }
        return -1;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return -1; }
    }
}
