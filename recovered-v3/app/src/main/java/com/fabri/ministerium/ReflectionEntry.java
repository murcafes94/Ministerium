package com.fabri.ministerium;

import org.json.JSONException;
import org.json.JSONObject;

public final class ReflectionEntry {
    public String id, source, sourceKey, title, subtitle, quote, reflection;
    public int bookIndex = -1, chapterIndex = -1, year, month, day;

    public JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("id", id).put("source", source).put("sourceKey", sourceKey)
                .put("title", title).put("subtitle", subtitle).put("quote", quote)
                .put("reflection", reflection).put("bookIndex", bookIndex)
                .put("chapterIndex", chapterIndex).put("year", year)
                .put("month", month).put("day", day);
        return value;
    }

    public static ReflectionEntry fromJson(JSONObject value) {
        ReflectionEntry entry = new ReflectionEntry();
        entry.id = value.optString("id"); entry.source = value.optString("source");
        entry.sourceKey = value.optString("sourceKey"); entry.title = value.optString("title");
        entry.subtitle = value.optString("subtitle"); entry.quote = value.optString("quote");
        entry.reflection = value.optString("reflection"); entry.bookIndex = value.optInt("bookIndex", -1);
        entry.chapterIndex = value.optInt("chapterIndex", -1); entry.year = value.optInt("year");
        entry.month = value.optInt("month"); entry.day = value.optInt("day"); return entry;
    }
}
