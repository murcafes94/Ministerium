package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime access to the build-generated, EPUB-free Liturgy of the Hours package. */
public final class CleanHoursAssets {
    private static final String BASE = "hours-clean/";
    private static final String VERSION_MARKER = ".ready-3.1.1-nav2";

    private CleanHoursAssets() {}

    public static boolean isAvailable(Context context, String volumeId) {
        if (volumeId == null || volumeId.isEmpty()) return false;
        try (InputStream ignored = context.getAssets().open(BASE + volumeId + "/manifest.json")) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static List<EpubTocEntry> tableOfContents(Context context, String volumeId)
            throws Exception {
        List<EpubTocEntry> result = new ArrayList<>();
        try (InputStream input = context.getAssets().open(BASE + volumeId + "/toc.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] parts = line.split("\\t", -1);
                if (parts.length < 4) continue;
                int depth;
                try { depth = Integer.parseInt(parts[3]); }
                catch (NumberFormatException ignored) { depth = 0; }
                result.add(new EpubTocEntry(parts[0], parts[1], parts[2], depth));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, NavigationTarget> navigation(Context context, String volumeId,
                                                            String sourcePath) throws Exception {
        Map<String, NavigationTarget> result = new LinkedHashMap<>();
        try (InputStream input = context.getAssets().open(BASE + volumeId + "/navigation.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] parts = line.split("\\t", -1);
                if (parts.length < 4 || !parts[0].equals(sourcePath)) continue;
                if (!result.containsKey(parts[1])) {
                    result.put(parts[1], new NavigationTarget(parts[2], parts[3]));
                }
            }
        }
        return result;
    }

    public static File ensureExtracted(Context context, String volumeId) throws Exception {
        File root = new File(context.getFilesDir(), "liturgy_hours_clean_31/" + volumeId);
        File marker = new File(root, VERSION_MARKER);
        if (marker.isFile()) return root;
        deleteTree(root);
        if (!root.mkdirs() && !root.isDirectory()) {
            throw new IllegalStateException("No se pudo preparar el texto limpio de la Liturgia de las Horas.");
        }
        copyListedFiles(context, volumeId, root);
        copySmallAsset(context, BASE + volumeId + "/toc.tsv", new File(root, "toc.tsv"));
        copySmallAsset(context, BASE + volumeId + "/navigation.tsv", new File(root, "navigation.tsv"));
        copySmallAsset(context, BASE + volumeId + "/manifest.json", new File(root, "manifest.json"));
        try (FileOutputStream output = new FileOutputStream(marker)) { output.write(1); }
        return root;
    }

    private static void copyListedFiles(Context context, String volumeId, File root) throws Exception {
        try (InputStream input = context.getAssets().open(BASE + volumeId + "/files.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            String rootPath = root.getCanonicalPath() + File.separator;
            while ((line = reader.readLine()) != null) {
                String relative = line.trim();
                if (relative.isEmpty()) continue;
                File target = new File(root, relative);
                if (!target.getCanonicalPath().startsWith(rootPath)) {
                    throw new IllegalStateException("Ruta no válida en el paquete de Horas.");
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("No se pudo crear la carpeta del texto litúrgico.");
                }
                copySmallAsset(context, BASE + volumeId + "/" + relative, target);
            }
        }
    }

    private static void copySmallAsset(Context context, String asset, File target) throws Exception {
        try (InputStream input = context.getAssets().open(asset);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public static final class NavigationTarget {
        public final String path;
        public final String fragment;
        NavigationTarget(String path, String fragment) {
            this.path = path;
            this.fragment = fragment;
        }
    }
}
