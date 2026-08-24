package com.fabri.ministerium.bible.semantic;

import java.util.List;

/** Storage contract used by the Bible UI and by liturgical reference resolution. */
public interface BibleRepository extends AutoCloseable {
    BibleEdition getEdition();
    List<BibleBook> listBooks();
    List<Integer> listChapters(String bookId);
    List<BibleVerse> getChapter(String bookId, int chapter);
    List<BibleVerse> getVerseRange(BibleReference reference);
    List<BibleVerse> search(String query, int limit);
    List<BibleToken> getTokens(String verseId);

    @Override
    void close();
}
