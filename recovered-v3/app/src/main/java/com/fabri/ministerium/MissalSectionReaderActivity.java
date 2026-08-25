package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/** Reader for the semantic/native Missal generated from Liturgia Papal PDFs. */
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
        language = "lat_es".equals(getIntent().getStringExtra(EXTRA_LANGUAGE)) ? "lat_es" : "es";
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
                MissalCompactView.inject(webView);
                ReaderContext context = readerContext();
                UniversalSelectionMenu.restoreHighlights(MissalSectionReaderActivity.this, webView, context.sourceKey);
            }
        });
        load();
    }

    private void load() {
        try {
            result = MissalDocument31.build(this, date, section, language);
            ((TextView) findViewById(R.id.txtReaderTitle)).setText(result.title);
            ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(result.subtitle);
            ReaderContext context = readerContext();
            UniversalSelectionMenu.attach(this, webView, context);
            ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context);
            webView.loadDataWithBaseURL("file:///android_asset/", result.html, "text/html", "UTF-8", null);
        } catch (Exception error) {
            Toast.makeText(this, error.getMessage() == null ? "No se pudo abrir esta sección del Misal." : error.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private ReaderContext readerContext() {
        String title = result == null ? "Misal Romano" : result.title;
        String subtitle = result == null ? LiturgicalCalendarRepository.dateLabel(date) : result.subtitle;
        String source = "missal31:" + date.get(Calendar.YEAR) + ":" + (date.get(Calendar.MONTH) + 1)
                + ":" + date.get(Calendar.DAY_OF_MONTH) + ":" + section + ":" + language;
        return new ReaderContext("Misal Romano · Liturgia Papal México", source, title, subtitle, "Liturgia", true);
    }

    private static String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
