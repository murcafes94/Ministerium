package com.fabri.ministerium;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class ReflectionStore {
    private static final String PREFS = "personal_reflections";
    private static final String KEY = "entries";
    private ReflectionStore() {}
    public static List<ReflectionEntry> all(Context context) {
        List<ReflectionEntry> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            for (int i = array.length() - 1; i >= 0; i--) result.add(ReflectionEntry.fromJson(array.getJSONObject(i)));
        } catch (Exception ignored) {}
        return result;
    }
    public static List<ReflectionEntry> forSource(Context context, String sourceKey) {
        List<ReflectionEntry> result = new ArrayList<>();
        for (ReflectionEntry entry : all(context)) if (sourceKey.equals(entry.sourceKey)) result.add(entry);
        return result;
    }
    public static void save(Context context, ReflectionEntry entry) {
        List<ReflectionEntry> entries = all(context); java.util.Collections.reverse(entries); entries.add(entry); write(context, entries);
    }
    public static void delete(Context context, String id) {
        List<ReflectionEntry> entries = all(context); java.util.Collections.reverse(entries);
        for (int i = entries.size() - 1; i >= 0; i--) if (id.equals(entries.get(i).id)) entries.remove(i);
        write(context, entries);
    }
    private static void write(Context context, List<ReflectionEntry> entries) {
        JSONArray array = new JSONArray();
        try { for (ReflectionEntry entry : entries) array.put(entry.toJson()); } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply();
    }
}
