package com.fabri.ministerium;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BibleRepository {
    public static final class Chapter {
        public final int number; public final String file; public final String fragment;
        Chapter(int number, String file, String fragment) {
            this.number = number; this.file = file; this.fragment = fragment;
        }
    }
    public static final class Book {
        public final String abbreviation; public final String title; public final String testament;
        public final List<Chapter> chapters;
        Book(String abbreviation, String title, String testament, List<Chapter> chapters) {
            this.abbreviation = abbreviation; this.title = title; this.testament = testament;
            this.chapters = Collections.unmodifiableList(chapters);
        }
    }
    private static List<Book> cache;
    private BibleRepository() {}
    public static String citationAbbreviation(Book book) {
        if (book == null) return "";
        if ("1 Co".equals(book.abbreviation)) return "1 Cor";
        if ("2 Co".equals(book.abbreviation)) return "2 Cor";
        return book.abbreviation;
    }
    public static synchronized List<Book> books(Context context) throws Exception {
        if (cache != null) return cache;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = context.getAssets().open("bible-index.json")) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        JSONArray values = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .getJSONArray("books");
        List<Book> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i); JSONArray chapterValues = value.getJSONArray("chapters");
            List<Chapter> chapters = new ArrayList<>();
            for (int j = 0; j < chapterValues.length(); j++) {
                JSONObject chapter = chapterValues.getJSONObject(j);
                chapters.add(new Chapter(chapter.getInt("number"), chapter.getString("file"), chapter.getString("fragment")));
            }
            result.add(new Book(value.getString("abbreviation"), value.getString("title"), value.getString("testament"), chapters));
        }
        cache = Collections.unmodifiableList(result); return cache;
    }
}
