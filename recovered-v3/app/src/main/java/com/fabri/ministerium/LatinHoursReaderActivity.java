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

import org.json.JSONObject;

import java.io.File;

/** Liturgia Horarum en latín con la misma jerarquía visual del lector español. */
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
                applySourceCleanup();
                ReaderPreferences.apply(LatinHoursReaderActivity.this, webView, false);
                LiturgicalWebStyle.apply(LatinHoursReaderActivity.this, webView);
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
                        title, "Latín", "Liturgia", true, false), null, false);

        try {
            File target = LatinContentManager.hourFile(this, year, path);
            webView.loadUrl(Uri.fromFile(target).toString());
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir esta Hora en latín.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * El paquete latino conserva a veces restos editoriales de la fuente. Se
     * limpian sin imponer colores inline para que la paleta compartida pueda
     * presentar himno, salmodia, lectura, responsorio, cántico, preces y oración
     * exactamente con la jerarquía del lector español.
     */
    private void applySourceCleanup() {
        String script = "(function(){"
                + "var old=document.getElementById('ministerium-latin-source-cleanup');"
                + "if(!old){old=document.createElement('style');old.id='ministerium-latin-source-cleanup';"
                + "old.textContent='.patka{display:none!important}img,table{max-width:100%!important;height:auto!important}';"
                + "document.head.appendChild(old);}"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){"
                + "var t=(links[i].textContent||'').trim();if(t==='↑'||t==='←'||t==='→'){"
                + "var p=links[i].parentElement;links[i].style.display='none';"
                + "if(p&&p.tagName==='P'&&(p.textContent||'').trim().length<12)p.style.display='none';}}"
                + "})()";
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
