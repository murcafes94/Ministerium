package org.ministerium.bible.semantic;

/** Canon-aware Bible book metadata. */
public final class BibleBook {
    private final String editionId;
    private final String bookId;
    private final String name;
    private final String shortName;
    private final String testament;
    private final int canonicalOrder;
    private final int chapterCount;

    public BibleBook(String editionId, String bookId, String name, String shortName, String testament,
                     int canonicalOrder, int chapterCount) {
        this.editionId = editionId;
        this.bookId = bookId;
        this.name = name;
        this.shortName = shortName;
        this.testament = testament;
        this.canonicalOrder = canonicalOrder;
        this.chapterCount = chapterCount;
    }

    public String getEditionId() { return editionId; }
    public String getBookId() { return bookId; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getTestament() { return testament; }
    public int getCanonicalOrder() { return canonicalOrder; }
    public int getChapterCount() { return chapterCount; }
}
