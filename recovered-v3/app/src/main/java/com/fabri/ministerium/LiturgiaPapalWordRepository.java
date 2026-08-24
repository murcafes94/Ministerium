package com.fabri.ministerium;

import java.text.Normalizer;
import java.util.Locale;

/** Extracts semantic blocks from Liturgia Papal Mexico's Liturgia de la Palabra PDF package. */
public final class LiturgiaPapalWordRepository {
    private LiturgiaPapalWordRepository() {}

    public static String niceneCreedHtml(android.content.Context context) throws Exception {
        String text = LiturgiaPapalMissalRepository.component(context, "es", "word");
        return render(extract(text,
                "Creo en un solo Dios",
                "Para utilidad de los fieles"));
    }

    public static String apostlesCreedHtml(android.content.Context context) throws Exception {
        String text = LiturgiaPapalMissalRepository.component(context, "es", "word");
        return render(extract(text,
                "Creo en Dios, Padre todopoderoso",
                "Después se hace la plegaria universal"));
    }

    private static String extract(String text, String startMarker, String endMarker) {
        String[] lines = text.split("\\r?\\n");
        int start = find(lines, startMarker, 0);
        if (start < 0) return "";
        int end = find(lines, endMarker, start + 1);
        if (end < 0) end = lines.length;
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) out.append(lines[i]).append('\n');
        return out.toString().trim();
    }

    private static int find(String[] lines, String marker, int from) {
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wanted) || value.startsWith(wanted)) return i;
        }
        return -1;
    }

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder("<div class=\"liturgia-papal creed-text\" data-missal-source=\"liturgia-papal-mexico\">");
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
