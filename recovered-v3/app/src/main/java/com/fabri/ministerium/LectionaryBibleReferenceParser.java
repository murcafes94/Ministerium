package com.fabri.ministerium;

import com.fabri.ministerium.bible.semantic.BibleBookIdResolver;
import com.fabri.ministerium.bible.semantic.BiblePassageReference;
import com.fabri.ministerium.bible.semantic.BibleReference;
import com.fabri.ministerium.bible.semantic.SemanticBiblePackages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts common Spanish lectionary citations into semantic Bible references. */
public final class LectionaryBibleReferenceParser {
    private static final Pattern START = Pattern.compile(
            "^\\s*((?:[123]\\s*)?[\\p{L}]+(?:\\s+[\\p{L}]+)?)\\s+(\\d+)\\s*,\\s*(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RANGE = Pattern.compile(
            "^(\\d+[a-z]?)(?:\\s*[-–—]\\s*(\\d+[a-z]?))?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NEW_CHAPTER = Pattern.compile(
            "^(\\d+)\\s*,\\s*(\\d+[a-z]?)(?:\\s*[-–—]\\s*(\\d+[a-z]?))?$",
            Pattern.CASE_INSENSITIVE);

    private LectionaryBibleReferenceParser() {}

    public static BiblePassageReference parse(String printedCitation) {
        if (printedCitation == null) return null;
        String cleaned = printedCitation.trim()
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("^[\\[(]+|[\\])]+$", "")
                .trim();
        Matcher start = START.matcher(cleaned);
        if (!start.matches()) return null;

        String printedBook = start.group(1).trim();
        String bookId = BibleBookIdResolver.resolve(printedBook, printedBook);
        if (bookId == null) return null;
        int chapter = Integer.parseInt(start.group(2));
        String tail = start.group(3).trim();

        List<BibleReference> segments = new ArrayList<>();
        String[] groups = tail.split("\\s*[.;]\\s*");
        int currentChapter = chapter;
        for (String group : groups) {
            if (group.isEmpty()) continue;
            Matcher nextChapter = NEW_CHAPTER.matcher(group);
            if (nextChapter.matches()) {
                currentChapter = Integer.parseInt(nextChapter.group(1));
                add(segments, bookId, currentChapter, nextChapter.group(2), nextChapter.group(3));
                continue;
            }
            Matcher range = RANGE.matcher(group);
            if (range.matches()) {
                add(segments, bookId, currentChapter, range.group(1), range.group(2));
            }
        }
        if (segments.isEmpty()) return null;
        return new BiblePassageReference(printedCitation, segments);
    }

    private static void add(List<BibleReference> segments, String bookId, int chapter,
                            String first, String last) {
        segments.add(new BibleReference(SemanticBiblePackages.DEFAULT_EDITION_ID,
                bookId, chapter, first, last == null ? first : last));
    }
}
