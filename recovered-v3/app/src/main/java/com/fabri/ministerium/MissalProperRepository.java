package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MissalProperRepository {
    public enum Part {
        DAY("", ""),
        COLLECT(" Colecta", "Colecta"),
        OFFERINGS(" Ofrendas", "Ofrendas"),
        COMMUNION_ANTIPHON(" Ant Com", "AntifonaComunion"),
        AFTER_COMMUNION(" Des Com", "DespuesComunion");

        final String suffix;
        final String fallbackFragment;
        Part(String suffix, String fallbackFragment) {
            this.suffix = suffix;
            this.fallbackFragment = fallbackFragment;
        }
    }

    public static final class Target {
        public final String filePath;
        public final String fragment;
        public final String title;
        Target(String filePath, String fragment, String title) {
            this.filePath = filePath;
            this.fragment = fragment;
            this.title = title;
        }
    }

    private static final String[] MONTHS = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };
    private static final Pattern LINK = Pattern.compile(
            "<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private MissalProperRepository() {}

    public static Target resolve(Context context, Calendar date, String celebration,
                                 Part part) throws Exception {
        File root = EpubUtils.ensureExtracted(context, HoursRepository.ROMAN_MISSAL);
        String wantedName = MONTHS[date.get(Calendar.MONTH)] + part.suffix;
        File index = findFile(root, wantedName);
        if (index == null) return null;
        String html = read(index);
        String dayId = "e" + date.get(Calendar.DAY_OF_MONTH);
        int start = idPosition(html, dayId);
        if (start < 0) return null;
        int end = nextDay(html, start + dayId.length());
        if (end < 0) end = html.length();
        String section = html.substring(start, end);
        String wanted = normalize(celebration);
        Matcher matcher = LINK.matcher(section);
        String bestHref = "";
        String bestTitle = "";
        int bestScore = -1;
        while (matcher.find()) {
            String title = matcher.group(2).replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ").trim();
            String href = matcher.group(1).replace("&amp;", "&");
            if (href.startsWith("#") || normalize(title).equals("misa")) continue;
            int score = score(wanted, normalize(title));
            if (score > bestScore) {
                bestScore = score;
                bestHref = href;
                bestTitle = title;
            }
        }
        if (bestHref.isEmpty() || bestScore < 1) return null;
        String[] parts = bestHref.split("#", 2);
        String file = parts[0].replace("%20", " ");
        String fragment = parts.length > 1 ? parts[1] : part.fallbackFragment;
        return new Target(file, fragment, bestTitle);
    }

    private static int score(String wanted, String candidate) {
        if (candidate.isEmpty()) return -1;
        if (wanted.contains(candidate) || candidate.contains(wanted)) return 100;
        int score = 0;
        for (String token : wanted.split(" ")) {
            if (token.length() >= 4 && !common(token) && candidate.contains(token)) score++;
        }
        return score;
    }

    private static boolean common(String token) {
        return "santo".equals(token) || "santa".equals(token) || "san".equals(token)
                || "papa".equals(token) || "virgen".equals(token)
                || "memoria".equals(token) || "fiesta".equals(token);
    }

    private static File findFile(File root, String wanted) {
        File[] files = root.listFiles();
        if (files == null) return null;
        String normalized = normalize(wanted);
        for (File file : files) {
            String name = file.getName().replaceFirst("(?i)\\.html?$", "");
            if (normalize(name).equals(normalized)) return file;
        }
        return null;
    }

    private static int idPosition(String html, String id) {
        int result = html.indexOf("id=\"" + id + "\"");
        if (result < 0) result = html.indexOf("id='" + id + "'");
        return result;
    }

    private static int nextDay(String html, int from) {
        Pattern pattern = Pattern.compile("id=[\"']e\\d{1,2}[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        return matcher.find(from) ? matcher.start() : -1;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String read(File file) throws Exception {
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
