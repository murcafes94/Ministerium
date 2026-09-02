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
     * El EPUB latino conserva reglas de maquetación pensadas para páginas fijas.
     * Aquí se eliminan anchos, centrados, flotados y espaciadores editoriales sin
     * alterar el texto. Después se aplica la misma jerarquía visual de la Liturgia
     * de las Horas española.
     */
    private void applySourceCleanup() {
        String script = "(function(){"
                + "document.body.classList.add('ministerium-latin-hours');"
                + "var old=document.getElementById('ministerium-latin-source-cleanup');"
                + "if(!old){old=document.createElement('style');old.id='ministerium-latin-source-cleanup';"
                + "old.textContent='"
                + ".patka{display:none!important}"
                + "html,body{width:100%!important;max-width:none!important;min-width:0!important;margin:0!important;}"
                + "body{padding:22px!important;}"
                + "body div,body section,body article,body main,body header,body footer,body blockquote{width:auto!important;max-width:100%!important;min-width:0!important;margin-left:0!important;margin-right:0!important;float:none!important;position:static!important;}"
                + "body p,body li{max-width:100%!important;margin-left:0!important;margin-right:0!important;}"
                + "img,table{max-width:100%!important;height:auto!important;}"
                + "table{width:100%!important;}"
                + ".ministerium-latin-prose{text-align:justify!important;text-align-last:left!important;line-height:1.68!important;-webkit-hyphens:auto!important;hyphens:auto!important;}"
                + ".ministerium-latin-heading{font-weight:700!important;text-align:left!important;margin:1.45em 0 .65em!important;}"
                + ".ministerium-latin-rubric{font-style:italic!important;text-align:left!important;font-size:.9em!important;}"
                + ".ministerium-latin-psalm,.ministerium-latin-antiphon{text-align:left!important;text-align-last:auto!important;-webkit-hyphens:none!important;hyphens:none!important;}';"
                + "document.head.appendChild(old);}"
                + "var blocks=document.querySelectorAll('div,section,article,main,header,footer,blockquote,p');"
                + "for(var b=0;b<blocks.length;b++){var e=blocks[b];"
                + "e.style.removeProperty('width');e.style.removeProperty('min-width');e.style.removeProperty('max-width');"
                + "e.style.removeProperty('margin-left');e.style.removeProperty('margin-right');e.style.removeProperty('float');"
                + "e.style.removeProperty('left');e.style.removeProperty('right');"
                + "if((e.tagName==='DIV'||e.tagName==='P')&&!(e.textContent||'').trim()&&!e.querySelector('img,table,svg,audio,video')){"
                + "e.style.display='none';}}"
                + "function norm(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "var ps=document.querySelectorAll('p');for(var p=0;p<ps.length;p++){var t=norm(ps[p].textContent);if(!t)continue;"
                + "if(/^(HYMNUS|SALMODIA|LECTIO BREVIS|LECTIO|RESPONSORIUM BREVE|RESPONSORIUM|CANTICUM EVANGELICUM|PRECES|ORATIO)$/.test(t)||"
                + "/^(PSALMUS|CANTICUM)\\s+[0-9IVXLCDM]/.test(t)){ps[p].classList.add('ministerium-latin-heading');}"
                + "else if(/^ANT\\.|^ANTIPHONA\\b/.test(t)){ps[p].classList.add('ministerium-latin-antiphon');}"
                + "else if(/^PS\\.|^℟\\.|^℣\\.|^V\\.|^R\\./.test(t)){ps[p].classList.add('ministerium-latin-psalm');}"
                + "else if(/^(SACERDOS|DIACONUS|DEINDE|TUNC|POSTEA|SI |UBI |OMNES |POPULUS )/.test(t)){ps[p].classList.add('ministerium-latin-rubric');}"
                + "else if(t.length>95){ps[p].classList.add('ministerium-latin-prose');}}"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){"
                + "var t=(links[i].textContent||'').trim();if(t==='↑'||t==='←'||t==='→'){"
                + "var parent=links[i].parentElement;links[i].style.display='none';"
                + "if(parent&&parent.tagName==='P'&&(parent.textContent||'').trim().length<12)parent.style.display='none';}}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }

    private void back() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else finish();
    }

    @Override public void onBackPressed() { back(); }
    @Override protected void onDestroy() {
        if (webView != null) { WebViewCleanup.destroy(webView); webView = null; }
        super.onDestroy();
    }
    private static String value(String value) { return value == null ? "" : value; }
}
