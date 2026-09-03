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
        String css = "html{background:" + background + "!important;color:" + ink + "!important;}"
                + "body{background:" + background + "!important;color:" + ink + "!important;margin:0 auto!important;"
                + "padding:24px!important;box-sizing:border-box!important;width:100%!important;max-width:1040px!important;"
                + "overflow-wrap:break-word!important;-webkit-text-size-adjust:100%!important;}"
                + "body,body *{box-sizing:border-box!important;}"
                + "h1,h2,h3,h4,h5,h6,summary{font-family:inherit!important;text-align:left!important;hyphens:none!important;"
                + "-webkit-hyphens:none!important;letter-spacing:.01em!important;}"
                + "h1{font-size:1.46em!important;line-height:1.24!important;margin:.35em 0 1em!important;}"
                + "h2,h3,h4,summary{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;}"
                + "h2{font-size:1.22em!important;line-height:1.3!important;margin:1.5em 0 .72em!important;}"
                + "h3{font-size:1.08em!important;line-height:1.35!important;margin:1.25em 0 .62em!important;}"
                + "h4{font-size:1em!important;line-height:1.38!important;margin:1.05em 0 .55em!important;}"
                + "section{margin:0 0 2.2em!important;padding:0 0 1.25em!important;border:0!important;"
                + "border-bottom:1px solid " + border + "!important;border-radius:0!important;background:transparent!important;}"
                + "section>h2{font-size:1.18em!important;margin:1.45em 0 .8em!important;}"
                + ".liturgia-papal{margin:0!important;padding:0!important;width:100%!important;max-width:none!important;background:transparent!important;}"
                + ".liturgia-papal p{margin:.76em 0!important;line-height:1.68!important;}"
                + ".ministerium-prose,.ritual-body,.daily-proper .ministerium-prose{line-height:1.68!important;"
                + "text-align:justify!important;text-align-last:left!important;-webkit-hyphens:auto!important;hyphens:auto!important;}"
                + ".ministerium-source-heading{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;"
                + "font-weight:700!important;line-height:1.36!important;margin:1.35em 0 .6em!important;text-align:left!important;}"
                + ".ministerium-celebrant{margin:.78em 0!important;line-height:1.65!important;text-align:left!important;}"
                + ".ministerium-celebrant-label{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;"
                + "font-weight:700!important;margin-right:.28em!important;}"
                + ".ministerium-option-label{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;"
                + "font-size:.88em!important;font-weight:700!important;text-transform:uppercase!important;letter-spacing:.035em!important;"
                + "margin:1.05em 0 .35em!important;text-align:left!important;}"
                + ".ministerium-antiphon,.antiphon,.antiphona{font-weight:600!important;line-height:1.58!important;"
                + "text-align:left!important;margin:.8em 0!important;}"
                + ".ministerium-psalm,.psalm,.psalmus,.psalm-verse,.versus{line-height:1.62!important;text-align:left!important;"
                + "text-align-last:auto!important;hyphens:none!important;-webkit-hyphens:none!important;}"
                + ".lectionary-insert,.reading-section{margin:1.2em 0 2em!important;padding:0!important;border:0!important;background:transparent!important;}"
                + ".reading-reference,.reading-summary{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;text-align:left!important;}"
                + ".reading-text,.lectionary-reading{line-height:1.7!important;text-align:justify!important;text-align-last:left!important;}"
                + ".ministerium-people-response,.psalm-response{font-weight:700!important;margin:.9em 0!important;"
                + "padding:10px 13px!important;border-left:4px solid " + accent + "!important;border-radius:0 8px 8px 0!important;"
                + "background:" + surface + "!important;line-height:1.55!important;text-align:left!important;text-align-last:auto!important;}"
                + ".ministerium-source-rubric,.rubric,.rubrica,.rúbrica{color:" + muted + "!important;"
                + "-webkit-text-fill-color:" + muted + "!important;font-size:.89em!important;font-style:italic!important;"
                + "line-height:1.55!important;text-align:left!important;text-align-last:auto!important;hyphens:none!important;-webkit-hyphens:none!important;}"
                + ".ministerium-essential-rubric{border-left:3px solid " + accent + "!important;padding-left:10px!important;}"
                + ".pending,.proper-language-note{margin:1em 0!important;padding:12px 14px!important;border-left:4px solid " + accent
                + "!important;border-radius:0 8px 8px 0!important;background:" + surface + "!important;color:" + muted + "!important;"
                + "text-align:left!important;line-height:1.55!important;}"
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
