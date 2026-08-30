package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CatechismLocator {
    public static final class Target {
        public final String filePath;
        public final String searchText;
        Target(String filePath, String searchText) {
            this.filePath = filePath;
            this.searchText = searchText;
        }
    }

    private CatechismLocator() {}

    public static Target find(Context context, int numeral) throws Exception {
        HoursVolume volume = HoursRepository.find("catechism");
        File root = EpubUtils.ensureExtracted(context, volume);
        List<File> files = new ArrayList<>();
        collect(root, files);
        Pattern marker = Pattern.compile("(?is)(?:>|\\s)" + numeral
                + "(?:\\.|&nbsp;|\\s|<)");
        for (File file : files) {
            String html = read(file);
            if (marker.matcher(html).find()) {
                String path = root.toURI().relativize(file.toURI()).getPath();
                return new Target(path, String.valueOf(numeral));
            }
        }
        return null;
    }

    private static void collect(File file, List<File> result) {
        if (file.isFile()) {
            String name = file.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(".html") || name.endsWith(".xhtml")
                    || name.endsWith(".htm")) result.add(file);
            return;
        }
        File[] children = file.listFiles();
        if (children != null) for (File child : children) collect(child, result);
    }

    private static String read(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
