package com.fabri.ministerium;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import org.json.JSONObject;

public class MassReadingReaderActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_SCROLL_QUOTE = "scroll_quote";

    private Calendar date;
    private WebView webView;
    private String pendingQuote = "";
    private int pendingScrollY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mass_reading_reader);
        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        pendingQuote = getIntent().getStringExtra(EXTRA_SCROLL_QUOTE);
        if (pendingQuote == null) pendingQuote = "";
        pendingScrollY = getIntent().getIntExtra("restore_scroll_y", 0);
        webView = findViewById(R.id.massWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                applyStyle();
                ReaderPreferences.apply(MassReadingReaderActivity.this, webView, false);
                ReflectionUtils.injectHighlights(MassReadingReaderActivity.this, webView, sourceKey());
                ReadingMarkerUtils.injectHighlights(MassReadingReaderActivity.this, webView, sourceKey());
                UniversalSelectionMenu.restoreHighlights(MassReadingReaderActivity.this,
                        webView, sourceKey());
                if (!pendingQuote.isEmpty()) {
                    ReadingMarkerUtils.scrollToQuote(webView, pendingQuote);
                    pendingQuote = "";
                }
                if (pendingScrollY > 0) {
                    int y = pendingScrollY;
                    pendingScrollY = 0;
                    webView.postDelayed(() -> webView.scrollTo(0, y), 180);
                }
            }
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> back());
        findViewById(R.id.btnOriginalSource).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MassReadingsRepository.sourceUrl(date)));
            startActivity(intent);
        });
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), context(),
                new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return hasDate(-1); }
                    @Override public boolean canNext() { return hasDate(1); }
                    @Override public void previous() { moveDate(-1); }
                    @Override public void next() { moveDate(1); }
                }, false);
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
        showReading();
    }

    private void showReading() {
        try {
            String title = "Lecturas de la Misa";
            try { title = LiturgicalResolver.resolve(this, date).celebration; }
            catch (Exception ignored) {}
            ((TextView) findViewById(R.id.txtReaderTitle)).setText(title);
            ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                    dateLabel() + " · Leccionario sin conexión");
            UniversalSelectionMenu.attach(this, webView, context());
            ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
            webView.loadDataWithBaseURL(null, MassReadingsRepository.read(this, date),
                    "text/html", "UTF-8", null);
        } catch (Exception error) {
            Toast.makeText(this, "Estas lecturas todavía no están guardadas.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean hasDate(int amount) {
        Calendar wanted = (Calendar) date.clone();
        wanted.add(Calendar.DATE, amount);
        return MassReadingsRepository.has(this, wanted);
    }

    private void moveDate(int amount) {
        if (!hasDate(amount)) return;
        date.add(Calendar.DATE, amount);
        showReading();
    }

    private void back() {
        finish();
    }

    @Override public void onBackPressed() { back(); }

    private String sourceKey() {
        return "mass:" + date.get(Calendar.YEAR) + ":" + date.get(Calendar.MONTH)
                + ":" + date.get(Calendar.DAY_OF_MONTH);
    }
    private String dateLabel() {
        return String.format(java.util.Locale.US, "%02d/%02d/%04d",
                date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.YEAR));
    }
    private ReaderContext context() {
        return new ReaderContext("Leccionario", sourceKey(), "Lecturas del día",
                dateLabel(), "Lecturas del día", false);
    }

    @Override protected void onPause() {
        try {
            ContinueReadingStore.save(this, "Lecturas del día", dateLabel(),
                    MassReadingReaderActivity.class,
                    new JSONObject().put(EXTRA_YEAR, date.get(Calendar.YEAR))
                            .put(EXTRA_MONTH, date.get(Calendar.MONTH))
                            .put(EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH)),
                    webView.getScrollY());
        } catch (Exception ignored) {}
        super.onPause();
    }

    private void applyStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String wine = dark ? "#D9B96F" : "#772233";
        String card = dark ? "#332C28" : "#F5EDDF";
        String css = "html,body{background:" + background + "!important;color:" + ink
                + "!important}body,body *{color:" + ink + "!important;"
                + "-webkit-text-fill-color:" + ink + "!important}"
                + "body{font-family:serif;line-height:1.65;margin:0;padding:24px;"
                + "width:100%;max-width:none;box-sizing:border-box;overflow-wrap:anywhere}"
                + "body *{max-width:100%;box-sizing:border-box}img,table{max-width:100%!important;"
                + "height:auto!important}h1{font-size:1.45em;line-height:1.25}"
                + ".lectionary-label{text-align:center;font-size:1.25em;font-weight:bold;letter-spacing:.08em;margin:2em 0 1.2em}"
                + ".reading-section{margin:0 0 2.25em}.reading-section h2{color:" + wine
                + "!important;-webkit-text-fill-color:" + wine
                + "!important;font-size:1.18em;margin:1.8em 0 .75em}"
                + ".reading-summary{font-style:italic;text-align:center;color:" + wine + "!important;margin:.25em 0 1em;line-height:1.45}"
                + ".reading-reference{font-weight:normal;text-align:right;color:" + wine + "!important;margin:.25em 0 1.15em;line-height:1.45}"
                + ".reading-body{margin:0 0 1.15em;line-height:1.7}"
                + ".psalm-response{margin:1em 0 .8em;line-height:1.5}"
                + ".psalm-stanza{margin:0 0 .85em;padding-left:.55em;line-height:1.55}"
                + ".celebration{padding:14px;background:" + card + ";border-radius:8px;font-weight:bold}"
                + ".source{margin-top:2em;color:" + (dark ? "#C8BDB0" : "#6F665E")
                + "!important;-webkit-text-fill-color:"
                + (dark ? "#C8BDB0" : "#6F665E") + "!important;font-size:.85em}";
        css += ".ministerium-highlight{background:#F6E58D!important;color:#231F1B!important;"
                + "-webkit-text-fill-color:#231F1B!important;padding:1px 2px;border-radius:2px}"
                + "@media(min-width:700px){body{padding-left:48px;padding-right:48px}}"
                + "@media(min-width:1100px){body{padding-left:64px;padding-right:64px}}";
        String script = "(function(){var s=document.createElement('style');s.innerHTML='"
                + css.replace("'", "\\'") + "';document.head.appendChild(s);"
                + "var all=document.body.querySelectorAll('*');for(var i=0;i<all.length;i++){"
                + "all[i].style.setProperty('color','" + ink + "','important');"
                + "all[i].style.setProperty('-webkit-text-fill-color','" + ink + "','important');}"
                + "var headings=document.querySelectorAll('.reading-section h2');"
                + "for(var h=0;h<headings.length;h++){headings[h].style.setProperty('color','" + wine
                + "','important');headings[h].style.setProperty('-webkit-text-fill-color','"
                + wine + "','important');}"
                + "var accents=document.querySelectorAll('.reading-reference,.reading-summary');"
                + "for(var r=0;r<accents.length;r++){accents[r].style.setProperty('color','" + wine
                + "','important');accents[r].style.setProperty('-webkit-text-fill-color','"
                + wine + "','important');}"
                + "var source=document.querySelectorAll('.source');for(var m=0;m<source.length;m++){"
                + "source[m].style.setProperty('color','" + (dark ? "#C8BDB0" : "#6F665E")
                + "','important');source[m].style.setProperty('-webkit-text-fill-color','"
                + (dark ? "#C8BDB0" : "#6F665E") + "','important');}})()";
        webView.evaluateJavascript(script, null);
    }

    @Override protected void onDestroy() {
        if (webView != null) { WebViewCleanup.destroy(webView); webView = null; }
        super.onDestroy();
    }
}
