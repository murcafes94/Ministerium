package com.fabri.ministerium.bible.semantic;

/** Verse unit addressed by semantic IDs instead of page positions. */
public final class BibleVerse {
    private final String editionId;
    private final String bookId;
    private final int chapter;
    private final String verseLabel;
    private final int verseOrder;
    private final String text;
    private final boolean heading;
    private final boolean paragraphStart;

    public BibleVerse(String editionId, String bookId, int chapter, String verseLabel, int verseOrder,
                      String text, boolean heading, boolean paragraphStart) {
        this.editionId = editionId;
        this.bookId = bookId;
        this.chapter = chapter;
        this.verseLabel = verseLabel;
        this.verseOrder = verseOrder;
        this.text = text;
        this.heading = heading;
        this.paragraphStart = paragraphStart;
    }

    public String getEditionId() { return editionId; }
    public String getBookId() { return bookId; }
    public int getChapter() { return chapter; }
    public String getVerseLabel() { return verseLabel; }
    public int getVerseOrder() { return verseOrder; }
    public String getText() { return text; }
    public boolean isHeading() { return heading; }
    public boolean isParagraphStart() { return paragraphStart; }

    public String stableId() {
        return editionId + ":" + bookId + ":" + chapter + ":" + verseLabel;
    }
}
