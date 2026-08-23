package com.fabri.ministerium;

import android.content.Context;
import android.text.Html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Construye Misa + Laudes/Vísperas como un único documento continuo.
 *
 * La clase no abre Activities secundarias: toma los bloques de las fuentes ya
 * existentes (Liturgia de las Horas, Misal y Leccionario) y los ensambla en el
 * orden acordado para Ministerium 3.0.
 */
public final class CombinedMassComposer {
    public static final class Result {
        public final String title;
        public final String celebration;
        public final String html;

        Result(String title, String celebration, String html) {
            this.title = title;
            this.celebration = celebration;
            this.html = html;
        }
    }

    private static final Pattern BLOCK = Pattern.compile(
            "(?is)<(p|h1|h2|h3|h4|h5|h6)\\b[^>]*>.*?</\\1>");
    private static final Pattern ROW = Pattern.compile("(?is)<tr\\b[^>]*>.*?</tr>");
    private static final Pattern LINK = Pattern.compile(
            "(?is)<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>");

    private CombinedMassComposer() {}

    public static Result compose(Context context, Calendar selectedDate,
                                 String hourKey, String language) throws Exception {
        Calendar date = (Calendar) selectedDate.clone();
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        HourEntry hour = findHour(context, day, date, hourKey);
        if (hour == null) throw new IllegalStateException("No se encontró la Hora elegida.");

        String hourHtml = composeHour(context, day, date, hour);
        String psalmody = hourSection(hourHtml, "SALMODIA", "LECTURA BREVE");
        String canticle = hourSection(hourHtml, "CÁNTICO EVANGÉLICO", "PRECES");
        canticle = inlineGospelCanticle(canticle, hourKey);
        String intercessions = extractIntercessions(hourHtml);

        File missalRoot = EpubUtils.ensureExtracted(context, HoursRepository.ROMAN_MISSAL);
        String ordinary = readRequired(findFile(missalRoot, "OrdinarioMisa.html"));
        String proper = properDayHtml(context, date, day, missalRoot);

        String entrance = idSection(proper, "AntifonaEntrada", "Colecta");
        String collect = idSection(proper, "Colecta", "OracionFieles", "Ofrendas");
        String offerings = idSection(proper, "Ofrendas", "AntifonaComunion");
        String communionAntiphon = idSection(proper, "AntifonaComunion", "DespuesComunion");
        String afterCommunion = idSection(proper, "DespuesComunion");

        String kyrieGloria = rowsFromTextToAnchor(ordinary, "KYRIE", "AntesColecta");
        String niceneCreed = rowsByAnchors(ordinary, "Credo", "CredoAp");
        String apostlesCreed = rowsByAnchors(ordinary, "CredoAp", "OracionFieles");
        String preparation = rowsByAnchors(ordinary, "LiturgiaEucaristica", "AntesOfrendas");
        String prefaceDialogue = rowsByAnchors(ordinary, "AntesPE", "Prefacio");
        String communionIntro = rowsByAnchors(ordinary, "ritocomunion", "pater");
        String pater = rowsFromAnchorToText(ordinary, "pater", "LIBERA NOS");
        String communionAfterPater = rowsFromTextToAnchor(ordinary, "LIBERA NOS",
                "AntesAntifonaComunion");
        String communion = rowsByAnchors(ordinary, "AntifonaComunion",
                "AntesDespuesComunion");
        String conclusion = rowsByAnchors(ordinary, "Conclusion", null);

        String prefaces = resolvePrefaces(proper, missalRoot);
        String prayers = eucharisticPrayers(missalRoot);
        String readings = readings(context, date);
        boolean showCreed = creedRequired(context, date);

        String title = "Misa + " + ("vespers".equals(hourKey) ? "Vísperas" : "Laudes");
        StringBuilder body = new StringBuilder();
        body.append(section("Inicio de la celebración",
                clean(entrance) + signOfCross(language)));
        body.append(section("Salmodia de " + ("vespers".equals(hourKey) ? "Vísperas" : "Laudes"),
                clean(psalmody)));
        body.append(section("Santa Misa", table(clean(kyrieGloria))));
        body.append(section("Oración colecta", clean(collect)));
        body.append(section("Liturgia de la Palabra", clean(readings)));
        body.append("<section class=\"ministerium-section rubric-section\"><h2>Homilía</h2>"
                + "<p class=\"rubric\">Después del Evangelio se hace la homilía.</p></section>");

        if (showCreed) {
            body.append("<section class=\"ministerium-section\" id=\"creedBlock\"><h2>Profesión de fe</h2>"
                    + "<div class=\"choicebar\"><button class=\"selected\" id=\"creedNiceneButton\" "
                    + "onclick=\"setCreed('nicene')\">Niceno-constantinopolitano</button>"
                    + "<button id=\"creedApostlesButton\" onclick=\"setCreed('apostles')\">Apostólico</button>"
                    + "<button class=\"language-button\" id=\"creedLanguageButton\" "
                    + "onclick=\"cycleBlockLanguage('creedBlock','creedLanguageButton')\">ESP</button></div>"
                    + "<div id=\"creedNicene\" class=\"creed-choice\">" + table(clean(niceneCreed)) + "</div>"
                    + "<div id=\"creedApostles\" class=\"creed-choice hidden\">"
                    + table(clean(apostlesCreed)) + "</div></section>");
        }

        body.append(section("Preces de " + ("vespers".equals(hourKey) ? "Vísperas" : "Laudes"),
                clean(intercessions)));
        body.append(section("Liturgia eucarística", table(clean(preparation)) + clean(offerings)
                + "<div id=\"prefaceBlock\"><h3>Prefacio</h3>" + table(clean(prefaceDialogue))
                + prefaces + "</div>" + prayers));

        body.append("<section class=\"ministerium-section\" id=\"communionBlock\"><h2>Rito de la Comunión</h2>"
                + table(clean(communionIntro))
                + "<div class=\"inline-language\"><span>Padre nuestro</span>"
                + "<button class=\"language-button\" id=\"paterLanguageButton\" "
                + "onclick=\"cycleBlockLanguage('paterBlock','paterLanguageButton')\">ESP</button></div>"
                + "<div id=\"paterBlock\">" + table(clean(pater)) + "</div>"
                + table(clean(communionAfterPater)) + clean(communionAntiphon)
                + table(clean(communion)) + "</section>");

        body.append(section("Cántico evangélico de "
                + ("vespers".equals(hourKey) ? "Vísperas" : "Laudes"), clean(canticle)));
        body.append(section("Oración después de la Comunión", clean(afterCommunion)));
        body.append(section("Rito de conclusión", table(clean(conclusion))));

        return new Result(title, day.celebration + " · " + day.dateLabel,
                document(context, body.toString(), language));
    }

    private static HourEntry findHour(Context context, LiturgicalDay day, Calendar date,
                                      String hourKey) throws Exception {
        List<HourEntry> hours = DailyHoursRepository.hoursFor(context, day.temporalOffice, date);
        for (HourEntry entry : hours) if (hourKey.equals(entry.key)) return entry;
        return null;
    }

    private static String composeHour(Context context, LiturgicalDay day, Calendar date,
                                      HourEntry hour) throws Exception {
        HoursLink proper = null;
        CommonOfficeChoice common = null;
        for (HoursLink saint : day.saintOffices) {
            if (!saint.requiresProperOffice()) continue;
            proper = saint;
            List<CommonOfficeChoice> choices = SaintOfficeRepository.commonChoices(context, saint);
            common = choices.isEmpty() ? null : choices.get(0);
            break;
        }

        if (proper != null) {
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
            MemoryOffice memory = SaintOfficeRepository.compose(context, hour, proper, common,
                    ordinaryWeek, LiturgicalResolver.lectionaryCycle(date),
                    date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
            if (memory != null && memory.html != null && !memory.html.trim().isEmpty()) {
                return memory.html;
            }
        }

        File root = EpubUtils.ensureExtracted(context, hour.volume);
        File file = new File(root, hour.filePath);
        return readRequired(file);
    }

    private static String properDayHtml(Context context, Calendar date, LiturgicalDay day,
                                        File missalRoot) {
        try {
            MissalProperRepository.Target target = MissalProperRepository.resolve(
                    context, date, day.celebration, MissalProperRepository.Part.DAY);
            if (target != null) return readRequired(new File(missalRoot, target.filePath));
        } catch (Exception ignored) {}
        return "";
    }

    private static String readings(Context context, Calendar date) {
        try {
            if (!MassReadingsRepository.has(context, date)
                    && MassReadingsRepository.isCurrentMonth(date)) {
                try { MassReadingsRepository.syncDay(context, date); } catch (Exception ignored) {}
            }
            if (MassReadingsRepository.has(context, date)) {
                return bodyContent(MassReadingsRepository.read(context, date));
            }
        } catch (Exception ignored) {}
        return "<div class=\"missing-block\"><h3>Lecturas del día</h3>"
                + "<p>Las lecturas de esta fecha todavía no están guardadas en el dispositivo. "
                + "Actualiza el Leccionario y vuelve a abrir la celebración.</p></div>";
    }

    private static boolean creedRequired(Context context, Calendar date) {
        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true;
        try {
            List<LiturgicalEvent> events = LiturgicalCalendarRepository.eventsFor(context, date);
            for (LiturgicalEvent event : events) if (event.isSolemnity()) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private static String resolvePrefaces(String properHtml, File missalRoot) {
        if (properHtml == null || properHtml.isEmpty()) return "";
        Matcher matcher = LINK.matcher(properHtml);
        while (matcher.find()) {
            String label = normalizeText(matcher.group(2));
            String href = matcher.group(1).replace("%20", " ").replace("&amp;", "&");
            if (!label.contains("prefacio") && !normalizeText(href).contains("prefacio")) continue;
            String[] parts = href.split("#", 2);
            File file = new File(missalRoot, parts[0]);
            if (!file.isFile()) file = findFile(missalRoot, new File(parts[0]).getName());
            if (file == null || !file.isFile()) continue;
            try {
                String html = bodyContent(readRequired(file));
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    String fragment = idSection(html, parts[1]);
                    if (!fragment.isEmpty()) html = fragment;
                }
                return "<details class=\"preface-options\"><summary>Ver prefacio(s) disponibles</summary>"
                        + clean(html) + "</details>";
            } catch (Exception ignored) {}
        }
        return "<p class=\"rubric\">Use el prefacio que corresponde a la celebración.</p>";
    }

    private static String eucharisticPrayers(File missalRoot) {
        try {
            String firstThree = readRequired(findFile(missalRoot, "plegarias eucaristicas.htm"));
            String fourth = readRequired(findFile(missalRoot, "PLEGARIA EUCARISTICA IV.htm"));
            String p1 = rowsByAnchors(firstThree, "plegaria1", "plegaria2");
            String p2 = rowsByAnchors(firstThree, "plegaria2", "plegaria3");
            String p3 = rowsByAnchors(firstThree, "plegaria3", null);
            String p4 = rowsByAnchors(fourth, "plegaria4", null);
            return "<div class=\"eucharistic-prayers\"><h3>Plegaria eucarística</h3>"
                    + "<div class=\"choicebar prayer-choicebar\">"
                    + "<button onclick=\"setPrayer(1)\" id=\"prayerButton1\">I</button>"
                    + "<button onclick=\"setPrayer(2)\" id=\"prayerButton2\" class=\"selected\">II</button>"
                    + "<button onclick=\"setPrayer(3)\" id=\"prayerButton3\">III</button>"
                    + "<button onclick=\"setPrayer(4)\" id=\"prayerButton4\">IV</button></div>"
                    + "<p class=\"rubric small\">La Plegaria IV incluye prefacio propio; "
                    + "Ministerium la muestra como alternativa y debe respetarse la rúbrica de la celebración.</p>"
                    + prayerSection(1, p1, true) + prayerSection(2, p2, false)
                    + prayerSection(3, p3, true) + prayerSection(4, p4, true) + "</div>";
        } catch (Exception error) {
            return "<div class=\"missing-block\"><p>No se pudieron preparar las Plegarias Eucarísticas.</p></div>";
        }
    }

    private static String prayerSection(int number, String rows, boolean hidden) {
        return "<div id=\"prayer" + number + "\" class=\"eucharistic-prayer"
                + (hidden ? " hidden" : "") + "\"><h4>Plegaria Eucarística "
                + roman(number) + "</h4>" + table(clean(rows)) + "</div>";
    }

    private static String signOfCross(String language) {
        if ("lat_es".equals(language)) {
            return "<table class=\"missal-table sign-cross\"><tr><td class=\"izq\">"
                    + "<p><b>In nomine Patris</b> et Filii et Spiritus Sancti.</p><p><b>Amen.</b></p>"
                    + "</td><td class=\"dcha\"><p><b>En el nombre</b> del Padre, y del Hijo, "
                    + "y del Espíritu Santo.</p><p><b>Amén.</b></p></td></tr></table>";
        }
        return "<p class=\"rubric\">Se hace la señal de la cruz.</p>"
                + "<p><b>En el nombre del Padre, y del Hijo, y del Espíritu Santo.</b></p>"
                + "<p><b>Amén.</b></p>";
    }

    private static String inlineGospelCanticle(String section, String hourKey) {
        String canticle = "vespers".equals(hourKey) ? magnificat() : benedictus();
        Pattern link = Pattern.compile("(?is)<p\\b[^>]*>\\s*<a\\b[^>]*>\\s*"
                + ("vespers".equals(hourKey) ? "Magnificat" : "Benedictus")
                + "\\s*</a>\\s*</p>");
        Matcher matcher = link.matcher(section == null ? "" : section);
        if (matcher.find()) return matcher.replaceFirst(Matcher.quoteReplacement(canticle));
        return (section == null ? "" : section) + canticle;
    }

    private static String extractIntercessions(String html) {
        int start = blockPosition(html, "PRECES", 0, true);
        if (start < 0) return "";
        int end = blockPosition(html, "ORACIÓN", start + 1, false);
        if (end < 0) end = html.length();
        String section = html.substring(start, end);
        Matcher matcher = BLOCK.matcher(section);
        int cutStart = -1;
        int cutEnd = -1;
        while (matcher.find()) {
            String text = normalizeText(Html.fromHtml(matcher.group()).toString());
            if (text.contains("padre nuestro") && (text.contains("cristo")
                    || text.contains("palabras") || text.contains("terminando"))) {
                cutStart = matcher.start();
                cutEnd = matcher.end();
            }
        }
        if (cutStart >= 0) section = section.substring(0, cutStart) + section.substring(cutEnd);
        return section;
    }

    private static String hourSection(String html, String startMarker, String endMarker) {
        int start = blockPosition(html, startMarker, 0, true);
        if (start < 0) return "";
        int end = blockPosition(html, endMarker, start + 1, true);
        if (end < 0) end = html.length();
        return html.substring(start, end);
    }

    private static int blockPosition(String html, String marker, int from, boolean starts) {
        if (html == null || html.isEmpty()) return -1;
        String wanted = normalizeText(marker);
        Matcher matcher = BLOCK.matcher(html);
        if (from > 0) matcher.region(Math.min(from, html.length()), html.length());
        while (matcher.find()) {
            String text = normalizeText(Html.fromHtml(matcher.group()).toString());
            if (text.equals(wanted) || (starts && text.startsWith(wanted))) return matcher.start();
        }
        return -1;
    }

    private static String idSection(String html, String startId, String... endIds) {
        if (html == null || html.isEmpty()) return "";
        int anchor = idPosition(html, startId, 0);
        if (anchor < 0) return "";
        int start = blockStart(html, anchor);
        int end = html.length();
        if (endIds != null) {
            for (String endId : endIds) {
                if (endId == null || endId.isEmpty()) continue;
                int found = idPosition(html, endId, anchor + 1);
                if (found >= 0) end = Math.min(end, blockStart(html, found));
            }
        }
        return html.substring(Math.max(0, start), Math.max(start, end));
    }

    private static String rowsByAnchors(String html, String startId, String endId) {
        if (html == null || html.isEmpty()) return "";
        int startAnchor = idPosition(html, startId, 0);
        if (startAnchor < 0) return "";
        int start = rowStart(html, startAnchor);
        int end;
        if (endId == null) {
            int tableEnd = html.indexOf("</table>", startAnchor);
            end = tableEnd >= 0 ? tableEnd : html.length();
        } else {
            int endAnchor = idPosition(html, endId, startAnchor + 1);
            end = endAnchor >= 0 ? rowStart(html, endAnchor) : html.length();
        }
        return html.substring(Math.max(0, start), Math.max(start, end));
    }

    private static String rowsFromTextToAnchor(String html, String startText, String endId) {
        int start = rowPositionByText(html, startText, 0);
        if (start < 0) return "";
        int anchor = idPosition(html, endId, start + 1);
        int end = anchor >= 0 ? rowStart(html, anchor) : html.length();
        return html.substring(start, Math.max(start, end));
    }

    private static String rowsFromAnchorToText(String html, String startId, String endText) {
        int anchor = idPosition(html, startId, 0);
        if (anchor < 0) return "";
        int start = rowStart(html, anchor);
        int end = rowPositionByText(html, endText, anchor + 1);
        if (end < 0) end = html.length();
        return html.substring(start, Math.max(start, end));
    }

    private static int rowPositionByText(String html, String wantedText, int from) {
        String wanted = normalizeText(wantedText);
        Matcher matcher = ROW.matcher(html);
        if (from > 0) matcher.region(Math.min(from, html.length()), html.length());
        while (matcher.find()) {
            String text = normalizeText(Html.fromHtml(matcher.group()).toString());
            if (text.equals(wanted) || text.startsWith(wanted) || text.contains(wanted)) {
                return matcher.start();
            }
        }
        return -1;
    }

    private static int idPosition(String html, String id, int from) {
        if (html == null || id == null) return -1;
        Pattern pattern = Pattern.compile("(?i)id\\s*=\\s*[\\\"']"
                + Pattern.quote(id) + "[\\\"']");
        Matcher matcher = pattern.matcher(html);
        return matcher.find(Math.max(0, from)) ? matcher.start() : -1;
    }

    private static int blockStart(String html, int position) {
        int result = 0;
        String[] tags = {"<tr", "<h1", "<h2", "<h3", "<h4", "<p", "<div"};
        String lower = html.toLowerCase(Locale.ROOT);
        for (String tag : tags) {
            int found = lower.lastIndexOf(tag, Math.max(0, position));
            if (found > result) result = found;
        }
        return result;
    }

    private static int rowStart(String html, int position) {
        int found = html.toLowerCase(Locale.ROOT).lastIndexOf("<tr", Math.max(0, position));
        return found < 0 ? blockStart(html, position) : found;
    }

    private static String bodyContent(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) {
            start = lower.indexOf('>', start);
            if (start >= 0) start++;
        } else start = 0;
        int end = lower.lastIndexOf("</body>");
        if (end < 0 || end < start) end = html.length();
        return html.substring(start, end);
    }

    private static File findFile(File root, String name) {
        if (root == null || !root.exists()) return null;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(name)) return file;
        }
        for (File file : files) {
            if (!file.isDirectory()) continue;
            File found = findFile(file, name);
            if (found != null) return found;
        }
        return null;
    }

    private static String readRequired(File file) throws Exception {
        if (file == null || !file.isFile()) throw new IllegalStateException("Falta un texto del Misal.");
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String table(String content) {
        if (content == null || content.trim().isEmpty()) return "";
        return "<table class=\"missal-table\">" + content + "</table>";
    }

    private static String section(String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            content = "<p class=\"rubric\">Este bloque no pudo resolverse automáticamente.</p>";
        }
        return "<section class=\"ministerium-section\"><h2>" + escape(title)
                + "</h2>" + content + "</section>";
    }

    private static String clean(String html) {
        if (html == null) return "";
        String value = html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                .replaceAll("(?is)<link\\b[^>]*>", "")
                .replaceAll("(?is)<meta\\b[^>]*>", "")
                .replaceAll("(?is)<a\\s+[^>]*href=[\\\"'][^\\\"']*[\\\"'][^>]*>(.*?)</a>", "$1");
        return value;
    }

    private static String document(Context context, String body, String language) {
        boolean dark = ThemeUtils.isDark(context);
        String background = dark ? "#26211E" : "#FFFDF7";
        String surface = dark ? "#332C28" : "#FFFFFF";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String border = dark ? "#665746" : "#E2D7C7";
        String startLanguage = "lat_es".equals(language) ? "both" : "es";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body{margin:0;background:" + background + ";color:" + ink + ";}"
                + "body{font-family:serif;line-height:1.65;padding:20px 22px 72px;box-sizing:border-box;}"
                + "h2,h3,h4{color:" + wine + ";line-height:1.3}h2{font-size:1.25rem;margin-top:0;}"
                + ".ministerium-section{margin:0 0 22px;padding:18px;border:1px solid " + border
                + ";border-radius:12px;background:" + surface + ";overflow:hidden;}"
                + ".ministerium-section p{margin:.65em 0}.rubric{color:" + muted + ";font-style:italic;}"
                + ".small{font-size:.9rem}.missal-table{width:100%;border-collapse:collapse;table-layout:fixed;}"
                + ".missal-table td{vertical-align:top;padding:5px 8px;overflow-wrap:anywhere;}"
                + ".missal-table .izq{border-right:1px solid " + border + ";}"
                + ".choicebar{display:flex;gap:6px;overflow-x:auto;margin:8px 0 14px;padding-bottom:4px;}"
                + ".choicebar button,.language-button{border:1px solid " + wine + ";background:transparent;color:"
                + wine + ";border-radius:18px;padding:8px 12px;font-weight:bold;white-space:nowrap;}"
                + ".choicebar button.selected{background:" + wine + ";color:" + background + ";}"
                + ".hidden{display:none!important}.inline-language{display:flex;align-items:center;justify-content:space-between;"
                + "gap:12px;margin:12px 0 8px;font-weight:bold;color:" + wine + ";}"
                + ".preface-options{margin:10px 0 18px;padding:10px 12px;border:1px solid " + border
                + ";border-radius:9px}.preface-options summary{cursor:pointer;font-weight:bold;color:" + wine + ";}"
                + ".missing-block{padding:12px;border-left:4px solid " + wine + ";background:" + background + ";}"
                + ".reading-section{margin:18px 0}.reading-reference,.lectionary-label{color:" + wine
                + ";font-weight:bold}.psalm-response{font-weight:bold}.source{color:" + muted + ";font-size:.85rem;}"
                + ".ministerium-canticle{margin:14px 0;padding:14px;border-left:4px solid " + wine
                + ";background:" + background + ";}"
                + "body[data-global-lang='es'] .missal-table .izq{display:none!important;}"
                + "body[data-global-lang='es'] .missal-table .dcha{width:100%!important;border:0!important;}"
                + "#creedBlock[data-lang='es'] .izq,#paterBlock[data-lang='es'] .izq{display:none!important;}"
                + "#creedBlock[data-lang='lat'] .dcha,#paterBlock[data-lang='lat'] .dcha{display:none!important;}"
                + "#creedBlock[data-lang='lat'] .izq,#paterBlock[data-lang='lat'] .izq{display:table-cell!important;"
                + "width:100%!important;border:0!important;}"
                + "#creedBlock[data-lang='both'] .izq,#creedBlock[data-lang='both'] .dcha,"
                + "#paterBlock[data-lang='both'] .izq,#paterBlock[data-lang='both'] .dcha{display:table-cell!important;}"
                + "@media(max-width:620px){body{padding:16px 14px 64px}.ministerium-section{padding:14px}"
                + "body[data-global-lang='both'] .missal-table td{font-size:.94rem;padding:4px;} }"
                + "</style></head><body data-global-lang=\"" + startLanguage + "\">" + body
                + "<script>"
                + "function setCreed(which){var n=document.getElementById('creedNicene'),a=document.getElementById('creedApostles');"
                + "if(!n||!a)return;n.classList.toggle('hidden',which!=='nicene');a.classList.toggle('hidden',which!=='apostles');"
                + "document.getElementById('creedNiceneButton').classList.toggle('selected',which==='nicene');"
                + "document.getElementById('creedApostlesButton').classList.toggle('selected',which==='apostles');}"
                + "function setPrayer(n){for(var i=1;i<=4;i++){var p=document.getElementById('prayer'+i),b=document.getElementById('prayerButton'+i);"
                + "if(p)p.classList.toggle('hidden',i!==n);if(b)b.classList.toggle('selected',i===n);}"
                + "var pref=document.getElementById('prefaceBlock');if(pref)pref.style.display=n===4?'none':'';}"
                + "function cycleBlockLanguage(blockId,buttonId){var block=document.getElementById(blockId),button=document.getElementById(buttonId);if(!block)return;"
                + "var mode=block.getAttribute('data-lang')||document.body.getAttribute('data-global-lang')||'es';"
                + "mode=mode==='es'?'lat':(mode==='lat'?'both':'es');block.setAttribute('data-lang',mode);"
                + "if(button)button.textContent=mode==='es'?'ESP':(mode==='lat'?'LAT':'ESP/LAT');}"
                + "document.addEventListener('DOMContentLoaded',function(){var initial=document.body.getAttribute('data-global-lang')||'es';"
                + "var creed=document.getElementById('creedBlock'),pater=document.getElementById('paterBlock');"
                + "if(creed)creed.setAttribute('data-lang',initial);if(pater)pater.setAttribute('data-lang',initial);"
                + "var cb=document.getElementById('creedLanguageButton'),pb=document.getElementById('paterLanguageButton');"
                + "var label=initial==='both'?'ESP/LAT':(initial==='lat'?'LAT':'ESP');if(cb)cb.textContent=label;if(pb)pb.textContent=label;setPrayer(2);});"
                + "</script></body></html>";
    }

    private static String benedictus() {
        return "<div class=\"ministerium-canticle\"><h3>Benedictus · Lc 1,68-79</h3>"
                + "<p>Bendito sea el Señor, Dios de Israel, porque ha visitado y redimido a su pueblo,<br>"
                + "suscitándonos una fuerza de salvación en la casa de David, su siervo,<br>"
                + "según lo había predicho desde antiguo por boca de sus santos profetas.</p>"
                + "<p>Es la salvación que nos libra de nuestros enemigos y de la mano de todos los que nos odian;<br>"
                + "realizando la misericordia que tuvo con nuestros padres, recordando su santa alianza<br>"
                + "y el juramento que juró a nuestro padre Abrahán.</p>"
                + "<p>Para concedernos que, libres de temor, arrancados de la mano de los enemigos,<br>"
                + "le sirvamos con santidad y justicia, en su presencia, todos nuestros días.</p>"
                + "<p>Y a ti, niño, te llamarán profeta del Altísimo, porque irás delante del Señor a preparar sus caminos,<br>"
                + "anunciando a su pueblo la salvación, el perdón de sus pecados.</p>"
                + "<p>Por la entrañable misericordia de nuestro Dios, nos visitará el sol que nace de lo alto,<br>"
                + "para iluminar a los que viven en tiniebla y en sombra de muerte,<br>"
                + "para guiar nuestros pasos por el camino de la paz.</p>"
                + doxology() + "</div>";
    }

    private static String magnificat() {
        return "<div class=\"ministerium-canticle\"><h3>Magníficat · Lc 1,46-55</h3>"
                + "<p>Proclama mi alma la grandeza del Señor, se alegra mi espíritu en Dios, mi salvador;<br>"
                + "porque ha mirado la humillación de su esclava.</p>"
                + "<p>Desde ahora me felicitarán todas las generaciones, porque el Poderoso ha hecho obras grandes por mí: su nombre es santo,<br>"
                + "y su misericordia llega a sus fieles de generación en generación.</p>"
                + "<p>Él hace proezas con su brazo: dispersa a los soberbios de corazón,<br>"
                + "derriba del trono a los poderosos y enaltece a los humildes,<br>"
                + "a los hambrientos los colma de bienes y a los ricos los despide vacíos.</p>"
                + "<p>Auxilia a Israel, su siervo, acordándose de la misericordia<br>"
                + "—como lo había prometido a nuestros padres— en favor de Abrahán y su descendencia por siempre.</p>"
                + doxology() + "</div>";
    }

    private static String doxology() {
        return "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p>";
    }

    private static String roman(int value) {
        return value == 1 ? "I" : value == 2 ? "II" : value == 3 ? "III" : "IV";
    }

    private static String normalizeText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
