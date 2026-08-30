package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;

/**
 * Presentation-only filter for source books that print several seasonal
 * alternatives in the same page. It never changes the stored liturgical text;
 * it only hides the alternative that does not apply to the selected date.
 */
public final class HoursSeasonFilter {
    private HoursSeasonFilter() {}

    public static void apply(Context context, WebView webView) {
        if (!(context instanceof Activity) || webView == null) return;
        Activity activity = (Activity) context;
        Intent intent = activity.getIntent();
        if (intent == null) return;

        String name = activity.getClass().getSimpleName();
        if (!("HoursReaderActivity".equals(name)
                || "BilingualHoursReaderActivity".equals(name))) return;

        boolean easter = intent.getBooleanExtra(HoursReaderActivity.EXTRA_EASTER_SEASON, false);
        int ordinaryWeek = intent.getIntExtra(HoursReaderActivity.EXTRA_ORDINARY_WEEK, 0);
        // The bilingual reader uses the same literal key (ordinary_week), even
        // though its public constant lives on another class.
        if (!easter && ordinaryWeek <= 0) return;

        String mode = easter ? "EASTER" : "ORDINARY";
        String script = "(function(mode){"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')"
                + ".replace(/\\s+/g,' ').trim().toUpperCase().replace(/:$/,'');}"
                + "function marker(t){if(t==='TIEMPO PASCUAL'||t==='DURANTE EL TIEMPO PASCUAL')return'EASTER';"
                + "if(t==='FUERA DEL TIEMPO PASCUAL')return'NON_EASTER';"
                + "if(t==='TIEMPO ORDINARIO')return'ORDINARY';return'';}"
                + "function major(t){return /^(SALMODIA|LECTURA BREVE|RESPONSORIO BREVE|CANTICO EVANGELICO|PRECES|ORACION|CONCLUSION|HIMNO)$/.test(t);}"
                + "var c=[].slice.call(document.body.children),hide=false;"
                + "for(var i=0;i<c.length;i++){var t=n(c[i].textContent),m=marker(t);"
                + "if(m){hide=(mode==='EASTER')?(m!=='EASTER'):(m==='EASTER');c[i].style.display=hide?'none':'';continue;}"
                + "if(hide&&major(t)){hide=false;c[i].style.display='';continue;}"
                + "if(hide)c[i].style.display='none';}"
                + "})(" + org.json.JSONObject.quote(mode) + ")";
        webView.evaluateJavascript(script, null);
    }
}
