package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IntentionsStore {
    private static final String PREFERENCES = "prayer_intentions";
    private static final String KEY_ITEMS = "items";

    private IntentionsStore() {}

    public static List<String> get(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        String raw = preferences.getString(KEY_ITEMS, "[]");
        List<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) result.add(value);
            }
        } catch (JSONException ignored) {
            // Si el almacenamiento se dañó, se muestra una lista vacía sin cerrar la app.
        }
        return Collections.unmodifiableList(result);
    }

    public static void save(Context context, List<String> intentions) {
        JSONArray array = new JSONArray();
        for (String intention : intentions) {
            String value = intention == null ? "" : intention.trim();
            if (!value.isEmpty()) array.put(value);
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(KEY_ITEMS, array.toString()).apply();
    }
}
