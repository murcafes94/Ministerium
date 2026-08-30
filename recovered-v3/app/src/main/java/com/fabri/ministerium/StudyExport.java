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
        for (StudyEntry entry : entries) appendEntry(out, entry, false);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Markdown preparado para guardarse directamente dentro de un vault de Obsidian.
     * No depende de Obsidian ni de plugins: usa frontmatter, tags y wikilinks estándar.
     */
    public static byte[] obsidian(Context context) {
        List<StudyEntry> entries = StudyStore.all(context);
        String exported = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .format(new Date());
        StringBuilder out = new StringBuilder(12288);
        out.append("---\n")
                .append("title: \"Mi estudio — Ministerium\"\n")
                .append("source: Ministerium\n")
                .append("app_version: \"").append(yaml(BuildConfig.VERSION_NAME)).append("\"\n")
                .append("exported_at: \"").append(yaml(exported)).append("\"\n")
                .append("tags:\n  - ministerium\n  - estudio\n---\n\n")
                .append("# Mi estudio — Ministerium\n\n")
                .append("> Exportación compatible con Obsidian. Los IDs de contenido de Ministerium se conservan para poder volver a identificar cada anotación.\n\n");

        if (entries.isEmpty()) {
            out.append("_No hay anotaciones guardadas._\n");
            return out.toString().getBytes(StandardCharsets.UTF_8);
        }

        String lastSource = "";
        for (StudyEntry entry : entries) {
            String source = entry.source == null ? "" : entry.source.trim();
            if (!source.isEmpty() && !source.equals(lastSource)) {
                out.append("## [[").append(wikilink(source)).append("]]\n\n");
                lastSource = source;
            }
            appendEntry(out, entry, true);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendEntry(StringBuilder out, StudyEntry entry, boolean obsidian) {
        out.append(obsidian ? "### " : "## ").append(md(title(entry))).append("\n\n");
        out.append("- Tipo: ").append(md(label(entry.type))).append("\n");
        if (!entry.category.isEmpty()) out.append("- Categoría: ").append(md(entry.category)).append("\n");
        if (!entry.source.isEmpty()) out.append("- Fuente: ").append(md(entry.source)).append("\n");
        if (!entry.reference.isEmpty()) out.append("- Referencia: ").append(md(entry.reference)).append("\n");
        if (!entry.contentId.isEmpty()) out.append("- ID: `").append(code(entry.contentId)).append("`\n");
        if (!entry.tags.isEmpty()) {
            out.append("- Etiquetas: ");
            for (int i = 0; i < entry.tags.size(); i++) {
                if (i > 0) out.append(", ");
                if (obsidian) out.append('#').append(obsidianTag(entry.tags.get(i)));
                else out.append('`').append(code(entry.tags.get(i))).append('`');
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

    private static String yaml(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
    }

    private static String wikilink(String value) {
        return value == null ? "" : value.replace("[[", "").replace("]]", "")
                .replace("|", "-").replace("\r", " ").replace("\n", " ").trim();
    }

    private static String obsidianTag(String value) {
        if (value == null) return "ministerium";
        String tag = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}_/-]+", "-")
                .replaceAll("^-+|-+$", "");
        return tag.isEmpty() ? "ministerium" : tag;
    }
}
