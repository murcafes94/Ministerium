package com.fabri.ministerium;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class BibleHistoryStore {
    public static final class Entry {
        public final int bookIndex;
        public final int chapterIndex;
        public final String citation;
        public final String title;
        public final long openedAt;

        Entry(int bookIndex, int chapterIndex, String citation, String title, long openedAt) {
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.citation = citation;
            this.title = title;
            this.openedAt = openedAt;
        }
    }

    private static final String PREFS = "bible_history";
    private static final String KEY = "recent_chapters";
    private static final int MAXIMUM = 15;
    private BibleHistoryStore() {}

    public static List<Entry> recent(Context context) {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREFS,
                    Context.MODE_PRIVATE).getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject value = array.getJSONObject(i);
                result.add(new Entry(value.optInt("bookIndex"),
                        value.optInt("chapterIndex"), value.optString("citation"),
                        value.optString("title"), value.optLong("openedAt")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static void record(Context context, int bookIndex, int chapterIndex,
                              String citation, String title) {
        List<Entry> entries = recent(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry value = entries.get(i);
            if (value.bookIndex == bookIndex && value.chapterIndex == chapterIndex) {
                entries.remove(i);
            }
        }
        entries.add(0, new Entry(bookIndex, chapterIndex, citation, title,
                System.currentTimeMillis()));
        while (entries.size() > MAXIMUM) entries.remove(entries.size() - 1);
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) array.put(new JSONObject()
                    .put("bookIndex", entry.bookIndex)
                    .put("chapterIndex", entry.chapterIndex)
                    .put("citation", entry.citation).put("title", entry.title)
                    .put("openedAt", entry.openedAt));
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, array.toString()).apply();
    }
}
