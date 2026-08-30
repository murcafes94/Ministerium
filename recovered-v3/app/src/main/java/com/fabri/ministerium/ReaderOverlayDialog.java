package com.fabri.ministerium;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public final class ReaderOverlayDialog {
    private ReaderOverlayDialog() {}

    public static void show(Activity activity, String title, String bodyHtml) {
        boolean dark = ThemeUtils.isDark(activity);
        int background = Color.parseColor(dark ? "#181818" : "#FFFDF7");
        int header = Color.parseColor(dark ? "#242424" : "#6E1D2A");
        int foreground = Color.parseColor(dark ? "#F3EDE4" : "#2A2521");
        String accent = dark ? "#E1C57A" : "#6E1D2A";

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);

        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        toolbar.setBackgroundColor(header);
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(19);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button close = new Button(activity);
        close.setText("×");
        close.setTextSize(24);
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(Color.TRANSPARENT);
        toolbar.addView(close, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 48)));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        WebView reader = new WebView(activity);
        reader.setBackgroundColor(background);
        reader.getSettings().setTextZoom(105);
        reader.getSettings().setJavaScriptEnabled(true);
        reader.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleInternal(view, heading, url);
            }
            @Override public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return handleInternal(view, heading, request.getUrl().toString());
            }

            private boolean handleInternal(WebView view, TextView titleView, String url) {
                Uri uri = Uri.parse(url);
                if (!"ministerium".equals(uri.getScheme())) return false;
                if ("translate".equals(uri.getHost())) {
                    String text = uri.getQueryParameter("text");
                    String target = uri.getQueryParameter("target");
                    if (text == null || text.trim().isEmpty()) return true;
                    if (!("es".equals(target) || "la".equals(target) || "en".equals(target))) {
                        target = "es";
                    }
                    String label = "la".equals(target) ? "Latín"
                            : "en".equals(target) ? "Inglés" : "Español";
                    titleView.setText("Traductor en línea · " + label);
                    view.getSettings().setDomStorageEnabled(true);
                    String destination = "https://translate.google.com/?sl=auto&tl=" + target
                            + "&text=" + Uri.encode(text) + "&op=translate";
                    try {
                        view.loadUrl(destination);
                    } catch (Exception error) {
                        Toast.makeText(activity, "No se pudo abrir el traductor en línea.",
                                Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                if ("rae".equals(uri.getHost())) {
                    String word = uri.getQueryParameter("word");
                    if (word == null || word.trim().isEmpty()) return true;
                    titleView.setText("RAE · " + word);
                    view.evaluateJavascript("document.body.innerHTML='<p>Consultando RAE…</p>'", null);
                    new Thread(() -> {
                        try {
                            String html = RaeOnlineRepository.lookupHtml(activity, word);
                            activity.runOnUiThread(() -> view.evaluateJavascript(
                                    "document.body.innerHTML=" + JSONObject.quote(html), null));
                        } catch (Exception error) {
                            String message = error.getMessage() == null
                                    ? "No se pudo consultar RAE en este momento." : error.getMessage();
                            activity.runOnUiThread(() -> view.evaluateJavascript(
                                    "document.body.innerHTML=" + JSONObject.quote(
                                            "<article class=\"dictionary-card\"><h2>RAE</h2><p>"
                                                    + escape(message) + "</p><p class=\"dictionary-source\">Los diccionarios offline siguen disponibles.</p></article>"), null));
                        }
                    }).start();
                    return true;
                }
                return false;
            }
        });
        String css = "html,body{margin:0;background:" + (dark ? "#181818" : "#fffdf7")
                + ";color:" + (dark ? "#f3ede4" : "#2a2521") + ";font-family:serif;"
                + "line-height:1.58}body{padding:18px;box-sizing:border-box}"
                + "article,.dictionary-card{display:block;padding:17px;margin:0 0 14px;"
                + "border:1px solid " + (dark ? "#48413B" : "#D8C9B5")
                + ";border-left:5px solid " + accent + ";border-radius:10px;"
                + "background:" + (dark ? "#242424" : "#ffffff") + ";box-sizing:border-box}"
                + "h1,h2,h3{color:" + accent + ";line-height:1.25;margin:.1em 0 .45em}"
                + ".source,.dictionary-source{font-size:.84em;font-style:italic;color:"
                + (dark ? "#c8bdb0" : "#6f665e") + ";margin-top:0}"
                + ".translation-actions{display:flex;gap:8px;flex-wrap:wrap}"
                + ".translation-button{display:inline-block;padding:10px 14px;border-radius:8px;"
                + "border:1px solid " + accent + ";text-decoration:none;font-family:sans-serif;"
                + "font-weight:bold}"
                + "p{margin:.65em 0}a{color:" + accent + "}img,table{max-width:100%;height:auto}"
                + "*{overflow-wrap:anywhere}@media(min-width:700px){body{padding:22px}}";
        String document = "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>" + css + "</style></head><body>" + bodyHtml + "</body></html>";
        reader.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null);
        root.addView(reader, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        dialog.setContentView(root);
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(ignored -> reader.destroy());
        dialog.show();

        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = window.getAttributes();
        int widthDp = activity.getResources().getConfiguration().screenWidthDp;
        int heightDp = activity.getResources().getConfiguration().screenHeightDp;
        if (widthDp >= 700 && widthDp > heightDp) {
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * .47f);
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.END;
        } else {
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * .78f);
            params.gravity = Gravity.BOTTOM;
        }
        window.setAttributes(params);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }
}
