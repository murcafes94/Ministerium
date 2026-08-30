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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.Calendar;

/**
 * ES/LAT Hours reader. Tablets align semantic paragraph cards. Phones keep one
 * language full-width at a time so the text is never squeezed into columns.
 */
public class BilingualHoursReaderActivity extends ThemedActivity {
    public static final String EXTRA_SPANISH_VOLUME = "spanish_volume";
    public static final String EXTRA_SPANISH_PATH = "spanish_path";
    public static final String EXTRA_SPANISH_FRAGMENT = "spanish_fragment";
    public static final String EXTRA_SPANISH_SCROLL = "spanish_scroll";
    public static final String EXTRA_TITLE = "reader_title";
    public static final String EXTRA_HOUR_KEY = "hour_key";
    public static final String EXTRA_YEAR = "reader_year";
    public static final String EXTRA_MONTH = "reader_month";
    public static final String EXTRA_DAY = "reader_day";
    public static final String EXTRA_SUNDAY_OR_SOLEMNITY = "sunday_or_solemnity";
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
                ? "Español · Latín · párrafos sincronizados"
                : "Español / Latín · lectura a ancho completo");

        panes = findViewById(R.id.bilingualPanes);
        spanish = findViewById(R.id.spanishWebView);
        latin = findViewById(R.id.latinWebView);
        spanishScroll = value(getIntent().getStringExtra(EXTRA_SPANISH_SCROLL));

        Button primaryMode = findViewById(R.id.btnBoth);
        if (!wideParallel) primaryMode.setText("Español");

        configure(spanish, true);
        configure(latin, false);
        configureLayout();
        configureSynchronizedScroll();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        primaryMode.setOnClickListener(v -> showMode(0));
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
            if (!wideParallel) showMode(0);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar esta Hora bilingüe.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void configureLayout() {
        panes.setOrientation(LinearLayout.HORIZONTAL);
        applyPaneWeights();
    }

    private void configureSynchronizedScroll() {
        spanish.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                scheduleSynchronize(spanish, latin));
        latin.setOnScrollChangeListener((view, x, y, oldX, oldY) ->
                scheduleSynchronize(latin, spanish));
    }

    private void scheduleSynchronize(WebView source, WebView target) {
        if (!wideParallel || syncingScroll || source.getVisibility() != View.VISIBLE
                || target.getVisibility() != View.VISIBLE) return;
        if (pendingSync != null) syncHandler.removeCallbacks(pendingSync);
        pendingSync = () -> semanticSynchronize(source, target);
        syncHandler.postDelayed(pendingSync, 70L);
    }

    private void semanticSynchronize(WebView source, WebView target) {
        if (!wideParallel || syncingScroll) return;
        String probe = "(function(){var es=document.querySelectorAll('[data-ministerium-align-key]');"
                + "if(!es.length)return '';var y=window.scrollY+34,b=es[0],d=1e12;"
                + "for(var i=0;i<es.length;i++){var top=es[i].getBoundingClientRect().top+window.scrollY;"
                + "if(top<=y+28&&Math.abs(top-y)<d){b=es[i];d=Math.abs(top-y);}}"
                + "return b.getAttribute('data-ministerium-align-key')||'';})()";
        source.evaluateJavascript(probe, raw -> {
            String key = decode(raw);
            if (key.isEmpty()) return;
            String apply = "(function(k){var es=document.querySelectorAll('[data-ministerium-align-key]'),e=null;"
                    + "for(var i=0;i<es.length;i++){if(es[i].getAttribute('data-ministerium-align-key')===k){e=es[i];break;}}"
                    + "if(!e){var p=k.lastIndexOf(':'),prefix=p<0?k:k.substring(0,p+1),wanted=p<0?0:parseInt(k.substring(p+1),10),best=null,dist=1e9;"
                    + "for(var j=0;j<es.length;j++){var q=es[j].getAttribute('data-ministerium-align-key')||'';if(q.indexOf(prefix)!==0)continue;"
                    + "var n=parseInt(q.substring(prefix.length),10),dd=Math.abs(n-wanted);if(dd<dist){best=es[j];dist=dd;}}e=best;}"
                    + "if(!e)return false;var top=e.getBoundingClientRect().top+window.scrollY;window.scrollTo(0,Math.max(0,Math.round(top-18)));return true;})("
                    + JSONObject.quote(key) + ")";
            syncingScroll = true;
            target.evaluateJavascript(apply, result ->
                    target.postDelayed(() -> syncingScroll = false, 70L));
        });
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
                if (isSpanish) {
                    filterIntermediateHour();
                } else {
                    cleanLatinPrelude(webView);
                }
                webView.postDelayed(() -> applyParagraphCards(webView, isSpanish), 120L);
                UniversalSelectionMenu.restoreHighlights(BilingualHoursReaderActivity.this,
                        webView, sourceKey(isSpanish ? "es" : "la"));
            }
        });
    }

    private void loadSpanish() throws Exception {
        String hourKey = value(getIntent().getStringExtra(EXTRA_HOUR_KEY));
        if ("compline".equals(hourKey)) {
            loadSpanishCompline();
            return;
        }

        HoursVolume volume = HoursRepository.find(
                value(getIntent().getStringExtra(EXTRA_SPANISH_VOLUME)));
        if (volume == null) throw new IllegalStateException("Volumen español no válido.");
        String filePath = value(getIntent().getStringExtra(EXTRA_SPANISH_PATH));
        String fragment = value(getIntent().getStringExtra(EXTRA_SPANISH_FRAGMENT));
        File root = EpubUtils.ensureExtracted(this, volume);
        File target = new File(root, filePath);
        if (!target.isFile()) throw new IllegalStateException("Hora española no encontrada.");

        String html = null;
        String baseUrl = Uri.fromFile(target).toString();
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
            if (office != null && office.html != null && !office.html.trim().isEmpty()) {
                html = office.html;
                baseUrl = office.baseUrl;
            }
        }

        int ordinaryWeek = getIntent().getIntExtra(EXTRA_ORDINARY_WEEK, 0);
        if (html == null && "ordinary".equals(volume.id) && ordinaryWeek > 0) {
            html = OrdinaryReferenceResolver.resolve(root, filePath, ordinaryWeek,
                    value(getIntent().getStringExtra(EXTRA_CYCLE)),
                    getIntent().getIntExtra(EXTRA_READINGS_YEAR, 0));
        }

        if (!spanishScroll.isEmpty()) {
            html = IntermediateHourResolver.resolve(this, root, filePath, html,
                    spanishScroll,
                    getIntent().getBooleanExtra(EXTRA_SUNDAY_OR_SOLEMNITY, false),
                    ordinaryWeek);
        }

        if (html != null && !html.trim().isEmpty()) {
            spanish.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
            return;
        }

        String url = Uri.fromFile(target).toString();
        if (!fragment.isEmpty()) url += "#" + Uri.encode(fragment);
        spanish.loadUrl(url);
    }

    private void loadSpanishCompline() throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();
        selected.clear();
        selected.set(
                getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)),
                12, 0, 0);
        JSONObject data = ComplineContentRepository.load(this);
        JSONObject form = ComplineContentRepository.formForDay(
                data, selected.get(Calendar.DAY_OF_WEEK));
        if (form == null) throw new IllegalStateException("Formulario español de Completas ausente.");
        LiturgicalDay day = LiturgicalResolver.resolve(this, selected);
        String season = day.temporalOffice == null || day.temporalOffice.volume == null
                ? "ordinary" : day.temporalOffice.volume.id;
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selected);
        data.put("_ordinaryWeek", ordinaryWeek);
        String html = ComplineSemanticRenderer.render(this, data, form, season);
        spanish.loadDataWithBaseURL("https://ministerium.local/compline/",
                html, "text/html", "UTF-8", null);
    }

    private void showMode(int mode) {
        if (wideParallel) {
            spanish.setVisibility(mode == 1 ? View.GONE : View.VISIBLE);
            latin.setVisibility(View.VISIBLE);
        } else {
            spanish.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
            latin.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        }
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
        } else {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 0f);
        }
        int margin = Math.round(getResources().getDisplayMetrics().density * (wideParallel ? 4 : 2));
        params.setMargins(margin, margin, margin, margin);
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

    private void applyStyle(WebView view, boolean isSpanish) {
        boolean dark = ThemeUtils.isDark(this);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#D9B96F" : "#772233";
        String border = dark ? "#5A4D43" : "#E1D7C8";
        String card = dark ? "#302A26" : "#FFFFFF";
        String css = "html,body{background:" + bg + "!important;color:" + ink
                + "!important;width:100%!important;max-width:none!important;box-sizing:border-box}"
                + "body{font-family:serif!important;line-height:1.56!important;margin:0!important;"
                + "padding:12px!important;box-sizing:border-box;overflow-wrap:break-word!important}"
                + "body *{max-width:100%!important;box-sizing:border-box;color:" + ink
                + "!important;-webkit-text-fill-color:" + ink + "!important;text-shadow:none!important}"
                + "a,h1,h2,h3,h4,.redtitle,.redsmall1,.rojo,[style*=red],[style*=\"#CC0000\"],"
                + "[style*=\"#cc0000\"]{color:" + accent + "!important;-webkit-text-fill-color:"
                + accent + "!important}.patka{display:none!important}img,table{max-width:100%!important;height:auto!important}"
                + ".ministerium-align-card{display:block!important;margin:0 0 8px!important;padding:10px 12px!important;"
                + "border:1px solid " + border + "!important;border-radius:10px!important;background:" + card
                + "!important;scroll-margin-top:16px!important}.ministerium-align-heading{scroll-margin-top:16px!important;margin-top:14px!important}"
                + "@media(min-width:700px){body{padding:14px!important}.ministerium-align-card{padding:12px 14px!important}}";
        String script = "(function(){var old=document.getElementById('ministerium-bilingual-clean');if(old)old.remove();"
                + "var s=document.createElement('style');s.id='ministerium-bilingual-clean';s.textContent='"
                + css.replace("'", "\\'") + "';document.head.appendChild(s);"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){var t=(links[i].textContent||'').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "if(t==='↑'||/^\\[[olmvc123]+\\]$/.test(t)||t.indexOf('officium lectionis')>=0||t==='tertia →'||t==='sexta →'||t==='nona →')links[i].style.display='none';}"
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),x;while((x=w.nextNode())){if(/^\\s*\\[[OLMVC123]+\\]\\s*$/.test(x.nodeValue||''))x.nodeValue='';}"
                + "})()";
        view.evaluateJavascript(script, null);
    }

    private void cleanLatinPrelude(WebView view) {
        String marker = latinHourMarker();
        String script = "(function(){function n(v){return(v||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "var links=[].slice.call(document.querySelectorAll('a'));"
                + "for(var i=0;i<links.length;i++){var t=n(links[i].textContent);"
                + "if(t.indexOf('←')>=0||t.indexOf('→')>=0||t==='↑'||/^\\[[olmvc123]+\\]$/.test(t)){"
                + "var p=links[i].parentElement;links[i].style.display='none';"
                + "if(p&&p.tagName==='P'&&n(p.textContent).length<90)p.style.display='none';}}"
                + "var marker=" + JSONObject.quote(marker) + ";"
                + "if(marker){var blocks=[].slice.call(document.querySelectorAll('h1,h2,h3,h4,p'));"
                + "var cut=-1;for(var j=0;j<blocks.length;j++){var q=n(blocks[j].textContent);"
                + "if(q===marker||q.indexOf(marker)===0){cut=j;break;}}"
                + "if(cut>0){for(var k=0;k<cut;k++)blocks[k].style.display='none';}}"
                + "window.scrollTo(0,0);})()";
        view.evaluateJavascript(script, null);
    }

    private String latinHourMarker() {
        String key = value(getIntent().getStringExtra(EXTRA_HOUR_KEY));
        if ("invitatory".equals(key)) return "invitatorium";
        if ("office".equals(key)) return "officium lectionis";
        if ("lauds".equals(key)) return "laudes matutin";
        if ("terce".equals(key)) return "tertia";
        if ("sext".equals(key)) return "sexta";
        if ("none".equals(key)) return "nona";
        if ("vespers".equals(key)) return "vesper";
        if ("compline".equals(key)) return "completorium";
        return "";
    }

    private void applyParagraphCards(WebView view, boolean spanishSide) {
        String script = "(function(){if(document.body.getAttribute('data-ministerium-cards')==='1')return;"
                + "document.body.setAttribute('data-ministerium-cards','1');function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function sec(t,current){t=n(t);if(t.indexOf('himno')>=0||t.indexOf('hymnus')>=0)return'hymn';"
                + "if(t.indexOf('salmodia')>=0||t.indexOf('psalmodia')>=0)return'psalmody';"
                + "if(t.indexOf('lectura breve')>=0||t.indexOf('lectio brevis')>=0)return'reading';"
                + "if(t.indexOf('responsorio')>=0||t.indexOf('responsorium')>=0)return'responsory';"
                + "if(t.indexOf('cantico evangelico')>=0||t.indexOf('canticum evangelicum')>=0)return'canticle';"
                + "if(t==='preces'||t.indexOf('preces ')===0)return'intercessions';"
                + "if(t.indexOf('padre nuestro')>=0||t.indexOf('pater noster')>=0)return'pater';"
                + "if(t==='oracion'||t==='oratio'||t.indexOf('oracion conclusiva')>=0)return'prayer';return current;}"
                + "var blocks=[].slice.call(document.querySelectorAll('h1,h2,h3,h4,p,li,blockquote'));var section='opening',counts={};"
                + "for(var i=0;i<blocks.length;i++){var e=blocks[i];if(e.offsetParent===null)continue;var text=n(e.textContent);if(!text)continue;"
                + "if(/^h[1-4]$/i.test(e.tagName)){section=sec(text,section);e.classList.add('ministerium-align-heading');continue;}"
                + "if(e.closest&&e.closest('.ministerium-study-marker'))continue;section=sec(text,section);var k=counts[section]||0;counts[section]=k+1;"
                + "e.classList.add('ministerium-align-card');e.setAttribute('data-ministerium-align-key',section+':'+k);}})()";
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

    private static String decode(String raw) {
        if (raw == null || "null".equals(raw)) return "";
        try {
            Object value = new JSONTokener(raw).nextValue();
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            return raw.replaceFirst("^\\\"", "").replaceFirst("\\\"$", "")
                    .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        }
    }

    private static String value(String value) { return value == null ? "" : value; }
}
