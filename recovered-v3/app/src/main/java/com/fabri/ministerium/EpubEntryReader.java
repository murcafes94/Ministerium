package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads one internal EPUB file without extracting the whole book. */
public final class EpubEntryReader {
    private static final Map<String, String> CACHE = new LinkedHashMap<String, String>(12, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 16;
        }
    };

    private EpubEntryReader() {}

    public static String read(Context context, HoursVolume volume, String filePath) throws Exception {
        String wanted = normalize(filePath);
        String key = volume.id + "|" + wanted;
        synchronized (CACHE) {
            String cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        try (InputStream input = context.getAssets().open(volume.assetPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !normalize(entry.getName()).equals(wanted)) continue;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int count;
                while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
                String value = new String(output.toByteArray(), StandardCharsets.UTF_8);
                synchronized (CACHE) { CACHE.put(key, value); }
                return value;
            }
        }
        throw new IllegalStateException("No se encontró la entrada solicitada en el diccionario.");
    }

    private static String normalize(String value) {
        String result = value == null ? "" : value.replace('\\', '/');
        while (result.startsWith("./")) result = result.substring(2);
        while (result.startsWith("/")) result = result.substring(1);
        return result;
    }
}
