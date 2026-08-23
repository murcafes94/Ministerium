package com.fabri.ministerium;

import org.json.JSONException;
import org.json.JSONObject;

public final class ReadingMarker {
    public String id = "";
    public String source = "";
    public String sourceKey = "";
    public String citation = "";
    public String quote = "";
    public String subtitle = "";
    public String color = "gold";
    public int bookIndex = -1;
    public int chapterIndex = -1;
    public int year;
    public int month;
    public int day;
    public long createdAt;

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id).put("source", source).put("sourceKey", sourceKey)
                .put("citation", citation).put("quote", quote).put("subtitle", subtitle)
                .put("color", color).put("bookIndex", bookIndex)
                .put("chapterIndex", chapterIndex).put("year", year)
                .put("month", month).put("day", day).put("createdAt", createdAt);
    }

    public static ReadingMarker fromJson(JSONObject value) {
        ReadingMarker marker = new ReadingMarker();
        marker.id = value.optString("id");
        marker.source = value.optString("source");
        marker.sourceKey = value.optString("sourceKey");
        marker.citation = value.optString("citation");
        marker.quote = value.optString("quote");
        marker.subtitle = value.optString("subtitle");
        marker.color = value.optString("color", "gold");
        marker.bookIndex = value.optInt("bookIndex", -1);
        marker.chapterIndex = value.optInt("chapterIndex", -1);
        marker.year = value.optInt("year");
        marker.month = value.optInt("month");
        marker.day = value.optInt("day");
        marker.createdAt = value.optLong("createdAt");
        return marker;
    }
}
