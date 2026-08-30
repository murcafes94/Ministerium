package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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

        Map<String, Target> links = links(context, office.volume, day.filePath);
        Target invitatory = first(links.get("IN"),
                targetFromToc(toc, office.tocIndex, "INVITATORIO"),
                new Target(day.filePath, day.fragment));
        Target officeTarget = first(links.get("O"),
                targetFromToc(toc, office.tocIndex, "OFICIO DE LECTURAS", "OFICIO"));
        Target laudsTarget = first(links.get("L"),
                targetFromToc(toc, office.tocIndex, "LAUDES"));
        Target middleTarget = first(links.get("M"),
                targetFromToc(toc, office.tocIndex, "HORA INTERMEDIA", "TERCIA", "SEXTA", "NONA"));

        add(result, "invitatory", "Invitatorio", "Comienzo de la oración del día",
                office.volume, invitatory, "INVITATORIO", false);
        add(result, "office", "Oficio de lecturas", "Lecturas y salmodia",
                office.volume, officeTarget, "", false);
        add(result, "lauds", "Laudes", "Oración de la mañana",
                office.volume, laudsTarget, "", true);
        add(result, "terce", "Tercia", "Oración de media mañana",
                office.volume, middleTarget, "Tercia", false);
        add(result, "sext", "Sexta", "Oración del mediodía",
                office.volume, middleTarget, "Sexta", false);
        add(result, "none", "Nona", "Oración de media tarde",
                office.volume, middleTarget, "Nona", false);

        Target vespers = links.containsKey("2V") ? links.get("2V") : links.get("V");
        vespers = first(vespers,
                targetFromToc(toc, office.tocIndex, "II VÍSPERAS", "VÍSPERAS II", "VÍSPERAS"));
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
        Map<String, Target> targets = links(context, sundayOffice.volume, day.filePath);
        return first(targets.containsKey("1V") ? targets.get("1V") : targets.get("V"),
                targetFromToc(toc, sundayOffice.tocIndex,
                        "I VÍSPERAS", "PRIMERAS VÍSPERAS", "VÍSPERAS I", "VÍSPERAS"));
    }

    private static Target first(Target... targets) {
        if (targets == null) return null;
        for (Target target : targets) if (target != null) return target;
        return null;
    }

    /**
     * Fallback over the already-clean TOC. It never reopens the source EPUB.
     * This keeps cards usable even when one editorial navigation link is absent.
     */
    private static Target targetFromToc(List<EpubTocEntry> toc, int dayIndex,
                                        String... labels) {
        if (toc == null || dayIndex < 0 || dayIndex >= toc.size()) return null;
        EpubTocEntry day = toc.get(dayIndex);
        int dayDepth = day.depth;
        for (int i = dayIndex + 1; i < toc.size(); i++) {
            EpubTocEntry entry = toc.get(i);
            if (entry.depth <= dayDepth) break;
            String title = normalize(entry.title);
            for (String label : labels) {
                String wanted = normalize(label);
                if (title.equals(wanted) || title.startsWith(wanted)
                        || wanted.startsWith(title)) {
                    return new Target(entry.filePath, entry.fragment);
                }
            }
        }
        return null;
    }

    private static void add(List<HourEntry> result, String key, String title,
                            String subtitle, HoursVolume volume, Target target,
                            String scrollText, boolean showIntentions) {
        if (target == null) return;
        result.add(new HourEntry(key, title, subtitle, volume, target.path,
                target.fragment, scrollText, showIntentions));
    }

    private static Map<String, Target> links(Context context, HoursVolume volume,
                                             String sourcePath) throws Exception {
        if (CleanHoursAssets.isAvailable(context, volume.id)) {
            Map<String, CleanHoursAssets.NavigationTarget> clean =
                    CleanHoursAssets.navigation(context, volume.id, sourcePath);
            Map<String, Target> result = new LinkedHashMap<>();
            for (Map.Entry<String, CleanHoursAssets.NavigationTarget> entry : clean.entrySet()) {
                CleanHoursAssets.NavigationTarget target = entry.getValue();
                result.put(entry.getKey(), new Target(target.path, target.fragment));
            }
            if (result.containsKey("1V") && !result.containsKey("V")) {
                result.put("V", result.get("1V"));
            }
            return result;
        }

        File root = EpubUtils.ensureExtracted(context, volume);
        File dayFile = new File(root, sourcePath);
        return legacyLinks(sourcePath, read(dayFile));
    }

    private static Map<String, Target> legacyLinks(String sourcePath, String html)
            throws Exception {
        Map<String, Target> result = new LinkedHashMap<>();
        Matcher matcher = ANCHOR.matcher(html);
        while (matcher.find()) {
            String label = matcher.group(2).replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ").trim().toUpperCase(Locale.ROOT)
                    .replace("[", "").replace("]", "").trim();
            if (!("O".equals(label) || "L".equals(label) || "M".equals(label)
                    || "V".equals(label) || "1V".equals(label)
                    || "2V".equals(label) || "IN".equals(label) || "C".equals(label))) {
                continue;
            }
            String href = matcher.group(1).trim();
            String[] parts = href.split("#", 2);
            URI base = new URI(null, null, sourcePath, null);
            String path = parts[0].isEmpty() ? sourcePath
                    : base.resolve(parts[0]).normalize().getPath();
            while (path.startsWith("/")) path = path.substring(1);
            String fragment = parts.length > 1 ? parts[1] : "";
            result.put(label, new Target(path, fragment));
            if ("1V".equals(label) && !result.containsKey("V")) {
                result.put("V", new Target(path, fragment));
            }
        }
        return result;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ").trim();
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
