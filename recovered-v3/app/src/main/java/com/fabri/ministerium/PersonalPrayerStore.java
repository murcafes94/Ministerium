package com.fabri.ministerium;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class PersonalPrayerStore {
    private static final String PREFS = "personal_prayers", KEY = "entries";
    private PersonalPrayerStore() {}
    public static List<PersonalPrayer> all(Context context) {
        List<PersonalPrayer> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            for (int i = 0; i < values.length(); i++) result.add(PersonalPrayer.fromJson(values.getJSONObject(i)));
        } catch (Exception ignored) {}
        return result;
    }
    public static PersonalPrayer find(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (PersonalPrayer prayer : all(context)) {
            if (id.equals(prayer.id)) return prayer;
        }
        return null;
    }

    public static void save(Context context, PersonalPrayer prayer) {
        List<PersonalPrayer> values = all(context); boolean replaced = false;
        for (int i = 0; i < values.size(); i++) if (prayer.id.equals(values.get(i).id)) { values.set(i, prayer); replaced = true; break; }
        if (!replaced) values.add(0, prayer); write(context, values);
    }
    public static void delete(Context context, String id) {
        List<PersonalPrayer> values = all(context);
        for (int i = values.size() - 1; i >= 0; i--) if (id.equals(values.get(i).id)) values.remove(i);
        write(context, values);
    }
    private static void write(Context context, List<PersonalPrayer> values) {
        JSONArray array = new JSONArray();
        try { for (PersonalPrayer value : values) array.put(value.toJson()); } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply();
    }
}
