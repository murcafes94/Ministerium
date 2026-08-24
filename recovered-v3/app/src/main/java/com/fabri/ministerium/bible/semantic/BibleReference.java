package com.fabri.ministerium.bible.semantic;

import java.util.Objects;

/** Stable scripture reference independent from EPUB pages or visible book names. */
public final class BibleReference {
    private final String editionId;
    private final String bookId;
    private final int chapter;
    private final String verseStart;
    private final String verseEnd;

    public BibleReference(String editionId, String bookId, int chapter, String verseStart, String verseEnd) {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter <= 0) {
            throw new IllegalArgumentException("chapter must be greater than zero");
        }
        if (verseStart == null || verseStart.trim().isEmpty()) {
            throw new IllegalArgumentException("verseStart is required");
        }
        this.editionId = editionId;
        this.bookId = bookId.trim().toUpperCase();
        this.chapter = chapter;
        this.verseStart = verseStart.trim();
        this.verseEnd = verseEnd == null || verseEnd.trim().isEmpty() ? this.verseStart : verseEnd.trim();
    }

    public String getEditionId() { return editionId; }
    public String getBookId() { return bookId; }
    public int getChapter() { return chapter; }
    public String getVerseStart() { return verseStart; }
    public String getVerseEnd() { return verseEnd; }

    public String stableKey() {
        String prefix = editionId == null || editionId.isEmpty() ? "*" : editionId;
        return prefix + ":" + bookId + ":" + chapter + ":" + verseStart + "-" + verseEnd;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BibleReference)) return false;
        BibleReference that = (BibleReference) other;
        return chapter == that.chapter
                && Objects.equals(editionId, that.editionId)
                && bookId.equals(that.bookId)
                && verseStart.equals(that.verseStart)
                && verseEnd.equals(that.verseEnd);
    }

    @Override public int hashCode() {
        return Objects.hash(editionId, bookId, chapter, verseStart, verseEnd);
    }

    @Override public String toString() { return stableKey(); }
}
