package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Modo de lectura por páginas para lectores largos basados en WebView.
 *
 * La paginación se aplica únicamente a Biblia y documentos/libros. La Liturgia
 * bilingüe mantiene su desplazamiento vertical para no interferir con la
 * sincronización semántica ES/LAT.
 */
public final class ReaderPagination {
    private static final String PREFS = "reader_settings";
    private static final String MODE = "global_reading_mode_41";

    public static final String SCROLL = "scroll";
    public static final String PAGE = "page";

    private ReaderPagination() {}

    public static boolean supports(ReaderContext context) {
        if (context == null) return false;
        String category = context.category == null ? "" : context.category.toLowerCase(java.util.Locale.ROOT);
        return category.contains("biblia")
                || category.contains("documentos")
                || category.contains("magisterio")
                || category.contains("libros");
    }

    public static String mode(Context context) {
        String value = values(context).getString(MODE, SCROLL);
        return PAGE.equals(value) ? PAGE : SCROLL;
    }

    public static boolean isPageMode(Context context, ReaderContext readerContext) {
        return supports(readerContext) && PAGE.equals(mode(context));
    }

    public static void setMode(Context context, String mode) {
        values(context).edit().putString(MODE, PAGE.equals(mode) ? PAGE : SCROLL).apply();
    }

    public static String label(Context context) {
        return PAGE.equals(mode(context)) ? "Página" : "Desplazamiento";
    }

    /** Aplica o retira la maquetación paginada sin modificar el documento fuente. */
    public static void apply(Activity activity, WebView webView, ReaderContext context) {
        if (webView == null) return;
        boolean enabled = isPageMode(activity, context);
        int horizontal = Math.max(18, ReaderPreferences.horizontalPaddingPx(activity));
        int gap = Math.max(28, horizontal * 2);
        int column = Math.max(220, activity.getResources().getDisplayMetrics().widthPixels
                - horizontal * 2);

        String css;
        if (enabled) {
            css = "html{height:100%!important;overflow-x:hidden!important;overflow-y:hidden!important;}"
                    + "body{height:calc(100vh - 2px)!important;min-height:0!important;"
                    + "width:auto!important;max-width:none!important;margin:0!important;"
                    + "padding:18px " + horizontal + "px!important;"
                    + "column-width:" + column + "px!important;column-gap:" + gap + "px!important;"
                    + "column-fill:auto!important;overflow:visible!important;box-sizing:border-box!important;}"
                    + "h1,h2,h3,h4,blockquote,figure,table,.ministerium-canticle,"
                    + ".ministerium-dictionary-card{break-inside:avoid-column!important;}"
                    + "img{max-height:82vh!important;object-fit:contain!important;}";
        } else {
            css = "";
        }

        String script = "(function(){"
                + "var root=document.documentElement;"
                + "var style=document.getElementById('ministerium-pagination-41');"
                + "if(!style){style=document.createElement('style');style.id='ministerium-pagination-41';document.head.appendChild(style);}"
                + "style.innerHTML=" + JSONObject.quote(css) + ";"
                + "if(" + (enabled ? "true" : "false") + "){"
                + "root.classList.add('ministerium-page-mode');window.scrollTo(0,0);"
                + "window.__ministeriumPageStep=function(delta){"
                + "var sc=document.scrollingElement||document.documentElement;"
                + "var width=Math.max(1,window.innerWidth);"
                + "var max=Math.max(0,sc.scrollWidth-width);"
                + "var current=Math.max(0,sc.scrollLeft);"
                + "if(delta>0&&current>=max-4)return false;"
                + "if(delta<0&&current<=4)return false;"
                + "var target=Math.max(0,Math.min(max,current+(delta>0?width:-width)));"
                + "sc.scrollTo(target,0);return true;};"
                + "}else{root.classList.remove('ministerium-page-mode');"
                + "window.__ministeriumPageStep=null;window.scrollTo(0,window.scrollY||0);}})()";
        webView.evaluateJavascript(script, null);
    }

    /**
     * Avanza/retrocede una página. Si ya se alcanzó el borde del documento,
     * entrega el gesto al navegador del lector para pasar al capítulo/entrada.
     */
    public static void step(Activity activity, WebView webView, ReaderContext context,
                            ReaderChrome.Navigator navigator, int delta) {
        if (delta == 0) return;
        if (!isPageMode(activity, context)) {
            moveDocument(navigator, delta);
            return;
        }
        String js = "(function(){return window.__ministeriumPageStep?"
                + "window.__ministeriumPageStep(" + (delta > 0 ? "1" : "-1") + "):false;})()";
        webView.evaluateJavascript(js, result -> {
            if (!"true".equals(result)) moveDocument(navigator, delta);
        });
    }

    private static void moveDocument(ReaderChrome.Navigator navigator, int delta) {
        if (navigator == null) return;
        if (delta > 0 && navigator.canNext()) navigator.next();
        else if (delta < 0 && navigator.canPrevious()) navigator.previous();
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
