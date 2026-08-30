package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;

/** Builds the complete Ordinary of the Mass instead of routing that entry to Initial Rites. */
public final class MissalOrdinaryDocument41 {
    private MissalOrdinaryDocument41() {}

    public static MissalDocument31.Result build(Context context, Calendar date, String language)
            throws Exception {
        String lang = "la".equals(language) ? "la" : "es";
        String title = "la".equals(lang) ? "Ordo Missæ" : "Ordinario de la Misa";
        String languageLabel = "la".equals(lang) ? "Latín" : "Español";
        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        String text = LiturgiaPapalMissalRepository.component(context, lang, "ordinary_full");
        String html = document(context, title, render(text), lang);
        return new MissalDocument31.Result(title,
                day.celebration + " · " + day.dateLabel + " · " + languageLabel, html);
    }

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder(
                "<div class=\"liturgia-papal ministerium-complete-ordinary\" "
                        + "data-missal-source=\"liturgia-papal\">");
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String document(Context context, String title, String content, String lang) {
        boolean dark = ThemeUtils.isDark(context);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#6E1D2A";
        return "<!doctype html><html lang=\"" + lang + "\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=3\">"
                + "<style>html,body{margin:0;background:" + background + ";color:" + ink + ";}"
                + "body{font-family:Georgia,serif;font-size:18px;line-height:1.66;padding:18px 20px 60px;box-sizing:border-box;}"
                + ".document{max-width:900px;margin:0 auto;}h1{color:" + accent + ";font-size:1.5em;line-height:1.25;margin:.2em 0 1em;}"
                + ".liturgia-papal p{margin:.76em 0;}@media(max-width:599px){body{font-size:17px;padding:12px 16px 48px}}"
                + "@media(min-width:700px){body{font-size:20px;padding-left:48px;padding-right:48px}}</style>"
                + "</head><body><main class=\"document\"><h1>" + escape(title) + "</h1>"
                + content + "</main></body></html>";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
