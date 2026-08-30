package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CanonTextRepository {
    public static final class Entry {
        public final int canon;
        public final String reform;
        public final String date;
        public final String source;
        public final boolean spanishFromVatican;

        Entry(int canon, JSONObject value) {
            this.canon = canon;
            reform = value.optString("reform");
            date = value.optString("date");
            source = value.optString("source");
            spanishFromVatican = "vatican".equals(value.optString("spanish_status"));
        }
    }

    private static volatile Map<Integer, Entry> cache;

    private CanonTextRepository() {}

    public static Entry find(Context context, int canon) {
        try { return entries(context).get(canon); }
        catch (Exception ignored) { return null; }
    }

    private static Map<Integer, Entry> entries(Context context) throws Exception {
        if (cache != null) return cache;
        synchronized (CanonTextRepository.class) {
            if (cache != null) return cache;
            JSONObject root = new JSONObject(read(context));
            JSONObject canons = root.getJSONObject("canons");
            Map<Integer, Entry> loaded = new HashMap<>();
            Iterator<String> keys = canons.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int canon = Integer.parseInt(key);
                loaded.put(canon, new Entry(canon, canons.getJSONObject(key)));
            }
            cache = loaded;
            return loaded;
        }
    }

    private static String read(Context context) throws Exception {
        try (InputStream input = context.getAssets().open("canon-official-overrides.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
