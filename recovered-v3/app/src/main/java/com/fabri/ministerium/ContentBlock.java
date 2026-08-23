package com.fabri.ministerium;

import org.json.JSONException;
import org.json.JSONObject;

/** Unidad semántica común para textos estructurados de Ministerium 3.0. */
public final class ContentBlock {
    public enum Kind {
        TITLE, RUBRIC, ANTIPHON, PSALM, READING, RESPONSE, PRAYER,
        DIALOGUE, MEDITATION, NOTE, PARAGRAPH
    }

    public final String id;
    public final Kind kind;
    public final String title;
    public final String text;
    public final String reference;
    public final String language;

    public ContentBlock(String id, Kind kind, String title, String text,
                        String reference, String language) {
        this.id = value(id);
        this.kind = kind == null ? Kind.PARAGRAPH : kind;
        this.title = value(title);
        this.text = value(text);
        this.reference = value(reference);
        this.language = value(language);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("kind", kind.name())
                .put("title", title).put("text", text)
                .put("reference", reference).put("language", language);
    }

    public static ContentBlock fromJson(JSONObject value) {
        Kind kind;
        try {
            kind = Kind.valueOf(value.optString("kind", Kind.PARAGRAPH.name()));
        } catch (IllegalArgumentException ignored) {
            kind = Kind.PARAGRAPH;
        }
        return new ContentBlock(value.optString("id"), kind,
                value.optString("title"), value.optString("text"),
                value.optString("reference"), value.optString("language"));
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
