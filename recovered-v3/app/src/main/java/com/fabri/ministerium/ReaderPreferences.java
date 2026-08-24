package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

import org.json.JSONObject;

/** Preferencias persistentes de lectura compartidas por TODOS los lectores. */
public final class ReaderPreferences {
    private static final String PREFS = "reader_settings";
    private static final String SIZE = "global_text_zoom";
    private static final String FAMILY = "global_font_family";
    private static final String WEIGHT = "global_font_weight";
    private static final String LINE = "global_line_height";
    private static final String MARGIN = "global_reader_margin";
    private static final String BOOK_FAMILY_PREFIX = "book_font_family_";

    public static final String SERIF = "serif";
    public static final String SANS = "sans-serif";
    public static final String MONO = "monospace";
    public static final String PALATINO = "Palatino";

    public static final String MARGIN_WIDE = "wide";
    public static final String MARGIN_STANDARD = "standard";
    public static final String MARGIN_NARROW = "narrow";

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
        return sanitizeFamily(values(context).getString(FAMILY, SERIF));
    }

    public static void setFamily(Context context, String value) {
        values(context).edit().putString(FAMILY, sanitizeFamily(value)).apply();
    }

    /**
     * Fuente elegida para un libro/documento concreto. Si no existe elección,
     * hereda la familia global. Ningún módulo —incluida la Biblia— queda fuera
     * del cambio global de tipografía.
     */
    public static String familyFor(Context context, String sourceKey) {
        if (sourceKey == null || sourceKey.trim().isEmpty()) return family(context);
        return sanitizeFamily(values(context).getString(
                BOOK_FAMILY_PREFIX + safeKey(sourceKey), family(context)));
    }

    public static void setFamilyFor(Context context, String sourceKey, String value) {
        if (sourceKey == null || sourceKey.trim().isEmpty()) {
            setFamily(context, value);
            return;
        }
        values(context).edit().putString(BOOK_FAMILY_PREFIX + safeKey(sourceKey),
                sanitizeFamily(value)).apply();
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

    public static String margin(Context context) {
        String value = values(context).getString(MARGIN, MARGIN_STANDARD);
        if (MARGIN_WIDE.equals(value) || MARGIN_NARROW.equals(value)) return value;
        return MARGIN_STANDARD;
    }

    public static void setMargin(Context context, String value) {
        String safe = MARGIN_WIDE.equals(value) || MARGIN_NARROW.equals(value)
                ? value : MARGIN_STANDARD;
        values(context).edit().putString(MARGIN, safe).apply();
    }

    /** Padding lateral base CSS; en tablet se combina con max-width centrado. */
    public static int horizontalPaddingPx(Context context) {
        String value = margin(context);
        if (MARGIN_WIDE.equals(value)) return 52;
        if (MARGIN_NARROW.equals(value)) return 14;
        return 28;
    }

    public static String palatinoCssStack() {
        // No se distribuye ningún archivo de fuente: se usan fuentes instaladas
        // por el sistema/WebView y, si no existen, un serif del dispositivo.
        return "'Palatino Linotype','Book Antiqua',Palatino,serif";
    }

    private static String cssFamily(Context context) {
        String selected = family(context);
        return PALATINO.equals(selected) ? palatinoCssStack() : selected;
    }

    public static void reset(Context context) {
        values(context).edit().remove(SIZE).remove(FAMILY).remove(WEIGHT)
                .remove(LINE).remove(MARGIN).apply();
    }

    /**
     * Compatibilidad con lectores existentes. El antiguo parámetro que preservaba
     * una fuente fija para Biblia ya no crea excepciones: la familia GLOBAL gana.
     */
    public static void apply(Context context, WebView webView,
                             boolean ignoredLegacyPreserveTypeface) {
        if (webView == null) return;
        webView.getSettings().setTextZoom(textZoom(context));
        applyInternal(context, webView, cssFamily(context));
    }

    /**
     * Método heredado. Desde 3.1 también respeta la preferencia global; se mantiene
     * para no romper llamadas antiguas mientras se termina la migración.
     */
    public static void applyPalatino(Context context, WebView webView) {
        if (webView == null) return;
        webView.getSettings().setTextZoom(textZoom(context));
        applyInternal(context, webView, cssFamily(context));
    }

    /** Aplica la fuente persistida de un libro concreto o hereda la global. */
    public static void applyForSource(Context context, WebView webView, String sourceKey) {
        if (webView == null) return;
        webView.getSettings().setTextZoom(textZoom(context));
        String selected = familyFor(context, sourceKey);
        applyInternal(context, webView,
                PALATINO.equals(selected) ? palatinoCssStack() : selected);
    }

    private static void applyInternal(Context context, WebView webView, String family) {
        String palette = ThemeUtils.SEPIA.equals(ThemeUtils.getMode(context))
                ? "background:#F0E2C7!important;color:#30261E!important;" : "";
        int horizontal = horizontalPaddingPx(context);
        String css = "html,body{" + palette + "}body{font-family:" + family
                + "!important;font-weight:" + weight(context) + "!important;line-height:"
                + lineHeight(context) + "!important;width:100%!important;max-width:1040px!important;"
                + "margin-left:auto!important;margin-right:auto!important;"
                + "padding-left:" + horizontal + "px!important;padding-right:" + horizontal
                + "px!important;box-sizing:border-box!important;}"
                + "body,body p,body span,body div,body li,body td,body blockquote{font-family:"
                + family + "!important;}"
                + "@media(min-width:700px){body{padding-left:" + (horizontal + 12)
                + "px!important;padding-right:" + (horizontal + 12) + "px!important;}}";
        String script = "(function(){var s=document.getElementById('ministerium-reader-prefs');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-reader-prefs';"
                + "document.head.appendChild(s);}s.innerHTML=" + JSONObject.quote(css)
                + ";})()";
        webView.evaluateJavascript(script, null);
    }

    private static String sanitizeFamily(String value) {
        if (SANS.equals(value) || MONO.equals(value) || PALATINO.equals(value)) return value;
        return SERIF;
    }

    private static String safeKey(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_");
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
