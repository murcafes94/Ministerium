package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads one internal EPUB file without extracting the whole book. */
public final class EpubEntryReader {
    private static final int MAX_MEMORY_ENTRIES = 16;
    private static final int MAX_DISK_ENTRY_BYTES = 1024 * 1024;
    private static final long DISK_MAX_AGE_MS = 45L * 24L * 60L * 60L * 1000L;
    private static final Map<String, String> CACHE = new LinkedHashMap<String, String>(12, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_MEMORY_ENTRIES;
        }
    };

    private EpubEntryReader() {}

    public static String read(Context context, HoursVolume volume, String filePath) throws Exception {
        String wanted = normalize(filePath);
        String key = versionedKey(context, volume.id + "|" + volume.assetPath + "|" + wanted);
        synchronized (CACHE) {
            String cached = CACHE.get(key);
            if (cached != null) return cached;
        }

        String disk = readDisk(context, key);
        if (disk != null) {
            synchronized (CACHE) { CACHE.put(key, disk); }
            return disk;
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
                writeDisk(context, key, value);
                return value;
            }
        }
        throw new IllegalStateException("No se encontró la entrada solicitada en el diccionario.");
    }

    private static String readDisk(Context context, String key) {
        File file = cacheFile(context, key);
        if (!file.isFile()) return null;
        if (System.currentTimeMillis() - file.lastModified() > DISK_MAX_AGE_MS) {
            try { file.delete(); } catch (Exception ignored) {}
            return null;
        }
        if (file.length() <= 0 || file.length() > MAX_DISK_ENTRY_BYTES) return null;
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length())) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeDisk(Context context, String key, String value) {
        if (value == null) return;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_DISK_ENTRY_BYTES) return;
        File file = cacheFile(context, key);
        File dir = file.getParentFile();
        if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) return;
        File temp = new File(dir, file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(bytes);
            output.flush();
            if (!temp.renameTo(file)) {
                try (FileOutputStream direct = new FileOutputStream(file)) { direct.write(bytes); }
                temp.delete();
            }
        } catch (Exception ignored) {
            try { temp.delete(); } catch (Exception alsoIgnored) {}
        }
    }

    private static File cacheFile(Context context, String key) {
        return new File(new File(context.getCacheDir(), "ministerium-epub-entry-v1"), sha256(key) + ".html");
    }

    private static String versionedKey(Context context, String key) {
        long version = 0;
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode; }
        catch (Exception ignored) {}
        return version + "|" + key;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String normalize(String value) {
        String result = value == null ? "" : value.replace('\\', '/');
        while (result.startsWith("./")) result = result.substring(2);
        while (result.startsWith("/")) result = result.substring(1);
        return result;
    }
}
