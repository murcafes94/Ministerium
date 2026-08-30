package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Final rubric pass for Mass joined to Lauds or Vespers.
 *
 * OGLH 94 and 96 permit the celebration to begin with the entrance rites of
 * Mass and then continue with the psalmody of the Hour. The short reading of
 * the Hour and the penitential act are omitted. The prayer of the faithful is
 * celebrated in its usual place; only at a ferial morning Mass may the Lauds
 * intercessions replace the usual form. After Communion the Gospel canticle is
 * sung, followed by the post-Communion prayer and the normal conclusion.
 */
public final class CombinedMassRubrics41 {
    private static final Pattern INTERCESSIONS = Pattern.compile(
            "(?is)<section\\b(?=[^>]*data-semantic-id=\\\"combined:intercessions\\\")[^>]*>.*?</section>");

    private CombinedMassRubrics41() {}

    public static CombinedMassComposer.Result apply(Context context, Calendar date,
                                                     String hourKey,
                                                     CombinedMassComposer.Result source) {
        if (source == null || source.html == null) return source;
        String html = source.html;
        html = annotateOpening(html, hourKey);
        html = annotateKyrie(html);
        html = correctUniversalPrayer(context, date, hourKey, html);
        return new CombinedMassComposer.Result(source.title, source.celebration, html);
    }

    private static String annotateOpening(String html, String hourKey) {
        String hour = "vespers".equals(hourKey) ? "Vísperas" : "Laudes";
        String heading = "<h2>Inicio de la celebración</h2>";
        if (!html.contains(heading)) return html;
        String rubric = heading
                + "<p class=\"rubric ministerium-combined-rule\">"
                + "Esta forma comienza con el canto de entrada de la Misa, la procesión y el saludo del celebrante; "
                + "por eso se omiten la invocación inicial y el himno de " + hour + ". "
                + "A continuación se reza la salmodia de la Hora (OGLH 94 y 96)."
                + "</p>";
        return html.replace(heading, rubric);
    }

    private static String annotateKyrie(String html) {
        String heading = "<h2>Kyrie y Gloria</h2>";
        if (!html.contains(heading)) return html;
        String corrected = "<h2>Kyrie y Gloria</h2>"
                + "<p class=\"rubric ministerium-combined-rule\">"
                + "Se omite el acto penitencial. El Kyrie puede decirse según la oportunidad; "
                + "el Gloria se dice cuando lo prescriben las rúbricas."
                + "</p>";
        return html.replace(heading, corrected);
    }

    private static String correctUniversalPrayer(Context context, Calendar date,
                                                 String hourKey, String html) {
        Matcher matcher = INTERCESSIONS.matcher(html);
        if (!matcher.find()) return html;
        String oldSection = matcher.group();
        String oldBody = oldSection
                .replaceFirst("(?is)^.*?<h2\\b[^>]*>.*?</h2>", "")
                .replaceFirst("(?is)</section>\\s*$", "");

        String replacement;
        if (isFerialMorning(context, date, hourKey)) {
            replacement = "<section class=\"ministerium-section\" data-semantic-id=\"combined:intercessions\">"
                    + "<h2>Oración universal</h2>"
                    + "<p class=\"rubric\">En una Misa ferial de la mañana, las preces de Laudes pueden usarse "
                    + "en lugar del formulario corriente de la oración de los fieles (OGLH 94).</p>"
                    + "<div class=\"choicebar\">"
                    + "<button type=\"button\" id=\"combinedPrecesButton\" class=\"selected\" "
                    + "onclick=\"ministeriumUniversalPrayer('preces')\">Preces de Laudes</button>"
                    + "<button type=\"button\" id=\"combinedUniversalButton\" "
                    + "onclick=\"ministeriumUniversalPrayer('universal')\">Oración de los fieles</button></div>"
                    + "<div id=\"combinedPreces\">" + oldBody + "</div>"
                    + "<div id=\"combinedUniversal\" hidden><p class=\"rubric\">"
                    + "Se hace la oración universal en su lugar y forma acostumbrados en la Misa.</p></div>"
                    + "<script>function ministeriumUniversalPrayer(which){var p=document.getElementById('combinedPreces'),u=document.getElementById('combinedUniversal'),"
                    + "bp=document.getElementById('combinedPrecesButton'),bu=document.getElementById('combinedUniversalButton'),x=which==='preces';"
                    + "if(p)p.hidden=!x;if(u)u.hidden=x;if(bp)bp.classList.toggle('selected',x);if(bu)bu.classList.toggle('selected',!x);}</script>"
                    + "</section>";
        } else {
            replacement = "<section class=\"ministerium-section\" data-semantic-id=\"combined:intercessions\">"
                    + "<h2>Oración universal</h2>"
                    + "<p class=\"rubric\">Se hace la oración de los fieles en su lugar y forma acostumbrados en la Misa. "
                    + "Las preces de la Hora no sustituyen aquí la oración universal.</p>"
                    + "</section>";
        }
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private static boolean isFerialMorning(Context context, Calendar date, String hourKey) {
        if (!"lauds".equals(hourKey) || date == null
                || date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return false;
        try {
            LiturgicalEvent primary = LiturgicalResolver.primaryEvent(
                    LiturgicalCalendarRepository.eventsFor(context, date));
            if (primary != null && (primary.isFeast() || primary.isSolemnity())) return false;
            LiturgicalDay day = LiturgicalResolver.resolve(context, date);
            if (day != null && day.saintOffices != null) {
                for (HoursLink saint : day.saintOffices) {
                    if (saint != null && saint.requiresProperOffice()) return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
}
