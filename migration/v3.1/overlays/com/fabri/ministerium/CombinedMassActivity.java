package com.fabri.ministerium;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

/** Continuous Lauds/Vespers + Mass reader according to OGLH 93-96. */
public class CombinedMassActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "combined_mass_year";
    public static final String EXTRA_MONTH = "combined_mass_month";
    public static final String EXTRA_DAY = "combined_mass_day";
    public static final String EXTRA_HOUR = "combined_mass_hour";
    public static final String EXTRA_LANGUAGE = "combined_mass_language";

    private Calendar date;
    private String hourKey;
    private String language;
    private LiturgicalDay liturgicalDay;
    private HourEntry hour;
    private WebView webView;
    private boolean preparing;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_continuous);

        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        hourKey = "vespers".equals(getIntent().getStringExtra(EXTRA_HOUR))
                ? "vespers" : "lauds";
        language = "lat_es".equals(getIntent().getStringExtra(EXTRA_LANGUAGE))
                ? "lat_es" : "es";

        ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText(hourName() + " con Misa");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ReaderChrome.bindTheme(this, findViewById(R.id.btnCombinedTheme));

        webView = findViewById(R.id.combinedWebView);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.parseColor(ThemeUtils.isDark(this) ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override public void onPageFinished(WebView view, String url) {
                ReaderPreferences.apply(CombinedMassActivity.this, webView, false);
                UniversalSelectionMenu.restoreHighlights(CombinedMassActivity.this,
                        webView, sourceKey());
            }
        });
        UniversalSelectionMenu.attach(this, webView, context());

        ((RadioGroup) findViewById(R.id.groupCombinedStart))
                .setOnCheckedChangeListener((group, checkedId) -> {
                    if (liturgicalDay != null && hour != null) loadDocument();
                });
        prepare();
    }

    private void prepare() {
        if (preparing) return;
        preparing = true;
        setStatus("Preparando la celebración completa…");
        new Thread(() -> {
            try {
                LiturgicalDay day = LiturgicalResolver.resolve(getApplicationContext(), date);
                List<HourEntry> hours = DailyHoursRepository.hoursFor(
                        getApplicationContext(), day.temporalOffice, date);
                HourEntry selected = null;
                for (HourEntry entry : hours) {
                    if (hourKey.equals(entry.key)) {
                        selected = entry;
                        break;
                    }
                }
                if (selected == null) throw new IllegalStateException("Hora no disponible");
                final HourEntry resolvedHour = selected;
                runOnUiThread(() -> {
                    liturgicalDay = day;
                    hour = resolvedHour;
                    preparing = false;
                    ((TextView) findViewById(R.id.txtCombinedMassCelebration)).setText(
                            day.celebration + " · " + day.dateLabel);
                    loadDocument();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    preparing = false;
                    Toast.makeText(this,
                            "No se pudo preparar esta celebración unida.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadDocument() {
        if (liturgicalDay == null || hour == null || preparing) return;
        preparing = true;
        boolean massEntrance = ((RadioGroup) findViewById(R.id.groupCombinedStart))
                .getCheckedRadioButtonId() == R.id.startMassEntrance;
        setStatus(massEntrance
                ? "Entrada de la Misa → salmodia → Misa → cántico evangélico"
                : "Versículo e himno → salmodia → Misa → cántico evangélico");
        new Thread(() -> {
            try {
                String html = CombinedCelebrationDocument.build(getApplicationContext(), date,
                        liturgicalDay, hour, massEntrance, language);
                runOnUiThread(() -> {
                    preparing = false;
                    webView.loadDataWithBaseURL("https://ministerium.local/combined/",
                            html, "text/html", "UTF-8", null);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    preparing = false;
                    setStatus("No se pudo ensamblar uno de los textos de la celebración.");
                    Toast.makeText(this, "No se pudo ensamblar la celebración completa.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        if (!"ministerium".equals(uri.getScheme()) || !"sync-readings".equals(uri.getHost())) {
            return false;
        }
        if (!MassReadingsRepository.isCurrentMonth(date)) {
            Toast.makeText(this,
                    "La fuente de lecturas solo permite actualizar automáticamente el mes actual.",
                    Toast.LENGTH_LONG).show();
            return true;
        }
        setStatus("Guardando las lecturas del día…");
        new Thread(() -> {
            try {
                MassReadingsRepository.syncDay(getApplicationContext(), date);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lecturas guardadas.", Toast.LENGTH_SHORT).show();
                    loadDocument();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setStatus("No se pudieron guardar las lecturas. Comprueba la conexión.");
                    Toast.makeText(this, "No se pudieron actualizar las lecturas.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
        return true;
    }

    private void setStatus(String value) {
        ((TextView) findViewById(R.id.txtCombinedStatus)).setText(value);
    }

    private String hourName() {
        return "vespers".equals(hourKey) ? "Vísperas" : "Laudes";
    }

    private String sourceKey() {
        return "combined:" + date.get(Calendar.YEAR) + ":" + date.get(Calendar.DAY_OF_YEAR)
                + ":" + hourKey;
    }

    private ReaderContext context() {
        String title = liturgicalDay == null ? hourName() + " con Misa"
                : liturgicalDay.celebration + " · " + hourName() + " con Misa";
        return new ReaderContext("Celebración unida", sourceKey(), title, title,
                "Liturgia", true);
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
