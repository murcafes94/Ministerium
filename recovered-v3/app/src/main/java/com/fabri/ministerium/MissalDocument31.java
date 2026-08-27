package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;

/**
 * Stand-alone Missal document. Spanish and Latin are separate reading modes;
 * the old side-by-side Missal is intentionally no longer generated.
 */
public final class MissalDocument31 {
    public static final class Result {
        public final String title;
        public final String subtitle;
        public final String html;
        Result(String title, String subtitle, String html) {
            this.title = title;
            this.subtitle = subtitle;
            this.html = html;
        }
    }

    private MissalDocument31() {}

    public static Result build(Context context, Calendar date, String section, String language)
            throws Exception {
        String lang = "la".equals(language) ? "la" : "es";
        if (!LiturgiaPapalMissalRepository.isAvailable(context, lang)) {
            throw new IllegalStateException("Falta el contenido local del Misal Romano en "
                    + ("la".equals(lang) ? "latín" : "español") + ".");
        }
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        String key = section == null ? "day" : section;
        String content;
        String title;

        switch (key) {
            case "initial":
                title = "la".equals(lang) ? "Ritus initiales" : "Ritos iniciales";
                content = component(context, "initial", lang);
                break;
            case "word":
                title = "la".equals(lang) ? "Liturgia Verbi" : "Liturgia de la Palabra";
                content = word(context, date, day, lang);
                break;
            case "collect":
                title = "la".equals(lang) ? "Oratio collecta" : "Oración colecta";
                content = proper(context, date, day, LiturgiaPapalMissalRepository.COLLECT, lang);
                break;
            case "eucharist":
                title = "la".equals(lang) ? "Liturgia eucharistica" : "Liturgia eucarística";
                content = eucharistic(context, date, day, lang);
                break;
            case "offerings":
                title = "la".equals(lang) ? "Oratio super oblata" : "Oración sobre las ofrendas";
                content = proper(context, date, day, LiturgiaPapalMissalRepository.OFFERINGS, lang);
                break;
            case "prefaces":
                title = "la".equals(lang) ? "Præfationes" : "Prefacios";
                content = component(context, "prefaces", lang);
                break;
            case "prayers":
                title = "la".equals(lang) ? "Preces eucharisticæ" : "Plegarias eucarísticas";
                content = prayers(context, lang, false);
                break;
            case "communion":
                title = "la".equals(lang) ? "Ritus Communionis" : "Rito de la Comunión";
                content = communion(context, date, day, lang);
                break;
            case "communion_antiphon":
                title = "la".equals(lang) ? "Antiphona ad Communionem" : "Antífona de comunión";
                content = proper(context, date, day,
                        LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON, lang);
                break;
            case "post_communion":
                title = "la".equals(lang) ? "Oratio post Communionem" : "Oración después de la Comunión";
                content = proper(context, date, day,
                        LiturgiaPapalMissalRepository.POST_COMMUNION, lang);
                break;
            case "conclusion":
                title = "la".equals(lang) ? "Ritus conclusionis" : "Rito de conclusión";
                content = component(context, "conclusion", lang);
                break;
            case "commons":
            case "needs":
            case "votive":
            case "dead":
            case "saints":
                title = label(key);
                content = MissalReferenceCatalog.groupHtml(context, key, date, day);
                break;
            case "day":
            default:
                title = "la".equals(lang) ? "Missa diei" : "Misa del día";
                content = daily(context, date, day, lang);
                break;
        }
        String languageLabel = "la".equals(lang) ? "Latín" : "Español";
        return new Result(title, day.celebration + " · " + day.dateLabel + " · " + languageLabel,
                document(context, title, content, lang));
    }

    /** Correct liturgical order: readings precede homily and profession of faith. */
    private static String daily(Context context, Calendar date, LiturgicalDay day, String lang)
            throws Exception {
        StringBuilder out = new StringBuilder(64000);
        String entrance = proper(context, date, day, LiturgiaPapalMissalRepository.ENTRANCE, lang);
        if (!isPending(entrance)) out.append(block("la".equals(lang)
                ? "Antiphona ad introitum" : "Antífona de entrada", entrance, "mass:entrance"));
        out.append(block("la".equals(lang) ? "Ritus initiales" : "Ritos iniciales",
                component(context, "initial", lang), "mass:initial"));
        out.append(block("la".equals(lang) ? "Oratio collecta" : "Oración colecta",
                proper(context, date, day, LiturgiaPapalMissalRepository.COLLECT, lang), "mass:collect"));
        out.append(block("la".equals(lang) ? "Liturgia Verbi" : "Liturgia de la Palabra",
                word(context, date, day, lang), "mass:word"));
        out.append(block("la".equals(lang) ? "Liturgia eucharistica" : "Liturgia eucarística",
                eucharistic(context, date, day, lang), "mass:eucharist"));
        out.append(block("la".equals(lang) ? "Ritus Communionis" : "Rito de la Comunión",
                communion(context, date, day, lang), "mass:communion"));
        out.append(block("la".equals(lang) ? "Oratio post Communionem" : "Oración después de la Comunión",
                proper(context, date, day, LiturgiaPapalMissalRepository.POST_COMMUNION, lang),
                "mass:post-communion"));
        out.append(block("la".equals(lang) ? "Ritus conclusionis" : "Rito de conclusión",
                LiturgiaPapalMissalRepository.conclusionHtml(context, lang), "mass:conclusion"));
        return out.toString();
    }

    private static String word(Context context, Calendar date, LiturgicalDay day, String lang)
            throws Exception {
        StringBuilder out = new StringBuilder(18000);
        out.append("<div class=\"lectionary-insert\" data-semantic-id=\"mass:readings\"><h3>")
                .append("la".equals(lang) ? "Lectiones diei" : "Lecturas del día")
                .append("</h3>").append(readings(context, date)).append("</div>");
        out.append("<section class=\"missal-inline-section\" data-semantic-id=\"mass:homily\"><h3>")
                .append("la".equals(lang) ? "Homilia" : "Homilía").append("</h3></section>");
        if (creedRequired(context, date, day)) {
            out.append("<section class=\"missal-inline-section\" data-semantic-id=\"mass:creed\"><h3>")
                    .append("la".equals(lang) ? "Professio fidei" : "Profesión de fe")
                    .append("</h3>")
                    .append(LiturgiaPapalWordRepository.professionOfFaithHtml(context, lang))
                    .append("</section>");
        }
        out.append("<section class=\"missal-inline-section\" data-semantic-id=\"mass:universal-prayer\"><h3>")
                .append("la".equals(lang) ? "Oratio universalis" : "Oración universal")
                .append("</h3><p class=\"rubric\">")
                .append("la".equals(lang)
                        ? "Deinde fit oratio universalis, seu oratio fidelium."
                        : "Después se hace la oración universal u oración de los fieles.")
                .append("</p></section>");
        return out.toString();
    }

    private static String readings(Context context, Calendar date) {
        try {
            if (MassReadingsRepository.has(context, date)) {
                return body(MassReadingsRepository.read(context, date));
            }
        } catch (Exception ignored) {}
        return pending("Las lecturas de esta fecha no están guardadas. Sincroniza el Leccionario desde Ajustes → Actualizaciones.");
    }

    private static String eucharistic(Context context, Calendar date, LiturgicalDay day,
                                      String lang) throws Exception {
        StringBuilder out = new StringBuilder(36000);
        out.append(LiturgiaPapalMissalRepository.preparationHtml(context, lang));
        out.append("<section class=\"missal-inline-section\" data-semantic-id=\"mass:offerings\"><h3>")
                .append("la".equals(lang) ? "Oratio super oblata" : "Oración sobre las ofrendas")
                .append("</h3>")
                .append(proper(context, date, day, LiturgiaPapalMissalRepository.OFFERINGS, lang))
                .append("</section>");
        out.append("<section class=\"missal-inline-section\" data-semantic-id=\"mass:preface\"><h3>")
                .append("la".equals(lang) ? "Præfatio" : "Prefacio").append("</h3>")
                .append(LiturgiaPapalMissalRepository.prefaceDialogueHtml(context, lang));

        boolean properPrefaceRequired = false;
        if (isOrdinary(day) && !hasRequiredSaint(day)
                && date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && "es".equals(lang)) {
            String pref = LiturgiaPapalMissalRepository.ordinarySundayPrefacesHtml(context);
            if (!pref.isEmpty()) out.append(pref);
        } else {
            MissalReferenceCatalog.SaintReference saint = MissalReferenceCatalog.findSaint(
                    context, date, day == null ? "" : day.celebration);
            if (saint != null && !saint.preface.isEmpty()) {
                properPrefaceRequired = true;
                out.append("<p class=\"rubric preface-hint\">")
                        .append("la".equals(lang) ? "Præfatio propria: " : "Prefacio correspondiente: ")
                        .append(escape(saint.preface)).append(".</p>");
            }
        }
        out.append("</section>");
        out.append(prayers(context, lang, properPrefaceRequired));
        return out.toString();
    }

    /** Exactly one Eucharistic Prayer is visible; ES and Latin use their own source file. */
    private static String prayers(Context context, String lang,
                                  boolean properPrefaceRequired) throws Exception {
        if ("es".equals(lang)) {
            return LiturgiaPapalMissalRepository.eucharisticPrayersHtml(context,
                    properPrefaceRequired);
        }
        StringBuilder buttons = new StringBuilder();
        StringBuilder bodies = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            boolean disabled = i == 4 && properPrefaceRequired;
            buttons.append("<button type=\"button\" id=\"prayerButton").append(i).append("\"")
                    .append(disabled ? " disabled aria-disabled=\"true\""
                            : " onclick=\"setPrayer(" + i + ")\"")
                    .append(i == 2 ? " class=\"selected\" aria-pressed=\"true\""
                            : " aria-pressed=\"false\"")
                    .append(">").append(roman(i)).append("</button>");
            bodies.append("<div id=\"prayer").append(i).append("\" class=\"eucharistic-prayer")
                    .append(i == 2 ? "\"" : " hidden\"")
                    .append("><h4>Prex Eucharistica ").append(roman(i)).append("</h4>")
                    .append(LiturgiaPapalMissalRepository.eucharisticPrayerHtml(context, "la", i))
                    .append("</div>");
        }
        return "<div class=\"eucharistic-prayers\"><h3>Prex Eucharistica</h3>"
                + "<div class=\"choicebar prayer-choicebar\">" + buttons + "</div>"
                + bodies + "</div>";
    }

    private static String communion(Context context, Calendar date, LiturgicalDay day,
                                    String lang) throws Exception {
        String antiphon = proper(context, date, day,
                LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON, lang);
        return LiturgiaPapalMissalRepository.communionHtml(context, lang, antiphon);
    }

    /** Daily GDL propers take precedence; the ordinary package is the offline fallback. */
    private static String proper(Context context, Calendar date, LiturgicalDay day,
                                 String part, String lang) {
        DailyMassProperRepository.ProperDay daily = DailyMassProperRepository.cached(context, date);
        String text = dailyText(daily, part);
        if (!text.isEmpty()) {
            String rendered = DailyMassProperRepository.render(text);
            if ("la".equals(lang)) {
                return "<div class=\"proper-language-note\">Propio del día · fuente española</div>" + rendered;
            }
            return rendered;
        }
        if (isOrdinary(day) && !hasRequiredSaint(day)) {
            try {
                String value = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(context, date, part);
                if (!value.isEmpty()) return value;
            } catch (Exception ignored) {}
        }
        return pending("Texto propio de «" + escape(day == null ? "esta celebración" : day.celebration)
                + "» todavía no está guardado para esta fecha.");
    }

    private static String dailyText(DailyMassProperRepository.ProperDay day, String part) {
        if (day == null) return "";
        if (LiturgiaPapalMissalRepository.ENTRANCE.equals(part)) return day.entrance;
        if (LiturgiaPapalMissalRepository.COLLECT.equals(part)) return day.collect;
        if (LiturgiaPapalMissalRepository.OFFERINGS.equals(part)) return day.offerings;
        if (LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON.equals(part)) return day.communionAntiphon;
        if (LiturgiaPapalMissalRepository.POST_COMMUNION.equals(part)) return day.postCommunion;
        return "";
    }

    private static String component(Context context, String id, String lang) throws Exception {
        return render(LiturgiaPapalMissalRepository.component(context, lang, id));
    }

    private static boolean creedRequired(Context context, Calendar date, LiturgicalDay day) {
        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true;
        MissalReferenceCatalog.SaintReference saint = MissalReferenceCatalog.findSaint(
                context, date, day == null ? "" : day.celebration);
        if (saint != null && saint.creed) return true;
        try {
            LiturgicalEvent primary = LiturgicalResolver.primaryEvent(
                    LiturgicalCalendarRepository.eventsFor(context, date));
            return primary != null && primary.isSolemnity();
        } catch (Exception ignored) {
            return false;
        }
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

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder("<div class=\"liturgia-papal\">");
        for (String item : blocks) {
            String value = escape(item.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String block(String title, String content, String semanticId) {
        return "<section class=\"ministerium-section\" data-semantic-id=\"" + escape(semanticId)
                + "\"><h2>" + escape(title) + "</h2>" + content + "</section>";
    }

    private static String pending(String message) {
        return "<div class=\"pending\">" + message + "</div>";
    }

    private static boolean isPending(String html) {
        return html != null && html.contains("class=\"pending\"");
    }

    private static String body(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase(java.util.Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) {
            start = lower.indexOf('>', start);
            start = start < 0 ? 0 : start + 1;
        } else start = 0;
        int end = lower.lastIndexOf("</body>");
        if (end < start) end = html.length();
        return html.substring(start, end);
    }

    private static String document(Context context, String title, String content, String lang) {
        boolean dark = ThemeUtils.isDark(context);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String surface = dark ? "#332C28" : "#FFFFFF";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String border = dark ? "#665746" : "#E2D8CB";
        return "<!doctype html><html lang=\"" + lang + "\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=3\">"
                + "<style>html,body{margin:0;padding:0;background:" + bg + ";color:" + ink + ";}"
                + "body{font-family:Georgia,serif;font-size:18px;line-height:1.58;padding:18px 20px 54px;}"
                + ".document{max-width:900px;margin:0 auto;background:" + surface + ";padding:18px 22px;border-radius:10px;}"
                + "h1,h2,h3,h4{color:" + wine + ";line-height:1.25}h1{font-size:1.55em;margin:.2em 0 .9em}"
                + "h2{font-size:1.3em;margin:1.25em 0 .55em;border-bottom:1px solid " + border + ";padding-bottom:.28em}"
                + "h3{font-size:1.14em;margin:1.05em 0 .45em}p{margin:.68em 0}.rubric{color:" + muted + ";font-style:italic}"
                + ".ministerium-section{scroll-margin-top:12px}.missal-inline-section{margin:1em 0}"
                + ".pending{border-left:3px solid " + wine + ";padding:9px 12px;color:" + muted + ";background:rgba(128,128,128,.08)}"
                + ".daily-proper{border-left:3px solid " + wine + ";padding-left:12px}.proper-language-note{font:700 .76em sans-serif;color:" + muted + ";text-transform:uppercase;letter-spacing:.04em;margin:.5em 0}"
                + ".choicebar{display:flex;flex-wrap:wrap;gap:8px;margin:10px 0 16px}.choicebar button{min-height:42px;border:1px solid currentColor;border-radius:20px;background:transparent;color:inherit;padding:7px 14px;font:inherit}.choicebar button.selected{font-weight:700;outline:2px solid currentColor;outline-offset:1px}"
                + ".eucharistic-prayer[hidden],[hidden]{display:none!important}.lectionary-insert article>h1,.lectionary-insert .source{display:none!important}"
                + ".reading-section{margin:1.2em 0}.reading-summary{font-style:italic;font-weight:700}.reading-reference{font-weight:700}.psalm-response{font-weight:700}"
                + "@media(max-width:599px){body{font-size:17px;padding:10px 8px 42px}.document{padding:14px 14px;border-radius:0}.choicebar button{min-height:44px;flex:1 1 auto}}"
                + "@media(min-width:600px){body{font-size:20px;padding:22px 28px 70px}.document{padding:24px 34px}}"
                + "</style></head><body><main class=\"document\"><h1>" + escape(title) + "</h1>"
                + content + "</main></body></html>";
    }

    private static String label(String key) {
        if ("commons".equals(key)) return "Comunes";
        if ("needs".equals(key)) return "Por diversas necesidades";
        if ("votive".equals(key)) return "Misas votivas";
        if ("dead".equals(key)) return "Misas de difuntos";
        if ("saints".equals(key)) return "Propio de los santos";
        return "Misal Romano";
    }

    private static String roman(int value) {
        int[] numbers = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = value;
        for (int i = 0; i < numbers.length; i++) {
            while (remaining >= numbers[i]) {
                result.append(symbols[i]);
                remaining -= numbers[i];
            }
        }
        return result.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
