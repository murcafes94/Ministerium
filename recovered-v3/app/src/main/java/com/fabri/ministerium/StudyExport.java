package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Exportación portable de anotaciones, notas y reflexiones. */
public final class StudyExport {
    private StudyExport() {}

    public static byte[] json(Context context) throws Exception {
        List<StudyEntry> entries = StudyStore.all(context);
        JSONArray items = new JSONArray();
        for (StudyEntry entry : entries) items.put(entry.toJson());
        JSONObject document = new JSONObject()
                .put("format", "ministerium-study-export")
                .put("schema", 2)
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("exportedAt", System.currentTimeMillis())
                .put("items", items);
        return document.toString(2).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] markdown(Context context) {
        List<StudyEntry> entries = StudyStore.all(context);
        StringBuilder out = new StringBuilder(8192);
        out.append("# Mi estudio — Ministerium\n\n")
                .append("Exportado: ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()))
                .append("\n\n");
        if (entries.isEmpty()) {
            out.append("_No hay anotaciones guardadas._\n");
            return out.toString().getBytes(StandardCharsets.UTF_8);
        }
        for (StudyEntry entry : entries) {
            out.append("## ").append(md(title(entry))).append("\n\n");
            out.append("- Tipo: ").append(md(label(entry.type))).append("\n");
            if (!entry.category.isEmpty()) out.append("- Categoría: ").append(md(entry.category)).append("\n");
            if (!entry.source.isEmpty()) out.append("- Fuente: ").append(md(entry.source)).append("\n");
            if (!entry.reference.isEmpty()) out.append("- Referencia: ").append(md(entry.reference)).append("\n");
            if (!entry.contentId.isEmpty()) out.append("- ID: `").append(code(entry.contentId)).append("`\n");
            if (!entry.tags.isEmpty()) {
                out.append("- Etiquetas: ");
                for (int i = 0; i < entry.tags.size(); i++) {
                    if (i > 0) out.append(", ");
                    out.append('`').append(code(entry.tags.get(i))).append('`');
                }
                out.append("\n");
            }
            out.append("\n");
            if (!entry.quote.isEmpty()) {
                for (String line : entry.quote.split("\\r?\\n")) {
                    out.append("> ").append(line).append("\n");
                }
                out.append("\n");
            }
            if (!entry.body.isEmpty()) out.append(entry.body.trim()).append("\n\n");
            out.append("<!-- ministerium-anchor: ")
                    .append(code(entry.semanticUnitId)).append('|')
                    .append(entry.startOffset).append('-').append(entry.endOffset)
                    .append("; v=").append(entry.anchorVersion).append(" -->\n\n---\n\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String title(StudyEntry entry) {
        if (entry.title != null && !entry.title.trim().isEmpty()) return entry.title.trim();
        if (entry.reference != null && !entry.reference.trim().isEmpty()) return entry.reference.trim();
        return label(entry.type);
    }

    private static String label(String type) {
        if (StudyEntry.HIGHLIGHT.equals(type)) return "Subrayado";
        if (StudyEntry.NOTE.equals(type)) return "Nota";
        return "Reflexión";
    }

    private static String md(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("*", "\\*").replace("_", "\\_").replace("#", "\\#");
    }

    private static String code(String value) {
        return value == null ? "" : value.replace("`", "'").replace("\n", " ").replace("\r", " ");
    }
}
