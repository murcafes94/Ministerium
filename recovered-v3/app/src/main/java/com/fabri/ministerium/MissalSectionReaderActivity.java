package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/** Reader for the semantic/native Missal generated from Liturgia Papal sources. */
public class MissalSectionReaderActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "missal_reader_year";
    public static final String EXTRA_MONTH = "missal_reader_month";
    public static final String EXTRA_DAY = "missal_reader_day";
    public static final String EXTRA_SECTION = "missal_reader_section";
    public static final String EXTRA_LANGUAGE = "missal_reader_language";

    private MinisteriumWebView webView;
    private Calendar date;
    private String section;
    private String language;
    private MissalDocument31.Result result;
    private boolean selectionAttached;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_reader);
        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        section = value(getIntent().getStringExtra(EXTRA_SECTION), "day");
        language = "la".equals(getIntent().getStringExtra(EXTRA_LANGUAGE)) ? "la" : "es";
        webView = findViewById(R.id.hoursWebView);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReaderSearch).setVisibility(View.GONE);
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                ReaderPreferences.apply(MissalSectionReaderActivity.this, webView, false);
                LiturgicalWebStyle.apply(MissalSectionReaderActivity.this, webView);
                ProseParagraphNormalizer.inject(webView);
                MissalCompactView.inject(webView);
                MissalRuntimeFixes31.inject(webView);
                MissalAlternativeOptions31.inject(webView);
                ReaderContext context = readerContext();
                UniversalSelectionMenu.restoreHighlights(MissalSectionReaderActivity.this,
                        webView, context.sourceKey);
            }
        });
        loadAsync();
    }

    private void loadAsync() {
        TextView title = findViewById(R.id.txtReaderTitle);
        TextView subtitle = findViewById(R.id.txtReaderSubtitle);
        title.setText("la".equals(language) ? "Missale Romanum" : "Misal Romano");
        subtitle.setText("Preparando textos del día…");
        webView.setVisibility(View.INVISIBLE);
        new Thread(() -> {
            try {
                LiturgicalDayPackage dayPackage = LiturgicalDayCache.prepare(
                        getApplicationContext(), date, true);
                MissalDocument31.Result raw = "ordinary".equals(section)
                        ? MissalOrdinaryDocument41.build(
                                getApplicationContext(), date, language)
                        : MissalDocument31.build(
                                getApplicationContext(), date, section, language);
                String fallbackHtml = MercabaMissalFallback.apply(getApplicationContext(),
                        raw.html, section, language,
                        dayPackage.day == null ? "" : dayPackage.day.celebration);
                MissalDocument31.Result built = new MissalDocument31.Result(
                        raw.title, raw.subtitle, MissalLanguageGuard.sanitize(fallbackHtml, language));
                LiturgicalDayCache.prefetch(getApplicationContext(), date, 3);
                runOnUiThread(() -> show(built));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, error.getMessage() == null
                            ? "No se pudo abrir esta sección del Misal." : error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, "ministerium-missal-loader").start();
    }

    private void show(MissalDocument31.Result built) {
        result = built;
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(result.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(result.subtitle);
        ReaderContext context = readerContext();
        if (!selectionAttached) {
            UniversalSelectionMenu.attach(this, webView, context);
            ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context);
            selectionAttached = true;
        }
        webView.setVisibility(View.VISIBLE);
        webView.loadDataWithBaseURL("file:///android_asset/", result.html,
                "text/html", "UTF-8", null);
    }

    private ReaderContext readerContext() {
        String title = result == null
                ? ("la".equals(language) ? "Missale Romanum" : "Misal Romano") : result.title;
        String subtitle = result == null ? LiturgicalCalendarRepository.dateLabel(date) : result.subtitle;
        String source = "missal31:" + date.get(Calendar.YEAR) + ":" + (date.get(Calendar.MONTH) + 1)
                + ":" + date.get(Calendar.DAY_OF_MONTH) + ":" + section + ":" + language;
        String sourceName = "la".equals(language)
                ? "Missale Romanum · fuente latina local · respaldo español etiquetado"
                : "Misal Romano · Ecuador · Mercabá verificado · Guadalajara";
        return new ReaderContext(sourceName, source, title, subtitle, "Liturgia", true);
    }

    private static String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
