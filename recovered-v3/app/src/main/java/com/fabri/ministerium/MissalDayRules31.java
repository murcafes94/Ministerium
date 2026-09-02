package com.fabri.ministerium;

import android.content.Context;
import android.webkit.WebView;

import java.util.Calendar;

/** Applies calendar-dependent Missal rules that cannot live in the static Ordinary source. */
public final class MissalDayRules31 {
    private MissalDayRules31() {}

    public static void inject(Context context, WebView webView, Calendar date) {
        if (context == null || webView == null || date == null) return;
        if (gloriaRequired(context, date)) return;
        String script = "(function(){"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "var ps=Array.prototype.slice.call(document.querySelectorAll('p'));"
                + "for(var i=0;i<ps.length;i++){var t=n(ps[i].textContent);"
                + "if(t.indexOf('gloria a dios en el cielo')===0||t.indexOf('gloria in excelsis deo')===0){"
                + "var prev=ps[i].previousElementSibling;if(prev&&n(prev.textContent)==='gloria')prev.remove();ps[i].remove();break;}}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }

    static boolean gloriaRequired(Context context, Calendar date) {
        try {
            LiturgicalDay day = LiturgicalResolver.resolve(context, date);
            if (day != null && day.saintOffices != null) {
                for (HoursLink office : day.saintOffices) {
                    if (office != null && office.isFeastOrSolemnity()) return true;
                }
            }
            try {
                for (LiturgicalEvent event : LiturgicalCalendarRepository.eventsFor(context, date)) {
                    if (event.isFeast() || event.isSolemnity()) return true;
                }
            } catch (Exception ignored) {}
            if (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return false;
            String season = day == null || day.temporalOffice == null
                    || day.temporalOffice.volume == null ? "" : day.temporalOffice.volume.id;
            return !"advent".equals(season) && !"lent".equals(season);
        } catch (Exception ignored) {
            return date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        }
    }
}
