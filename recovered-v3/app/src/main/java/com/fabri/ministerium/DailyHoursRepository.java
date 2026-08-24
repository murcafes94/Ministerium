package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DailyHoursRepository {
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private DailyHoursRepository() {}

    public static List<HourEntry> hoursFor(Context context, HoursLink office,
                                           Calendar selectedDate)
            throws Exception {
        List<HourEntry> result = new ArrayList<>();
        if (office == null) return result;

        List<EpubTocEntry> toc = EpubUtils.tableOfContents(context, office.volume);
        if (office.tocIndex < 0 || office.tocIndex >= toc.size()) return result;
        EpubTocEntry day = toc.get(office.tocIndex);

        File root = EpubUtils.ensureExtracted(context, office.volume);
        File dayFile = new File(root, day.filePath);
        String html = read(dayFile);
        Map<String, Target> links = links(day.filePath, html);

        result.add(new HourEntry("invitatory", "Invitatorio",
                "Comienzo de la oración del día", office.volume,
                day.filePath, day.fragment, "INVITATORIO", false));
        add(result, "office", "Oficio de lecturas", "Lecturas y salmodia",
                office.volume, links.get("O"), "", false);
        add(result, "lauds", "Laudes", "Oración de la mañana",
                office.volume, links.get("L"), "", true);
        add(result, "terce", "Tercia", "Oración de media mañana",
                office.volume, links.get("M"), "Tercia", false);
        add(result, "sext", "Sexta", "Oración del mediodía",
                office.volume, links.get("M"), "Sexta", false);
        add(result, "none", "Nona", "Oración de media tarde",
                office.volume, links.get("M"), "Nona", false);
        Target vespers = links.containsKey("2V") ? links.get("2V") : links.get("V");
        HoursVolume vespersVolume = office.volume;
        String vespersTitle = "Vísperas";
        String vespersSubtitle = "Oración de la tarde";
        if (selectedDate != null
                && selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            Calendar sunday = (Calendar) selectedDate.clone();
            sunday.add(Calendar.DATE, 1);
            LiturgicalDay sundayOffice = LiturgicalResolver.resolve(context, sunday);
            Target firstVespers = firstVespers(context, sundayOffice.temporalOffice);
            if (firstVespers != null) {
                vespers = firstVespers;
                vespersVolume = sundayOffice.temporalOffice.volume;
                vespersTitle = "Vísperas I";
                vespersSubtitle = "Primeras Vísperas del domingo";
            }
        }
        add(result, "vespers", vespersTitle, vespersSubtitle,
                vespersVolume, vespers, "", true);

        // Completas ya no se localiza dentro del EPUB. La tarjeta diaria funciona
        // aunque los archivos Co1…Co7 no estén empaquetados: el lector semántico
        // resuelve el formulario directamente por fecha.
        String complineSubtitle = "Oración antes del descanso";
        if (selectedDate != null
                && selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            complineSubtitle = "Después de las I Vísperas del domingo";
        } else if (selectedDate != null
                && selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            complineSubtitle = "Después de las II Vísperas del domingo";
        }
        result.add(new HourEntry("compline", "Completas", complineSubtitle,
                HoursRepository.find("ordinary"), "semantic/compline-es.json",
                "", "", false));
        return result;
    }

    private static Target firstVespers(Context context, HoursLink sundayOffice)
            throws Exception {
        if (sundayOffice == null) return null;
        List<EpubTocEntry> toc = EpubUtils.tableOfContents(context, sundayOffice.volume);
        if (sundayOffice.tocIndex < 0 || sundayOffice.tocIndex >= toc.size()) return null;
        EpubTocEntry day = toc.get(sundayOffice.tocIndex);
        File root = EpubUtils.ensureExtracted(context, sundayOffice.volume);
        Map<String, Target> targets = links(day.filePath,
                read(new File(root, day.filePath)));
        return targets.containsKey("1V") ? targets.get("1V") : targets.get("V");
    }

    private static void add(List<HourEntry> result, String key, String title,
                            String subtitle, HoursVolume volume, Target target,
                            String scrollText, boolean showIntentions) {
        if (target == null) return;
        result.add(new HourEntry(key, title, subtitle, volume, target.path,
                target.fragment, scrollText, showIntentions));
    }

    private static Map<String, Target> links(String sourcePath, String html)
            throws Exception {
        Map<String, Target> result = new LinkedHashMap<>();
        Matcher matcher = ANCHOR.matcher(html);
        while (matcher.find()) {
            String label = matcher.group(2).replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ").trim().toUpperCase(Locale.ROOT);
            if (!("O".equals(label) || "L".equals(label) || "M".equals(label)
                    || "V".equals(label) || "1V".equals(label)
                    || "2V".equals(label) || "C".equals(label))) {
                continue;
            }
            String href = matcher.group(1).trim();
            String[] parts = href.split("#", 2);
            URI base = new URI(null, null, sourcePath, null);
            String path = base.resolve(parts[0]).normalize().getPath();
            while (path.startsWith("/")) path = path.substring(1);
            String fragment = parts.length > 1 ? parts[1] : "";
            result.put(label, new Target(path, fragment));
            if ("1V".equals(label) && !result.containsKey("V")) {
                result.put("V", new Target(path, fragment));
            }
        }
        return result;
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

    private static final class Target {
        final String path;
        final String fragment;

        Target(String path, String fragment) {
            this.path = path;
            this.fragment = fragment;
        }
    }
}
