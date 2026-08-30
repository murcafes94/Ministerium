package com.fabri.ministerium;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SaintOfficeRepository {
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRAYER_HEADING = Pattern.compile(
            "<p\\b[^>]*>\\s*(?:<span\\b[^>]*>)?\\s*Oraci[oó]n\\s*"
                    + "(?:</span>)?\\s*</p>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final CommonCandidate[] COMMONS = {
            new CommonCandidate("COMÚN DE LA SANTÍSIMA VIRGEN MARÍA",
                    "COMUN DE LA SANTISIMA VIRGEN MARIA",
                    "COMÚN DE LA SMA. VIRGEN MARÍA"),
            new CommonCandidate("COMÚN DE APÓSTOLES", "COMUN DE APOSTOLES",
                    "COMÚN DE APÓSTOLES"),
            new CommonCandidate("COMÚN DE VARIOS MÁRTIRES", "COMUN DE VARIOS MARTIRES",
                    "COMÚN DE VARIOS MÁRTIRES"),
            new CommonCandidate("COMÚN DE UN MÁRTIR", "COMUN DE UN MARTIR",
                    "COMÚN DE UN MÁRTIR"),
            new CommonCandidate("COMÚN DE PASTORES", "COMUN DE PASTORES",
                    "COMÚN DE PASTORES"),
            new CommonCandidate("COMÚN DE DOCTORES DE LA IGLESIA",
                    "COMUN DE DOCTORES DE LA IGLESIA",
                    "COMÚN DE DOCTORES DE LA IGLESIA"),
            new CommonCandidate("COMÚN DE VÍRGENES", "COMUN DE VIRGENES",
                    "COMÚN DE VÍRGENES"),
            new CommonCandidate("COMÚN DE SANTOS VARONES", "COMUN DE SANTOS VARONES",
                    "COMÚN DE SANTOS VARONES"),
            new CommonCandidate("COMÚN DE SANTAS MUJERES", "COMUN DE SANTAS MUJERES",
                    "COMÚN DE SANTAS MUJERES")
    };

    private SaintOfficeRepository() {}

    public static List<CommonOfficeChoice> commonChoices(Context context, HoursLink saint)
            throws Exception {
        if (saint == null) return Collections.emptyList();
        EpubTocEntry saintEntry = tocEntry(context, saint);
        File root = EpubUtils.ensureExtracted(context, saint.volume);
        String section = saintSection(read(new File(root, saintEntry.filePath)),
                saintEntry.fragment);
        String normalized = normalize(section.replaceAll("<[^>]+>", " "));
        List<LocatedChoice> located = new ArrayList<>();
        for (CommonCandidate candidate : COMMONS) {
            int at = normalized.indexOf(candidate.needle);
            if (at < 0) continue;
            int index = EpubUtils.findEntryIndex(context, saint.volume, candidate.tocTitle);
            if (index < 0) continue;
            EpubTocEntry entry = EpubUtils.tableOfContents(context, saint.volume).get(index);
            located.add(new LocatedChoice(at, new CommonOfficeChoice(candidate.title,
                    entry.filePath, entry.fragment)));
        }
        Collections.sort(located, Comparator.comparingInt(value -> value.position));
        List<CommonOfficeChoice> result = new ArrayList<>();
        for (LocatedChoice choice : located) result.add(choice.choice);
        return result;
    }

    public static MemoryOffice compose(Context context, HourEntry temporal,
                                       HoursLink saint, CommonOfficeChoice common,
                                       int ordinaryWeek, String lectionaryCycle,
                                       int readingsYear)
            throws Exception {
        if (temporal == null || saint == null) return null;
        if ("invitatory".equals(temporal.key)) {
            return composeInvitatory(context, temporal, saint, common);
        }
        if ("office".equals(temporal.key)) {
            return composeOfficeOfReadings(context, temporal, saint, common, ordinaryWeek,
                    lectionaryCycle, readingsYear);
        }
        if (saint.isFeastOrSolemnity()
                && ("terce".equals(temporal.key) || "sext".equals(temporal.key)
                || "none".equals(temporal.key))) {
            return composeMajorIntermediateHour(context, temporal, saint, common);
        }
        if (!("lauds".equals(temporal.key) || "vespers".equals(temporal.key))) {
            return null;
        }

        File temporalRoot = EpubUtils.ensureExtracted(context, temporal.volume);
        String temporalHtml = read(new File(temporalRoot, temporal.filePath));
        String temporalBody = body(temporalHtml);
        int shortReading = paragraphStart(temporalBody, "LECTURA BREVE");
        if (shortReading < 0) return null;

        HoursVolume santoral = HoursRepository.find("sanctoral");
        File saintRoot = EpubUtils.ensureExtracted(context, santoral);
        EpubTocEntry saintEntry = tocEntry(context, saint);
        String saintHtml = read(new File(saintRoot, saintEntry.filePath));
        String saintSection = saintSection(saintHtml, saintEntry.fragment);
        String properHour = properHourSection(saintRoot, saintEntry.filePath,
                saintSection, temporal.key);
        boolean forceTemporalPsalmody = isMaryQueen(saint);
        if (!forceTemporalPsalmody) {
            properHour = expandReferencedPsalmody(saintRoot, saintEntry.filePath, properHour);
        }

        String commonHour = commonHourSection(saintRoot, common, temporal.key);

        if (!forceTemporalPsalmody && hasPsalmody(properHour)) {
            String fullHour = properHour;
            String properAntiphon = properGospelAntiphon(saintSection, temporal.key);
            if (!properAntiphon.isEmpty()) {
                fullHour = replaceGospelAntiphon(fullHour, properAntiphon);
            }
            String properPrayer = properPrayer(saintSection);
            if (!properPrayer.isEmpty()) {
                int sourcePrayer = prayerHeadingStart(fullHour);
                if (sourcePrayer >= 0) {
                    fullHour = fullHour.substring(0, sourcePrayer) + properPrayer;
                }
            }
            String note = celebrationNote(saint,
                    "Antífonas y salmodia propias según el formulario · oración propia");
            return new MemoryOffice(document(note + fullHour),
                    Uri.fromFile(new File(saintRoot, saintEntry.filePath)).toString());
        }

        if (saint.isFeastOrSolemnity()) {
            String fullHour = commonHour;
            if (fullHour.isEmpty()) return null;
            String properAntiphon = properGospelAntiphon(saintSection, temporal.key);
            if (!properAntiphon.isEmpty()) {
                fullHour = replaceGospelAntiphon(fullHour, properAntiphon);
            }
            String properPrayer = properPrayer(saintSection);
            if (!properPrayer.isEmpty()) {
                int sourcePrayer = prayerHeadingStart(fullHour);
                if (sourcePrayer >= 0) {
                    fullHour = fullHour.substring(0, sourcePrayer) + properPrayer;
                }
            }
            String note = celebrationNote(saint, "Oficio propio o del común correspondiente");
            String basePath = common == null ? saintEntry.filePath : common.filePath;
            return new MemoryOffice(document(note + fullHour),
                    Uri.fromFile(new File(saintRoot, basePath)).toString());
        }

        String sourceHour = paragraphStart(properHour, "LECTURA BREVE") >= 0
                ? properHour : commonHour;
        int sourceReading = paragraphStart(sourceHour, "LECTURA BREVE");
        String suffix = sourceReading >= 0
                ? sourceHour.substring(sourceReading)
                : temporalBody.substring(shortReading);

        String memoryHymn = hymnSection(properHour);
        String hymnSource = "propio del santo";
        if (memoryHymn.isEmpty()) {
            memoryHymn = hymnSection(commonHour);
            hymnSource = common == null ? "la memoria" : common.title;
        }
        String temporalPrefix = temporalBody.substring(0, shortReading);
        if (!memoryHymn.isEmpty()) {
            temporalPrefix = replaceHymn(temporalPrefix, memoryHymn);
        }

        String properAntiphon = properGospelAntiphon(saintSection, temporal.key);
        if (!properAntiphon.isEmpty()) {
            suffix = replaceGospelAntiphon(suffix, properAntiphon);
        }

        String properPrayer = properPrayer(saintSection);
        if (!properPrayer.isEmpty()) {
            int sourcePrayer = prayerHeadingStart(suffix);
            if (sourcePrayer >= 0) suffix = suffix.substring(0, sourcePrayer)
                    + properPrayer;
        }

        String note = "<div class=\"ministerium-memory-note\"><strong>Memoria de "
                + escapeHtml(saint.title) + "</strong><br/>Himno: "
                + escapeHtml(hymnSource) + " · salmodia de la feria"
                + (!properAntiphon.isEmpty() ? " · antífona evangélica propia" : "")
                + " · oración propia</div>";
        String html = document(note + temporalPrefix + suffix);
        return new MemoryOffice(html,
                Uri.fromFile(new File(temporalRoot, temporal.filePath)).toString());
    }

    private static MemoryOffice composeInvitatory(Context context, HourEntry temporal,
                                                   HoursLink saint,
                                                   CommonOfficeChoice common)
            throws Exception {
        File temporalRoot = EpubUtils.ensureExtracted(context, temporal.volume);
        String temporalHtml = read(new File(temporalRoot, temporal.filePath));
        String temporalBody = temporal.fragment == null || temporal.fragment.isEmpty()
                ? body(temporalHtml)
                : fragmentSection(temporalHtml, temporal.fragment, null);

        EpubTocEntry saintEntry = tocEntry(context, saint);
        File saintRoot = EpubUtils.ensureExtracted(context, saint.volume);
        String saintSection = saintSection(
                read(new File(saintRoot, saintEntry.filePath)), saintEntry.fragment);
        String antiphon = properInvitatoryAntiphon(saintSection);
        String source = "propia del santo";

        if (antiphon.isEmpty() && common != null) {
            String commonHtml = read(new File(saintRoot, common.filePath));
            String commonSection = common.fragment == null || common.fragment.isEmpty()
                    ? body(commonHtml)
                    : fragmentSection(commonHtml, common.fragment, null);
            antiphon = properInvitatoryAntiphon(commonSection);
            source = "tomada de " + common.title;
        }
        if (antiphon.isEmpty()) return null;

        String composed = replaceInvitatoryAntiphon(temporalBody, antiphon);
        if (composed.equals(temporalBody)) return null;
        String note = celebrationNote(saint, "Antífona del Invitatorio " + source);
        return new MemoryOffice(document(note + composed),
                Uri.fromFile(new File(temporalRoot, temporal.filePath)).toString());
    }

    private static MemoryOffice composeOfficeOfReadings(Context context, HourEntry temporal,
                                                         HoursLink saint,
                                                         CommonOfficeChoice common,
                                                         int ordinaryWeek,
                                                         String lectionaryCycle,
                                                         int readingsYear) throws Exception {
        File temporalRoot = EpubUtils.ensureExtracted(context, temporal.volume);
        String temporalHtml = ordinaryWeek > 0
                ? OrdinaryReferenceResolver.resolve(temporalRoot, temporal.filePath,
                        ordinaryWeek, lectionaryCycle, readingsYear)
                : read(new File(temporalRoot, temporal.filePath));
        String temporalBody = body(temporalHtml);
        int secondReading = paragraphStart(temporalBody, "SEGUNDA LECTURA");
        if (secondReading < 0) return null;

        EpubTocEntry saintEntry = tocEntry(context, saint);
        File saintRoot = EpubUtils.ensureExtracted(context, saint.volume);
        String saintHtml = read(new File(saintRoot, saintEntry.filePath));
        String saintSection = saintSection(saintHtml, saintEntry.fragment);
        String properOffice = properHourSection(saintRoot, saintEntry.filePath,
                saintSection, "office");
        String commonOffice = commonHourSection(saintRoot, common, "office");
        String properReadingSource = paragraphStart(properOffice, "SEGUNDA LECTURA") >= 0
                ? properOffice : saintSection;
        int properReading = paragraphStart(properReadingSource, "SEGUNDA LECTURA");
        if (properReading < 0) return null;
        int gospelAntiphons = paragraphStart(properReadingSource,
                "ANTÍFONAS DEL CÁNTICO EVANGÉLICO");
        int saintPrayer = prayerHeadingStart(properReadingSource);
        int properReadingEnd = gospelAntiphons > properReading
                ? gospelAntiphons : saintPrayer > properReading
                ? saintPrayer : properReadingSource.length();
        String properSecondReading = properReadingSource.substring(
                properReading, properReadingEnd);
        String prayer = properPrayer(saintSection);
        if (prayer.isEmpty()) prayer = properPrayer(properOffice);
        if (!prayer.isEmpty()) properSecondReading += prayer;

        boolean fullProperOffice = saint.isFeastOrSolemnity()
                && hasPsalmody(properOffice)
                && paragraphStart(properOffice, "PRIMERA LECTURA") >= 0;
        String baseBody = fullProperOffice ? properOffice
                : saint.isFeastOrSolemnity() && !commonOffice.isEmpty()
                ? commonOffice : temporalBody;
        int baseSecondReading = paragraphStart(baseBody, "SEGUNDA LECTURA");
        if (baseSecondReading < 0) baseSecondReading = secondReading;
        String prefix = baseBody.substring(0, baseSecondReading);

        String memoryHymn = hymnSection(properOffice);
        String hymnSource = "propio del santo";
        if (memoryHymn.isEmpty()) {
            memoryHymn = hymnSection(commonOffice);
            hymnSource = common == null ? "la feria" : common.title;
        }
        if (!memoryHymn.isEmpty()) prefix = replaceHymn(prefix, memoryHymn);

        String description = saint.isFeastOrSolemnity()
                ? "Himno: " + hymnSource + " · salmodia y primera lectura "
                + (fullProperOffice ? "propias · " : "del común · ")
                + "segunda lectura y oración propias"
                : "Himno: " + hymnSource + " · salmodia y primera lectura de la feria · "
                + "segunda lectura y oración propias";
        String note = celebrationNote(saint, description);
        File baseFile = fullProperOffice
                ? new File(saintRoot, saintEntry.filePath)
                : saint.isFeastOrSolemnity() && common != null
                && !commonOffice.isEmpty()
                ? new File(saintRoot, common.filePath)
                : new File(temporalRoot, temporal.filePath);
        return new MemoryOffice(document(note + prefix + properSecondReading),
                Uri.fromFile(baseFile).toString());
    }

    private static MemoryOffice composeMajorIntermediateHour(Context context,
                                                              HourEntry temporal,
                                                              HoursLink saint,
                                                              CommonOfficeChoice common)
            throws Exception {
        EpubTocEntry saintEntry = tocEntry(context, saint);
        File saintRoot = EpubUtils.ensureExtracted(context, saint.volume);
        String saintHtml = read(new File(saintRoot, saintEntry.filePath));
        String saintSection = saintSection(saintHtml, saintEntry.fragment);
        String proper = properHourSection(saintRoot, saintEntry.filePath,
                saintSection, "middle");
        String commonHour = commonHourSection(saintRoot, common, "middle");
        String source = hasPsalmody(proper) ? proper : commonHour;
        if (source.isEmpty()) return null;
        String basePath = hasPsalmody(proper)
                ? saintEntry.filePath : common == null ? saintEntry.filePath : common.filePath;
        return new MemoryOffice(document(celebrationNote(saint,
                "Hora intermedia del propio o del común correspondiente") + source),
                Uri.fromFile(new File(saintRoot, basePath)).toString());
    }

    private static EpubTocEntry tocEntry(Context context, HoursLink link) throws Exception {
        List<EpubTocEntry> entries = EpubUtils.tableOfContents(context, link.volume);
        if (link.tocIndex < 0 || link.tocIndex >= entries.size()) {
            throw new IllegalArgumentException("Entrada del Santoral no válida");
        }
        return entries.get(link.tocIndex);
    }

    private static Map<String, String> commonHourLinks(String html, String fragment) {
        int start = idPosition(html, fragment);
        if (start < 0) start = 0;
        int end = Math.min(html.length(), start + 2500);
        Matcher matcher = ANCHOR.matcher(html.substring(start, end));
        Map<String, String> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String label = matcher.group(2).replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ").trim().toUpperCase(Locale.ROOT);
            if (!("O".equals(label) || "L".equals(label) || "M".equals(label)
                    || "V".equals(label) || "1V".equals(label)
                    || "2V".equals(label))) continue;
            String href = matcher.group(1);
            int hash = href.indexOf('#');
            if (hash >= 0 && hash + 1 < href.length()) {
                result.put(label, href.substring(hash + 1));
            }
        }
        return result;
    }

    private static String commonHourSection(File root, CommonOfficeChoice common,
                                            String hourKey) throws Exception {
        if (common == null) return "";
        String html = read(new File(root, common.filePath));
        Map<String, String> links = commonHourLinks(html, common.fragment);
        String wanted = "office".equals(hourKey) ? "O"
                : "lauds".equals(hourKey) ? "L"
                : "middle".equals(hourKey) ? "M" : "2V";
        String fragment = links.get(wanted);
        if (fragment == null && "2V".equals(wanted)) fragment = links.get("V");
        if (fragment == null) return "";
        return fragmentSection(html, fragment, nextFragment(html, fragment, links));
    }

    private static String properHourSection(File root, String sourcePath,
                                            String section, String hourKey)
            throws Exception {
        Matcher matcher = ANCHOR.matcher(section);
        Map<String, HourTarget> links = new LinkedHashMap<>();
        while (matcher.find()) {
            String label = normalize(matcher.group(2).replaceAll("<[^>]+>", " "));
            String canonical = canonicalHourLabel(label);
            if (canonical.isEmpty()) continue;
            HourTarget target = hourTarget(sourcePath, matcher.group(1));
            if (target != null) links.put(canonical, target);
        }
        String wanted = "office".equals(hourKey) ? "OFFICE"
                : "lauds".equals(hourKey) ? "LAUDS"
                : "middle".equals(hourKey) ? "MIDDLE" : "VESPERS";
        HourTarget target = links.get(wanted);
        if (target == null && "office".equals(hourKey)) {
            int start = paragraphStart(section, "OFICIO DE LECTURA");
            if (start < 0) start = paragraphStart(section, "OFICIO DE LECTURAS");
            if (start >= 0) {
                int end = section.length();
                for (HourTarget candidate : links.values()) {
                    if (!candidate.path.equals(sourcePath)) continue;
                    int position = idPosition(section, candidate.fragment);
                    if (position > start && position < end) end = tagStart(section, position);
                }
                return section.substring(start, Math.max(start, end));
            }
        }
        if (target == null) return "";
        String targetHtml = target.path.equals(sourcePath)
                ? section : read(new File(root, target.path));
        return fragmentSection(targetHtml, target.fragment,
                nextHourFragment(targetHtml, target, links));
    }

    private static String canonicalHourLabel(String label) {
        if ("O".equals(label) || "OFICIO".equals(label)
                || "OFICIO DE LECTURA".equals(label)
                || "OFICIO DE LECTURAS".equals(label)) return "OFFICE";
        if ("L".equals(label) || "LAUDES".equals(label)) return "LAUDS";
        if ("M".equals(label) || "HORA INTERMEDIA".equals(label)) return "MIDDLE";
        if ("V".equals(label) || "1V".equals(label) || "2V".equals(label)
                || "VISPERAS".equals(label) || "I VISPERAS".equals(label)
                || "II VISPERAS".equals(label)) return "VESPERS";
        return "";
    }

    private static boolean hasPsalmody(String section) {
        int start = paragraphStart(section, "SALMODIA");
        if (start < 0) start = paragraphStart(section, "ANT. 1.");
        if (start < 0) return false;
        int end = firstParagraphStartAfter(section, start,
                "LECTURA BREVE", "PRIMERA LECTURA", "SEGUNDA LECTURA");
        if (end < 0) end = section.length();
        String psalmody = normalize(section.substring(start, end).replaceAll("<[^>]+>", " "));
        int headings = count(psalmody, "SALMO ") + count(psalmody, "CANTICO ");
        return headings >= 3 && psalmody.length() > 700;
    }

    private static boolean isMaryQueen(HoursLink saint) {
        if (saint == null || saint.isFeastOrSolemnity()) return false;
        String title = normalize(saint.title);
        return title.contains("MARIA REINA")
                || title.contains("SANTISIMA VIRGEN MARIA REINA");
    }

    private static String expandReferencedPsalmody(File root, String sourcePath,
                                                     String hour) throws Exception {
        if (hour == null || hour.isEmpty() || hasPsalmody(hour)) return hour;
        int salmodia = paragraphStart(hour, "SALMODIA");
        int ant1 = paragraphStart(hour, "ANT. 1.");
        int ant2 = paragraphStart(hour, "ANT. 2.");
        int ant3 = paragraphStart(hour, "ANT. 3.");
        int reading = paragraphStart(hour, "LECTURA BREVE");
        if (salmodia < 0) salmodia = ant1;
        if (salmodia < 0 || ant1 < 0 || ant2 < 0 || ant3 < 0 || reading < 0) return hour;
        List<HourTarget> targets = new ArrayList<>();
        String rubric = normalize(hour.substring(ant1, reading).replaceAll("<[^>]+>", " "));
        if (rubric.contains("LOS SALMOS Y EL CANTICO SE TOMAN DEL DOMINGO I DEL SALTERIO")) {
            targets.add(new HourTarget("OEBPS/Text/Section0002.html", "filepos2256303"));
            targets.add(new HourTarget("OEBPS/Text/Section0002.html", "filepos2257391"));
            targets.add(new HourTarget("OEBPS/Text/Section0002.html", "filepos2260088"));
        } else {
            Matcher matcher = ANCHOR.matcher(hour.substring(ant1, reading));
            while (matcher.find() && targets.size() < 3) {
                HourTarget target = hourTarget(sourcePath, matcher.group(1));
                if (target != null) targets.add(target);
            }
        }
        if (targets.size() != 3) return hour;

        String[] psalms = new String[3];
        for (int i = 0; i < targets.size(); i++) {
            HourTarget target = targets.get(i);
            String html = read(new File(root, target.path));
            String end = null;
            for (int j = i + 1; j < targets.size(); j++) {
                if (targets.get(j).path.equals(target.path)) {
                    end = targets.get(j).fragment;
                    break;
                }
            }
            psalms[i] = fragmentSection(html, target.fragment, end);
        }
        if (psalms[0].isEmpty() || psalms[1].isEmpty() || psalms[2].isEmpty()) return hour;

        String antiphon1 = paragraph(hour, ant1);
        String antiphon2 = paragraph(hour, ant2);
        String antiphon3 = paragraph(hour, ant3);
        int ant1End = ant1 + antiphon1.length();
        return hour.substring(0, ant1End)
                + psalms[0] + antiphon1
                + antiphon2 + psalms[1] + antiphon2
                + antiphon3 + psalms[2] + antiphon3
                + hour.substring(reading);
    }

    private static String paragraph(String html, int start) {
        int end = html.indexOf("</p>", start);
        return end < 0 ? "" : html.substring(start, end + 4);
    }

    private static int count(String value, String token) {
        int result = 0;
        int at = 0;
        while ((at = value.indexOf(token, at)) >= 0) {
            result++;
            at += token.length();
        }
        return result;
    }

    private static String celebrationNote(HoursLink saint, String description) {
        String rank = "S".equals(saint.liturgicalRank) ? "Solemnidad"
                : "F".equals(saint.liturgicalRank) ? "Fiesta" : "Memoria";
        return "<div class=\"ministerium-memory-note\"><strong>" + rank + " de "
                + escapeHtml(saint.title) + "</strong><br/>"
                + escapeHtml(description) + "</div>";
    }

    private static HourTarget hourTarget(String sourcePath, String href)
            throws Exception {
        String[] parts = href.split("#", 2);
        if (parts.length < 2 || parts[1].isEmpty()) return null;
        URI base = new URI(null, null, sourcePath, null);
        String path = parts[0].isEmpty()
                ? sourcePath : base.resolve(parts[0]).normalize().getPath();
        while (path.startsWith("/")) path = path.substring(1);
        return new HourTarget(path, parts[1]);
    }

    private static String nextHourFragment(String html, HourTarget current,
                                           Map<String, HourTarget> links) {
        int currentPosition = idPosition(html, current.fragment);
        int nextPosition = Integer.MAX_VALUE;
        String next = null;
        for (HourTarget candidate : links.values()) {
            if (candidate == null || !candidate.path.equals(current.path)
                    || candidate.fragment.equals(current.fragment)) continue;
            int position = idPosition(html, candidate.fragment);
            if (position > currentPosition && position < nextPosition) {
                nextPosition = position;
                next = candidate.fragment;
            }
        }
        return next;
    }

    private static String nextFragment(String html, String current,
                                       Map<String, String> links) {
        int currentPosition = idPosition(html, current);
        int nextPosition = Integer.MAX_VALUE;
        String next = null;
        for (String candidate : links.values()) {
            if (candidate == null || candidate.equals(current)) continue;
            int position = idPosition(html, candidate);
            if (position > currentPosition && position < nextPosition) {
                nextPosition = position;
                next = candidate;
            }
        }
        return next;
    }

    private static String hymnSection(String hour) {
        if (hour == null || hour.isEmpty()) return "";
        int start = paragraphStart(hour, "HIMNO");
        if (start < 0) return "";
        int end = firstParagraphStartAfter(hour, start,
                "SALMODIA", "ANT. 1.", "LECTURA BREVE",
                "PRIMERA LECTURA", "SEGUNDA LECTURA");
        if (end < 0) return "";
        return hour.substring(start, end);
    }

    private static String replaceHymn(String temporalPrefix, String memoryHymn) {
        int start = paragraphStart(temporalPrefix, "HIMNO");
        if (start < 0) return temporalPrefix;
        int end = firstParagraphStartAfter(temporalPrefix, start,
                "SALMODIA", "ANT. 1.", "LECTURA BREVE",
                "PRIMERA LECTURA", "SEGUNDA LECTURA");
        if (end < 0) return temporalPrefix;
        return temporalPrefix.substring(0, start) + memoryHymn
                + temporalPrefix.substring(end);
    }

    private static String properGospelAntiphon(String section, String hourKey) {
        String label = "lauds".equals(hourKey) ? "Laudes" : "V[ií]speras";
        Pattern pattern = Pattern.compile(
                "<p\\b[^>]*>\\s*<span\\b[^>]*>\\s*" + label
                        + "\\s*:\\s*</span>(.*?)</p>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(section);
        if (!matcher.find()) return "";
        return matcher.group(1)
                .replaceAll("(?is)<a\\b[^>]*>.*?</a>", "")
                .trim();
    }

    private static String properInvitatoryAntiphon(String section) {
        if (section == null || section.isEmpty()) return "";
        Pattern markerPattern = Pattern.compile(
                "<(?:p|h[1-6])\\b[^>]*>.*?Invitatorio.*?</(?:p|h[1-6])>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher marker = markerPattern.matcher(section);
        if (!marker.find()) return "";
        String tail = section.substring(marker.end(), Math.min(section.length(), marker.end() + 1800));
        Pattern antPattern = Pattern.compile(
                "<p\\b[^>]*>\\s*(?:<span\\b[^>]*>)?\\s*Ant\\.\\s*(?:</span>)?\\s*(.*?)</p>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher ant = antPattern.matcher(tail);
        if (!ant.find()) return "";
        return ant.group(1).replaceAll("(?is)<a\\b[^>]*>.*?</a>", "").trim();
    }

    private static String replaceInvitatoryAntiphon(String temporalBody,
                                                     String antiphon) {
        int instruction = paragraphStart(temporalBody, "con la antífona");
        if (instruction < 0) instruction = paragraphStart(
                temporalBody, "con la antifona");
        if (instruction < 0) return temporalBody;
        int instructionEnd = temporalBody.indexOf("</p>", instruction);
        if (instructionEnd < 0) return temporalBody;
        instructionEnd += 4;
        Pattern paragraph = Pattern.compile(
                "(<p\\b[^>]*>)(.*?)(</p>)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = paragraph.matcher(temporalBody);
        if (!matcher.find(instructionEnd)) return temporalBody;
        String replacement = matcher.group(1)
                + "<span class=\"rojo\">Ant.</span> " + antiphon
                + matcher.group(3);
        return temporalBody.substring(0, matcher.start()) + replacement
                + temporalBody.substring(matcher.end());
    }

    private static String replaceGospelAntiphon(String hour, String antiphon) {
        int heading = paragraphStart(hour, "CÁNTICO EVANGÉLICO");
        if (heading < 0) return hour;
        String tail = hour.substring(heading);
        Pattern pattern = Pattern.compile(
                "(<p\\b[^>]*>\\s*<span\\b[^>]*>\\s*Ant\\.\\s*</span>)(.*?)(</p>)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(tail);
        if (!matcher.find()) return hour;
        String replacement = matcher.group(1) + " " + antiphon + matcher.group(3);
        return hour.substring(0, heading) + tail.substring(0, matcher.start())
                + replacement + tail.substring(matcher.end());
    }

    private static int firstParagraphStartAfter(String html, int after,
                                                String... markers) {
        int result = Integer.MAX_VALUE;
        String lower = html.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            int at = lower.indexOf(marker.toLowerCase(Locale.ROOT), after + 1);
            if (at < 0) continue;
            int paragraph = html.lastIndexOf("<p", at);
            if (paragraph > after && paragraph < result) result = paragraph;
        }
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private static String fragmentSection(String html, String startFragment,
                                          String endFragment) {
        int start = idPosition(html, startFragment);
        if (start < 0) return "";
        start = tagStart(html, start);
        int end = endFragment == null ? bodyEnd(html) : idPosition(html, endFragment);
        if (end < 0) end = bodyEnd(html);
        if (end < start) end = html.length();
        return html.substring(start, tagStart(html, end));
    }

    private static String saintSection(String html, String fragment) {
        int start = idPosition(html, fragment);
        if (start < 0) return body(html);
        start = tagStart(html, start);
        int next = html.indexOf("id=\"sigil_toc_id_", start + 20);
        if (next < 0) next = html.indexOf("id='sigil_toc_id_", start + 20);
        int end = next < 0 ? bodyEnd(html) : tagStart(html, next);
        return html.substring(start, Math.max(start, end));
    }

    private static String properPrayer(String section) {
        // La oración propia pertenece al formulario actual. Tomar la última
        // cabecera «Oración» puede saltar al santo siguiente cuando varios
        // formularios comparten el mismo XHTML (p. ej. san Agustín / santa Rosa).
        // Por eso se toma la primera oración válida dentro del bloque del santo.
        Matcher matcher = PRAYER_HEADING.matcher(section);
        if (!matcher.find()) return "";
        int headingStart = matcher.start();
        int headingEnd = matcher.end();
        Pattern paragraph = Pattern.compile("<p\\b[^>]*>.*?</p>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher following = paragraph.matcher(section);
        while (following.find(headingEnd)) {
            String plain = following.group().replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
            if (!plain.isEmpty()) {
                return section.substring(headingStart, headingEnd) + following.group();
            }
            headingEnd = following.end();
        }
        return "";
    }

    private static int prayerHeadingStart(String html) {
        Matcher matcher = PRAYER_HEADING.matcher(html);
        return matcher.find() ? matcher.start() : -1;
    }

    private static int paragraphStart(String html, String marker) {
        int at = html.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
        if (at < 0) return -1;
        return html.lastIndexOf("<p", at);
    }

    private static int idPosition(String html, String fragment) {
        if (fragment == null || fragment.isEmpty()) return -1;
        int result = html.indexOf("id=\"" + fragment + "\"");
        if (result < 0) result = html.indexOf("id='" + fragment + "'");
        return result;
    }

    private static int tagStart(String html, int from) {
        int result = html.lastIndexOf('<', Math.max(0, from));
        return result < 0 ? Math.max(0, from) : result;
    }

    private static String body(String html) {
        int start = html.toLowerCase(Locale.ROOT).indexOf("<body");
        if (start < 0) return html;
        start = html.indexOf('>', start);
        if (start < 0) return html;
        int end = bodyEnd(html);
        return html.substring(start + 1, Math.max(start + 1, end));
    }

    private static int bodyEnd(String html) {
        int result = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return result < 0 ? html.length() : result;
    }

    private static String document(String body) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"></head><body>"
                + body + "</body></html>";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
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

    private static final class CommonCandidate {
        final String title;
        final String needle;
        final String tocTitle;

        CommonCandidate(String title, String needle, String tocTitle) {
            this.title = title;
            this.needle = needle;
            this.tocTitle = tocTitle;
        }
    }

    private static final class LocatedChoice {
        final int position;
        final CommonOfficeChoice choice;

        LocatedChoice(int position, CommonOfficeChoice choice) {
            this.position = position;
            this.choice = choice;
        }
    }

    private static final class HourTarget {
        final String path;
        final String fragment;

        HourTarget(String path, String fragment) {
            this.path = path;
            this.fragment = fragment;
        }
    }
}