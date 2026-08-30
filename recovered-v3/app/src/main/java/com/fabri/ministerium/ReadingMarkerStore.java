package com.fabri.ministerium;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReadingMarkerStore {
    private static final String PREFS = "reading_markers";
    private static final String KEY = "entries";
    private ReadingMarkerStore() {}

    public static List<ReadingMarker> all(Context context) {
        List<ReadingMarker> result = chronological(context);
        Collections.reverse(result);
        return result;
    }

    public static List<ReadingMarker> forSource(Context context, String sourceKey) {
        List<ReadingMarker> result = new ArrayList<>();
        for (ReadingMarker marker : all(context)) {
            if (sourceKey.equals(marker.sourceKey)) result.add(marker);
        }
        return result;
    }

    public static void save(Context context, ReadingMarker marker) {
        List<ReadingMarker> entries = chronological(context);
        entries.add(marker);
        write(context, entries);
    }

    public static void delete(Context context, String id) {
        List<ReadingMarker> entries = chronological(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (id.equals(entries.get(i).id)) entries.remove(i);
        }
        write(context, entries);
    }

    private static List<ReadingMarker> chronological(Context context) {
        List<ReadingMarker> result = new ArrayList<>();
        try {
            String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY, "[]");
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                result.add(ReadingMarker.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static void write(Context context, List<ReadingMarker> entries) {
        JSONArray array = new JSONArray();
        try {
            for (ReadingMarker entry : entries) array.put(entry.toJson());
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, array.toString()).apply();
    }
}
