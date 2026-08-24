package com.fabri.ministerium.bible.semantic;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only SQLite implementation used by installed Ministerium Bible packages. */
public final class SqliteBibleRepository implements BibleRepository {
    private final SQLiteDatabase database;
    private BibleEdition edition;

    private SqliteBibleRepository(SQLiteDatabase database) {
        this.database = database;
    }

    public static SqliteBibleRepository openReadOnly(File file) {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Bible package does not exist");
        }
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
                file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        return new SqliteBibleRepository(db);
    }

    @Override public BibleEdition getEdition() {
        if (edition == null) {
            edition = new BibleEdition(
                    meta("edition_id"), meta("name"), meta("abbreviation"), meta("language"),
                    meta("canon"), meta("source_format"), meta("version"), meta("copyright_notice"),
                    meta("license_id"), meta("content_hash"), meta("min_app_version"));
        }
        return edition;
    }

    private String meta(String key) {
        Cursor cursor = database.rawQuery("SELECT value FROM metadata WHERE key = ? LIMIT 1", new String[]{key});
        try { return cursor.moveToFirst() ? cursor.getString(0) : null; }
        finally { cursor.close(); }
    }

    @Override public List<BibleBook> listBooks() {
        ArrayList<BibleBook> books = new ArrayList<>();
        Cursor cursor = database.rawQuery(
                "SELECT book_key, name, short_name, testament, canonical_order, chapter_count "
                        + "FROM books ORDER BY canonical_order", null);
        try {
            while (cursor.moveToNext()) {
                books.add(new BibleBook(getEdition().getEditionId(), cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getInt(5)));
            }
        } finally { cursor.close(); }
        return books;
    }

    @Override public List<Integer> listChapters(String bookId) {
        ArrayList<Integer> chapters = new ArrayList<>();
        Cursor cursor = database.rawQuery(
                "SELECT chapter_number FROM chapters WHERE book_key = ? ORDER BY chapter_number",
                new String[]{bookId});
        try { while (cursor.moveToNext()) chapters.add(cursor.getInt(0)); }
        finally { cursor.close(); }
        return chapters;
    }

    @Override public List<BibleVerse> getChapter(String bookId, int chapter) {
        ArrayList<BibleVerse> verses = new ArrayList<>();
        Cursor cursor = database.rawQuery(
                "SELECT v.verse_label, v.verse_order, v.text, v.is_heading, v.paragraph_start "
                        + "FROM verses v JOIN chapters c ON c.id = v.chapter_id "
                        + "WHERE c.book_key = ? AND c.chapter_number = ? ORDER BY v.verse_order",
                new String[]{bookId, String.valueOf(chapter)});
        try {
            while (cursor.moveToNext()) {
                verses.add(new BibleVerse(getEdition().getEditionId(), bookId, chapter,
                        cursor.getString(0), cursor.getInt(1), cursor.getString(2),
                        cursor.getInt(3) != 0, cursor.getInt(4) != 0));
            }
        } finally { cursor.close(); }
        return verses;
    }

    @Override public List<BibleVerse> getVerseRange(BibleReference reference) {
        if (reference == null) return Collections.emptyList();
        if (reference.getEditionId() != null && !reference.getEditionId().equals(getEdition().getEditionId())) {
            return Collections.emptyList();
        }
        List<BibleVerse> chapter = getChapter(reference.getBookId(), reference.getChapter());
        int start = -1;
        int end = -1;
        for (int i = 0; i < chapter.size(); i++) {
            BibleVerse verse = chapter.get(i);
            if (start < 0 && reference.getVerseStart().equals(verse.getVerseLabel())) start = i;
            if (reference.getVerseEnd().equals(verse.getVerseLabel())) end = i;
        }
        if (start < 0) return Collections.emptyList();
        if (end < start) end = start;
        return new ArrayList<>(chapter.subList(start, end + 1));
    }

    @Override public List<BibleVerse> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        int safeLimit = Math.max(1, Math.min(limit, 500));
        ArrayList<BibleVerse> verses = new ArrayList<>();
        Cursor cursor = database.rawQuery(
                "SELECT c.book_key, c.chapter_number, v.verse_label, v.verse_order, v.text, v.is_heading, v.paragraph_start "
                        + "FROM verses v JOIN chapters c ON c.id = v.chapter_id "
                        + "WHERE v.text LIKE ? ORDER BY c.id, v.verse_order LIMIT ?",
                new String[]{"%" + query.trim() + "%", String.valueOf(safeLimit)});
        try {
            while (cursor.moveToNext()) {
                verses.add(new BibleVerse(getEdition().getEditionId(), cursor.getString(0), cursor.getInt(1),
                        cursor.getString(2), cursor.getInt(3), cursor.getString(4),
                        cursor.getInt(5) != 0, cursor.getInt(6) != 0));
            }
        } finally { cursor.close(); }
        return verses;
    }

    @Override public List<BibleToken> getTokens(String verseId) {
        if (verseId == null || verseId.isEmpty()) return Collections.emptyList();
        ArrayList<BibleToken> tokens = new ArrayList<>();
        Cursor cursor = database.rawQuery(
                "SELECT token_id, verse_id, position, surface, language, lemma, strong_id, morphology, source_dataset "
                        + "FROM tokens WHERE verse_id = ? ORDER BY position", new String[]{verseId});
        try {
            while (cursor.moveToNext()) {
                tokens.add(new BibleToken(cursor.getString(0), cursor.getString(1), cursor.getInt(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),
                        cursor.getString(7), cursor.getString(8)));
            }
        } finally { cursor.close(); }
        return tokens;
    }

    @Override public void close() {
        if (database.isOpen()) database.close();
    }
}
