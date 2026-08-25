package com.fabri.ministerium;

import android.content.Context;
import android.webkit.WebView;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/** Enhances the Spanish Missal with local ES/LAT prayer switches without duplicating screens. */
public final class MissalInteractiveOptions {
    private MissalInteractiveOptions() {}

    public static void inject(Context context, WebView webView, boolean replaceRawCreeds) {
        if (context == null || webView == null) return;
        try {
            String creed = replaceRawCreeds
                    ? LiturgiaPapalWordRepository.professionOfFaithHtml(context) : "";
            String pater = paterNosterHtml(context);
            String script = "(function(){"
                    + "window.setPrayer=window.setPrayer||function(n){for(var i=1;i<=4;i++){"
                    + "var p=document.getElementById('prayer'+i),b=document.getElementById('prayerButton'+i);var active=i===n;"
                    + "if(p){p.hidden=!active;p.classList.toggle('hidden',!active);}if(b&&!b.disabled){b.classList.toggle('selected',active);b.setAttribute('aria-pressed',active?'true':'false');}}};"
                    + "window.ministeriumPrayerLanguage=window.ministeriumPrayerLanguage||function(id,lang){"
                    + "var box=document.querySelector('[data-prayer-language=\"'+id+'\"]');if(!box)return;"
                    + "var es=box.querySelector('[data-language=\"es\"]'),la=box.querySelector('[data-language=\"la\"]');"
                    + "if(es)es.hidden=lang!=='es';if(la)la.hidden=lang!=='la';var buttons=box.querySelectorAll('.ministerium-language-switch button');"
                    + "for(var i=0;i<buttons.length;i++)buttons[i].classList.toggle('selected',(i===0&&lang==='es')||(i===1&&lang==='la'));};"
                    + "window.ministeriumChooseCredo=window.ministeriumChooseCredo||function(which){"
                    + "var n=document.getElementById('ministeriumCredoNicene'),a=document.getElementById('ministeriumCredoApostles');"
                    + "var bn=document.getElementById('ministeriumCredoNiceneButton'),ba=document.getElementById('ministeriumCredoApostlesButton');"
                    + "var apost=which==='apostles';if(n)n.hidden=apost;if(a)a.hidden=!apost;"
                    + "if(bn)bn.classList.toggle('selected',!apost);if(ba)ba.classList.toggle('selected',apost);};"
                    + "function firstParagraph(prefix){var ps=document.querySelectorAll('p');for(var i=0;i<ps.length;i++){"
                    + "var t=(ps[i].textContent||'').trim();if(t.indexOf(prefix)===0)return ps[i];}return null;}"
                    + "var walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);var node;while((node=walker.nextNode())){"
                    + "if(node.nodeValue&&node.nodeValue.indexOf('\\\\n')>=0)node.nodeValue=node.nodeValue.replace(/\\\\n/g,' ');}"
                    + (replaceRawCreeds
                    ? "if(!document.querySelector('[data-ministerium-creed-selector]')){"
                    + "var nic=firstParagraph('Creo en un solo Dios');var note=firstParagraph('Para utilidad de los fieles');"
                    + "var apo=firstParagraph('Creo en Dios, Padre todopoderoso');if(nic&&apo){"
                    + "var holder=document.createElement('div');holder.innerHTML=" + JSONObject.quote(creed) + ";"
                    + "var newNode=holder.firstElementChild;if(newNode)nic.parentNode.insertBefore(newNode,nic);nic.remove();if(note)note.remove();apo.remove();}}"
                    : "")
                    + "if(!document.querySelector('[data-prayer-language=\"pater-noster\"]')){"
                    + "var pn=firstParagraph('Padre nuestro, que estás en el cielo');if(pn){"
                    + "var ph=document.createElement('div');ph.innerHTML=" + JSONObject.quote(pater) + ";"
                    + "var pnode=ph.firstElementChild;if(pnode)pn.parentNode.replaceChild(pnode,pn);}}"
                    + "window.setPrayer(2);"
                    + "if(!document.getElementById('ministeriumMissalInteractiveStyle')){var s=document.createElement('style');"
                    + "s.id='ministeriumMissalInteractiveStyle';s.textContent='"
                    + ".hidden{display:none!important}.eucharistic-prayer[hidden]{display:none!important}.choicebar{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0 12px}.choicebar button{border:1px solid currentColor;border-radius:18px;background:transparent;color:inherit;padding:6px 12px;font:inherit}.choicebar button.selected{font-weight:700;text-decoration:underline}.ministerium-language-switch{display:flex;gap:8px;margin:8px 0 12px}.ministerium-language-switch button{border:1px solid currentColor;border-radius:18px;background:transparent;color:inherit;padding:6px 12px;font:inherit}.ministerium-language-switch button.selected{font-weight:700;text-decoration:underline}.ministerium-creed-selector{margin:12px 0;padding:12px;border:1px solid rgba(128,128,128,.35);border-radius:10px}.ministerium-creed-selector .choicebar{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px}.ministerium-creed-selector .choicebar button{border-radius:18px;padding:7px 12px}.ministerium-prayer-language [data-language=\"la\"]{font-style:normal}';document.head.appendChild(s);}"
                    + "})()";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {}
    }

    private static String paterNosterHtml(Context context) throws Exception {
        String spanish = extract(LiturgiaPapalMissalRepository.component(context, "es", "communion"),
                "Padre nuestro, que estás en el cielo",
                "El sacerdote, con las manos extendidas, prosigue él solo", 0);
        String latin = extract(LiturgiaPapalMissalRepository.component(context, "la", "communion"),
                "Pater noster, qui es in cælis",
                "Manibus extensis, sacerdos solus prosequitur", 0);
        return bilingual("pater-noster", spanish, latin);
    }

    private static String bilingual(String id, String spanish, String latin) {
        return "<div class=\"ministerium-prayer-language\" data-prayer-language=\"" + escape(id) + "\">"
                + "<div class=\"ministerium-language-switch\">"
                + "<button type=\"button\" class=\"selected\" onclick=\"ministeriumPrayerLanguage('"
                + escapeJs(id) + "','es')\">ES</button>"
                + "<button type=\"button\" onclick=\"ministeriumPrayerLanguage('"
                + escapeJs(id) + "','la')\">LAT</button></div>"
                + "<div data-language=\"es\">" + render(spanish) + "</div>"
                + "<div data-language=\"la\" hidden>" + render(latin) + "</div></div>";
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
        text = text.replace("\\n", "\n");
        String[] blocks = text.trim().split("\\n\\s*\\n");
        StringBuilder html = new StringBuilder("<div class=\"liturgia-papal prayer-text\">");
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
