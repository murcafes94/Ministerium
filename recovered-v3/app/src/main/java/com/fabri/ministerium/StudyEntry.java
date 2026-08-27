package com.fabri.ministerium;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Anotación personal portable.
 *
 * El anclaje usa unidad semántica + offsets + texto exacto + contexto. Los
 * campos nuevos son deliberadamente opcionales para conservar las notas y
 * subrayados creados en versiones anteriores.
 */
public final class StudyEntry {
    public static final String HIGHLIGHT = "highlight";
    public static final String NOTE = "note";
    public static final String MEDITATION = "meditation";
    public static final String BOOKMARK = "bookmark";
    public static final int CURRENT_ANCHOR_VERSION = 3;

    public String id = "";
    public String type = MEDITATION;
    public String category = "Libres";
    public String source = "";
    /** Clave heredada usada para restaurar datos de versiones anteriores. */
    public String sourceKey = "";
    /** Identificador canónico estable para relaciones entre módulos. */
    public String contentId = "";
    public String title = "";
    public String reference = "";
    /** Cita normalizada para mostrar al usuario. */
    public String quote = "";
    /** Texto exacto seleccionado, conservado para reanclar tras cambios menores. */
    public String anchorText = "";
    public String body = "";
    public String color = "yellow";
    /** fill, underline, double, box, margin, question, important. */
    public String style = "fill";
    /** note, star, idea, question, important, prayer, study, bookmark. */
    public String icon = "note";
    public final List<String> tags = new ArrayList<>();

    /** Unidad semántica estable del documento, por ejemplo compline.psalmody. */
    public String semanticUnitId = "";
    /** Offset de caracteres dentro de semanticUnitId. -1 conserva compatibilidad antigua. */
    public int startOffset = -1;
    public int endOffset = -1;
    /** Contexto textual alrededor de la selección para desambiguar frases repetidas. */
    public String prefix = "";
    public String suffix = "";
    public int anchorVersion = CURRENT_ANCHOR_VERSION;

    public long createdAt;
    public long updatedAt;

    public JSONObject toJson() throws JSONException {
        JSONArray tagArray = new JSONArray();
        for (String tag : tags) if (tag != null && !tag.trim().isEmpty()) tagArray.put(tag.trim());
        return new JSONObject().put("id", id).put("type", type)
                .put("category", category).put("source", source)
                .put("sourceKey", sourceKey).put("contentId", contentId)
                .put("title", title).put("reference", reference).put("quote", quote)
                .put("anchorText", anchorText).put("body", body).put("color", color)
                .put("style", style).put("icon", icon)
                .put("tags", tagArray)
                .put("semanticUnitId", semanticUnitId)
                .put("startOffset", startOffset).put("endOffset", endOffset)
                .put("prefix", prefix).put("suffix", suffix)
                .put("anchorVersion", anchorVersion)
                .put("createdAt", createdAt).put("updatedAt", updatedAt);
    }

    public static StudyEntry fromJson(JSONObject value) {
        StudyEntry entry = new StudyEntry();
        entry.id = value.optString("id");
        entry.type = value.optString("type", MEDITATION);
        entry.category = value.optString("category", "Libres");
        entry.source = value.optString("source");
        entry.sourceKey = value.optString("sourceKey");
        entry.contentId = value.optString("contentId", entry.sourceKey);
        entry.title = value.optString("title");
        entry.reference = value.optString("reference");
        entry.quote = value.optString("quote");
        entry.anchorText = value.optString("anchorText", entry.quote);
        entry.body = value.optString("body");
        entry.color = value.optString("color", "yellow");
        entry.style = value.optString("style", "fill");
        entry.icon = value.optString("icon", NOTE.equals(entry.type) ? "note"
                : BOOKMARK.equals(entry.type) ? "bookmark" : "note");
        JSONArray tags = value.optJSONArray("tags");
        if (tags != null) for (int i = 0; i < tags.length(); i++) {
            String tag = tags.optString(i, "").trim();
            if (!tag.isEmpty() && !entry.tags.contains(tag)) entry.tags.add(tag);
        }
        entry.semanticUnitId = value.optString("semanticUnitId");
        entry.startOffset = value.optInt("startOffset", -1);
        entry.endOffset = value.optInt("endOffset", -1);
        entry.prefix = value.optString("prefix");
        entry.suffix = value.optString("suffix");
        entry.anchorVersion = value.optInt("anchorVersion",
                entry.semanticUnitId.isEmpty() ? 1 : CURRENT_ANCHOR_VERSION);
        entry.createdAt = value.optLong("createdAt");
        entry.updatedAt = value.optLong("updatedAt", entry.createdAt);
        return entry;
    }
}
