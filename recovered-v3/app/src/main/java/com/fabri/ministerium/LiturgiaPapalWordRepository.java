package com.fabri.ministerium;

import java.text.Normalizer;
import java.util.Locale;

/** Extracts semantic blocks from Liturgia Papal's Spanish and Latin Liturgia de la Palabra. */
public final class LiturgiaPapalWordRepository {
    private LiturgiaPapalWordRepository() {}

    public static String niceneCreedHtml(android.content.Context context) throws Exception {
        String spanish = extract(LiturgiaPapalMissalRepository.component(context, "es", "word"),
                "Creo en un solo Dios", "Para utilidad de los fieles", 0);
        String latin = extract(LiturgiaPapalMissalRepository.component(context, "la", "word"),
                "Credo in unum Deum", "Loco symboli", 0);
        return bilingual("creed-nicene", spanish, latin);
    }

    public static String apostlesCreedHtml(android.content.Context context) throws Exception {
        String spanishText = LiturgiaPapalMissalRepository.component(context, "es", "word");
        String spanish = extract(spanishText,
                "Creo en Dios, Padre todopoderoso",
                "Después se hace la plegaria universal", 0);

        String latinText = LiturgiaPapalMissalRepository.component(context, "la", "word");
        String[] latinLines = latinText.split("\\r?\\n");
        int alternate = find(latinLines, "Loco symboli", 0);
        String latin = extract(latinText,
                "Credo in unum Deum", "Deinde fit oratio universalis",
                alternate < 0 ? 0 : alternate + 1);
        return bilingual("creed-apostles", spanish, latin);
    }

    /** Selector used by the stand-alone Spanish Missal: two creeds, each with ES/LAT. */
    public static String professionOfFaithHtml(android.content.Context context) throws Exception {
        return "<div class=\"ministerium-creed-selector\" data-ministerium-creed-selector=\"1\">"
                + "<div class=\"choicebar\"><button type=\"button\" class=\"selected\" "
                + "id=\"ministeriumCredoNiceneButton\" onclick=\"ministeriumChooseCredo('nicene')\">"
                + "Niceno-constantinopolitano</button>"
                + "<button type=\"button\" id=\"ministeriumCredoApostlesButton\" "
                + "onclick=\"ministeriumChooseCredo('apostles')\">De los Apóstoles</button></div>"
                + "<div id=\"ministeriumCredoNicene\">" + niceneCreedHtml(context) + "</div>"
                + "<div id=\"ministeriumCredoApostles\" hidden>" + apostlesCreedHtml(context) + "</div>"
                + "<script>window.ministeriumChooseCredo=window.ministeriumChooseCredo||function(which){"
                + "var n=document.getElementById('ministeriumCredoNicene'),a=document.getElementById('ministeriumCredoApostles');"
                + "var bn=document.getElementById('ministeriumCredoNiceneButton'),ba=document.getElementById('ministeriumCredoApostlesButton');"
                + "var apost=which==='apostles';if(n)n.hidden=apost;if(a)a.hidden=!apost;"
                + "if(bn)bn.classList.toggle('selected',!apost);if(ba)ba.classList.toggle('selected',apost);};</script>"
                + "</div>";
    }

    private static String bilingual(String id, String spanish, String latin) {
        return "<div class=\"ministerium-prayer-language\" data-prayer-language=\"" + escape(id) + "\">"
                + "<div class=\"ministerium-language-switch\">"
                + "<button type=\"button\" class=\"selected\" onclick=\"ministeriumPrayerLanguage('"
                + escapeJs(id) + "','es')\">ES</button>"
                + "<button type=\"button\" onclick=\"ministeriumPrayerLanguage('"
                + escapeJs(id) + "','la')\">LAT</button></div>"
                + "<div data-language=\"es\">" + render(spanish) + "</div>"
                + "<div data-language=\"la\" hidden>" + render(latin) + "</div>"
                + "<script>window.ministeriumPrayerLanguage=window.ministeriumPrayerLanguage||function(id,lang){"
                + "var box=document.querySelector('[data-prayer-language=\"'+id+'\"]');if(!box)return;"
                + "var es=box.querySelector('[data-language=\"es\"]'),la=box.querySelector('[data-language=\"la\"]');"
                + "if(es)es.hidden=lang!=='es';if(la)la.hidden=lang!=='la';var buttons=box.querySelectorAll('.ministerium-language-switch button');"
                + "for(var i=0;i<buttons.length;i++)buttons[i].classList.toggle('selected',(i===0&&lang==='es')||(i===1&&lang==='la'));};</script>"
                + "</div>";
    }

    private static String extract(String text, String startMarker, String endMarker, int from) {
        String[] lines = text.split("\\r?\\n");
        int start = find(lines, startMarker, from);
        if (start < 0) return "";
        int end = find(lines, endMarker, start + 1);
        if (end < 0) end = lines.length;
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            String line = lines[i];
            if ("ORDE MISSÆ".equalsIgnoreCase(line.trim())) continue;
            out.append(line).append('\n');
        }
        return out.toString().trim();
    }

    private static int find(String[] lines, String marker, int from) {
        String wanted = normalize(marker);
        for (int i = Math.max(0, from); i < lines.length; i++) {
            String value = normalize(lines[i]);
            if (value.equals(wanted) || value.startsWith(wanted)) return i;
        }
        return -1;
    }

    private static String render(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder("<div class=\"liturgia-papal creed-text\" data-missal-source=\"liturgia-papal\">");
        for (String block : blocks) {
            String value = escape(block.trim()).replace("\n", "<br>");
            if (!value.isEmpty()) html.append("<p>").append(value).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeJs(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
