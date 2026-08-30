package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/** Continuous Mass + Lauds/Vespers celebration using OGLH 93–96. */
public class CombinedMassActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "combined_mass_year";
    public static final String EXTRA_MONTH = "combined_mass_month";
    public static final String EXTRA_DAY = "combined_mass_day";
    public static final String EXTRA_HOUR = "combined_mass_hour";
    public static final String EXTRA_LANGUAGE = "combined_mass_language";

    private Calendar date;
    private String hourKey;
    private String language;
    private WebView webView;
    private TextView subtitleView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_mass);
        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        hourKey = "vespers".equals(getIntent().getStringExtra(EXTRA_HOUR)) ? "vespers" : "lauds";
        // La celebración unida usa por ahora la Liturgia de las Horas española.
        language = "es";
        ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText("Misa + " + hourName());
        subtitleView = findViewById(R.id.txtCombinedMassSubtitle);
        webView = findViewById(R.id.combinedMassWebView);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        configureWebView();
        ReaderChrome.attach(this, webView, findViewById(R.id.combinedMassHeader),
                readerContext(), null, false);
        buildCelebration();
    }

    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.parseColor(ThemeUtils.isDark(this) ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return url != null && !url.startsWith("javascript:");
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request == null || request.getUrl() == null ? "" : request.getUrl().toString();
                return !url.startsWith("javascript:");
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ReaderPreferences.apply(CombinedMassActivity.this, webView, false);
                LiturgicalWebStyle.apply(CombinedMassActivity.this, webView);
                MissalCompactView.inject(webView);
                MissalRuntimeFixes31.inject(webView);
                MissalAlternativeOptions31.inject(webView);
                UniversalSelectionMenu.restoreHighlights(CombinedMassActivity.this,
                        webView, readerContext().sourceKey);
            }
        });
    }

    private void buildCelebration() {
        subtitleView.setText("Preparando celebración…");
        webView.setVisibility(WebView.INVISIBLE);
        new Thread(() -> {
            try {
                if (MassReadingsRepository.isCurrentMonth(date)) {
                    if (!MassReadingsRepository.has(getApplicationContext(), date)) {
                        try { MassReadingsRepository.syncDay(getApplicationContext(), date); }
                        catch (Exception ignored) {}
                    }
                    DailyMassProperRepository.getOrSync(getApplicationContext(), date);
                }
                CombinedMassComposer.Result composed = CombinedMassComposer31.compose(
                        getApplicationContext(), date, hourKey, language);
                CombinedMassComposer.Result result = CombinedMassRubrics41.apply(
                        getApplicationContext(), date, hourKey, composed);
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText(result.title);
                    subtitleView.setText(result.celebration);
                    webView.setVisibility(WebView.VISIBLE);
                    webView.loadDataWithBaseURL("file:///android_asset/", result.html,
                            "text/html", "UTF-8", null);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    subtitleView.setText(error.getMessage() == null
                            ? "No se pudo preparar la celebración completa."
                            : error.getMessage());
                    Toast.makeText(this, "No se pudo unir la Misa con " + hourName() + ".",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "ministerium-combined-mass-loader").start();
    }

    private ReaderContext readerContext() {
        String sourceKey = "combined-mass:" + date.get(Calendar.YEAR) + "-"
                + (date.get(Calendar.MONTH) + 1) + "-" + date.get(Calendar.DAY_OF_MONTH)
                + ":" + hourKey + ":" + language;
        String title = "Misa + " + hourName();
        String reference = LiturgicalCalendarRepository.dateLabel(date) + " · " + title;
        return new ReaderContext("Celebración unida", sourceKey, title, reference,
                "Liturgia", true);
    }

    private String hourName() { return "vespers".equals(hourKey) ? "Vísperas" : "Laudes"; }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
