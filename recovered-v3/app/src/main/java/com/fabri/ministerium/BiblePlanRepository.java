package com.fabri.ministerium;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BiblePlanRepository {
    public static final class Plan {
        public final String id;
        public final String title;
        public final String subtitle;
        public final int days;
        private final boolean gospelsOnly;
        private final int bookIndex;

        Plan(String id, String title, String subtitle, int days, boolean gospelsOnly,
             int bookIndex) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.days = days;
            this.gospelsOnly = gospelsOnly;
            this.bookIndex = bookIndex;
        }
    }

    public static final class DayReading {
        public final int day;
        public final String citation;
        public final int bookIndex;
        public final int chapterIndex;
        public final int chapterCount;
        private final List<ChapterRef> chapters;

        DayReading(int day, String citation, int bookIndex, int chapterIndex,
                   List<ChapterRef> chapters) {
            this.day = day;
            this.citation = citation;
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.chapters = new ArrayList<>(chapters);
            this.chapterCount = chapters.size();
        }

        public boolean contains(int book, int chapter) {
            for (ChapterRef value : chapters) {
                if (value.bookIndex == book && value.chapterIndex == chapter) return true;
            }
            return false;
        }
    }

    private static final List<Plan> PLANS = Arrays.asList(
            new Plan("bible_365", "Biblia completa en 365 días",
                    "De Génesis a Apocalipsis · 3 o 4 capítulos diarios", 365, false, -1),
            new Plan("gospels_89", "Los cuatro Evangelios en 89 días",
                    "Un capítulo diario de Mateo, Marcos, Lucas y Juan", 89, true, -1)
    );

    private BiblePlanRepository() {}

    public static List<Plan> plans() { return PLANS; }

    public static Plan find(String id) {
        for (Plan plan : PLANS) if (plan.id.equals(id)) return plan;
        return null;
    }

    public static Plan find(Context context, String id) {
        Plan standard = find(id);
        if (standard != null) return standard;
        if (id != null && id.startsWith("custom_bible_")) {
            try {
                int days = Integer.parseInt(id.substring("custom_bible_".length()));
                return days < 1 ? null : new Plan(id, "Biblia completa en " + days + " días",
                        "Distribución personalizada de todos los capítulos", days, false, -1);
            } catch (Exception error) { return null; }
        }
        if (id == null || !id.startsWith("book_")) return null;
        try {
            String value = id.substring("book_".length());
            int marker = value.indexOf("_days_");
            int bookIndex = Integer.parseInt(marker < 0 ? value : value.substring(0, marker));
            List<BibleRepository.Book> books = BibleRepository.books(context);
            if (bookIndex < 0 || bookIndex >= books.size()) return null;
            BibleRepository.Book book = books.get(bookIndex);
            int days = marker < 0 ? book.chapters.size()
                    : Integer.parseInt(value.substring(marker + "_days_".length()));
            days = Math.max(1, Math.min(book.chapters.size(), days));
            return new Plan(id, "Completar " + book.title + " en " + days + " días",
                    "Un capítulo diario · " + book.testament, days, false, bookIndex);
        } catch (Exception error) {
            return null;
        }
    }

    public static DayReading reading(Context context, Plan plan, int requestedDay)
            throws Exception {
        List<BibleRepository.Book> books = BibleRepository.books(context);
        List<ChapterRef> chapters = new ArrayList<>();
        for (int bookIndex = 0; bookIndex < books.size(); bookIndex++) {
            BibleRepository.Book book = books.get(bookIndex);
            if (plan.bookIndex >= 0 && plan.bookIndex != bookIndex) continue;
            if (plan.gospelsOnly && !isGospel(book.abbreviation)) continue;
            for (int chapterIndex = 0; chapterIndex < book.chapters.size(); chapterIndex++) {
                chapters.add(new ChapterRef(bookIndex, chapterIndex,
                        book.chapters.get(chapterIndex).number));
            }
        }
        int day = Math.max(1, Math.min(plan.days, requestedDay));
        int start = (int) Math.floor((day - 1) * chapters.size() / (double) plan.days);
        int endExclusive = (int) Math.floor(day * chapters.size() / (double) plan.days);
        if (endExclusive <= start) endExclusive = Math.min(chapters.size(), start + 1);
        List<ChapterRef> selected = chapters.subList(start, endExclusive);
        ChapterRef first = selected.get(0);
        return new DayReading(day, citation(books, selected), first.bookIndex,
                first.chapterIndex, selected);
    }

    private static String citation(List<BibleRepository.Book> books,
                                   List<ChapterRef> chapters) {
        StringBuilder result = new StringBuilder();
        int start = 0;
        while (start < chapters.size()) {
            int end = start;
            while (end + 1 < chapters.size()
                    && chapters.get(end + 1).bookIndex == chapters.get(start).bookIndex
                    && chapters.get(end + 1).number == chapters.get(end).number + 1) end++;
            ChapterRef first = chapters.get(start);
            ChapterRef last = chapters.get(end);
            if (result.length() > 0) result.append("; ");
            result.append(BibleRepository.citationAbbreviation(
                    books.get(first.bookIndex))).append(' ').append(first.number);
            if (last.number != first.number) result.append('–').append(last.number);
            start = end + 1;
        }
        return result.toString();
    }

    private static boolean isGospel(String abbreviation) {
        return "Mt".equals(abbreviation) || "Mc".equals(abbreviation)
                || "Lc".equals(abbreviation) || "Jn".equals(abbreviation);
    }

    private static final class ChapterRef {
        final int bookIndex;
        final int chapterIndex;
        final int number;

        ChapterRef(int bookIndex, int chapterIndex, int number) {
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.number = number;
        }
    }
}
