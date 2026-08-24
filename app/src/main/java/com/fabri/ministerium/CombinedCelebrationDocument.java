package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds one local HTML document for Lauds/Vespers immediately joined to Mass.
 *
 * The source texts are not rewritten here: this class embeds the same local
 * Hours, Roman Missal and saved lectionary documents already used by the app,
 * then limits each embedded block to the portions required by OGLH 93-96.
 */
public final class CombinedCelebrationDocument {
    private CombinedCelebrationDocument() {}

    public static String build(Context context, Calendar date, LiturgicalDay day,
                               HourEntry hour, boolean massEntrance,
                               String missalLanguage) throws Exception {
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
        String cycle = LiturgicalResolver.lectionaryCycle(date);
        int readingsYear = date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1;

        String hourHtml = hourDocument(context, hour, ordinaryWeek, cycle, readingsYear);
        String startHtml = missalEntry(context, "Inicio");
        String eucharistHtml = missalEntry(context, "Credo");
        String conclusionHtml = missalEntry(context, "RitoConclusión", "Rito Conclusión", "Rito de conclusión");
        String collectHtml = proper(context, date, day.celebration, MissalProperRepository.Part.COLLECT);
        String afterCommunionHtml = proper(context, date, day.celebration,
                MissalProperRepository.Part.AFTER_COMMUNION);
        String readingsHtml = MassReadingsRepository.has(context, date)
                ? body(MassReadingsRepository.read(context, date))
                : "<div class=\"ministerium-missing\"><h2>Liturgia de la Palabra</h2>"
                + "<p>Las lecturas de esta fecha todavía no están guardadas en el dispositivo.</p>"
                + "<p><a href=\"ministerium://sync-readings\">Guardar las lecturas de hoy y continuar</a></p></div>";

        StringBuilder out = new StringBuilder(32768);
        out.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<style>")
                .append(baseCss("lat_es".equals(missalLanguage)))
                .append("</style></head><body>")
                .append("<header class=\"union-header\"><div class=\"kicker\">OGLH 93–96 · celebración unida</div><h1>")
                .append(escape(day.celebration)).append("</h1><p>")
                .append(escape(day.dateLabel)).append(" · ")
                .append("vespers".equals(hour.key) ? "Vísperas" : "Laudes")
                .append(" con Misa</p></header>");

        if (massEntrance) {
            section(out, "mass-greeting", "Ritos iniciales", body(startHtml));
        }
        section(out, "hour-before", massEntrance ? "Salmodia" : "Inicio y salmodia",
                body(hourHtml));
        section(out, "mass-kyrie", "Kyrie y Gloria", body(startHtml));
        section(out, "mass-collect", "Oración colecta", collectHtml);
        section(out, "mass-readings", "Liturgia de la Palabra", readingsHtml);
        section(out, "mass-eucharist", "De la profesión de fe a la Comunión", body(eucharistHtml));
        section(out, "hour-canticle", "Cántico evangélico", body(hourHtml));
        section(out, "mass-after-communion", "Oración después de la Comunión", afterCommunionHtml);
        section(out, "mass-conclusion", "Conclusión", body(conclusionHtml));

        out.append("<script>").append(filterScript(massEntrance)).append("</script>")
                .append("</body></html>");
        return out.toString();
    }

    private static String hourDocument(Context context, HourEntry hour, int ordinaryWeek,
                                       String cycle, int readingsYear) throws Exception {
        File root = EpubUtils.ensureExtracted(context, hour.volume);
        String resolved = null;
        if ("ordinary".equals(hour.volume.id) && ordinaryWeek > 0) {
            resolved = OrdinaryReferenceResolver.resolve(root, hour.filePath,
                    ordinaryWeek, cycle, readingsYear);
        }
        if (resolved != null) return resolved;
        return read(new File(root, hour.filePath));
    }

    private static String missalEntry(Context context, String... candidates) throws Exception {
        int index = EpubUtils.findEntryIndex(context, HoursRepository.ROMAN_MISSAL, candidates);
        if (index < 0) return "<p class=\"ministerium-missing\">No se encontró este bloque del Ordinario.</p>";
        List<EpubTocEntry> entries = EpubUtils.tableOfContents(context, HoursRepository.ROMAN_MISSAL);
        EpubTocEntry entry = entries.get(index);
        File root = EpubUtils.ensureExtracted(context, HoursRepository.ROMAN_MISSAL);
        return read(new File(root, entry.filePath));
    }

    private static String proper(Context context, Calendar date, String celebration,
                                 MissalProperRepository.Part part) {
        try {
            MissalProperRepository.Target target = MissalProperRepository.resolve(
                    context, date, celebration, part);
            if (target == null) return missingProper(part);
            File root = EpubUtils.ensureExtracted(context, HoursRepository.ROMAN_MISSAL);
            String html = read(new File(root, target.filePath));
            return fragment(html, target.fragment);
        } catch (Exception error) {
            return missingProper(part);
        }
    }

    private static String missingProper(MissalProperRepository.Part part) {
        String name = part == MissalProperRepository.Part.COLLECT
                ? "colecta" : "oración después de la Comunión";
        return "<p class=\"ministerium-missing\">No se encontró automáticamente la "
                + name + " propia de esta celebración. Comprueba el propio del Misal.</p>";
    }

    private static void section(StringBuilder out, String id, String label, String html) {
        out.append("<section id=\"").append(id).append("\" class=\"union-section\">")
                .append("<div class=\"section-label\">").append(escape(label)).append("</div>")
                .append("<div class=\"source-body\">").append(html).append("</div></section>");
    }

    private static String body(String html) {
        if (html == null || html.isEmpty()) return "";
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start >= 0) {
            int open = html.indexOf('>', start);
            int end = lower.lastIndexOf("</body>");
            if (open >= 0) return html.substring(open + 1, end > open ? end : html.length());
        }
        return html;
    }

    private static String fragment(String html, String fragment) {
        if (fragment == null || fragment.trim().isEmpty()) return body(html);
        Matcher marker = Pattern.compile("(?i)\\bid\\s*=\\s*([\"'])"
                + Pattern.quote(fragment) + "\\1").matcher(html);
        if (!marker.find()) return body(html);
        int at = marker.start();
        String lower = html.toLowerCase(Locale.ROOT);
        String[] blocks = {"tr", "section", "div", "p"};
        for (String tag : blocks) {
            int start = lower.lastIndexOf("<" + tag, at);
            if (start < 0) continue;
            int end = lower.indexOf("</" + tag + ">", at);
            if (end > at) return html.substring(start, end + tag.length() + 3);
        }
        return body(html);
    }

    private static String baseCss(boolean bilingualMissal) {
        return "html,body{margin:0;background:#fffdf7;color:#2a2521}body{font-family:serif;line-height:1.62;padding:18px;box-sizing:border-box}"
                + ".union-header{padding:8px 2px 20px;border-bottom:1px solid #d8c9b5}.union-header h1{margin:.25em 0;color:#6e1d2a;font-size:1.55rem}.union-header p{margin:0;color:#6f665e}.kicker,.section-label{font-family:sans-serif;text-transform:uppercase;letter-spacing:.06em;font-size:.74rem;font-weight:bold;color:#6e1d2a}"
                + ".union-section{padding:22px 0;border-bottom:1px solid #e6dccf}.section-label{margin-bottom:10px}.source-body{max-width:100%;overflow-wrap:anywhere}.source-body img,.source-body table{max-width:100%;height:auto}.ministerium-missing{padding:14px;border-left:4px solid #6e1d2a;background:#f5eddf}.ministerium-missing a{color:#6e1d2a;font-weight:bold}"
                + ".ministerium-canticle{margin:12px 0;padding:14px;border-left:4px solid #6e1d2a;background:#f5eddf}"
                + (bilingualMissal ? "" : ".source-body .izq{display:none!important}.source-body .dcha{width:100%!important}")
                + "@media(min-width:700px){body{padding-left:42px;padding-right:42px}}";
    }

    private static String filterScript(boolean massEntrance) {
        return "(function(){"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "function kids(id){var r=document.querySelector('#'+id+' .source-body');return r?Array.prototype.slice.call(r.children):[];}"
                + "function rows(id){var r=document.querySelector('#'+id+' .source-body');return r?Array.prototype.slice.call(r.querySelectorAll('tr')):[];}"
                // Hour before Mass: full opening+hymn+psalmody, or psalmody only after Mass greeting.
                + "var h=kids('hour-before'),cut=h.length,hy=-1,sa=-1;for(var i=0;i<h.length;i++){var t=n(h[i].textContent);if(t.indexOf('LECTURA BREVE')===0){cut=i;break;}if(t==='HIMNO')hy=i;if(t==='SALMODIA'&&sa<0)sa=i;}"
                + "for(var i=cut;i<h.length;i++)h[i].style.display='none';"
                + (massEntrance ? "if(sa>=0)for(var i=0;i<sa;i++)h[i].style.display='none';" : "")
                // Mass greeting only, never penitential act.
                + "var g=rows('mass-greeting'),ge=g.length;for(var i=0;i<g.length;i++){if(n(g[i].textContent)==='ACTO PENITENCIAL'){ge=i;break;}}for(var i=ge;i<g.length;i++)g[i].style.display='none';"
                // Kyrie/Gloria block, omitting penitential act and ending before collect/Word.
                + "var k=rows('mass-kyrie'),ks=-1,ke=k.length;for(var i=0;i<k.length;i++){var t=n(k[i].textContent);if(ks<0&&t==='KYRIE')ks=i;if(k[i].querySelector('#AntesColecta')||t==='LITURGIA DE LA PALABRA'){ke=i;break;}}if(ks<0)ks=0;for(var i=0;i<ks;i++)k[i].style.display='none';for(var i=ke;i<k.length;i++)k[i].style.display='none';"
                // Eucharistic block begins at Creed and stops before after-Communion prayer.
                + "var e=rows('mass-eucharist'),es=-1,ee=e.length;for(var i=0;i<e.length;i++){if(es<0&&n(e[i].textContent)==='CREDO')es=i;if(e[i].querySelector('#AntesDespuesComunion')){ee=i;break;}}if(es<0)es=0;for(var i=0;i<es;i++)e[i].style.display='none';for(var i=ee;i<e.length;i++)e[i].style.display='none';"
                // Gospel canticle only: after Communion and before preces.
                + "var c=kids('hour-canticle'),cs=-1,ce=c.length;for(var i=0;i<c.length;i++){var t=n(c[i].textContent);if(cs<0&&t==='CANTICO EVANGELICO')cs=i;else if(cs>=0&&t==='PRECES'){ce=i;break;}}if(cs>=0){for(var i=0;i<cs;i++)c[i].style.display='none';for(var i=ce;i<c.length;i++)c[i].style.display='none';}"
                + "window.scrollTo(0,0);})();";
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

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
