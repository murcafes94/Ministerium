package com.fabri.ministerium;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONTokener;
import org.json.JSONObject;

import java.io.File;

/**
 * ES/LAT reader. Wide screens synchronize by shared semantic anchors and fall
 * back to proportional progress when one side lacks an equivalent block.
 * Phones keep each language full-width and do not force pixel alignment.
 */
public class BilingualHoursReaderActivity extends ThemedActivity {
    public static final String EXTRA_SPANISH_VOLUME = "spanish_volume";
    public static final String EXTRA_SPANISH_PATH = "spanish_path";
    public static final String EXTRA_SPANISH_FRAGMENT = "spanish_fragment";
    public static final String EXTRA_SPANISH_SCROLL = "spanish_scroll";
    public static final String EXTRA_TITLE = "reader_title";
    public static final String EXTRA_LATIN_YEAR = "latin_year";
    public static final String EXTRA_LATIN_PATH = "latin_path";
    public static final String EXTRA_MEMORY_VOLUME = "memory_volume";
    public static final String EXTRA_MEMORY_INDEX = "memory_index";
    public static final String EXTRA_MEMORY_TITLE = "memory_title";
    public static final String EXTRA_MEMORY_RANK = "memory_rank";
    public static final String EXTRA_MEMORY_HOUR = "memory_hour";
    public static final String EXTRA_COMMON_FILE = "common_file";
    public static final String EXTRA_COMMON_FRAGMENT = "common_fragment";
    public static final String EXTRA_COMMON_TITLE = "common_title";
    public static final String EXTRA_ORDINARY_WEEK = "ordinary_week";
    public static final String EXTRA_CYCLE = "lectionary_cycle";
    public static final String EXTRA_READINGS_YEAR = "readings_year";

    private WebView spanish;
    private WebView latin;
    private LinearLayout panes;
    private String spanishScroll;
    private boolean syncingScroll;
    private boolean wideParallel;
    private final Handler syncHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSync;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bilingual_reader);

        wideParallel = getResources().getConfiguration().screenWidthDp >= 700;
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(
                value(getIntent().getStringExtra(EXTRA_TITLE)));
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(wideParallel
                ? "Español · Latín · lectura paralela"
                : "Español arriba · Latín abajo · ancho completo");

        panes = findViewById(R.id.bilingualPanes);
        spanish = findViewById(R.id.spanishWebView);
        latin = findViewById(R.id.latinWebView);
        spanishScroll = value(getIntent().getStringExtra(EXTRA_SPANISH_SCROLL));

        configure(spanish, true);
        configure(latin, false);
        configureLayout();
        configureSynchronizedScroll();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBoth).setOnClickListener(v -> showMode(0));
        findViewById(R.id.btnLatin).setOnClickListener(v -> showMode(1));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));

        ReaderContext spanishContext = new ReaderContext("Liturgia de las Horas",
                sourceKey("es"), value(getIntent().getStringExtra(EXTRA_TITLE)),
                "Español · Latín", "Liturgia", true);
        ReaderContext latinContext = new ReaderContext("Liturgia en Latín",
                sourceKey("la"), value(getIntent().getStringExtra(EXTRA_TITLE)),
                "Latín", "Liturgia", true, false);
        UniversalSelectionMenu.attach(this, spanish, spanishContext);
        UniversalSelectionMenu.attach(this, latin, latinContext);
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), spanish, spanishContext);
        attachPinch(spanish);
        attachPinch(latin);

        try {
            loadSpanish();
            int year = getIntent().getIntExtra(EXTRA_LATIN_YEAR, 2026);
            File latinFile = LatinContentManager.hourFile(this, year,
                    value(getIntent().getStringExtra(EXTRA_LATIN_PATH)));
            latin.loadUrl(Uri.fromFile(latinFile).toString());
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar esta Hora bilingüe.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void configureLayout() {
        panes.setOrientation(wideParallel ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        applyPaneWeights();
    }

    private void configureSynchronizedScroll() {
        spanish.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                scheduleSynchronize(spanish, latin, y));
        latin.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                scheduleSynchronize(latin, spanish, y));
    }

    private void scheduleSynchronize(WebView source, WebView target, int sourceY) {
        if (!wideParallel || syncingScroll || source.getVisibility() != View.VISIBLE
                || target.getVisibility() != View.VISIBLE) return;
        if (pendingSync != null) syncHandler.removeCallbacks(pendingSync);
        pendingSync = () -> semanticSynchronize(source, target, sourceY);
        syncHandler.postDelayed(pendingSync, 75L);
    }

    private void semanticSynchronize(WebView source, WebView target, int sourceY) {
        if (!wideParallel || syncingScroll) return;
        String probe = "(function(){var es=document.querySelectorAll('[data-ministerium-align-key]');"
                + "if(!es.length)return '';var y=window.scrollY+28,b=es[0],d=1e12;"
                + "for(var i=0;i<es.length;i++){var r=es[i].getBoundingClientRect(),top=r.top+window.scrollY;"
                + "var x=Math.abs(top-y);if(top<=y+24&&x<d){b=es[i];d=x;}}"
                + "var rr=b.getBoundingClientRect(),h=Math.max(1,rr.height),top=rr.top+window.scrollY;"
                + "var p=Math.max(0,Math.min(1,(y-top)/h));return JSON.stringify({k:b.getAttribute('data-ministerium-align-key'),p:p});})()";
        source.evaluateJavascript(probe, raw -> {
            try {
                Object decoded = new JSONTokener(raw).nextValue();
                if (decoded == null || decoded.toString().isEmpty()) {
                    proportionalSynchronize(source, target, sourceY);
                    return;
                }
                JSONObject anchor = new JSONObject(decoded.toString());
                String key = anchor.optString("k");
                double progress = anchor.optDouble("p", 0d);
                if (key.isEmpty()) {
                    proportionalSynchronize(source, target, sourceY);
                    return;
                }
                String apply = "(function(k,p){var es=document.querySelectorAll('[data-ministerium-align-key]'),e=null;"
                        + "for(var i=0;i<es.length;i++){if(es[i].getAttribute('data-ministerium-align-key')===k){e=es[i];break;}}"
                        + "if(!e)return false;var r=e.getBoundingClientRect(),top=r.top+window.scrollY;"
                        + "window.scrollTo(0,Math.max(0,Math.round(top+p*Math.max(1,r.height)-28)));return true;})("
                        + JSONObject.quote(key) + "," + progress + ")";
                syncingScroll = true;
                target.evaluateJavascript(apply, result -> {
                    boolean aligned = "true".equalsIgnoreCase(result);
                    if (!aligned) proportionalSynchronizeInternal(source, target, sourceY);
                    target.postDelayed(() -> syncingScroll = false, 75L);
                });
            } catch (Exception error) {
                proportionalSynchronize(source, target, sourceY);
            }
        });
    }

    private void proportionalSynchronize(WebView source, WebView target, int sourceY) {
        if (syncingScroll) return;
        syncingScroll = true;
        proportionalSynchronizeInternal(source, target, sourceY);
        target.postDelayed(() -> syncingScroll = false, 60L);
    }

    private void proportionalSynchronizeInternal(WebView source, WebView target, int sourceY) {
        int sourceRange = Math.round(source.getContentHeight() * source.getScale()) - source.getHeight();
        int targetRange = Math.round(target.getContentHeight() * target.getScale()) - target.getHeight();
        if (sourceRange <= 0 || targetRange <= 0) return;
        float progress = Math.max(0f, Math.min(1f, sourceY / (float) sourceRange));
        target.scrollTo(target.getScrollX(), Math.round(progress * targetRange));
    }

    private void configure(WebView view, boolean isSpanish) {
        view.getSettings().setAllowFileAccess(true);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView webView, String url) {
                applyStyle(webView);
                ReaderPreferences.apply(BilingualHoursReaderActivity.this, webView, false);
                UniversalSelectionMenu.restoreHighlights(BilingualHoursReaderActivity.this,
                        webView, sourceKey(isSpanish ? "es" : "la"));
                if (isSpanish) filterIntermediateHour();
            }
        });
    }

    private void loadSpanish() throws Exception {
        HoursVolume volume = HoursRepository.find(
                value(getIntent().getStringExtra(EXTRA_SPANISH_VOLUME)));
        if (volume == null) throw new IllegalStateException("Volumen español no válido.");
        String filePath = value(getIntent().getStringExtra(EXTRA_SPANISH_PATH));
        String fragment = value(getIntent().getStringExtra(EXTRA_SPANISH_FRAGMENT));
        File root = EpubUtils.ensureExtracted(this, volume);
        File target = new File(root, filePath);
        if (!target.isFile()) throw new IllegalStateException("Hora española no encontrada.");

        String memoryVolumeId = value(getIntent().getStringExtra(EXTRA_MEMORY_VOLUME));
        String memoryHour = value(getIntent().getStringExtra(EXTRA_MEMORY_HOUR));
        if (!memoryVolumeId.isEmpty() && !memoryHour.isEmpty()) {
            HoursVolume memoryVolume = HoursRepository.find(memoryVolumeId);
            if (memoryVolume == null) {
                throw new IllegalStateException("Volumen propio de la celebración no válido.");
            }
            HoursLink saint = new HoursLink(memoryVolume,
                    getIntent().getIntExtra(EXTRA_MEMORY_INDEX, -1),
                    value(getIntent().getStringExtra(EXTRA_MEMORY_TITLE)), "Celebración", "",
                    value(getIntent().getStringExtra(EXTRA_MEMORY_RANK)));
            String commonFile = value(getIntent().getStringExtra(EXTRA_COMMON_FILE));
            CommonOfficeChoice common = commonFile.isEmpty() ? null : new CommonOfficeChoice(
                    value(getIntent().getStringExtra(EXTRA_COMMON_TITLE)), commonFile,
                    value(getIntent().getStringExtra(EXTRA_COMMON_FRAGMENT)));
            HourEntry temporal = new HourEntry(memoryHour,
                    value(getIntent().getStringExtra(EXTRA_TITLE)), "", volume,
                    filePath, fragment, spanishScroll, false);
            MemoryOffice office = SaintOfficeRepository.compose(this, temporal, saint, common,
                    getIntent().getIntExtra(EXTRA_ORDINARY_WEEK, 0),
                    value(getIntent().getStringExtra(EXTRA_CYCLE)),
                    getIntent().getIntExtra(EXTRA_READINGS_YEAR, 0));
            if (office != null) {
                spanish.loadDataWithBaseURL(office.baseUrl, office.html,
                        "text/html", "UTF-8", null);
                return;
            }
        }

        int ordinaryWeek = getIntent().getIntExtra(EXTRA_ORDINARY_WEEK, 0);
        if ("ordinary".equals(volume.id) && ordinaryWeek > 0) {
            String html = OrdinaryReferenceResolver.resolve(root, filePath, ordinaryWeek,
                    value(getIntent().getStringExtra(EXTRA_CYCLE)),
                    getIntent().getIntExtra(EXTRA_READINGS_YEAR, 0));
            spanish.loadDataWithBaseURL(Uri.fromFile(target).toString(), html,
                    "text/html", "UTF-8", null);
            return;
        }
        String url = Uri.fromFile(target).toString();
        if (!fragment.isEmpty()) url += "#" + Uri.encode(fragment);
        spanish.loadUrl(url);
    }

    private void showMode(int mode) {
        spanish.setVisibility(mode == 1 ? View.GONE : View.VISIBLE);
        latin.setVisibility(View.VISIBLE);
        applyPaneWeights();
    }

    private void applyPaneWeights() {
        boolean both = spanish.getVisibility() == View.VISIBLE && latin.getVisibility() == View.VISIBLE;
        applySize(spanish, both);
        applySize(latin, both);
    }

    private void applySize(WebView view, boolean both) {
        if (view.getVisibility() == View.GONE) return;
        LinearLayout.LayoutParams params;
        if (wideParallel && both) {
            params = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        } else if (!wideParallel && both) {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        } else {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 0f);
        }
        params.setMargins(5, 5, 5, 5);
        view.setLayoutParams(params);
    }

    private void attachPinch(WebView view) {
        final float[] accumulated = {1f};
        ScaleGestureDetector detector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector scale) {
                        accumulated[0] *= scale.getScaleFactor();
                        if (accumulated[0] > 1.12f || accumulated[0] < .89f) {
                            ReaderPreferences.changeTextZoom(BilingualHoursReaderActivity.this,
                                    accumulated[0] > 1f ? 5 : -5);
                            int zoom = ReaderPreferences.textZoom(BilingualHoursReaderActivity.this);
                            spanish.getSettings().setTextZoom(zoom);
                            latin.getSettings().setTextZoom(zoom);
                            accumulated[0] = 1f;
                        }
                        return true;
                    }
                    @Override public void onScaleEnd(ScaleGestureDetector scale) {
                        accumulated[0] = 1f;
                    }
                });
        view.setOnTouchListener((target, event) -> {
            detector.onTouchEvent(event);
            return detector.isInProgress();
        });
    }

    private void applyStyle(WebView view) {
        boolean dark = ThemeUtils.isDark(this);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#772233";
        String css = "html,body{background:" + bg + "!important;color:" + ink
                + "!important;width:100%!important;max-width:none!important;box-sizing:border-box}"
                + "body{font-family:serif!important;line-height:1.58!important;margin:0!important;"
                + "padding:18px!important;box-sizing:border-box;overflow-wrap:break-word!important}"
                + "body *{max-width:100%!important;box-sizing:border-box;color:" + ink
                + "!important;-webkit-text-fill-color:" + ink + "!important;text-shadow:none!important}"
                + "a,h1,h2,h3,h4,.redtitle,.redsmall1,.rojo,[style*=red],[style*=\"#CC0000\"],"
                + "[style*=\"#cc0000\"]{color:" + accent + "!important;-webkit-text-fill-color:"
                + accent + "!important}.patka{display:none!important}img,table{max-width:100%!important;height:auto!important}";
        String script = "(function(){var old=document.getElementById('ministerium-bilingual-clean');if(old)old.remove();"
                + "var s=document.createElement('style');s.id='ministerium-bilingual-clean';s.textContent='"
                + css.replace("'", "\\'") + "';document.head.appendChild(s);"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){var t=(links[i].textContent||'').trim();"
                + "if(t==='↑'||/^\\[[OLMVC123]+\\]$/.test(t))links[i].style.display='none';}"
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),x;while((x=w.nextNode())){"
                + "if(/^\\s*\\[[OLMVC123]+\\]\\s*$/.test(x.nodeValue||''))x.nodeValue='';}})()";
        view.evaluateJavascript(script, null);
    }

    private void filterIntermediateHour() {
        if (spanishScroll.isEmpty()) return;
        String wanted = spanishScroll.toLowerCase(java.util.Locale.ROOT).replace("'", "\\'");
        String script = "(function(){var wanted='" + wanted
                + "';var names=['tercia','sexta','nona'];var c=[].slice.call(document.body.children);"
                + "var starts=[];for(var i=0;i<c.length;i++){var t=(c[i].textContent||'').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "for(var n=0;n<names.length;n++)if(t.indexOf(names[n])===0){starts.push({index:i,name:names[n]});break;}}"
                + "for(var s=0;s<starts.length;s++){var e=s+1<starts.length?starts[s+1].index:c.length;"
                + "if(starts[s].name!==wanted)for(var j=starts[s].index;j<e;j++)c[j].style.display='none';}})()";
        spanish.evaluateJavascript(script, null);
    }

    @Override protected void onDestroy() {
        if (pendingSync != null) syncHandler.removeCallbacks(pendingSync);
        if (spanish != null) spanish.destroy();
        if (latin != null) latin.destroy();
        super.onDestroy();
    }

    private String sourceKey(String language) {
        return "bilingual:" + language + ":"
                + value(getIntent().getStringExtra(EXTRA_SPANISH_VOLUME)) + ":"
                + value(getIntent().getStringExtra(EXTRA_SPANISH_PATH)) + ":"
                + value(getIntent().getStringExtra(EXTRA_TITLE));
    }

    private static String value(String value) { return value == null ? "" : value; }
}
