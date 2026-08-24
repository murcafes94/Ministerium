package com.fabri.ministerium;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Conserva una posición reciente por módulo y una entrada global para Inicio. */
public final class ContinueReadingStore {
    private static final String PREFS = "continue_reading_v3";
    private static final String LATEST = "latest";

    public static final class Position {
        public final String module;
        public final String title;
        public final String activity;
        public final JSONObject extras;
        public final int scrollY;

        Position(String module, String title, String activity,
                 JSONObject extras, int scrollY) {
            this.module = module;
            this.title = title;
            this.activity = activity;
            this.extras = extras;
            this.scrollY = scrollY;
        }
    }

    private ContinueReadingStore() {}

    public static void save(Context context, String module, String title,
                            Class<?> activity, JSONObject extras, int scrollY) {
        if (module == null || module.trim().isEmpty() || activity == null) return;
        try {
            JSONObject value = new JSONObject().put("module", module)
                    .put("title", title == null ? "" : title)
                    .put("activity", activity.getName())
                    .put("extras", extras == null ? new JSONObject() : extras)
                    .put("scrollY", Math.max(0, scrollY))
                    .put("updatedAt", System.currentTimeMillis());
            values(context).edit().putString("module_" + module, value.toString())
                    .putString(LATEST, value.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static Position latest(Context context) {
        return parse(values(context).getString(LATEST, ""));
    }

    public static Position forModule(Context context, String module) {
        return parse(values(context).getString("module_" + module, ""));
    }

    public static boolean open(Context context, Position position) {
        if (position == null) return false;
        try {
            Class<?> activity = Class.forName(position.activity);
            Intent intent = new Intent(context, activity);

            java.util.Iterator<String> iterator = position.extras.keys();
            java.util.List<String> nameList = new java.util.ArrayList<>();
            while (iterator.hasNext()) {
                nameList.add(iterator.next());
            }
            String[] names = nameList.toArray(new String[0]);

            for (String name : names) {
                Object value = position.extras.get(name);
                if (value instanceof Integer) intent.putExtra(name, (Integer) value);
                else if (value instanceof Boolean) intent.putExtra(name, (Boolean) value);
                else if (value instanceof Long) intent.putExtra(name, (Long) value);
                else intent.putExtra(name, String.valueOf(value));
            }
            intent.putExtra("restore_scroll_y", position.scrollY);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Position parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            JSONObject value = new JSONObject(raw);
            return new Position(value.optString("module"), value.optString("title"),
                    value.optString("activity"), value.optJSONObject("extras") == null
                    ? new JSONObject() : value.optJSONObject("extras"),
                    value.optInt("scrollY"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
