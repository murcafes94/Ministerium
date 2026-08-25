package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Installs the build-generated Latin 2026 package; readiness is marked after validation. */
public final class CleanLatinHoursAssets {
    private static final String BASE = "hours-clean/latin/";

    private CleanLatinHoursAssets() {}

    public static boolean isAvailable(Context context, int year) {
        try (InputStream ignored = context.getAssets().open(BASE + year + "/manifest.json")) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static File install(Context context, int year, File targetRoot) throws Exception {
        String base = BASE + year + "/";
        if (new File(targetRoot, ".ready").isFile()) return targetRoot;
        deleteTree(targetRoot);
        if (!targetRoot.mkdirs() && !targetRoot.isDirectory()) {
            throw new IllegalStateException("No se pudo preparar la Liturgia Horarum limpia.");
        }
        String canonicalRoot = targetRoot.getCanonicalPath() + File.separator;
        try (InputStream input = context.getAssets().open(base + "files.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String relative = line.trim();
                if (relative.isEmpty()) continue;
                File target = new File(targetRoot, relative);
                if (!target.getCanonicalPath().startsWith(canonicalRoot)) {
                    throw new IllegalStateException("Ruta inválida en Liturgia Horarum limpia.");
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("No se pudo crear una carpeta de Liturgia Horarum.");
                }
                copy(context, base + relative, target);
            }
        }
        return targetRoot;
    }

    private static void copy(Context context, String asset, File target) throws Exception {
        try (InputStream input = context.getAssets().open(asset);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    private static void deleteTree(File file) throws Exception {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        if (!file.delete()) throw new IllegalStateException("No se pudo reemplazar el contenido latino anterior.");
    }
}
