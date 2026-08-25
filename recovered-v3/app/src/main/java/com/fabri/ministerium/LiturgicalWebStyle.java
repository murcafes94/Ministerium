package com.fabri.ministerium;

import android.content.Context;
import android.webkit.WebView;

import org.json.JSONObject;

/** Shared visual line for Lectionary-like liturgical readers. */
public final class LiturgicalWebStyle {
    private LiturgicalWebStyle() {}

    public static void apply(Context context, WebView webView) {
        if (webView == null) return;
        boolean dark = ThemeUtils.isDark(context);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#772233";
        String surface = dark ? "#332C28" : "#F5EDDF";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String border = dark ? "#594D43" : "#DED2C2";
        String css = "html,body{background:" + background + "!important;color:" + ink + "!important;}"
                + "body{margin:0!important;padding:24px!important;box-sizing:border-box!important;max-width:1040px!important;}"
                + "h1{font-size:1.42em!important;line-height:1.25!important;margin:.4em 0 1em!important;}"
                + "h2,h3,h4,summary{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;}"
                + "section{margin:0 0 2.2em!important;padding:0 0 1.25em!important;border:0!important;"
                + "border-bottom:1px solid " + border + "!important;border-radius:0!important;background:transparent!important;}"
                + "section>h2{font-size:1.18em!important;margin:1.45em 0 .8em!important;}"
                + ".liturgia-papal{margin:0!important;padding:0!important;background:transparent!important;}"
                + ".liturgia-papal p{margin:.72em 0!important;line-height:1.68!important;}"
                + ".lectionary-insert,.reading-section{margin:1.2em 0 2em!important;padding:0!important;border:0!important;background:transparent!important;}"
                + ".reading-reference,.reading-summary{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;}"
                + ".ministerium-people-response,.psalm-response{font-weight:700!important;margin:.85em 0!important;"
                + "padding:10px 13px!important;border-left:4px solid " + accent + "!important;border-radius:0 8px 8px 0!important;"
                + "background:" + surface + "!important;}"
                + ".ministerium-source-rubric,.rubric,.rubrica,.rúbrica{color:" + muted + "!important;"
                + "-webkit-text-fill-color:" + muted + "!important;font-size:.88em!important;font-style:italic!important;}"
                + ".ministerium-essential-rubric{border-left:3px solid " + accent + "!important;padding-left:10px!important;}"
                + ".pending{margin:1em 0!important;padding:12px 14px!important;border-left:4px solid " + accent
                + "!important;border-radius:0 8px 8px 0!important;background:" + surface + "!important;color:" + muted + "!important;}"
                + ".parallel{gap:18px!important}.lang{color:" + accent + "!important;border-bottom:1px solid " + border + "!important;}"
                + "#ministerium-rubric-toggle{color:" + accent + "!important;border-color:" + accent + "!important;}"
                + "@media(min-width:700px){body{padding-left:48px!important;padding-right:48px!important;}}"
                + "@media(min-width:1100px){body{padding-left:64px!important;padding-right:64px!important;}}";
        String script = "(function(){var s=document.getElementById('ministerium-liturgical-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-liturgical-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + JSONObject.quote(css) + ";})()";
        webView.evaluateJavascript(script, null);
    }
}
