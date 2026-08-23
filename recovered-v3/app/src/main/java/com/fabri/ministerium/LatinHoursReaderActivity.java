package com.fabri.ministerium;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class LatinHoursReaderActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "latin_year";
    public static final String EXTRA_PATH = "latin_path";
    public static final String EXTRA_TITLE = "latin_title";

    private WebView webView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_reader);

        int year = getIntent().getIntExtra(EXTRA_YEAR, 2026);
        String path = value(getIntent().getStringExtra(EXTRA_PATH));
        String title = value(getIntent().getStringExtra(EXTRA_TITLE));
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                "Liturgia Horarum · latine · " + year);
        webView = findViewById(R.id.hoursWebView);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                applyStyle();
                ReaderPreferences.apply(LatinHoursReaderActivity.this, webView, false);
            }
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> back());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btnReaderMore).setVisibility(View.GONE);
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader),
                new ReaderContext("Liturgia en Latín", "latin:" + year + ":" + path,
                        title, "Latín", "Liturgia", true), null, false);

        try {
            File target = LatinContentManager.hourFile(this, year, path);
            webView.loadUrl(Uri.fromFile(target).toString());
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir esta Hora en latín.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void applyStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#772233";
        String css = "html,body{background:" + background + "!important;color:" + ink
                + "!important;width:100%!important;max-width:none!important;box-sizing:border-box}"
                + "body,body *{color:" + ink + "!important;"
                + "-webkit-text-fill-color:" + ink + "!important;text-shadow:none!important;"
                + "-webkit-text-shadow:none!important}"
                + "body{font-family:serif!important;line-height:1.65!important;"
                + "margin:0!important;padding:24px!important;box-sizing:border-box;"
                + "overflow-wrap:anywhere!important}body *{max-width:100%;box-sizing:border-box}"
                + "a,.redtitle,.redsmall1,[style*=red],[style*=\"#CC0000\"],"
                + "[style*=\"#cc0000\"]{color:" + accent + "!important;"
                + "-webkit-text-fill-color:" + accent + "!important}"
                + "img,table{max-width:100%!important;height:auto!important}"
                + ".patka{display:none!important}"
                + "@media(min-width:700px){body{padding-left:48px!important;"
                + "padding-right:48px!important}}"
                + "@media(min-width:1100px){body{padding-left:64px!important;"
                + "padding-right:64px!important}}";
        String script = "(function(){var s=document.createElement('style');s.innerHTML='"
                + css.replace("'", "\\'") + "';document.head.appendChild(s);"
                + "var all=document.body.querySelectorAll('*');for(var i=0;i<all.length;i++){"
                + "all[i].style.setProperty('color','" + ink + "','important');"
                + "all[i].style.setProperty('-webkit-text-fill-color','" + ink + "','important');"
                + "all[i].style.setProperty('text-shadow','none','important');}"
                + "var marked=document.querySelectorAll('a,.redtitle,.redsmall1,[style*=red],"
                + "[style*=\"#CC0000\"],[style*=\"#cc0000\"]');"
                + "for(var m=0;m<marked.length;m++){marked[m].style.setProperty('color','" + accent
                + "','important');marked[m].style.setProperty('-webkit-text-fill-color','"
                + accent + "','important');}"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){"
                + "if((links[i].textContent||'').trim()==='↑')links[i].style.display='none';}})()";
        webView.evaluateJavascript(script, null);
    }

    private void back() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else finish();
    }

    @Override public void onBackPressed() { back(); }
    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
    private static String value(String value) { return value == null ? "" : value; }
}
