package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LiturgicalCalendarRepository {
    private static final String ASSET_2026 = "calendar/gcatholic-2026-es-EC.ics";
    private static final String DOWNLOAD =
            "https://gcatholic.org/calendar/ics/%d-es-EC.ics?v=3";
    private static final Pattern PSALTER = Pattern.compile("Psalter Week ([IVX]+)");
    private static final String[] MONTHS = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private static final Map<Integer, Map<String, List<LiturgicalEvent>>> CACHE =
            new HashMap<>();

    public interface UpdateCallback {
        void complete(boolean updated);
    }

    private LiturgicalCalendarRepository() {}

    public static List<LiturgicalEvent> eventsFor(Context context, Calendar date)
            throws IOException {
        List<LiturgicalEvent> events = data(context, date.get(Calendar.YEAR)).get(key(date));
        return events == null ? Collections.emptyList() : events;
    }

    public static String primaryCelebration(Context context, Calendar date) throws IOException {
        List<LiturgicalEvent> events = eventsFor(context, date);
        return events.isEmpty() ? "Feria del día" : events.get(0).summary;
    }

    public static String psalterWeek(Context context, Calendar date) throws IOException {
        for (LiturgicalEvent event : eventsFor(context, date)) {
            if (!event.psalterWeek.isEmpty()) return event.psalterWeek;
        }
        return "";
    }

    public static List<LiturgicalDateHit> search(Context context, String query, int maximum)
            throws IOException {
        String wanted = normalize(query);
        List<LiturgicalDateHit> results = new ArrayList<>();

        if ("hoy".equals(wanted)) {
            Calendar today = Calendar.getInstance();
            results.add(hitFor(context, today));
            return results;
        }

        int year = Calendar.getInstance().get(Calendar.YEAR);
        for (Map.Entry<String, List<LiturgicalEvent>> item : data(context, year).entrySet()) {
            Calendar date = fromKey(item.getKey());
            String label = dateLabel(date);
            StringBuilder all = new StringBuilder(label)
                    .append(' ').append(numericLabel(date))
                    .append(' ').append(date.get(Calendar.DAY_OF_MONTH))
                    .append(' ').append(MONTHS[date.get(Calendar.MONTH)])
                    .append(' ').append(String.format(Locale.US, "%02d/%02d/%04d",
                            date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH) + 1,
                            date.get(Calendar.YEAR)));
            for (LiturgicalEvent event : item.getValue()) {
                all.append(' ').append(event.summary);
            }
            if (normalize(all.toString()).contains(wanted)) {
                results.add(hitFor(date, item.getValue()));
                if (results.size() >= maximum) break;
            }
        }
        return results;
    }

    public static String dateLabel(Calendar date) {
        return date.get(Calendar.DAY_OF_MONTH) + " de "
                + MONTHS[date.get(Calendar.MONTH)] + " de " + date.get(Calendar.YEAR);
    }

    private static LiturgicalDateHit hitFor(Context context, Calendar date) throws IOException {
        return hitFor(date, eventsFor(context, date));
    }

    private static LiturgicalDateHit hitFor(Calendar date, List<LiturgicalEvent> events) {
        String celebration = events.isEmpty() ? "Liturgia del día" : events.get(0).summary;
        StringBuilder alternatives = new StringBuilder();
        for (int i = 1; i < events.size(); i++) {
            if (alternatives.length() > 0) alternatives.append(" · ");
            alternatives.append(events.get(i).summary);
        }
        String snippet = dateLabel(date) + (alternatives.length() == 0
                ? " · Calendario litúrgico de Ecuador"
                : " · También: " + alternatives);
        return new LiturgicalDateHit(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH), celebration, snippet);
    }

    public static boolean hasCalendar(Context context, int year) {
        return year == 2026 || calendarFile(context, year).isFile();
    }

    public static void ensureCurrentYear(Context context) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        if (hasCalendar(context, year)) return;
        updateYear(context, year, null);
    }

    public static void updateYear(Context context, int year, UpdateCallback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            boolean updated = false;
            File target = calendarFile(app, year);
            File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
            HttpURLConnection connection = null;
            try {
                URL url = new URL(String.format(Locale.US, DOWNLOAD, year));
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "Ministerium Android");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + connection.getResponseCode());
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Calendar directory unavailable");
                }
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                String head;
                try (InputStream input = new FileInputStream(temporary)) {
                    byte[] buffer = new byte[(int) Math.min(4096, temporary.length())];
                    int count = input.read(buffer);
                    head = count <= 0 ? "" : new String(buffer, 0, count, StandardCharsets.UTF_8);
                }
                if (!head.contains("BEGIN:VCALENDAR")) throw new IOException("Invalid calendar");
                if (target.exists() && !target.delete()) throw new IOException("Old calendar busy");
                if (!temporary.renameTo(target)) throw new IOException("Could not save calendar");
                synchronized (CACHE) { CACHE.remove(year); }
                data(app, year);
                updated = true;
            } catch (Exception ignored) {
                if (temporary.exists()) temporary.delete();
            } finally {
                if (connection != null) connection.disconnect();
            }
            boolean result = updated;
            if (callback != null) callback.complete(result);
        }, "ministerium-calendar-update").start();
    }

    private static Map<String, List<LiturgicalEvent>> data(Context context, int year)
            throws IOException {
        synchronized (LiturgicalCalendarRepository.class) {
            Map<String, List<LiturgicalEvent>> current = CACHE.get(year);
            if (current != null) return current;
            current = Collections.unmodifiableMap(readCalendar(context, year));
            CACHE.put(year, current);
            return current;
        }
    }

    private static Map<String, List<LiturgicalEvent>> readCalendar(Context context, int year)
            throws IOException {
        List<String> lines = new ArrayList<>();
        InputStream source;
        File stored = calendarFile(context, year);
        if (stored.isFile()) source = new FileInputStream(stored);
        else if (year == 2026) source = context.getAssets().open(ASSET_2026);
        else return new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line.startsWith(" ") || line.startsWith("\t")) && !lines.isEmpty()) {
                    int last = lines.size() - 1;
                    lines.set(last, lines.get(last) + line.substring(1));
                } else {
                    lines.add(line);
                }
            }
        }

        Map<String, List<LiturgicalEvent>> result = new LinkedHashMap<>();
        boolean inEvent = false;
        String date = "";
        String summary = "";
        String description = "";
        for (String line : lines) {
            if ("BEGIN:VEVENT".equals(line)) {
                inEvent = true;
                date = "";
                summary = "";
                description = "";
                continue;
            }
            if ("END:VEVENT".equals(line)) {
                if (inEvent && date.length() == 8 && !summary.isEmpty()) {
                    String rank = rank(summary);
                    String color = color(summary);
                    String cleanSummary = cleanSummary(summary);
                    Matcher matcher = PSALTER.matcher(unescape(description));
                    String psalter = matcher.find() ? matcher.group(1) : "";
                    LiturgicalEvent event = new LiturgicalEvent(
                            date, cleanSummary, rank, psalter, color);
                    result.computeIfAbsent(date, ignored -> new ArrayList<>()).add(event);
                }
                inEvent = false;
                continue;
            }
            if (!inEvent) continue;
            if (line.startsWith("DTSTART")) {
                int colon = line.indexOf(':');
                if (colon >= 0) date = line.substring(colon + 1).trim();
            } else if (line.startsWith("SUMMARY:")) {
                summary = unescape(line.substring("SUMMARY:".length()));
            } else if (line.startsWith("DESCRIPTION:")) {
                description = line.substring("DESCRIPTION:".length());
            }
        }
        for (Map.Entry<String, List<LiturgicalEvent>> item : result.entrySet()) {
            item.setValue(Collections.unmodifiableList(item.getValue()));
        }
        return result;
    }

    private static File calendarFile(Context context, int year) {
        return new File(new File(context.getFilesDir(), "calendar"),
                "gcatholic-" + year + "-es-EC.ics");
    }

    private static String rank(String summary) {
        int start = summary.indexOf('[');
        int end = summary.indexOf(']', start + 1);
        return start >= 0 && end > start ? summary.substring(start + 1, end).trim() : "";
    }

    private static String color(String summary) {
        if (summary.contains("🟢")) return "Verde";
        if (summary.contains("⚪")) return "Blanco";
        if (summary.contains("🔴")) return "Rojo";
        if (summary.contains("🟣")) return "Morado";
        return "";
    }

    private static String cleanSummary(String value) {
        String result = value.trim();
        while (!result.isEmpty() && !Character.isLetterOrDigit(result.charAt(0))
                && result.charAt(0) != '[') {
            result = result.substring(1).trim();
        }
        if (result.startsWith("[")) {
            int end = result.indexOf(']');
            if (end >= 0) result = result.substring(end + 1).trim();
        }
        return result;
    }

    private static String unescape(String value) {
        return value.replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private static String key(Calendar date) {
        return String.format(Locale.US, "%04d%02d%02d",
                date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar fromKey(String key) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(Integer.parseInt(key.substring(0, 4)),
                Integer.parseInt(key.substring(4, 6)) - 1,
                Integer.parseInt(key.substring(6, 8)), 12, 0, 0);
        return date;
    }

    private static String numericLabel(Calendar date) {
        return date.get(Calendar.DAY_OF_MONTH) + "/" + (date.get(Calendar.MONTH) + 1)
                + "/" + date.get(Calendar.YEAR);
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
