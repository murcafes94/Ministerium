package com.fabri.ministerium;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ScaleGestureDetector;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private boolean spanishReady;
    private boolean latinReady;
    private int alignmentGeneration;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bilingual_reader);

        ((TextView) findViewById(R.id.txtReaderTitle)).setText(
                value(getIntent().getStringExtra(EXTRA_TITLE)));
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                "Español · Latín · antífonas y salmos alineados");
        panes = findViewById(R.id.bilingualPanes);
        spanish = findViewById(R.id.spanishWebView);
        latin = findViewById(R.id.latinWebView);
        spanishScroll = value(getIntent().getStringExtra(EXTRA_SPANISH_SCROLL));
        configure(spanish, true);
        configure(latin, false);
        configureSynchronizedScroll();
        setParallelOrientation();

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
                "Latín", "Liturgia", true);
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

    private void configureSynchronizedScroll() {
        spanish.setOnScrollChangeListener((view, scrollX, scrollY, oldX, oldY) ->
                synchronize(spanish, latin, scrollY));
        latin.setOnScrollChangeListener((view, scrollX, scrollY, oldX, oldY) ->
                synchronize(latin, spanish, scrollY));
    }

    /**
     * Después de insertar espaciadores semánticos las dos columnas siguen sin
     * tener exactamente la misma altura. Copiar píxeles absolutos hacía que la
     * columna más larga se desplazara progresivamente. Se sincroniza ahora por
     * proporción dentro del recorrido útil de cada WebView.
     */
    private void synchronize(WebView source, WebView target, int sourceY) {
        if (syncingScroll || source.getVisibility() != View.VISIBLE
                || target.getVisibility() != View.VISIBLE) return;
        int sourceRange = Math.round(source.getContentHeight() * source.getScale())
                - source.getHeight();
        int targetRange = Math.round(target.getContentHeight() * target.getScale())
                - target.getHeight();
        if (sourceRange <= 0 || targetRange <= 0) return;
        float progress = Math.max(0f, Math.min(1f, sourceY / (float) sourceRange));
        int targetY = Math.round(progress * targetRange);
        syncingScroll = true;
        target.scrollTo(target.getScrollX(), Math.max(0, Math.min(targetY, targetRange)));
        target.postDelayed(() -> syncingScroll = false, 60L);
    }

    private void configure(WebView view, boolean isSpanish) {
        view.getSettings().setAllowFileAccess(true);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView webView, String url) {
                applyStyle(webView, isSpanish);
                ReaderPreferences.apply(BilingualHoursReaderActivity.this, webView, false);
                UniversalSelectionMenu.restoreHighlights(BilingualHoursReaderActivity.this,
                        webView, sourceKey(isSpanish ? "es" : "la"));
                if (isSpanish) filterIntermediateHour();
                if (isSpanish) spanishReady = true; else latinReady = true;
                scheduleAlignment();
            }
        });
    }

    private void loadSpanish() throws Exception {
        HoursVolume volume = HoursRepository.find(value(
                getIntent().getStringExtra(EXTRA_SPANISH_VOLUME)));
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
            if (memoryVolume == null) throw new IllegalStateException(
                    "Volumen propio de la celebración no válido.");
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

    private void setParallelOrientation() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        panes.setOrientation(widthDp >= 700 ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        applyPaneWeights();
        scheduleAlignment();
    }

    private void showMode(int mode) {
        spanish.setVisibility(mode == 1 ? View.GONE : View.VISIBLE);
        latin.setVisibility(View.VISIBLE);
        applyPaneWeights();
        if (mode == 0) scheduleAlignment(); else clearAlignment();
    }

    private void applyPaneWeights() {
        boolean horizontal = panes.getOrientation() == LinearLayout.HORIZONTAL;
        boolean both = spanish.getVisibility() == View.VISIBLE && latin.getVisibility() == View.VISIBLE;
        applySize(spanish, horizontal, both);
        applySize(latin, horizontal, both);
    }

    private void applySize(WebView view, boolean horizontal, boolean both) {
        if (view.getVisibility() == View.GONE) return;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                horizontal && both ? 0 : LinearLayout.LayoutParams.MATCH_PARENT,
                !horizontal && both ? 0 : LinearLayout.LayoutParams.MATCH_PARENT,
                both ? 1f : 0f);
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
                            clearAlignment();
                            scheduleAlignment();
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

    private void scheduleAlignment() {
        if (!spanishReady || !latinReady || spanish.getVisibility() != View.VISIBLE
                || latin.getVisibility() != View.VISIBLE) return;
        int generation = ++alignmentGeneration;
        panes.postDelayed(() -> {
            if (generation != alignmentGeneration) return;
            removeAlignmentSpacers();
            spanish.evaluateJavascript(alignmentMeasureScript(), spanishValue ->
                    latin.evaluateJavascript(alignmentMeasureScript(), latinValue ->
                            applyAlignment(generation, spanishValue, latinValue)));
        }, 420L);
    }

    private void applyAlignment(int generation, String spanishValue, String latinValue) {
        if (generation != alignmentGeneration) return;
        try {
            JSONArray spanishMeasures = new JSONArray(spanishValue);
            JSONArray latinMeasures = new JSONArray(latinValue);
            Map<String, Double> latinTops = new LinkedHashMap<>();
            for (int i = 0; i < latinMeasures.length(); i++) {
                JSONObject value = latinMeasures.getJSONObject(i);
                latinTops.put(value.getString("key"), value.getDouble("top"));
            }
            JSONArray spanishSpaces = new JSONArray();
            JSONArray latinSpaces = new JSONArray();
            double addedSpanish = 0;
            double addedLatin = 0;
            for (int i = 0; i < spanishMeasures.length(); i++) {
                JSONObject spanishMeasure = spanishMeasures.getJSONObject(i);
                String key = spanishMeasure.getString("key");
                Double latinTop = latinTops.get(key);
                if (latinTop == null) continue;
                double spanishTop = spanishMeasure.getDouble("top") + addedSpanish;
                double adjustedLatinTop = latinTop + addedLatin;
                double difference = spanishTop - adjustedLatinTop;
                if (Math.abs(difference) < 3 || Math.abs(difference) > 2400) continue;
                JSONObject spacer = new JSONObject();
                spacer.put("key", key);
                spacer.put("height", Math.round(Math.abs(difference)));
                if (difference < 0) {
                    spanishSpaces.put(spacer);
                    addedSpanish += Math.abs(difference);
                } else {
                    latinSpaces.put(spacer);
                    addedLatin += difference;
                }
            }
            spanish.evaluateJavascript(applyAlignmentScript(spanishSpaces), null);
            latin.evaluateJavascript(applyAlignmentScript(latinSpaces), null);
        } catch (Exception ignored) {
            clearAlignment();
        }
    }

    private String alignmentMeasureScript() {
        return "(function(){var old=document.querySelectorAll('.ministerium-align-spacer');"
                + "for(var z=0;z<old.length;z++)old[z].parentNode.removeChild(old[z]);"
                + "var tagged=document.querySelectorAll('[data-ministerium-align-key]');"
                + "for(var q=0;q<tagged.length;q++)tagged[q].removeAttribute('data-ministerium-align-key');"
                + "function n(v){return (v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')"
                + ".replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "function k(t){if(/^HIMNO(?:\\s|$)/.test(t)||/^HYMNUS(?:\\s|$)/.test(t))return'hymn';"
                + "if(/^SALMODIA(?:\\s|$)/.test(t))return'psalmody';"
                + "if(/^(ANT\\.?|ANTIFONA|ANTIPHONA)\\s*[123](?:\\.|\\s|$)/.test(t))return'antiphon';"
                + "if(/^CANTICO EVANGELICO/.test(t)||/^CANTICUM EVANGELICUM/.test(t))return'gospel';"
                + "if(/^(SALMO|PSALMUS)\\s*\\d/.test(t)||/^(CANTICO|CANTICUM)(?:\\s|$)/.test(t))return'psalm';"
                + "if(/^LECTURA BREVE/.test(t)||/^LECTIO BREVIS/.test(t))return'reading';"
                + "if(/^RESPONSORIO BREVE/.test(t)||/^RESPONSORIUM BREVE/.test(t))return'responsory';"
                + "if(/^PRECES(?:\\s|$)/.test(t))return'intercessions';"
                + "if(/^ORACION(?:\\s|:|$)/.test(t)||/^ORATIO(?:\\s|:|$)/.test(t))return'prayer';return'';}"
                + "var nodes=document.querySelectorAll('p,h1,h2,h3,h4,h5,h6,.psalm-name');"
                + "var counts={},out=[];for(var i=0;i<nodes.length;i++){var kind=k(n(nodes[i].textContent));"
                + "if(!kind)continue;counts[kind]=(counts[kind]||0)+1;var key=kind+':'+counts[kind];"
                + "nodes[i].setAttribute('data-ministerium-align-key',key);"
                + "out.push({key:key,top:nodes[i].getBoundingClientRect().top+window.pageYOffset});}return out;})()";
    }

    private String applyAlignmentScript(JSONArray spaces) {
        return "(function(items){for(var i=0;i<items.length;i++){var e=document.querySelector("
                + "'[data-ministerium-align-key=\\\"'+items[i].key+'\\\"]');if(!e||!e.parentNode)continue;"
                + "var d=document.createElement('div');d.className='ministerium-align-spacer';"
                + "d.setAttribute('aria-hidden','true');d.style.height=items[i].height+'px';"
                + "d.style.margin='0';d.style.padding='0';e.parentNode.insertBefore(d,e);}})("
                + spaces.toString() + ")";
    }

    private void clearAlignment() {
        alignmentGeneration++;
        removeAlignmentSpacers();
    }

    private void removeAlignmentSpacers() {
        String script = "(function(){var a=document.querySelectorAll('.ministerium-align-spacer');"
                + "for(var i=0;i<a.length;i++)a[i].parentNode.removeChild(a[i]);})()";
        spanish.evaluateJavascript(script, null);
        latin.evaluateJavascript(script, null);
    }

    private void applyStyle(WebView view, boolean isSpanish) {
        boolean dark = ThemeUtils.isDark(this);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#772233";
        String css = "html,body{background:" + bg + "!important;color:" + ink
                + "!important;width:100%!important;max-width:none!important;box-sizing:border-box}"
                + "body,body *{color:" + ink + "!important;"
                + "-webkit-text-fill-color:" + ink + "!important;text-shadow:none!important;"
                + "-webkit-text-shadow:none!important}"
                + "body{font-family:serif!important;line-height:1.58!important;"
                + "margin:0!important;padding:20px!important;box-sizing:border-box;"
                + "overflow-wrap:anywhere!important}body *{max-width:100%;box-sizing:border-box}"
                + "a,h1,h2,h3,h4,.redtitle,.redsmall1,.rojo,[style*=red],"
                + "[style*=\"#CC0000\"],[style*=\"#cc0000\"]{color:" + accent
                + "!important;-webkit-text-fill-color:" + accent + "!important}"
                + "img,table{max-width:100%!important;height:auto!important}.patka{display:none!important}"
                + "@media(min-width:700px){body{padding-left:32px!important;"
                + "padding-right:32px!important}}";
        String script = "(function(){var s=document.createElement('style');s.innerHTML='"
                + css.replace("'", "\\'") + "';document.head.appendChild(s);"
                + "var all=document.body.querySelectorAll('*');for(var i=0;i<all.length;i++){"
                + "all[i].style.setProperty('color','" + ink + "','important');"
                + "all[i].style.setProperty('-webkit-text-fill-color','" + ink + "','important');"
                + "all[i].style.setProperty('text-shadow','none','important');}"
                + "var marked=document.querySelectorAll('a,h1,h2,h3,h4,.redtitle,.redsmall1,.rojo,"
                + "[style*=red],[style*=\"#CC0000\"],[style*=\"#cc0000\"]');"
                + "for(var m=0;m<marked.length;m++){marked[m].style.setProperty('color','" + accent
                + "','important');marked[m].style.setProperty('-webkit-text-fill-color','"
                + accent + "','important');}"
                + "var a=document.querySelectorAll('a');for(var i=0;i<a.length;i++){"
                + "if((a[i].textContent||'').trim()==='↑')a[i].style.display='none';}})()";
        view.evaluateJavascript(script, null);
    }

    private void filterIntermediateHour() {
        if (spanishScroll.isEmpty()) return;
        String wanted = spanishScroll.toLowerCase(java.util.Locale.ROOT)
                .replace("'", "\\'");
        String script = "(function(){var wanted='" + wanted
                + "';var names=['tercia','sexta','nona'];var c=[].slice.call(document.body.children);"
                + "var starts=[];for(var i=0;i<c.length;i++){var t=(c[i].textContent||'')"
                + ".replace(/\\s+/g,' ').trim().toLowerCase();for(var n=0;n<names.length;n++)"
                + "if(t.indexOf(names[n])===0){starts.push({index:i,name:names[n]});break;}}"
                + "for(var s=0;s<starts.length;s++){var e=s+1<starts.length?starts[s+1].index:c.length;"
                + "if(starts[s].name!==wanted)for(var j=starts[s].index;j<e;j++)c[j].style.display='none';}})()";
        spanish.evaluateJavascript(script, null);
    }

    @Override protected void onDestroy() {
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
