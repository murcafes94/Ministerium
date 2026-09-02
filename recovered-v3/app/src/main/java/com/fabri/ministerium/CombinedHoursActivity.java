package com.fabri.ministerium;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class CombinedHoursActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "combined_year";
    public static final String EXTRA_MONTH = "combined_month";
    public static final String EXTRA_DAY = "combined_day";
    public static final String EXTRA_SAINT_VOLUME = "combined_saint_volume";
    public static final String EXTRA_SAINT_INDEX = "combined_saint_index";
    public static final String EXTRA_SAINT_TITLE = "combined_saint_title";
    public static final String EXTRA_SAINT_RANK = "combined_saint_rank";
    public static final String EXTRA_COMMON_FILE = "combined_common_file";
    public static final String EXTRA_COMMON_FRAGMENT = "combined_common_fragment";
    public static final String EXTRA_COMMON_TITLE = "combined_common_title";

    private WebView webView;
    private ReaderContext readerContext;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_reader);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(
                "Oficio de lecturas + Laudes");
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                "Unión litúrgica · OGLH 99");
        webView = findViewById(R.id.hoursWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.setBackgroundColor(Color.TRANSPARENT);
        readerContext = new ReaderContext("Liturgia de las Horas",
                sourceKey(), "Oficio de lecturas + Laudes", "OGLH 99",
                "Liturgia", true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                ReaderPreferences.apply(CombinedHoursActivity.this, webView, false);
                applyStyle();
                LiturgyConclusionEnhancer.inject(webView,
                        LiturgyPreferences.isOrdained(CombinedHoursActivity.this));
                UniversalSelectionMenu.restoreHighlights(CombinedHoursActivity.this,
                        webView, sourceKey());
            }
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SearchActivity.class)));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, readerContext);
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), readerContext,
                null, false);
        load();
    }

    private void load() {
        Calendar now = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)),
                12, 0, 0);
        final Calendar selected = date;
        new Thread(() -> {
            try {
                HoursLink saint = saint();
                CommonOfficeChoice common = common();
                CombinedHoursRepository.Result result = CombinedHoursRepository.officeAndLauds(
                        getApplicationContext(), selected, saint, common);
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                            result.celebration + " · OGLH 99");
                    webView.loadDataWithBaseURL(result.baseUrl, result.html,
                            "text/html", "UTF-8", null);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No se pudo componer esta unión litúrgica.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private HoursLink saint() {
        HoursVolume volume = HoursRepository.find(getIntent().getStringExtra(
                EXTRA_SAINT_VOLUME));
        if (volume == null) return null;
        return new HoursLink(volume, getIntent().getIntExtra(EXTRA_SAINT_INDEX, -1),
                value(getIntent().getStringExtra(EXTRA_SAINT_TITLE)), "Celebración", "",
                value(getIntent().getStringExtra(EXTRA_SAINT_RANK)));
    }

    private CommonOfficeChoice common() {
        String file = value(getIntent().getStringExtra(EXTRA_COMMON_FILE));
        return file.isEmpty() ? null : new CommonOfficeChoice(
                value(getIntent().getStringExtra(EXTRA_COMMON_TITLE)), file,
                value(getIntent().getStringExtra(EXTRA_COMMON_FRAGMENT)));
    }

    private void applyStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String css = "html,body{background:" + background + "!important;color:" + ink
                + "!important}body{padding:24px!important;box-sizing:border-box;line-height:1.65;}"
                + "h1,h2,h3,a,.rojo{color:" + wine + "!important;}"
                + ".ministerium-union-note{padding:14px;margin:14px 0 24px;border-left:4px solid "
                + wine + ";background:" + (dark ? "#332C28" : "#F5EDDF") + ";}"
                + "section[data-block]{margin-top:28px;}img{max-width:100%;height:auto;}";
        webView.evaluateJavascript("(function(){var s=document.createElement('style');s.innerHTML="
                + org.json.JSONObject.quote(css) + ";document.head.appendChild(s);})()", null);
    }

    private String sourceKey() {
        return "hours:combined:" + getIntent().getIntExtra(EXTRA_YEAR, 0) + ":"
                + getIntent().getIntExtra(EXTRA_MONTH, 0) + ":"
                + getIntent().getIntExtra(EXTRA_DAY, 0);
    }

    private static String value(String value) { return value == null ? "" : value; }

    @Override protected void onDestroy() {
        if (webView != null) { WebViewCleanup.destroy(webView); webView = null; }
        super.onDestroy();
    }
}
