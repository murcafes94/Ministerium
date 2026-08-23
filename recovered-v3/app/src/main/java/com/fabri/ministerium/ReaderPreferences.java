package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

import org.json.JSONObject;

/** Preferencias únicas de lectura para todos los documentos salvo la fuente bíblica. */
public final class ReaderPreferences {
    private static final String PREFS = "reader_settings";
    private static final String SIZE = "global_text_zoom";
    private static final String FAMILY = "global_font_family";
    private static final String WEIGHT = "global_font_weight";
    private static final String LINE = "global_line_height";
    public static final String SERIF = "serif";
    public static final String SANS = "sans-serif";
    public static final String MONO = "monospace";

    private ReaderPreferences() {}

    public static int textZoom(Context context) {
        return values(context).getInt(SIZE, migrateLegacySize(context));
    }

    public static void setTextZoom(Context context, int value) {
        values(context).edit().putInt(SIZE, clamp(value, 80, 180)).apply();
    }

    public static int changeTextZoom(Context context, int delta) {
        int next = clamp(textZoom(context) + delta, 80, 180);
        setTextZoom(context, next);
        return next;
    }

    public static String family(Context context) {
        String value = values(context).getString(FAMILY, SERIF);
        return SANS.equals(value) || MONO.equals(value) ? value : SERIF;
    }

    public static void setFamily(Context context, String value) {
        values(context).edit().putString(FAMILY,
                SANS.equals(value) || MONO.equals(value) ? value : SERIF).apply();
    }

    public static int weight(Context context) {
        return clamp(values(context).getInt(WEIGHT, 400), 300, 700);
    }

    public static void setWeight(Context context, int value) {
        values(context).edit().putInt(WEIGHT, clamp(value, 300, 700)).apply();
    }

    public static float lineHeight(Context context) {
        return Math.max(1.25f, Math.min(2.1f,
                values(context).getFloat(LINE, 1.65f)));
    }

    public static void setLineHeight(Context context, float value) {
        values(context).edit().putFloat(LINE,
                Math.max(1.25f, Math.min(2.1f, value))).apply();
    }

    public static void reset(Context context) {
        values(context).edit().remove(SIZE).remove(FAMILY).remove(WEIGHT)
                .remove(LINE).apply();
    }

    public static void apply(Context context, WebView webView,
                             boolean preserveBibleTypeface) {
        if (webView == null) return;
        webView.getSettings().setTextZoom(textZoom(context));
        String family = preserveBibleTypeface ? "inherit" : family(context);
        String palette = ThemeUtils.SEPIA.equals(ThemeUtils.getMode(context))
                ? "background:#F0E2C7!important;color:#30261E!important;" : "";
        String css = "html,body{" + palette + "}body{font-family:" + family + "!important;font-weight:"
                + weight(context) + "!important;line-height:"
                + lineHeight(context) + "!important;}";
        String script = "(function(){var s=document.getElementById('ministerium-reader-prefs');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-reader-prefs';"
                + "document.head.appendChild(s);}s.innerHTML=" + JSONObject.quote(css)
                + ";})()";
        webView.evaluateJavascript(script, null);
    }

    private static int migrateLegacySize(Context context) {
        SharedPreferences values = values(context);
        if (values.contains("bible_text_zoom")) return values.getInt("bible_text_zoom", 112);
        if (values.contains("epub_text_zoom")) return values.getInt("epub_text_zoom", 110);
        return 110;
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
