package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Carga el paquete semántico de Completas separado del código Java. */
public final class ComplineContentRepository {
    private static final String ASSET = "semantic/compline-es.json";
    private static JSONObject cached;

    private ComplineContentRepository() {}

    public static synchronized JSONObject load(Context context) throws Exception {
        if (cached != null) return new JSONObject(cached.toString());
        try (InputStream input = context.getAssets().open(ASSET);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            cached = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            return new JSONObject(cached.toString());
        }
    }

    public static JSONArray penitentialFormulas(JSONObject data) {
        return data.optJSONArray("penitentialFormulas") == null
                ? new JSONArray() : data.optJSONArray("penitentialFormulas");
    }

    public static JSONArray hymns(JSONObject data) {
        return data.optJSONArray("hymns") == null ? new JSONArray() : data.optJSONArray("hymns");
    }

    public static JSONArray marianAntiphons(JSONObject data, boolean easterSeason) {
        JSONArray source = data.optJSONArray("marianAntiphons");
        JSONArray result = new JSONArray();
        if (source != null) {
            for (int i = 0; i < source.length(); i++) result.put(source.optJSONObject(i));
        }
        if (easterSeason) {
            JSONObject easter = data.optJSONObject("easterMarianAntiphon");
            if (easter != null) result.put(easter);
        }
        return result;
    }

    public static String conclusion(JSONObject data) {
        return data.optString("conclusion", "");
    }

    public static String penitentialConclusion(JSONObject data) {
        return data.optString("penitentialConclusion", "");
    }
}
