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

    /**
     * Rearma la paginación después de una carga WebView. Se programan varios
     * intentos breves porque ReaderChrome puede enlazarse antes de loadUrl/loadData.
     */
    public static void arm(Activity activity, WebView webView, ReaderContext context) {
        if (webView == null || !supports(context)) return;
        apply(activity, webView, context);
        webView.postDelayed(() -> apply(activity, webView, context), 180L);
        webView.postDelayed(() -> apply(activity, webView, context), 650L);
        webView.postDelayed(() -> apply(activity, webView, context), 1400L);
    }

    /** Aplica o retira la maquetación paginada sin modificar el documento fuente. */
    public static void apply(Activity activity, WebView webView, ReaderContext context) {
        if (webView == null || !supports(context)) return;
        boolean enabled = isPageMode(activity, context);
        int horizontal = Math.max(18, ReaderPreferences.horizontalPaddingPx(activity));
        int gap = Math.max(28, horizontal * 2);

        String css;
        if (enabled) {
            css = "html{height:100%!important;overflow-x:hidden!important;overflow-y:hidden!important;}"
                    + "body{height:calc(100vh - 2px)!important;min-height:0!important;"
                    + "width:auto!important;max-width:none!important;margin:0!important;"
                    + "padding:18px " + horizontal + "px!important;"
                    + "column-width:calc(100vw - " + (horizontal * 2) + "px)!important;"
                    + "column-gap:" + gap + "px!important;column-fill:auto!important;"
                    + "overflow:visible!important;box-sizing:border-box!important;}"
                    + "h1,h2,h3,h4,blockquote,figure,table,.ministerium-canticle,"
                    + ".ministerium-dictionary-card{break-inside:avoid-column!important;}"
                    + "img{max-height:82vh!important;object-fit:contain!important;}";
        } else {
            css = "";
        }

        String script = "(function(){"
                + "if(!document.head||!document.documentElement)return false;"
                + "var root=document.documentElement;"
                + "var style=document.getElementById('ministerium-pagination-41');"
                + "if(!style){style=document.createElement('style');style.id='ministerium-pagination-41';document.head.appendChild(style);}"
                + "style.innerHTML=" + JSONObject.quote(css) + ";"
                + "if(" + (enabled ? "true" : "false") + "){"
                + "root.classList.add('ministerium-page-mode');"
                + "var storageKey='ministerium-page-41:'+(location.href||'document').split('#')[0];"
                + "var saved=0;try{saved=parseInt(localStorage.getItem(storageKey)||'0',10)||0;}catch(e){}"
                + "window.__ministeriumPageStep=function(delta){"
                + "var sc=document.scrollingElement||document.documentElement;"
                + "var width=Math.max(1,window.innerWidth);"
                + "var max=Math.max(0,sc.scrollWidth-width);"
                + "var current=Math.max(0,sc.scrollLeft);"
                + "if(delta>0&&current>=max-4)return false;"
                + "if(delta<0&&current<=4)return false;"
                + "var target=Math.max(0,Math.min(max,current+(delta>0?width:-width)));"
                + "var page=Math.max(0,Math.round(target/width));"
                + "try{localStorage.setItem(storageKey,String(page));}catch(e){}"
                + "window.scrollTo(target,0);return true;};"
                + "setTimeout(function(){var sc=document.scrollingElement||document.documentElement;"
                + "var width=Math.max(1,window.innerWidth);var max=Math.max(0,sc.scrollWidth-width);"
                + "window.scrollTo(Math.max(0,Math.min(max,saved*width)),0);},0);"
                + "}else{root.classList.remove('ministerium-page-mode');"
                + "window.__ministeriumPageStep=null;window.scrollTo(0,window.scrollY||0);}return true;})()";
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
            moveDocument(activity, webView, context, navigator, delta);
            return;
        }
        String js = "(function(){return window.__ministeriumPageStep?"
                + "window.__ministeriumPageStep(" + (delta > 0 ? "1" : "-1") + "):false;})()";
        webView.evaluateJavascript(js, result -> {
            if (!"true".equals(result)) {
                moveDocument(activity, webView, context, navigator, delta);
            }
        });
    }

    private static void moveDocument(Activity activity, WebView webView, ReaderContext context,
                                     ReaderChrome.Navigator navigator, int delta) {
        if (navigator == null) return;
        boolean moved = false;
        if (delta > 0 && navigator.canNext()) {
            navigator.next();
            moved = true;
        } else if (delta < 0 && navigator.canPrevious()) {
            navigator.previous();
            moved = true;
        }
        if (moved && isPageMode(activity, context)) arm(activity, webView, context);
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
