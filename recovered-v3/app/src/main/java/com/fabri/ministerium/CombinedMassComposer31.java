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

/** Continuous Mass + Lauds/Vespers celebration according to OGLH 93–96. */
public final class CombinedMassComposer31 {
    private static final Pattern BLOCK = Pattern.compile(
            "(?is)<(p|h1|h2|h3|h4|h5|h6)\\b[^>]*>.*?</\\1>");

    private CombinedMassComposer31() {}

    public static CombinedMassComposer.Result compose(Context context, Calendar selectedDate,
                                                       String hourKey, String language)
            throws Exception {
        Calendar date = (Calendar) selectedDate.clone();
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        HourEntry hour = findHour(context, day, date, hourKey);
        if (hour == null) throw new IllegalStateException("No se encontró " + hourName(hourKey) + " para esta fecha.");
        if (!LiturgiaPapalMissalRepository.isAvailable(context, "es")) {
            throw new IllegalStateException("Falta el contenido local del Misal Romano.");
        }

        String hourHtml = composeHour(context, day, date, hour);
        String psalmody = section(hourHtml, "SALMODIA", "LECTURA BREVE");
        String canticle = section(hourHtml, "CÁNTICO EVANGÉLICO", "PRECES");
        canticle = addCanticleIfNeeded(canticle, hourKey);
        String intercessions = cleanIntercessions(section(hourHtml, "PRECES", "ORACIÓN"));

        LiturgicalEvent primary = primaryEvent(context, date);
        boolean gloria = gloriaRequired(day, primary, date);
        boolean creed = creedRequired(primary, date);
        boolean requiredSaint = hasRequiredSaint(day);
        boolean ordinary = isOrdinary(day) && !requiredSaint;

        ProperParts proper = dailyProper(context, date);
        if (!proper.complete && ordinary) proper = ordinaryProper(context, date);
        if (!proper.complete) proper = ProperParts.missing(day.celebration);

        String start = proper.entrance + openingRite(context);
        String massStart = LiturgiaPapalMissalRepository.initialMassHtml(context, gloria);
        String readings = readings(context, date);
        String preparation = LiturgiaPapalMissalRepository.preparationHtml(context, "es")
                + proper.offerings;
        String preface = prefaceBlock(context, date, day, primary, ordinary);
        boolean properPrefaceRequired = properPrefaceRequired(day, primary, ordinary);
        String prayers = LiturgiaPapalMissalRepository.eucharisticPrayersHtml(
                context, properPrefaceRequired);
        String communion = LiturgiaPapalMissalRepository.communionHtml(
                context, "es", proper.communionAntiphon);
        String conclusion = LiturgiaPapalMissalRepository.conclusionHtml(context, "es");

        StringBuilder body = new StringBuilder(52000);
        body.append(sectionHtml("combined:opening", "Inicio de la celebración", start));
        body.append(sectionHtml("combined:psalmody", "Salmodia de " + hourName(hourKey), psalmody));
        body.append(sectionHtml("combined:kyrie-gloria", "Kyrie y Gloria", massStart));
        body.append(sectionHtml("combined:collect", "Oración colecta", proper.collect));
        body.append(sectionHtml("combined:word", "Liturgia de la Palabra", readings));
        body.append("<section class=\"ministerium-section\" data-semantic-id=\"combined:homily\">"
                + "<h2>Homilía</h2></section>");

        if (creed) body.append(creedBlock(context));

        body.append(sectionHtml("combined:intercessions", "Preces de " + hourName(hourKey),
                intercessions.isEmpty()
                        ? "<p class=\"rubric\">Se hacen las preces de la Hora como oración universal.</p>"
                        : intercessions));
        body.append(sectionHtml("combined:eucharist", "Liturgia eucarística",
                preparation + preface + prayers));
        body.append(sectionHtml("combined:communion", "Rito de la Comunión", communion));
        body.append(sectionHtml("combined:gospel-canticle",
                "Cántico evangélico de " + hourName(hourKey), canticle));
        body.append(sectionHtml("combined:post-communion",
                "Oración después de la Comunión", proper.postCommunion));
        body.append(sectionHtml("combined:conclusion", "Rito de conclusión", conclusion));

        String title = "Misa + " + hourName(hourKey);
        String html = document(context, body.toString());
        return new CombinedMassComposer.Result(title,
                day.celebration + " · " + day.dateLabel, html);
    }

    private static ProperParts dailyProper(Context context, Calendar date) {
        try {
            DailyMassProperRepository.ProperDay day = DailyMassProperRepository.cached(context, date);
            if (day == null || !day.isComplete()) return ProperParts.missing("Propio del día");
            return new ProperParts(
                    DailyMassProperRepository.render(day.entrance),
                    DailyMassProperRepository.render(day.collect),
                    DailyMassProperRepository.render(day.offerings),
                    DailyMassProperRepository.render(day.communionAntiphon),
                    DailyMassProperRepository.render(day.postCommunion), true);
        } catch (Exception ignored) {
            return ProperParts.missing("Propio del día");
        }
    }

    private static ProperParts ordinaryProper(Context context, Calendar date) {
        try {
            String entrance = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(
                    context, date, LiturgiaPapalMissalRepository.ENTRANCE);
            String collect = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(
                    context, date, LiturgiaPapalMissalRepository.COLLECT);
            String offerings = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(
                    context, date, LiturgiaPapalMissalRepository.OFFERINGS);
            String communion = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(
                    context, date, LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON);
            String post = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(
                    context, date, LiturgiaPapalMissalRepository.POST_COMMUNION);
            boolean complete = !entrance.isEmpty() && !collect.isEmpty()
                    && !offerings.isEmpty() && !post.isEmpty();
            if (complete) return new ProperParts(entrance, collect, offerings, communion, post, true);
        } catch (Exception ignored) {}
        return ProperParts.missing("Tiempo Ordinario");
    }

    private static String prefaceBlock(Context context, Calendar date, LiturgicalDay day,
                                       LiturgicalEvent primary, boolean ordinary) {
        try {
            String dialogue = LiturgiaPapalMissalRepository.prefaceDialogueHtml(context, "es");
            String options = "";
            if (ordinary && date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                options = LiturgiaPapalMissalRepository.ordinarySundayPrefacesHtml(context);
            }
            if (options.isEmpty()) {
                options = "<p class=\"rubric\">" + escape(prefaceHint(day, primary)) + "</p>";
            }
            return "<div id=\"prefaceBlock\" data-semantic-id=\"mass:preface\"><h3>Prefacio</h3>"
                    + dialogue + options + "</div>";
        } catch (Exception error) {
            return "<p class=\"rubric\">Prefacio según el formulario de la celebración.</p>";
        }
    }

    private static String prefaceHint(LiturgicalDay day, LiturgicalEvent primary) {
        String normalized = normalize(day == null ? "" : day.celebration);
        if (normalized.contains("apostol")) return "Corresponde el prefacio de los Apóstoles.";
        if (normalized.contains("martir")) return "Corresponde el prefacio indicado para los mártires.";
        if (primary != null && primary.isSolemnity()) return "Corresponde el prefacio propio de la solemnidad.";
        if (primary != null && primary.isFeast()) return "Corresponde el prefacio propio de la fiesta.";
        return "Prefacio según el formulario del día.";
    }

    private static boolean properPrefaceRequired(LiturgicalDay day, LiturgicalEvent primary,
                                                 boolean ordinary) {
        if (primary != null && (primary.isFeast() || primary.isSolemnity())) return true;
        if (!ordinary && day != null && day.temporalOffice != null
                && day.temporalOffice.volume != null) {
            String id = day.temporalOffice.volume.id;
            return "lent".equals(id) || "easter".equals(id)
                    || "advent".equals(id) || "christmas".equals(id);
        }
        return false;
    }

    private static String creedBlock(Context context) {
        try {
            return "<section class=\"ministerium-section\" data-semantic-id=\"mass:creed\" id=\"creedBlock\">"
                    + "<h2>Profesión de fe</h2>"
                    + LiturgiaPapalWordRepository.professionOfFaithHtml(context, "es")
                    + "</section>";
        } catch (Exception error) {
            return sectionHtml("mass:creed", "Profesión de fe",
                    "<p class=\"rubric\">Se hace la profesión de fe.</p>");
        }
    }

    private static String openingRite(Context context) {
        try {
            String[] lines = LiturgiaPapalMissalRepository.component(context, "es", "initial")
                    .split("\\r?\\n");
            int sign = findLine(lines, "en el nombre del padre", 0);
            int amen = findLine(lines, "amen", sign + 1);
            int greeting = findLine(lines, "el senor este con ustedes", amen + 1);
            int response = findLine(lines, "y con tu espiritu", greeting + 1);
            if (sign >= 0 && amen > sign && greeting > amen && response > greeting) {
                return "<div class=\"liturgia-papal opening-rite\" data-missal-source=\"liturgia-papal-mexico\">"
                        + "<p class=\"rubric\">Terminado el canto de entrada, todos se santiguan.</p>"
                        + "<p><b>" + escape(lines[sign].trim()) + "</b></p>"
                        + "<p><b>" + escape(lines[amen].trim()) + "</b></p>"
                        + "<p><b>" + escape(lines[greeting].trim()) + "</b></p>"
                        + "<p><b>" + escape(lines[response].trim()) + "</b></p></div>";
            }
        } catch (Exception ignored) {}
        return "<p class=\"rubric\">Se realizan los ritos iniciales.</p>";
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
            MemoryOffice memory = SaintOfficeRepository.compose(context, hour, proper, common,
                    LiturgicalResolver.ordinaryWeekNumber(date),
                    LiturgicalResolver.lectionaryCycle(date),
                    date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
            if (memory != null && memory.html != null && !memory.html.trim().isEmpty()) {
                return memory.html;
            }
        }
        File root = EpubUtils.ensureExtracted(context, hour.volume);
        String resolved = null;
        if ("ordinary".equals(hour.volume.id)) {
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
            if (ordinaryWeek > 0) {
                resolved = OrdinaryReferenceResolver.resolve(root, hour.filePath,
                        ordinaryWeek, LiturgicalResolver.lectionaryCycle(date),
                        date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
            }
        }
        if (resolved != null && !resolved.trim().isEmpty()) return resolved;
        return readRequired(new File(root, hour.filePath));
    }

    private static String readings(Context context, Calendar date) {
        try {
            if (MassReadingsRepository.has(context, date)) {
                return bodyContent(MassReadingsRepository.read(context, date));
            }
        } catch (Exception ignored) {}
        return "<p class=\"rubric\"><b>Lecturas no guardadas.</b> Sincroniza el Leccionario desde Ajustes → Actualizaciones.</p>";
    }

    private static LiturgicalEvent primaryEvent(Context context, Calendar date) {
        try {
            return LiturgicalResolver.primaryEvent(
                    LiturgicalCalendarRepository.eventsFor(context, date));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean gloriaRequired(LiturgicalDay day, LiturgicalEvent primary,
                                           Calendar date) {
        if (primary != null && (primary.isFeast() || primary.isSolemnity())) return true;
        if (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return false;
        if (day == null || day.temporalOffice == null || day.temporalOffice.volume == null) return true;
        String season = day.temporalOffice.volume.id;
        return !"advent".equals(season) && !"lent".equals(season);
    }

    private static boolean creedRequired(LiturgicalEvent primary, Calendar date) {
        return date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                || (primary != null && primary.isSolemnity());
    }

    private static boolean isOrdinary(LiturgicalDay day) {
        return day != null && day.temporalOffice != null && day.temporalOffice.volume != null
                && "ordinary".equals(day.temporalOffice.volume.id);
    }

    private static boolean hasRequiredSaint(LiturgicalDay day) {
        if (day == null || day.saintOffices == null) return false;
        for (HoursLink office : day.saintOffices) {
            if (office != null && office.requiresProperOffice()) return true;
        }
        return false;
    }

    private static String section(String html, String startMarker, String endMarker) {
        int start = blockPosition(html, startMarker, 0, true);
        if (start < 0) return "";
        int end = blockPosition(html, endMarker, start + 1, true);
        if (end < 0) end = html.length();
        return cleanLinks(html.substring(start, end));
    }

    private static int blockPosition(String html, String marker, int from, boolean starts) {
        if (html == null || html.isEmpty()) return -1;
        String wanted = normalize(marker);
        Matcher matcher = BLOCK.matcher(html);
        if (from > 0) matcher.region(Math.min(from, html.length()), html.length());
        while (matcher.find()) {
            String text = normalize(Html.fromHtml(matcher.group()).toString());
            if (text.equals(wanted) || (starts && text.startsWith(wanted))) return matcher.start();
        }
        return -1;
    }

    private static String cleanIntercessions(String html) {
        if (html == null || html.isEmpty()) return "";
        String value = html.replaceAll("(?is)<p\\b[^>]*>.*?(?:Padre nuestro|oración conclusiva).*?</p>", "");
        return value.replaceAll("(?is)<p\\b[^>]*>\\s*Contenido pendiente de incorporar\\.?\\s*</p>", "").trim();
    }

    private static String addCanticleIfNeeded(String section, String hourKey) {
        String plain = normalize(Html.fromHtml(section == null ? "" : section).toString());
        boolean vespers = "vespers".equals(hourKey);
        if (vespers && plain.contains("proclama mi alma la grandeza del senor")) return section;
        if (!vespers && plain.contains("bendito sea el senor dios de israel")) return section;
        return (section == null ? "" : section) + (vespers ? magnificat() : benedictus());
    }

    private static String benedictus() {
        return "<div class=\"ministerium-canticle\"><h3>Benedictus · Lc 1,68-79</h3>"
                + "<p>Bendito sea el Señor, Dios de Israel, porque ha visitado y redimido a su pueblo; "
                + "suscitándonos una fuerza de salvación en la casa de David, su siervo.</p>"
                + "<p>Por la entrañable misericordia de nuestro Dios, nos visitará el sol que nace de lo alto, "
                + "para iluminar a los que viven en tiniebla y en sombra de muerte, para guiar nuestros pasos por el camino de la paz.</p></div>";
    }

    private static String magnificat() {
        return "<div class=\"ministerium-canticle\"><h3>Magníficat · Lc 1,46-55</h3>"
                + "<p>Proclama mi alma la grandeza del Señor, se alegra mi espíritu en Dios, mi salvador.</p></div>";
    }

    private static String sectionHtml(String id, String title, String content) {
        String value = content == null || content.trim().isEmpty()
                ? "<p class=\"rubric\">No se encontró un texto adicional para esta parte.</p>"
                : content;
        return "<section class=\"ministerium-section\" data-semantic-id=\"" + escape(id) + "\">"
                + "<h2>" + escape(title) + "</h2>" + value + "</section>";
    }

    private static String document(Context context, String body) {
        boolean dark = ThemeUtils.isDark(context);
        String background = dark ? "#26211E" : "#FFFDF7";
        String surface = dark ? "#332C28" : "#FFFFFF";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String border = dark ? "#665746" : "#E2D7C7";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body{margin:0;background:" + background + ";color:" + ink + ";}"
                + "body{font-family:serif;line-height:1.65;padding:18px 16px 72px;box-sizing:border-box;max-width:980px;margin:auto;}"
                + ".ministerium-section{margin:0 0 18px;padding:16px;border:1px solid " + border + ";border-radius:12px;background:" + surface + ";overflow:hidden;}"
                + "h2,h3,h4{color:" + wine + ";line-height:1.3}.rubric{color:" + muted + ";font-style:italic;}"
                + ".source-warning{padding:10px 12px;border-left:3px solid " + wine + ";background:" + background + ";color:" + muted + ";}"
                + ".choicebar{display:flex;gap:7px;overflow-x:auto;margin:8px 0 13px}.choicebar button{min-height:42px;border:1px solid " + wine + ";background:transparent;color:" + wine + ";border-radius:18px;padding:8px 12px;font-weight:bold;}"
                + ".choicebar button.selected{background:" + wine + ";color:" + background + ";}.hidden{display:none!important;}"
                + ".liturgia-papal p{margin:.7em 0}.ministerium-canticle{padding:10px;border-left:3px solid " + wine + ";}"
                + "@media(max-width:599px){body{padding:10px 8px 56px;font-size:17px}.ministerium-section{padding:12px;border-radius:8px}.choicebar button{min-height:44px}}"
                + "@media(min-width:600px){body{padding:24px 28px 80px;font-size:20px}.ministerium-section{padding:20px 24px}}"
                + "</style></head><body>" + body
                + "<script>function setCreed(w){var n=document.getElementById('ministeriumCredoNicene'),a=document.getElementById('ministeriumCredoApostles');"
                + "var nb=document.getElementById('ministeriumCredoNiceneButton'),ab=document.getElementById('ministeriumCredoApostlesButton');var x=w==='nicene';"
                + "if(n)n.hidden=!x;if(a)a.hidden=x;if(nb)nb.classList.toggle('selected',x);if(ab)ab.classList.toggle('selected',!x);}" 
                + "function setPrayer(n){for(var i=1;i<=4;i++){var p=document.getElementById('prayer'+i),b=document.getElementById('prayerButton'+i);var x=i===n;if(p){p.hidden=!x;p.classList.toggle('hidden',!x);}if(b&&!b.disabled){b.classList.toggle('selected',x);b.setAttribute('aria-pressed',x?'true':'false');}}var pref=document.getElementById('prefaceBlock');if(pref)pref.style.display=n===4?'none':'';}"
                + "</script></body></html>";
    }

    private static String cleanLinks(String value) {
        return value == null ? "" : value.replaceAll(
                "(?is)<a\\s+[^>]*href=[\\\"'][^\\\"']*[\\\"'][^>]*>(.*?)</a>", "$1");
    }

    private static String bodyContent(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) {
            start = lower.indexOf('>', start);
            start = start < 0 ? 0 : start + 1;
        } else start = 0;
        int end = lower.lastIndexOf("</body>");
        if (end < start) end = html.length();
        return html.substring(start, end);
    }

    private static String readRequired(File file) throws Exception {
        if (file == null || !file.isFile()) throw new IllegalStateException("Falta el texto de la Hora.");
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static int findLine(String[] lines, String marker, int from) {
        if (from < 0) return -1;
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wanted) || value.startsWith(wanted)) return i;
        }
        return -1;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String hourName(String key) {
        return "vespers".equals(key) ? "Vísperas" : "Laudes";
    }

    private static final class ProperParts {
        final String entrance;
        final String collect;
        final String offerings;
        final String communionAntiphon;
        final String postCommunion;
        final boolean complete;

        ProperParts(String entrance, String collect, String offerings,
                    String communionAntiphon, String postCommunion, boolean complete) {
            this.entrance = entrance;
            this.collect = collect;
            this.offerings = offerings;
            this.communionAntiphon = communionAntiphon;
            this.postCommunion = postCommunion;
            this.complete = complete;
        }

        static ProperParts missing(String celebration) {
            String note = "<p class=\"source-warning\"><b>" + escape(celebration)
                    + ".</b> Texto propio no disponible todavía en el paquete local.</p>";
            return new ProperParts(note, note, note, "", note, false);
        }
    }
}
