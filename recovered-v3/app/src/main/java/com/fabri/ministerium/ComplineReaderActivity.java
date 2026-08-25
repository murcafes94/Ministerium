package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Lector de Completas construido desde datos semánticos, sin abrir el EPUB. */
public class ComplineReaderActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "compline_year";
    public static final String EXTRA_MONTH = "compline_month";
    public static final String EXTRA_DAY = "compline_day";

    private MinisteriumWebView webView;
    private Calendar selectedDate;
    private ReaderContext readerContext;
    private boolean easterSeason;

    @Override protected boolean usesPrayerFocus() { return true; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_reader);

        Calendar now = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        selectedDate.clear();
        selectedDate.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);

        ((TextView) findViewById(R.id.txtReaderTitle)).setText("Completas");
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(dateLabel());
        webView = findViewById(R.id.hoursWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setBackgroundColor(Color.TRANSPARENT);

        readerContext = new ReaderContext("Liturgia de las Horas",
                sourceKey(), "Completas", dateLabel(), "Liturgia", true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                ReaderPreferences.apply(ComplineReaderActivity.this, webView, false);
                ComplineMarianLanguage.inject(ComplineReaderActivity.this, webView, easterSeason);
                UniversalSelectionMenu.restoreHighlights(ComplineReaderActivity.this, webView, sourceKey());
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SearchActivity.class)));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, readerContext);
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), readerContext, null, false);
        loadSemanticCompline();
    }

    private void loadSemanticCompline() {
        try {
            JSONObject data = ComplineContentRepository.load(this);
            JSONObject form = ComplineContentRepository.formForDay(data, selectedDate.get(Calendar.DAY_OF_WEEK));
            if (form == null) throw new IllegalStateException("Formulario de Completas ausente");

            LiturgicalDay day = LiturgicalResolver.resolve(this, selectedDate);
            String season = day.temporalOffice == null || day.temporalOffice.volume == null
                    ? "ordinary" : day.temporalOffice.volume.id;
            easterSeason = "easter".equals(ComplineContentRepository.normalizeSeason(season));
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
            data.put("_ordinaryWeek", ordinaryWeek);
            String volume = ComplineContentRepository.liturgicalVolume(season, ordinaryWeek);
            String week = "ordinary".equals(ComplineContentRepository.normalizeSeason(season))
                    && ordinaryWeek > 0 ? " · semana " + roman(ordinaryWeek) : "";
            ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                    day.celebration + " · Tomo " + volume + week + " · "
                            + form.optString("title", dateLabel()));
            String html = ComplineSemanticRenderer.render(this, data, form, season);
            webView.loadDataWithBaseURL("https://ministerium.local/compline/",
                    html, "text/html", "UTF-8", null);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar Completas desde el paquete local.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static String roman(int value) {
        final int[] numbers = {10, 9, 5, 4, 1};
        final String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int left = value;
        for (int i = 0; i < numbers.length; i++) {
            while (left >= numbers[i]) { result.append(symbols[i]); left -= numbers[i]; }
        }
        return result.toString();
    }

    private String dateLabel() {
        return new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(selectedDate.getTime());
    }

    private String sourceKey() {
        return String.format(Locale.US, "hours:compline:%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
