package com.fabri.ministerium;

import android.content.Context;

import com.fabri.ministerium.bible.semantic.BibleBook;
import com.fabri.ministerium.bible.semantic.BibleBookIdResolver;
import com.fabri.ministerium.bible.semantic.BibleVerse;
import com.fabri.ministerium.bible.semantic.SemanticBiblePackages;
import com.fabri.ministerium.bible.semantic.SemanticBibleRenderer;
import com.fabri.ministerium.bible.semantic.SqliteBibleRepository;

import java.util.List;

/** Non-breaking bridge: semantic SQLite when installed, current EPUB otherwise. */
public final class SemanticBibleReaderBridge {
    private SemanticBibleReaderBridge() {}

    public static String renderIfAvailable(Context context, BibleRepository.Book legacyBook,
                                           int chapterNumber) {
        String bookId = BibleBookIdResolver.resolve(legacyBook.abbreviation, legacyBook.title);
        if (bookId == null) return null;

        SqliteBibleRepository repository = null;
        try {
            repository = SemanticBiblePackages.openIfInstalled(
                    context, SemanticBiblePackages.DEFAULT_EDITION_ID);
            if (repository == null) return null;

            BibleBook semanticBook = null;
            List<BibleBook> books = repository.listBooks();
            for (BibleBook candidate : books) {
                if (bookId.equals(candidate.getBookId())) {
                    semanticBook = candidate;
                    break;
                }
            }
            if (semanticBook == null) return null;
            List<BibleVerse> verses = repository.getChapter(bookId, chapterNumber);
            if (verses.isEmpty()) return null;
            return SemanticBibleRenderer.chapter(repository.getEdition(), semanticBook,
                    chapterNumber, verses);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (repository != null) repository.close();
        }
    }
}
