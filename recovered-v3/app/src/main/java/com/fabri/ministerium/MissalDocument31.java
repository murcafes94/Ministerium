package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;
import java.util.List;

/** Stand-alone Missal documents sourced from Liturgia Papal, never from the old Missal EPUB. */
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
        if (!LiturgiaPapalMissalRepository.isAvailable(context, "es")) {
            throw new IllegalStateException("Falta el paquete de Liturgia Papal México.");
        }
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        String key = section == null ? "day" : section;
        String content;
        String title;

        switch (key) {
            case "initial":
                title = "Ritos iniciales";
                content = component(context, "initial", language);
                break;
            case "word":
                title = "Liturgia de la Palabra";
                content = wordWithReadings(context, date, language);
                break;
            case "collect":
                title = "Oración colecta";
                content = proper(context, date, day, LiturgiaPapalMissalRepository.COLLECT);
                break;
            case "eucharist":
                title = "Liturgia eucarística";
                content = eucharistic(context, date, day, language);
                break;
            case "offerings":
                title = "Oración sobre las ofrendas";
                content = proper(context, date, day, LiturgiaPapalMissalRepository.OFFERINGS);
                break;
            case "prefaces":
                title = "Prefacios";
                content = component(context, "prefaces", language);
                break;
            case "prayers":
                title = "Plegarias eucarísticas";
                content = prayers(context, language);
                break;
            case "communion":
                title = "Rito de la Comunión";
                content = communion(context, date, day, language);
                break;
            case "communion_antiphon":
                title = "Antífona de comunión";
                content = proper(context, date, day,
                        LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON);
                break;
            case "post_communion":
                title = "Oración después de la Comunión";
                content = proper(context, date, day,
                        LiturgiaPapalMissalRepository.POST_COMMUNION);
                break;
            case "conclusion":
                title = "Rito de conclusión";
                content = component(context, "conclusion", language);
                break;
            case "commons":
            case "needs":
            case "votive":
            case "dead":
            case "saints":
                title = label(key);
                content = pending("Este grupo de formularios todavía no está normalizado desde una fuente de Liturgia Papal México. No se abrirá el antiguo Misal EPUB.");
                break;
            case "day":
            default:
                title = "Misa del día";
                content = daily(context, date, day, language);
                break;
        }
        return new Result(title, day.celebration + " · " + day.dateLabel,
                document(context, title, content, language));
    }

    private static String daily(Context context, Calendar date, LiturgicalDay day, String language)
            throws Exception {
        StringBuilder out = new StringBuilder(64000);
        out.append(block("Ritos iniciales", component(context, "initial", language)));
        out.append(block("Oración colecta",
                proper(context, date, day, LiturgiaPapalMissalRepository.COLLECT)));
        out.append(block("Liturgia de la Palabra", wordWithReadings(context, date, language)));
        out.append(block("Liturgia eucarística", eucharistic(context, date, day, language)));
        out.append(block("Rito de la Comunión", communion(context, date, day, language)));
        out.append(block("Oración después de la Comunión",
                proper(context, date, day, LiturgiaPapalMissalRepository.POST_COMMUNION)));
        out.append(block("Rito de conclusión", component(context, "conclusion", language)));
        return out.toString();
    }

    private static String wordWithReadings(Context context, Calendar date, String language)
            throws Exception {
        String readings = "";
        try {
            if (!MassReadingsRepository.has(context, date)
                    && MassReadingsRepository.isCurrentMonth(date)) {
                try { MassReadingsRepository.syncDay(context, date); } catch (Exception ignored) {}
            }
            if (MassReadingsRepository.has(context, date)) {
                readings = body(MassReadingsRepository.read(context, date));
            }
        } catch (Exception ignored) {}
        if (readings.isEmpty()) readings = pending("Las lecturas de esta fecha todavía no están guardadas en el Leccionario local.");
        return component(context, "word", language)
                + "<div class=\"lectionary-insert\"><h3>Lecturas del día</h3>" + readings + "</div>";
    }

    private static String eucharistic(Context context, Calendar date, LiturgicalDay day,
                                      String language) throws Exception {
        String offerings = proper(context, date, day, LiturgiaPapalMissalRepository.OFFERINGS);
        StringBuilder out = new StringBuilder();
        if ("lat_es".equals(language)) {
            out.append(component(context, "eucharistic_liturgy", language));
        } else {
            out.append(LiturgiaPapalMissalRepository.preparationHtml(context, "es"));
        }
        out.append(offerings);
        out.append("<h3>Prefacio</h3>");
        out.append(LiturgiaPapalMissalRepository.prefaceDialogueHtml(context, "es"));
        if (isOrdinary(day) && !hasRequiredSaint(day)
                && date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            out.append(LiturgiaPapalMissalRepository.ordinarySundayPrefacesHtml(context));
        } else {
            out.append(pending("El prefacio propio/común de esta celebración se incorporará desde su fuente verificada; no se sustituye por el EPUB."));
        }
        out.append("<h3>Plegarias eucarísticas</h3>").append(prayers(context, language));
        return out.toString();
    }

    private static String prayers(Context context, String language) throws Exception {
        if (!"lat_es".equals(language)) {
            return LiturgiaPapalMissalRepository.eucharisticPrayersHtml(context, false);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            out.append("<details").append(i == 2 ? " open" : "").append("><summary>Plegaria Eucarística ")
                    .append(roman(i)).append("</summary>")
                    .append(parallel(
                            LiturgiaPapalMissalRepository.component(context, "es", "eucharistic_prayer_" + i),
                            LiturgiaPapalMissalRepository.component(context, "la", "eucharistic_prayer_" + i)))
                    .append("</details>");
        }
        return out.toString();
    }

    private static String communion(Context context, Calendar date, LiturgicalDay day,
                                    String language) throws Exception {
        String antiphon = proper(context, date, day,
                LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON);
        if (!"lat_es".equals(language)) {
            return LiturgiaPapalMissalRepository.communionHtml(context, "es", antiphon);
        }
        return parallel(
                LiturgiaPapalMissalRepository.component(context, "es", "communion"),
                LiturgiaPapalMissalRepository.component(context, "la", "communion"))
                + "<h3>Antífona propia</h3>" + antiphon;
    }

    private static String proper(Context context, Calendar date, LiturgicalDay day, String part) {
        if (isOrdinary(day) && !hasRequiredSaint(day)) {
            try {
                String value = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(context, date, part);
                if (!value.isEmpty()) return value;
            } catch (Exception ignored) {}
        }
        return pending("El propio de «" + escape(day == null ? "esta celebración" : day.celebration)
                + "» todavía no está normalizado en el paquete de Liturgia Papal México. Se bloqueó el fallback al Misal EPUB para evitar un formulario equivocado.");
    }

    private static String component(Context context, String id, String language) throws Exception {
        if (!"lat_es".equals(language)) return render(
                LiturgiaPapalMissalRepository.component(context, "es", id));
        return parallel(LiturgiaPapalMissalRepository.component(context, "es", id),
                LiturgiaPapalMissalRepository.component(context, "la", id));
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
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String parallel(String spanish, String latin) {
        return "<div class=\"parallel\"><div class=\"col\"><div class=\"lang\">ES</div>"
                + render(spanish) + "</div><div class=\"col\"><div class=\"lang\">LA</div>"
                + render(latin) + "</div></div>";
    }

    private static String block(String title, String content) {
        return "<section><h2>" + escape(title) + "</h2>" + content + "</section>";
    }

    private static String pending(String message) {
        return "<div class=\"pending\">" + message + "</div>";
    }

    private static String body(String html) {
        if (html == null) return "";
        String lower = html.toLowerCase(java.util.Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) { start = lower.indexOf('>', start); start = start < 0 ? 0 : start + 1; }
        else start = 0;
        int end = lower.lastIndexOf("</body>");
        if (end < start) end = html.length();
        return html.substring(start, end);
    }

    private static String document(Context context, String title, String content, String language) {
        boolean dark = ThemeUtils.isDark(context);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String surface = dark ? "#332C28" : "#FFFFFF";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String border = dark ? "#665746" : "#E2D7C7";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body{margin:0;background:" + bg + ";color:" + ink + "}body{font-family:serif;line-height:1.62;padding:20px 18px 70px;box-sizing:border-box;max-width:1000px;margin:auto}"
                + "section{margin:0 0 20px;padding:16px;border:1px solid " + border + ";border-radius:12px;background:" + surface + "}h1,h2,h3,summary{color:" + wine + "}.source{color:" + muted + ";font-style:italic}.pending{padding:12px;border-left:4px solid " + wine + ";background:" + bg + ";color:" + muted + "}.parallel{display:grid;grid-template-columns:1fr 1fr;gap:14px}.col{min-width:0}.lang{font-weight:bold;color:" + wine + ";border-bottom:1px solid " + border + ";padding-bottom:4px}.lectionary-insert{margin:16px 0;padding:12px;border:1px solid " + border + ";border-radius:9px}@media(max-width:680px){.parallel{grid-template-columns:1fr}.col+.col{border-top:1px solid " + border + ";padding-top:12px}}"
                + "</style></head><body><h1>" + escape(title) + "</h1><p class=\"source\">Fuente: Liturgia Papal · Misal Romano, versión de México"
                + ("lat_es".equals(language) ? " + Missale Romanum latino" : "")
                + " · sin Misal EPUB</p>" + content + "</body></html>";
    }

    private static String label(String key) {
        if ("commons".equals(key)) return "Comunes";
        if ("needs".equals(key)) return "Por diversas necesidades";
        if ("votive".equals(key)) return "Misas votivas";
        if ("dead".equals(key)) return "Misas de difuntos";
        return "Propio de los santos";
    }

    private static String roman(int value) {
        return value == 1 ? "I" : value == 2 ? "II" : value == 3 ? "III" : "IV";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
