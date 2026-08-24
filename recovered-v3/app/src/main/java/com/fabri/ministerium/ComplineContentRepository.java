package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Carga y resuelve el paquete semántico de Completas. */
public final class ComplineContentRepository {
    private static final String ASSET = "semantic/compline-es.json";
    private static JSONObject cached;

    private ComplineContentRepository() {}

    public static synchronized JSONObject load(Context context) throws Exception {
        if (cached != null) return new JSONObject(cached.toString());
        try (InputStream input = context.getAssets().open(ASSET);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            cached = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            return new JSONObject(cached.toString());
        }
    }

    public static JSONObject formForDay(JSONObject data, int calendarDayOfWeek) {
        JSONArray forms = data.optJSONArray("forms");
        if (forms == null) return null;
        for (int i = 0; i < forms.length(); i++) {
            JSONObject form = forms.optJSONObject(i);
            if (form != null && form.optInt("calendarDayOfWeek", -1) == calendarDayOfWeek) {
                return form;
            }
        }
        return null;
    }

    public static JSONArray penitentialFormulas(JSONObject data) {
        return array(data.optJSONArray("penitentialFormulas"));
    }

    /**
     * Devuelve los himnos propios del tiempo cuando el tomo local los contiene.
     * Adviento/Navidad reutilizan la colección que esos mismos tomos enlazan.
     * El paquete pascual actual no contiene Completas; hasta incorporar una fuente
     * española verificada conserva el conjunto base en vez de inventar textos.
     */
    public static JSONArray hymnsForSeason(JSONObject data, String season) {
        JSONObject sets = data.optJSONObject("hymnSets");
        if (sets == null) return new JSONArray();
        String key = normalizeSeason(season);
        JSONObject selected = sets.optJSONObject(key);
        if (selected == null) selected = sets.optJSONObject("ordinary");
        return resolveItems(sets, selected, 0);
    }

    private static JSONArray resolveItems(JSONObject sets, JSONObject selected, int depth) {
        if (selected == null || depth > 4) return new JSONArray();
        JSONArray items = selected.optJSONArray("items");
        if (items != null) return array(items);
        String reuse = selected.optString("reuse", "");
        return reuse.isEmpty() ? new JSONArray()
                : resolveItems(sets, sets.optJSONObject(reuse), depth + 1);
    }

    public static JSONArray marianAntiphons(JSONObject data, boolean easterSeason) {
        JSONArray result = array(data.optJSONArray("marianAntiphons"));
        if (easterSeason) {
            JSONObject easter = data.optJSONObject("easterMarianAntiphon");
            if (easter != null) result.put(easter);
        }
        return result;
    }

    public static String examinationRubric(JSONObject data) {
        return data.optString("examinationRubric", "");
    }

    public static JSONObject invocation(JSONObject data) {
        JSONObject result = data.optJSONObject("invocation");
        return result == null ? new JSONObject() : result;
    }

    public static String penitentialConclusion(JSONObject data) {
        return data.optString("penitentialConclusion", "");
    }

    public static String normalizeSeason(String value) {
        if (value == null) return "ordinary";
        String season = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("advent".equals(season) || "christmas".equals(season)
                || "lent".equals(season) || "easter".equals(season)) return season;
        return "ordinary";
    }

    private static JSONArray array(JSONArray source) {
        JSONArray result = new JSONArray();
        if (source == null) return result;
        for (int i = 0; i < source.length(); i++) result.put(source.opt(i));
        return result;
    }
}
