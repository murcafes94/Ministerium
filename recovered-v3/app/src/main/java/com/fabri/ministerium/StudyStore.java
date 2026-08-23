package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Archivo personal local con escritura atómica y migración no destructiva de 2.3.2. */
public final class StudyStore {
    private static final String FILE = "ministerium-study-v3.json";
    private static final String PREFS = "ministerium_study_migration";
    private static final String MIGRATED = "legacy_2_3_2";

    private StudyStore() {}

    public static synchronized List<StudyEntry> all(Context context) {
        List<StudyEntry> entries = read(context);
        migrateLegacy(context, entries);
        Collections.sort(entries, (left, right) -> Long.compare(right.updatedAt, left.updatedAt));
        return entries;
    }

    public static List<StudyEntry> ofType(Context context, String type) {
        List<StudyEntry> result = new ArrayList<>();
        for (StudyEntry entry : all(context)) if (type.equals(entry.type)) result.add(entry);
        return result;
    }

    public static List<StudyEntry> forSource(Context context, String sourceKey) {
        List<StudyEntry> result = new ArrayList<>();
        for (StudyEntry entry : all(context)) {
            if (sourceKey != null && sourceKey.equals(entry.sourceKey)) result.add(entry);
        }
        return result;
    }

    public static synchronized void save(Context context, StudyEntry entry) {
        List<StudyEntry> entries = read(context);
        long now = System.currentTimeMillis();
        if (entry.id == null || entry.id.isEmpty()) entry.id = UUID.randomUUID().toString();
        if (entry.createdAt <= 0) entry.createdAt = now;
        entry.updatedAt = now;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entry.id.equals(entries.get(i).id)) entries.remove(i);
        }
        entries.add(entry);
        write(context, entries);
    }

    public static synchronized StudyEntry delete(Context context, String id) {
        List<StudyEntry> entries = read(context);
        StudyEntry deleted = null;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (id.equals(entries.get(i).id)) deleted = entries.remove(i);
        }
        write(context, entries);
        return deleted;
    }

    private static List<StudyEntry> read(Context context) {
        List<StudyEntry> result = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE);
        if (!file.isFile()) return result;
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONArray values = new JSONArray(new String(output.toByteArray(),
                    StandardCharsets.UTF_8));
            for (int i = 0; i < values.length(); i++) {
                result.add(StudyEntry.fromJson(values.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static void write(Context context, List<StudyEntry> entries) {
        JSONArray values = new JSONArray();
        try {
            for (StudyEntry entry : entries) values.put(entry.toJson());
            File file = new File(context.getFilesDir(), FILE);
            File temporary = new File(context.getFilesDir(), FILE + ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(values.toString().getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
            }
            if (file.exists() && !file.delete()) return;
            temporary.renameTo(file);
        } catch (Exception ignored) {}
    }

    private static void migrateLegacy(Context context, List<StudyEntry> target) {
        if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(MIGRATED, false)) return;
        for (ReadingMarker marker : ReadingMarkerStore.all(context)) {
            StudyEntry entry = new StudyEntry();
            entry.id = "marker:" + marker.id;
            entry.type = StudyEntry.HIGHLIGHT;
            entry.category = category(marker.source);
            entry.source = marker.source;
            entry.sourceKey = marker.sourceKey;
            entry.reference = marker.citation;
            entry.title = marker.citation;
            entry.quote = marker.quote;
            entry.color = "gold".equals(marker.color) ? "yellow" : marker.color;
            entry.createdAt = marker.createdAt > 0 ? marker.createdAt : System.currentTimeMillis();
            entry.updatedAt = entry.createdAt;
            target.add(entry);
        }
        for (ReflectionEntry reflection : ReflectionStore.all(context)) {
            StudyEntry entry = new StudyEntry();
            entry.id = "reflection:" + reflection.id;
            entry.type = StudyEntry.MEDITATION;
            entry.category = category(reflection.source);
            entry.source = reflection.source;
            entry.sourceKey = reflection.sourceKey;
            entry.title = reflection.title;
            entry.reference = reflection.subtitle;
            entry.quote = reflection.quote;
            entry.body = reflection.reflection;
            entry.createdAt = System.currentTimeMillis();
            entry.updatedAt = entry.createdAt;
            target.add(entry);
        }
        write(context, target);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(MIGRATED, true).apply();
    }

    private static String category(String source) {
        if (source == null) return "Documentos/libros";
        String value = source.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("biblia")) return "Biblia";
        if (value.contains("misa") || value.contains("lectura")) return "Lecturas del día";
        if (value.contains("liturgia") || value.contains("hora")) return "Liturgia";
        return "Documentos/libros";
    }
}
