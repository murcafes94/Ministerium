package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;

/**
 * Structural catalog for the parts of the Roman Missal that are still being
 * normalized from Liturgia Papal sources.
 *
 * Source policy:
 * - Ecuador calendar decides which celebration occurs.
 * - Liturgia Papal Mexico remains the preferred source of liturgical text.
 * - Liturgia Papal's Propio/annual calendar is the primary structural check.
 * - Curas.com.ar is used only as a secondary structural map (common/form type).
 * - Ciudad Redonda may be used as a secondary date/readings check.
 *
 * This catalog contains metadata only. It never imports prayers from the
 * Argentine edition or pastoral texts from Ciudad Redonda.
 */
public final class MissalReferenceCatalog {
    private static final String ASSET = "missal-reference-catalog.json";

    public static final class SaintReference {
        public final String title;
        public final String rank;
        public final String color;
        public final boolean gloria;
        public final boolean creed;
        public final String preface;
        public final String solemnBlessing;
        public final String readings;
        public final JSONArray commons;

        SaintReference(JSONObject item) {
            title = item.optString("title", "");
            rank = item.optString("rank", "");
            color = item.optString("color", "");
            gloria = item.optBoolean("gloria", false);
            creed = item.optBoolean("creed", false);
            preface = item.optString("preface", "");
            solemnBlessing = item.optString("solemnBlessing", "");
            readings = item.optString("readings", "");
            commons = item.optJSONArray("commons") == null ? new JSONArray() : item.optJSONArray("commons");
        }
    }

    private MissalReferenceCatalog() {}

    public static SaintReference findSaint(Context context, Calendar date, String celebration) {
        if (context == null || date == null) return null;
        try {
            JSONArray saints = load(context).optJSONArray("saints");
            if (saints == null) return null;
            String wanted = normalize(celebration);
            SaintReference sameDate = null;
            for (int i = 0; i < saints.length(); i++) {
                JSONObject item = saints.optJSONObject(i);
                if (item == null) continue;
                if (item.optInt("month") != date.get(Calendar.MONTH) + 1
                        || item.optInt("day") != date.get(Calendar.DAY_OF_MONTH)) continue;
                SaintReference candidate = new SaintReference(item);
                if (sameDate == null) sameDate = candidate;
                String title = normalize(candidate.title);
                if (!wanted.isEmpty() && (title.contains(wanted) || wanted.contains(title)
                        || overlap(title, wanted))) return candidate;
            }
            return sameDate;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String groupHtml(Context context, String key, Calendar date, LiturgicalDay day) {
        try {
            if ("saints".equals(key)) return saintsHtml(context, date, day);
            JSONObject groups = load(context).optJSONObject("groups");
            JSONArray sections = groups == null ? null : groups.optJSONArray(key);
            if (sections == null) return unavailable();
            StringBuilder html = new StringBuilder(12000);
            html.append(sourceNote());
            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.optJSONObject(i);
                if (section == null) continue;
                html.append("<section class=\"reference-group\"><h3>")
                        .append(escape(section.optString("title", ""))).append("</h3>");
                JSONArray items = section.optJSONArray("items");
                if (items != null) {
                    html.append("<div class=\"reference-items\">");
                    for (int j = 0; j < items.length(); j++) {
                        html.append("<div class=\"reference-item\"><span>")
                                .append(escape(items.optString(j))).append("</span>"
                                        + "<small>Formulario reconocido · texto litúrgico pendiente de paquete verificado</small></div>");
                    }
                    html.append("</div>");
                }
                html.append("</section>");
            }
            return html.toString();
        } catch (Exception ignored) {
            return unavailable();
        }
    }

    public static String dayHintHtml(Context context, Calendar date, String celebration) {
        SaintReference ref = findSaint(context, date, celebration);
        if (ref == null) return "";
        StringBuilder html = new StringBuilder("<aside class=\"reference-day-hint\"><strong>Estructura verificada</strong><br>");
        html.append(escape(ref.title));
        if (!ref.rank.isEmpty()) html.append(" · ").append(escape(ref.rank));
        if (!ref.color.isEmpty()) html.append(" · ").append(escape(capitalize(ref.color)));
        if (ref.gloria) html.append(" · Gloria");
        if (ref.creed) html.append(" · Credo");
        if (!ref.preface.isEmpty()) html.append("<br>Prefacio: ").append(escape(ref.preface));
        if (!ref.readings.isEmpty()) html.append(" · Lecturas ").append(escape(ref.readings));
        if (ref.commons.length() > 0) {
            html.append("<br>Común relacionado: ");
            for (int i = 0; i < ref.commons.length(); i++) {
                if (i > 0) html.append(" / ");
                html.append(escape(ref.commons.optString(i)));
            }
        }
        if (!ref.solemnBlessing.isEmpty()) {
            html.append("<br>Bendición solemne opcional: ").append(escape(ref.solemnBlessing));
        }
        return html.append("</aside>").toString();
    }

    private static String saintsHtml(Context context, Calendar date, LiturgicalDay day) throws Exception {
        JSONObject root = load(context);
        JSONArray saints = root.optJSONArray("saints");
        StringBuilder html = new StringBuilder(10000);
        html.append(sourceNote());
        html.append("<section class=\"reference-group\"><h3>Propio de los santos · ")
                .append(monthName(date == null ? Calendar.getInstance() : date)).append("</h3>");
        if (day != null && date != null) {
            String hint = dayHintHtml(context, date, day.celebration);
            if (!hint.isEmpty()) html.append(hint);
        }
        int month = date == null ? Calendar.getInstance().get(Calendar.MONTH) + 1
                : date.get(Calendar.MONTH) + 1;
        int count = 0;
        if (saints != null) {
            for (int i = 0; i < saints.length(); i++) {
                JSONObject item = saints.optJSONObject(i);
                if (item == null || item.optInt("month") != month) continue;
                count++;
                SaintReference ref = new SaintReference(item);
                html.append("<div class=\"reference-item saint-reference\"><span><b>")
                        .append(item.optInt("day")).append(" · ").append(escape(ref.title))
                        .append("</b></span><small>").append(escape(ref.rank));
                if (ref.gloria) html.append(" · Gloria");
                if (ref.creed) html.append(" · Credo");
                if (!ref.preface.isEmpty()) html.append(" · ").append(escape(ref.preface));
                html.append("</small></div>");
            }
        }
        if (count == 0) {
            html.append("<div class=\"pending\">El índice estructural de este mes todavía no se ha refrescado. "
                    + "El calendario local de Ecuador sigue determinando la celebración.</div>");
        }
        return html.append("</section>").toString();
    }

    private static String sourceNote() {
        return "<div class=\"reference-source\"><b>Catálogo estructural</b><br>"
                + "Calendario de Ecuador + Liturgia Papal como prioridad. Curas.com.ar se usa solo "
                + "para contrastar la estructura de Comunes y formularios; no se copian sus oraciones. "
                + "Ciudad Redonda queda como comprobación secundaria de calendario/lecturas.</div>";
    }

    private static String unavailable() {
        return "<div class=\"pending\">No se pudo abrir el catálogo estructural local.</div>";
    }

    private static JSONObject load(Context context) throws Exception {
        try (InputStream input = context.getAssets().open(ASSET);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static boolean overlap(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        String[] tokens = a.length() < b.length() ? a.split(" ") : b.split(" ");
        int meaningful = 0;
        int matches = 0;
        String target = a.length() < b.length() ? b : a;
        for (String token : tokens) {
            if (token.length() < 4 || "santo".equals(token) || "santa".equals(token)) continue;
            meaningful++;
            if (target.contains(token)) matches++;
        }
        return meaningful > 0 && matches * 2 >= meaningful;
    }

    private static String monthName(Calendar date) {
        return new java.text.SimpleDateFormat("MMMM", new Locale("es", "EC"))
                .format(date.getTime());
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
