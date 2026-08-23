package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Copia portátil, íntegra y restaurable de los datos creados por la persona. */
public final class BackupManager {
    private static final int SCHEMA = 1;
    private static final String STUDY_FILE = "ministerium-study-v3.json";
    private static final String[] PREFERENCES = {
            "personal_prayers", "reader_settings", "reader_tts",
            "continue_reading_v3", "ministerium_preferences", "bible_reading_plan",
            "gospel_reminder", "study_editor_drafts", "prayer_intentions",
            "ministerium_favorites", "personal_reflections", "prayer_reminder",
            "liturgy_settings_v3", "reading_markers", "bible_history",
            "bible_plan_reminder", "ministerium_study_migration", "personal_settings_v3"
    };

    private BackupManager() {}

    public static byte[] create(Context context) throws Exception {
        JSONObject payload = new JSONObject();
        JSONObject preferences = new JSONObject();
        for (String name : PREFERENCES) {
            preferences.put(name, encode(context.getSharedPreferences(
                    name, Context.MODE_PRIVATE)));
        }
        payload.put("preferences", preferences);
        JSONObject files = new JSONObject();
        File study = new File(context.getFilesDir(), STUDY_FILE);
        if (study.isFile()) files.put(STUDY_FILE, read(study));
        payload.put("files", files);

        String rawPayload = payload.toString();
        JSONObject document = new JSONObject();
        document.put("format", "ministerium-backup");
        document.put("schema", SCHEMA);
        document.put("createdAt", System.currentTimeMillis());
        document.put("appVersion", BuildConfig.VERSION_NAME);
        document.put("sha256", sha256(rawPayload.getBytes(StandardCharsets.UTF_8)));
        document.put("payload", rawPayload);
        return document.toString(2).getBytes(StandardCharsets.UTF_8);
    }

    public static void restore(Context context, byte[] bytes) throws Exception {
        JSONObject document = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!"ministerium-backup".equals(document.optString("format"))
                || document.optInt("schema", -1) != SCHEMA) {
            throw new IllegalArgumentException("El archivo no es una copia compatible de Ministerium.");
        }
        String rawPayload = document.getString("payload");
        String expected = document.getString("sha256");
        String actual = sha256(rawPayload.getBytes(StandardCharsets.UTF_8));
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IllegalArgumentException("La copia está incompleta o fue modificada.");
        }
        JSONObject payload = new JSONObject(rawPayload);
        JSONObject preferences = payload.getJSONObject("preferences");
        JSONObject files = payload.optJSONObject("files");

        // Se valida todo antes de reemplazar la información local.
        for (String name : PREFERENCES) {
            JSONObject values = preferences.optJSONObject(name);
            if (values != null) validate(values);
        }
        String study = files == null ? null : files.optString(STUDY_FILE, null);
        if (study != null) new JSONArray(study);

        for (String name : PREFERENCES) {
            JSONObject values = preferences.optJSONObject(name);
            if (values != null) decode(context.getSharedPreferences(
                    name, Context.MODE_PRIVATE), values);
        }
        if (study != null) writeAtomically(new File(context.getFilesDir(), STUDY_FILE), study);
        PrayerReminderScheduler.restore(context);
        GospelReminderScheduler.restore(context);
        BiblePlanReminderScheduler.restore(context);
    }

    private static JSONObject encode(SharedPreferences preferences) throws Exception {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            Object value = entry.getValue();
            JSONObject encoded = new JSONObject();
            if (value instanceof String) {
                encoded.put("type", "string").put("value", value);
            } else if (value instanceof Boolean) {
                encoded.put("type", "boolean").put("value", value);
            } else if (value instanceof Integer) {
                encoded.put("type", "int").put("value", value);
            } else if (value instanceof Long) {
                encoded.put("type", "long").put("value", value);
            } else if (value instanceof Float) {
                encoded.put("type", "float").put("value", ((Float) value).doubleValue());
            } else if (value instanceof Set) {
                JSONArray items = new JSONArray();
                for (Object item : (Set<?>) value) items.put(String.valueOf(item));
                encoded.put("type", "strings").put("value", items);
            } else {
                continue;
            }
            result.put(entry.getKey(), encoded);
        }
        return result;
    }

    private static void validate(JSONObject values) throws Exception {
        JSONArray names = values.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            JSONObject encoded = values.getJSONObject(names.getString(i));
            String type = encoded.getString("type");
            if (!("string".equals(type) || "boolean".equals(type) || "int".equals(type)
                    || "long".equals(type) || "float".equals(type)
                    || "strings".equals(type))) {
                throw new IllegalArgumentException("La copia contiene un dato desconocido.");
            }
            if ("strings".equals(type)) encoded.getJSONArray("value");
            else encoded.get("value");
        }
    }

    private static void decode(SharedPreferences preferences, JSONObject values)
            throws Exception {
        SharedPreferences.Editor editor = preferences.edit().clear();
        JSONArray names = values.names();
        if (names != null) for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            JSONObject encoded = values.getJSONObject(key);
            String type = encoded.getString("type");
            if ("string".equals(type)) editor.putString(key, encoded.getString("value"));
            else if ("boolean".equals(type)) editor.putBoolean(key, encoded.getBoolean("value"));
            else if ("int".equals(type)) editor.putInt(key, encoded.getInt("value"));
            else if ("long".equals(type)) editor.putLong(key, encoded.getLong("value"));
            else if ("float".equals(type)) editor.putFloat(key,
                    (float) encoded.getDouble("value"));
            else if ("strings".equals(type)) {
                JSONArray array = encoded.getJSONArray("value");
                Set<String> set = new HashSet<>();
                for (int j = 0; j < array.length(); j++) set.add(array.getString(j));
                editor.putStringSet(key, set);
            }
        }
        if (!editor.commit()) throw new IllegalStateException("No se pudieron restaurar los datos.");
    }

    private static byte[] readBytes(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String read(File file) throws Exception {
        return new String(readBytes(file), StandardCharsets.UTF_8);
    }

    private static void writeAtomically(File target, String value) throws Exception {
        File temporary = new File(target.getParentFile(), target.getName() + ".restore");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (target.exists() && !target.delete()) {
            temporary.delete();
            throw new IllegalStateException("No se pudo reemplazar la copia local.");
        }
        if (!temporary.renameTo(target)) throw new IllegalStateException(
                "No se pudo terminar la restauración.");
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
