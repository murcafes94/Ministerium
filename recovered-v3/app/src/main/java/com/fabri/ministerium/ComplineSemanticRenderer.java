package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

/** Convierte el modelo semántico de Completas en una sola superficie de lectura. */
public final class ComplineSemanticRenderer {
    private ComplineSemanticRenderer() {}

    public static String render(Context context, JSONObject data, JSONObject form,
                                String season) {
        boolean dark = ThemeUtils.isDark(context);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#E1C57A" : "#6E1D2A";
        String panel = dark ? "#332C28" : "#F5EDDF";
        String border = dark ? "#65564D" : "#D8C9B5";
        String activeInk = dark ? "#201A17" : "#FFFFFF";

        StringBuilder html = new StringBuilder(24000);
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<style>")
                .append("html,body{margin:0;background:").append(background)
                .append(";color:").append(ink).append("}body{padding:22px;box-sizing:border-box;")
                .append("font-family:serif;line-height:1.65;max-width:1040px;margin:0 auto}")
                .append("h1{font-size:1.55em;margin:.1em 0 .2em;color:").append(accent).append("}")
                .append("h2{font-size:1.1em;margin:1.75em 0 .7em;color:").append(accent)
                .append(";letter-spacing:.035em;text-transform:uppercase}")
                .append("h3{font-size:1em;margin:1.2em 0 .5em;color:").append(accent).append("}")
                .append("p{margin:.72em 0}.meta{font-size:.88em;color:").append(dark ? "#C8BDB0" : "#6F665E")
                .append("}.rubric{font-size:.92em;font-style:italic;color:").append(accent).append("}")
                .append(".pre{white-space:pre-line}.antiphon{font-weight:600;color:").append(accent).append("}")
                .append(".psalm-title{font-weight:700;color:").append(accent).append("}")
                .append(".subtitle{font-size:.88em;font-style:italic;color:").append(dark ? "#D6C9BC" : "#6F665E")
                .append("}.dialogue{white-space:pre-line}.semantic-section{scroll-margin-top:70px}")
                .append(".choice-box{padding:14px;margin:10px 0 18px;background:").append(panel)
                .append(";border-left:4px solid ").append(accent).append(";border-radius:0 9px 9px 0}")
                .append(".choice-nav{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px}")
                .append(".choice-nav button{border:1px solid ").append(accent)
                .append(";border-radius:18px;padding:8px 12px;background:transparent;color:").append(accent)
                .append(";font:inherit}.choice-nav button.active{background:").append(accent)
                .append(";color:").append(activeInk).append("}.choice-content{white-space:pre-line}")
                .append(".choice-content[hidden]{display:none}.unit{margin:.72em 0}")
                .append(".divider{height:1px;background:").append(border).append(";margin:1.5em 0}")
                .append("@media(min-width:700px){body{padding:30px 46px}}")
                .append("</style></head><body>");

        html.append("<article data-document=\"compline\">")
                .append("<h1>Completas</h1><p class=\"meta\">")
                .append(escape(form.optString("title", "Oración antes del descanso")))
                .append(" · ").append(escape(seasonLabel(season))).append("</p>");

        JSONObject invocation = ComplineContentRepository.invocation(data);
        sectionStart(html, "compline.invocation", "Invocación inicial");
        paragraph(html, "dialogue", invocation.optString("verse", ""));
        paragraph(html, "dialogue", invocation.optString("response", ""));
        String doxology = invocation.optString("doxology", "");
        if (!"lent".equals(ComplineContentRepository.normalizeSeason(season))) {
            String alleluia = invocation.optString("alleluiaOutsideLent", "");
            if (!alleluia.isEmpty()) doxology += " " + alleluia;
        }
        paragraph(html, "pre", doxology);
        sectionEnd(html);

        sectionStart(html, "compline.examination", "Examen de conciencia");
        paragraph(html, "rubric", ComplineContentRepository.examinationRubric(data));
        sectionEnd(html);

        sectionStart(html, "compline.penance", "Acto penitencial");
        choiceBlock(html, "penance", ComplineContentRepository.penitentialFormulas(data));
        paragraph(html, "rubric",
                "Si preside la celebración un ministro, él solo dice la conclusión siguiente; en caso contrario, la dicen todos:");
        paragraph(html, "dialogue", ComplineContentRepository.penitentialConclusion(data));
        sectionEnd(html);

        sectionStart(html, "compline.hymn", "Himno");
        choiceBlock(html, "hymn", ComplineContentRepository.hymnsForSeason(data, season));
        sectionEnd(html);

        sectionStart(html, "compline.psalmody", "Salmodia");
        JSONArray psalmody = form.optJSONArray("psalmody");
        if (psalmody != null) {
            for (int i = 0; i < psalmody.length(); i++) {
                JSONObject block = psalmody.optJSONObject(i);
                if (block == null) continue;
                String kind = block.optString("kind", "text");
                String css = "antiphon".equals(kind) ? "antiphon pre unit"
                        : "psalm_title".equals(kind) ? "psalm-title pre unit"
                        : "subtitle".equals(kind) ? "subtitle pre unit" : "pre unit";
                paragraph(html, css, normalizeDialogues(block.optString("text", "")));
            }
        }
        sectionEnd(html);

        JSONObject reading = form.optJSONObject("shortReading");
        sectionStart(html, "compline.short-reading", "Lectura breve");
        if (reading != null) {
            paragraph(html, "psalm-title", reading.optString("reference", ""));
            paragraph(html, "pre", reading.optString("text", ""));
        }
        sectionEnd(html);

        sectionStart(html, "compline.responsory", "Responsorio breve");
        JSONArray responsory = form.optJSONArray("responsory");
        if (responsory != null) {
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < responsory.length(); i++) {
                if (i > 0) lines.append('\n');
                lines.append(normalizeDialogues(responsory.optString(i, "")));
            }
            paragraph(html, "dialogue", lines.toString());
        }
        sectionEnd(html);

        sectionStart(html, "compline.nunc-dimittis", "Cántico evangélico");
        JSONArray canticle = form.optJSONArray("gospelCanticle");
        if (canticle != null) {
            for (int i = 0; i < canticle.length(); i++) {
                JSONObject block = canticle.optJSONObject(i);
                if (block == null) continue;
                String kind = block.optString("kind", "text");
                String css = "antiphon".equals(kind) ? "antiphon pre unit"
                        : "title".equals(kind) ? "psalm-title pre unit" : "pre unit";
                paragraph(html, css, normalizeDialogues(block.optString("text", "")));
            }
        }
        sectionEnd(html);

        sectionStart(html, "compline.prayer", "Oración");
        paragraph(html, "pre", form.optString("prayer", ""));
        sectionEnd(html);

        sectionStart(html, "compline.conclusion", "Conclusión");
        JSONArray conclusion = form.optJSONArray("conclusion");
        if (conclusion != null) {
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < conclusion.length(); i++) {
                if (i > 0) value.append('\n');
                value.append(normalizeDialogues(conclusion.optString(i, "")));
            }
            paragraph(html, "dialogue", value.toString());
        }
        sectionEnd(html);

        sectionStart(html, "compline.marian", "Antífona final de la Santísima Virgen");
        choiceBlock(html, "marian", ComplineContentRepository.marianAntiphons(data,
                "easter".equals(ComplineContentRepository.normalizeSeason(season))));
        sectionEnd(html);

        html.append("</article><script>")
                .append("function ministeriumSelect(group,index){var box=document.querySelector('[data-choice-group=\"'+group+'\"]');if(!box)return;")
                .append("var buttons=box.querySelectorAll('.choice-nav button');var items=box.querySelectorAll('.choice-content');")
                .append("for(var i=0;i<items.length;i++){var on=i===index;items[i].hidden=!on;if(buttons[i]){buttons[i].classList.toggle('active',on);buttons[i].setAttribute('aria-pressed',on?'true':'false');}}}")
                .append("</script></body></html>");
        return html.toString();
    }

    private static void sectionStart(StringBuilder out, String id, String title) {
        out.append("<section class=\"semantic-section\" data-block=\"")
                .append(escape(id)).append("\"><h2>").append(escape(title)).append("</h2>");
    }

    private static void sectionEnd(StringBuilder out) {
        out.append("</section>");
    }

    private static void choiceBlock(StringBuilder out, String group, JSONArray items) {
        if (items == null || items.length() == 0) {
            paragraph(out, "rubric", "Contenido pendiente de una fuente verificada.");
            return;
        }
        out.append("<div class=\"choice-box\" data-choice-group=\"").append(escape(group)).append("\">");
        if (items.length() > 1) {
            out.append("<div class=\"choice-nav\" role=\"group\">");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                out.append("<button type=\"button\" aria-pressed=\"")
                        .append(i == 0 ? "true" : "false").append("\" class=\"")
                        .append(i == 0 ? "active" : "").append("\" onclick=\"ministeriumSelect('")
                        .append(js(group)).append("',").append(i).append(")\">")
                        .append(escape(item.optString("title", "Opción " + (i + 1))))
                        .append("</button>");
            }
            out.append("</div>");
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            out.append("<div class=\"choice-content\" data-choice-index=\"").append(i).append("\"");
            if (i != 0) out.append(" hidden");
            out.append(">").append(formatText(item.optString("text", ""))).append("</div>");
        }
        out.append("</div>");
    }

    private static void paragraph(StringBuilder out, String css, String text) {
        if (text == null || text.trim().isEmpty()) return;
        out.append("<p class=\"").append(escape(css)).append("\">")
                .append(formatText(text)).append("</p>");
    }

    private static String normalizeDialogues(String value) {
        if (value == null) return "";
        return value.replaceAll("(?m)^V\\.\\s*", "℣. ")
                .replaceAll("(?m)^R\\.\\s*", "℟. ");
    }

    private static String formatText(String value) {
        return escape(normalizeDialogues(value)).replace("\n", "<br>");
    }

    private static String seasonLabel(String season) {
        String value = ComplineContentRepository.normalizeSeason(season);
        if ("advent".equals(value)) return "Adviento";
        if ("christmas".equals(value)) return "Navidad";
        if ("lent".equals(value)) return "Cuaresma";
        if ("easter".equals(value)) return "Tiempo Pascual";
        return "Tiempo Ordinario";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String js(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
