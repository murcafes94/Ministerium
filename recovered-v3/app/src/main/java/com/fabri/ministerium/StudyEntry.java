package com.fabri.ministerium;

import org.json.JSONException;
import org.json.JSONObject;

public final class StudyEntry {
    public static final String HIGHLIGHT = "highlight";
    public static final String NOTE = "note";
    public static final String MEDITATION = "meditation";

    public String id = "";
    public String type = MEDITATION;
    public String category = "Libres";
    public String source = "";
    public String sourceKey = "";
    public String title = "";
    public String reference = "";
    public String quote = "";
    public String body = "";
    public String color = "yellow";
    public long createdAt;
    public long updatedAt;

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("type", type)
                .put("category", category).put("source", source)
                .put("sourceKey", sourceKey).put("title", title)
                .put("reference", reference).put("quote", quote)
                .put("body", body).put("color", color)
                .put("createdAt", createdAt).put("updatedAt", updatedAt);
    }

    public static StudyEntry fromJson(JSONObject value) {
        StudyEntry entry = new StudyEntry();
        entry.id = value.optString("id");
        entry.type = value.optString("type", MEDITATION);
        entry.category = value.optString("category", "Libres");
        entry.source = value.optString("source");
        entry.sourceKey = value.optString("sourceKey");
        entry.title = value.optString("title");
        entry.reference = value.optString("reference");
        entry.quote = value.optString("quote");
        entry.body = value.optString("body");
        entry.color = value.optString("color", "yellow");
        entry.createdAt = value.optLong("createdAt");
        entry.updatedAt = value.optLong("updatedAt", entry.createdAt);
        return entry;
    }
}
