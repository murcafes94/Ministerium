package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Celebración continua de Misa + Laudes o Misa + Vísperas.
 *
 * A diferencia de la primera implementación 3.0, esta Activity no funciona
 * como un menú que abre otras pantallas. El contenido completo se compone y
 * se presenta en un solo WebView desplazable, de principio a fin.
 */
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
    private TextView celebrationView;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_mass);

        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)),
                12, 0, 0);
        hourKey = "vespers".equals(getIntent().getStringExtra(EXTRA_HOUR))
                ? "vespers" : "lauds";
        language = "lat_es".equals(getIntent().getStringExtra(EXTRA_LANGUAGE))
                ? "lat_es" : "es";

        ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText(
                "Misa + " + hourName());
        celebrationView = findViewById(R.id.txtCombinedMassCelebration);
        statusView = findViewById(R.id.txtCombinedMassStatus);
        webView = findViewById(R.id.combinedMassWebView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        configureWebView();
        buildCelebration();
    }

    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.parseColor(
                ThemeUtils.isDark(this) ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // La celebración combinada debe permanecer en una sola pantalla.
                return url != null && !url.startsWith("javascript:");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request == null || request.getUrl() == null
                        ? "" : request.getUrl().toString();
                return !url.startsWith("javascript:");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Biblia y Misal usan la familia Palatino (con fallback serif del sistema).
                ReaderPreferences.applyPalatino(CombinedMassActivity.this, webView);
                statusView.setText("");
            }
        });
    }

    private void buildCelebration() {
        celebrationView.setText("Preparando celebración…");
        statusView.setText("Uniendo Liturgia de las Horas, Misal y Leccionario…");
        webView.setVisibility(WebView.INVISIBLE);

        new Thread(() -> {
            try {
                CombinedMassComposer.Result result = CombinedMassPolisher.compose(
                        getApplicationContext(), date, hourKey, language);
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText(result.title);
                    celebrationView.setText(result.celebration);
                    statusView.setText("Celebración completa · una sola pantalla");
                    webView.setVisibility(WebView.VISIBLE);
                    webView.loadDataWithBaseURL("file:///android_asset/", result.html,
                            "text/html", "UTF-8", null);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    statusView.setText("No se pudo preparar la celebración completa.");
                    Toast.makeText(this,
                            "No se pudo unir la Misa con " + hourName() + ".",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String hourName() {
        return "vespers".equals(hourKey) ? "Vísperas" : "Laudes";
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
