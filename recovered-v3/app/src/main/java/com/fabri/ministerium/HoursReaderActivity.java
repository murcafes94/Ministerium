package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import org.json.JSONObject;

public class HoursReaderActivity extends ThemedActivity {
    public static final String EXTRA_VOLUME_ID = "volume_id";
    public static final String EXTRA_TOC_INDEX = "toc_index";
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_FRAGMENT = "fragment";
    public static final String EXTRA_ENTRY_TITLE = "entry_title";
    public static final String EXTRA_SCROLL_TEXT = "scroll_text";
    public static final String EXTRA_FIND_TEXT = "find_text";
    public static final String EXTRA_DICTIONARY_TERM = "dictionary_term";
    public static final String EXTRA_SHOW_INTENTIONS = "show_intentions";
    public static final String EXTRA_MEMORY_SAINT_VOLUME_ID = "memory_saint_volume_id";
    public static final String EXTRA_MEMORY_SAINT_TOC_INDEX = "memory_saint_toc_index";
    public static final String EXTRA_MEMORY_SAINT_TITLE = "memory_saint_title";
    public static final String EXTRA_MEMORY_SAINT_RANK = "memory_saint_rank";
    public static final String EXTRA_MEMORY_COMMON_FILE = "memory_common_file";
    public static final String EXTRA_MEMORY_COMMON_FRAGMENT = "memory_common_fragment";
    public static final String EXTRA_MEMORY_COMMON_TITLE = "memory_common_title";
    public static final String EXTRA_MEMORY_HOUR_KEY = "memory_hour_key";
    public static final String EXTRA_ORDINARY_WEEK = "ordinary_week";
    public static final String EXTRA_LECTIONARY_CYCLE = "lectionary_cycle";
    public static final String EXTRA_READINGS_YEAR = "readings_year";
    public static final String EXTRA_EASTER_SEASON = "easter_season";
    public static final String EXTRA_SUNDAY_OR_SOLEMNITY = "sunday_or_solemnity";
    public static final String EXTRA_MISSAL_LANGUAGE = "missal_language";
    public static final String EXTRA_COMBINED_SEGMENT = "combined_segment";
    public static final String EXTRA_SKIP_HYMN = "skip_hymn";

    private HoursVolume volume;
    private List<EpubTocEntry> entries;
    private int position;
    private WebView webView;
    private boolean directMode;
    private boolean showIntentions;
    private String directFilePath = "";
    private String directFragment = "";
    private String directTitle = "";
    private String intermediateHour = "";
    private String dictionaryTerm = "";
    private String pendingFragment = "";
    private String memoryHourKey = "";
    private int ordinaryWeek;
    private String lectionaryCycle = "";
    private int readingsYear;
    private String pendingFindText = "";
    private int pendingScrollY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_reader);

        volume = HoursRepository.find(getIntent().getStringExtra(EXTRA_VOLUME_ID));
        if (volume == null) {
            finish();
            return;
        }
        position = getIntent().getIntExtra(EXTRA_TOC_INDEX, 0);
        directFilePath = value(getIntent().getStringExtra(EXTRA_FILE_PATH));
        directFragment = value(getIntent().getStringExtra(EXTRA_FRAGMENT));
        directTitle = value(getIntent().getStringExtra(EXTRA_ENTRY_TITLE));
        intermediateHour = value(getIntent().getStringExtra(EXTRA_SCROLL_TEXT));
        dictionaryTerm = value(getIntent().getStringExtra(EXTRA_DICTIONARY_TERM));
        memoryHourKey = value(getIntent().getStringExtra(EXTRA_MEMORY_HOUR_KEY));
        ordinaryWeek = getIntent().getIntExtra(EXTRA_ORDINARY_WEEK, 0);
        lectionaryCycle = value(getIntent().getStringExtra(EXTRA_LECTIONARY_CYCLE));
        readingsYear = getIntent().getIntExtra(EXTRA_READINGS_YEAR, 0);
        pendingFindText = value(getIntent().getStringExtra(EXTRA_FIND_TEXT));
        pendingScrollY = getIntent().getIntExtra("restore_scroll_y", 0);
        showIntentions = getIntent().getBooleanExtra(EXTRA_SHOW_INTENTIONS, false);
        directMode = !directFilePath.isEmpty();

        webView = findViewById(R.id.hoursWebView);

        findViewById(R.id.btnBack).setOnClickListener(v -> goBackOrFinish());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));

        configureWebView();
        try {
            if (directMode) {
                showDirectEntry();
            } else {
                entries = EpubUtils.tableOfContents(this, volume);
                position = Math.max(0, Math.min(entries.size() - 1, position));
                showTocEntry();
            }
            ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), context(),
                    new ReaderChrome.Navigator() {
                        @Override public boolean canPrevious() { return !directMode && navigableIndex(-1) >= 0; }
                        @Override public boolean canNext() { return !directMode && navigableIndex(1) >= 0; }
                        @Override public void previous() { move(-1); }
                        @Override public void next() { move(1); }
                    }, false);
            bindReaderActions();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir este volumen.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void configureWebView() {
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        boolean dark = ThemeUtils.isDark(this);
        webView.setBackgroundColor(Color.parseColor(dark ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleSpecialUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleSpecialUrl(request.getUrl().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                applyReadingStyle();
                ReaderPreferences.apply(HoursReaderActivity.this, webView, false);
                prepareDictionaryLayout();
                cleanDevotionalSourceNavigation();
                filterIntermediateHour();
                filterCombinedSegment();
                String combinedSegment = value(getIntent().getStringExtra(
                        EXTRA_COMBINED_SEGMENT));
                if (combinedSegment.isEmpty() && ("compline".equals(memoryHourKey)
                        || directTitle.toLowerCase(java.util.Locale.ROOT).contains("completas"))) {
                    ComplineEnhancer.inject(webView, LiturgyPreferences.isOrdained(
                                    HoursReaderActivity.this),
                            getIntent().getBooleanExtra(EXTRA_EASTER_SEASON, false));
                } else if (combinedSegment.isEmpty() && (memoryHourKey.equals("lauds") || memoryHourKey.equals("vespers")
                        || directTitle.toLowerCase(java.util.Locale.ROOT).contains("laudes")
                        || directTitle.toLowerCase(java.util.Locale.ROOT).contains("vísperas"))) {
                    LiturgyConclusionEnhancer.inject(webView,
                            LiturgyPreferences.isOrdained(HoursReaderActivity.this));
                }
                expandGospelCanticles();
                injectIntentions();
                UniversalSelectionMenu.restoreHighlights(HoursReaderActivity.this,
                        webView, sourceKey());
                scrollToPendingFragment();
                scrollToDictionaryTerm();
                scrollToFindText();
                if (pendingScrollY > 0) {
                    final int y = pendingScrollY;
                    pendingScrollY = 0;
                    webView.postDelayed(() -> webView.scrollTo(0, y), 180);
                }
            }
        });
    }

    private boolean handleSpecialUrl(String url) {
        if (url != null && url.startsWith("ministerium://intentions")) {
            startActivity(new Intent(this, PrayerIntentionsActivity.class));
            return true;
        }
        return false;
    }

    private void showDirectEntry() throws Exception {
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(directTitle);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                HoursRepository.isDictionary(volume)
                        ? volume.title + " · sin conexión"
                        : HoursRepository.isReference(volume)
                        ? "Magisterio · " + volume.title + " · sin conexión"
                        : "Liturgia de las Horas · " + volume.title);
        if (!showMemoryOffice()) load(directFilePath, directFragment);
        bindReaderActions();
    }

    private boolean showMemoryOffice() throws Exception {
        String saintVolumeId = value(getIntent().getStringExtra(
                EXTRA_MEMORY_SAINT_VOLUME_ID));
        if (saintVolumeId.isEmpty() || memoryHourKey.isEmpty()) return false;
        HoursVolume saintVolume = HoursRepository.find(saintVolumeId);
        if (saintVolume == null) return false;
        HoursLink saint = new HoursLink(saintVolume,
                getIntent().getIntExtra(EXTRA_MEMORY_SAINT_TOC_INDEX, -1),
                value(getIntent().getStringExtra(EXTRA_MEMORY_SAINT_TITLE)),
                "Celebración", "",
                value(getIntent().getStringExtra(EXTRA_MEMORY_SAINT_RANK)));
        String commonFile = value(getIntent().getStringExtra(EXTRA_MEMORY_COMMON_FILE));
        CommonOfficeChoice common = commonFile.isEmpty() ? null : new CommonOfficeChoice(
                value(getIntent().getStringExtra(EXTRA_MEMORY_COMMON_TITLE)), commonFile,
                value(getIntent().getStringExtra(EXTRA_MEMORY_COMMON_FRAGMENT)));
        HourEntry temporal = new HourEntry(memoryHourKey, directTitle, "", volume,
                directFilePath, directFragment, intermediateHour, showIntentions);
        MemoryOffice memory = SaintOfficeRepository.compose(this, temporal, saint, common,
                ordinaryWeek, lectionaryCycle, readingsYear);
        if (memory == null) return false;
        webView.loadDataWithBaseURL(memory.baseUrl, memory.html,
                "text/html", "UTF-8", null);
        return true;
    }

    private void showTocEntry() throws Exception {
        EpubTocEntry entry = entries.get(position);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(entry.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                HoursRepository.isDevotional(volume)
                        ? "Devocionario · Opus Dei"
                        : HoursRepository.isLatin2026(volume)
                        ? "Liturgia Horarum · latine · 2026"
                        : HoursRepository.isRomanMissal(volume)
                        ? "Misal Diario Romano · sin conexión"
                        : HoursRepository.isDictionary(volume)
                        ? volume.title + " · sin conexión"
                        : HoursRepository.isRatzingerWayOfCross(volume)
                        ? "Viacrucis de Joseph Ratzinger · 2005 · sin conexión"
                        : HoursRepository.isReference(volume)
                        ? "Magisterio · " + volume.title + " · sin conexión"
                        : "Liturgia de las Horas · " + volume.title);
        load(entry.filePath, entry.fragment);
        bindReaderActions();
    }

    private void load(String filePath, String fragment) throws Exception {
        File root = EpubUtils.ensureExtracted(this, volume);
        File target = new File(root, filePath);
        if (!target.exists()) throw new IllegalStateException(
                "No se encontró el texto seleccionado.");
        pendingFragment = fragment;
        String html = null;
        if ("ordinary".equals(volume.id) && ordinaryWeek > 0) {
            html = OrdinaryReferenceResolver.resolve(root, filePath,
                    ordinaryWeek, lectionaryCycle, readingsYear);
        }
        if (directMode && !intermediateHour.isEmpty()) {
            html = IntermediateHourResolver.resolve(this, root, filePath, html,
                    intermediateHour, getIntent().getBooleanExtra(
                            EXTRA_SUNDAY_OR_SOLEMNITY, false), ordinaryWeek);
        }
        if (html != null) {
            webView.loadDataWithBaseURL(Uri.fromFile(target).toString(), html,
                    "text/html", "UTF-8", null);
            return;
        }
        String url = Uri.fromFile(target).toString();
        if (!fragment.isEmpty()) url += "#" + Uri.encode(fragment);
        webView.loadUrl(url);
    }

    private void move(int delta) {
        int next = navigableIndex(delta);
        if (next < 0) return;
        position = next;
        getIntent().putExtra(EXTRA_TOC_INDEX, position);
        try {
            showTocEntry();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el texto seleccionado.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private int navigableIndex(int delta) {
        if (entries == null || delta == 0) return -1;
        int next = position + delta;
        while (next >= 0 && next < entries.size()) {
            if (!HoursRepository.isDevotional(volume)
                    || !HoursRepository.shouldHideDevotionalEntry(entries.get(next).title)) {
                return next;
            }
            next += delta;
        }
        return -1;
    }

    private void applyReadingStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String background = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String muted = dark ? "#C8BDB0" : "#6F665E";
        String wine = dark ? "#D9B96F" : "#6E1D2A";
        String css = "html,body{background:" + background + "!important;color:" + ink
                + "!important;}body,body *{color:" + ink + "!important;"
                + "-webkit-text-fill-color:" + ink + "!important;text-shadow:none!important;"
                + "-webkit-text-shadow:none!important;}"
                + "body{font-family:serif!important;line-height:1.65!important;width:100%!important;"
                + "max-width:none!important;margin:0!important;padding:24px!important;box-sizing:border-box;"
                + "overflow-wrap:anywhere!important;}body *{max-width:100%;box-sizing:border-box;}"
                + "h1,h2,h3,h4,.titulo{color:" + wine + "!important;"
                + "-webkit-text-fill-color:" + wine + "!important;line-height:1.25!important;}"
                + ".masnegrita{color:" + wine + "!important;-webkit-text-fill-color:"
                + wine + "!important;font-weight:bold!important;}"
                + "body a,body a:link,body a:active,body a:visited,body a:hover,"
                + "body a:focus,body a:visited:hover{color:" + wine
                + "!important;-webkit-text-fill-color:" + wine
                + "!important;}small,.rubrica,.rúbrica{color:" + muted + "!important;"
                + "-webkit-text-fill-color:" + muted
                + "!important;}img{max-width:100%!important;height:auto!important;}"
                + ".rojo,.redtitle,.redsmall1,[style*=red],[style*=\"#CC0000\"],"
                + "[style*=\"#cc0000\"]{color:" + wine + "!important;"
                + "-webkit-text-fill-color:" + wine + "!important;}"
                + ".ministerium-memory-note{margin:0 0 20px;padding:14px;border-radius:8px;"
                + "background:" + (dark ? "#332C28" : "#F5EDDF") + ";color:" + ink + ";}"
                + ".ministerium-canticle{margin:18px 0;padding:16px;border-left:4px solid "
                + wine + ";background:" + (dark ? "#332C28" : "#F5EDDF") + ";}"
                + ".ministerium-canticle h3{margin-top:0;}"
                + ".ministerium-auto-hymn,.ministerium-complementary{margin:14px 0 20px;"
                + "padding:14px;border-left:4px solid " + wine + ";background:"
                + (dark ? "#332C28" : "#F5EDDF") + ";}"
                + ".ministerium-complementary summary{cursor:pointer;color:" + wine
                + ";font-weight:bold;}"
                + ".ministerium-source-note{color:" + muted + "!important;"
                + "-webkit-text-fill-color:" + muted + "!important;font-style:italic;}"
                + "#ministerium-intentions{margin:18px 0;padding:16px;border-left:4px solid "
                + wine + ";background:" + (dark ? "#332C28" : "#F5EDDF") + ";}"
                + "#ministerium-intentions h3{margin:0 0 8px;color:" + wine + "!important;}"
                + "#ministerium-intentions ul{padding-left:22px;}"
                + ".ministerium-dictionary-hit{border-left:4px solid " + wine
                + ";background:" + (dark ? "#43372D" : "#F7E8B8")
                + "!important;padding:12px!important;border-radius:8px;}"
                + ".ministerium-dictionary-entry,.ministerium-dictionary-card{"
                + "display:block;margin:0 auto 18px!important;padding:18px!important;"
                + "border:1px solid " + (dark ? "#665746" : "#D8C9B5")
                + "!important;border-left:4px solid " + wine + "!important;"
                + "border-radius:10px!important;background:"
                + (dark ? "#332C28" : "#FFFFFF") + "!important;box-sizing:border-box;}"
                + ".ministerium-dictionary-entry h1,.ministerium-dictionary-card h1{"
                + "margin-top:0!important;}"
                + ".dictionary-reference{font-style:italic;color:" + muted
                + "!important;-webkit-text-fill-color:" + muted + "!important;}"
                + "@media(min-width:700px){body{padding-left:48px!important;"
                + "padding-right:48px!important}}"
                + "@media(min-width:1100px){body{padding-left:64px!important;"
                + "padding-right:64px!important}}";
        if (HoursRepository.isRomanMissal(volume)
                && "es".equals(getIntent().getStringExtra(EXTRA_MISSAL_LANGUAGE))) {
            css += ".izq{display:none!important}.dcha{width:100%!important;}"
                    + "table{width:100%!important;}";
        }
        String script = "(function(){var s=document.getElementById('ministerium-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + quoteForJavaScript(css) + ";"
                + "var all=document.body.querySelectorAll('*');for(var i=0;i<all.length;i++){"
                + "all[i].style.setProperty('color','" + ink + "','important');"
                + "all[i].style.setProperty('-webkit-text-fill-color','" + ink + "','important');"
                + "all[i].style.setProperty('text-shadow','none','important');}"
                + "var muted=document.querySelectorAll('small,.rubrica,.rúbrica');"
                + "for(var m=0;m<muted.length;m++){muted[m].style.setProperty('color','" + muted
                + "','important');muted[m].style.setProperty('-webkit-text-fill-color','"
                + muted + "','important');}"
                + "var accent=document.querySelectorAll('a,h1,h2,h3,h4,.titulo,.masnegrita,.rojo,.redtitle,"
                + ".redsmall1,[style*=red],[style*=\"#CC0000\"],[style*=\"#cc0000\"]');"
                + "for(var a=0;a<accent.length;a++){accent[a].style.setProperty('color','" + wine
                + "','important');accent[a].style.setProperty('-webkit-text-fill-color','"
                + wine + "','important');}})()";
        webView.evaluateJavascript(script, null);
    }

    private void prepareDictionaryLayout() {
        if (!directMode || !HoursRepository.isDictionary(volume)) return;
        if (HoursRepository.isBiblicalDictionary(volume)) {
            String script = "(function(){var wanted=document.getElementById("
                    + quoteForJavaScript(directFragment) + ");"
                    + "var entries=document.querySelectorAll('.ministerium-dictionary-entry');"
                    + "for(var i=0;i<entries.length;i++){entries[i].style.setProperty('display',"
                    + "entries[i]===wanted?'block':'none','important');}"
                    + "if(wanted){wanted.classList.add('ministerium-dictionary-card');}})()";
            webView.evaluateJavascript(script, null);
        } else if (HoursRepository.isTheologyDictionary(volume)) {
            String script = "(function(){if(document.querySelector('.ministerium-dictionary-card'))return;"
                    + "var card=document.createElement('article');card.className='ministerium-dictionary-card';"
                    + "while(document.body.firstChild)card.appendChild(document.body.firstChild);"
                    + "document.body.appendChild(card);window.scrollTo(0,0);})()";
            webView.evaluateJavascript(script, null);
        }
    }

    private void cleanDevotionalSourceNavigation() {
        if (!HoursRepository.isDevotional(volume)) return;
        String script = "(function(){function n(v){return(v||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "var c=[].slice.call(document.querySelectorAll('a,button'));"
                + "for(var i=0;i<c.length;i++){var t=n(c[i].textContent);"
                + "if(t==='seccion'||t==='indice'){var p=c[i].parentElement;c[i].style.display='none';"
                + "if(p){var q=n(p.textContent);if(q==='seccion indice'||q==='indice seccion'||"
                + "q==='seccion'||q==='indice')p.style.display='none';}}}})()";
        webView.evaluateJavascript(script, null);
    }

    private void expandGospelCanticles() {
        String benedictus = "<section class=\"ministerium-canticle\"><h3>Benedictus</h3>"
                + "<p>Bendito sea el Señor, Dios de Israel, porque ha visitado y redimido a su pueblo,<br/>"
                + "suscitándonos una fuerza de salvación en la casa de David, su siervo,<br/>"
                + "según lo había predicho desde antiguo por boca de sus santos profetas.</p>"
                + "<p>Es la salvación que nos libra de nuestros enemigos y de la mano de todos los que nos odian;<br/>"
                + "realizando la misericordia que tuvo con nuestros padres, recordando su santa alianza<br/>"
                + "y el juramento que juró a nuestro padre Abrahán.</p>"
                + "<p>Para concedernos que, libres de temor, arrancados de la mano de los enemigos,<br/>"
                + "le sirvamos con santidad y justicia, en su presencia, todos nuestros días.</p>"
                + "<p>Y a ti, niño, te llamarán profeta del Altísimo, porque irás delante del Señor a preparar sus caminos,<br/>"
                + "anunciando a su pueblo la salvación, el perdón de sus pecados.</p>"
                + "<p>Por la entrañable misericordia de nuestro Dios, nos visitará el sol que nace de lo alto,<br/>"
                + "para iluminar a los que viven en tiniebla y en sombra de muerte,<br/>"
                + "para guiar nuestros pasos por el camino de la paz.</p>"
                + "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br/>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p></section>";
        String magnificat = "<section class=\"ministerium-canticle\"><h3>Magníficat</h3>"
                + "<p>Proclama mi alma la grandeza del Señor, se alegra mi espíritu en Dios, mi salvador;<br/>"
                + "porque ha mirado la humillación de su esclava.</p>"
                + "<p>Desde ahora me felicitarán todas las generaciones, porque el Poderoso ha hecho obras grandes por mí: su nombre es santo,<br/>"
                + "y su misericordia llega a sus fieles de generación en generación.</p>"
                + "<p>Él hace proezas con su brazo: dispersa a los soberbios de corazón,<br/>"
                + "derriba del trono a los poderosos y enaltece a los humildes,<br/>"
                + "a los hambrientos los colma de bienes y a los ricos los despide vacíos.</p>"
                + "<p>Auxilia a Israel, su siervo, acordándose de la misericordia<br/>"
                + "—como lo había prometido a nuestros padres— en favor de Abrahán y su descendencia por siempre.</p>"
                + "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br/>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p></section>";
        String script = "(function(){var a=Array.prototype.slice.call(document.querySelectorAll('a'));"
                + "for(var i=0;i<a.length;i++){var t=(a[i].textContent||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').trim().toLowerCase();var h=null;"
                + "if(t==='benedictus')h=" + quoteForJavaScript(benedictus) + ";"
                + "if(t==='magnificat')h=" + quoteForJavaScript(magnificat) + ";"
                + "if(h){var p=a[i].closest('p')||a[i];p.insertAdjacentHTML('beforebegin',h);p.remove();}}"
                + "var body=(document.body.textContent||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').toUpperCase();"
                + "if(!document.querySelector('.ministerium-canticle')"
                + "&&body.indexOf('PROCLAMA MI ALMA LA GRANDEZA')<0"
                + "&&body.indexOf('BENDITO SEA EL SENOR, DIOS DE ISRAEL')<0"
                + "&&body.indexOf('CANTICO EVANGELICO')>=0){"
                + "var nodes=document.querySelectorAll('p,span,h1,h2,h3,h4'),preces=null;"
                + "for(var j=0;j<nodes.length;j++){var n=(nodes[j].textContent||'')"
                + ".replace(/\\s+/g,' ').trim().toUpperCase();if(n==='PRECES'){preces=nodes[j];break;}}"
                + "if(preces){preces=preces.closest('p')||preces;preces.insertAdjacentHTML('beforebegin',"
                + "body.indexOf('LAUDES')>=0?" + quoteForJavaScript(benedictus) + ":"
                + quoteForJavaScript(magnificat) + ");}}})()";
        webView.evaluateJavascript(script, null);
    }

    private void filterIntermediateHour() {
        if (!directMode || intermediateHour.isEmpty()) return;
        String script = "(function(){var wanted=" + quoteForJavaScript(intermediateHour)
                + ".toLowerCase();var names=['tercia','sexta','nona'];"
                + "var children=Array.prototype.slice.call(document.body.children);var starts=[];"
                + "for(var i=0;i<children.length;i++){var t=(children[i].textContent||'')"
                + ".replace(/\\s+/g,' ').trim().toLowerCase();for(var n=0;n<names.length;n++){"
                + "if(t.indexOf(names[n])===0){starts.push({index:i,name:names[n]});break;}}}"
                + "for(var s=0;s<starts.length;s++){var end=s+1<starts.length?starts[s+1].index:children.length;"
                + "if(starts[s].name!==wanted){for(var j=starts[s].index;j<end;j++)"
                + "children[j].style.display='none';}}window.scrollTo(0,0);})()";
        webView.evaluateJavascript(script, null);
    }

    private void filterCombinedSegment() {
        String segment = value(getIntent().getStringExtra(EXTRA_COMBINED_SEGMENT));
        if (segment.isEmpty()) return;
        String script;
        if ("hour_before_mass".equals(segment) || "hour_hymn".equals(segment)
                || "hour_psalmody".equals(segment)) {
            boolean hymnOnly = "hour_hymn".equals(segment);
            boolean psalmodyOnly = "hour_psalmody".equals(segment);
            script = "(function(skip){function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var a=Array.prototype.slice.call(document.body.children),cut=a.length,h=-1,s=-1;"
                    + "for(var i=0;i<a.length;i++){var t=n(a[i].textContent);if(t.indexOf('LECTURA BREVE')===0){cut=i;break;}if(t==='HIMNO')h=i;if(t==='SALMODIA'&&s<0)s=i;}"
                    + "for(var j=cut;j<a.length;j++)a[j].style.display='none';if(skip&&h>=0&&s>h)for(var k=h;k<s;k++)a[k].style.display='none';"
                    + "if(" + (hymnOnly ? "true" : "false") + "&&s>=0)for(var q=s;q<a.length;q++)a[q].style.display='none';"
                    + "if(" + (psalmodyOnly ? "true" : "false") + "&&s>=0)for(var p=0;p<s;p++)a[p].style.display='none';"
                    + "if(" + (hymnOnly ? "true" : "false") + "){var d=document.createElement('section');d.className='ministerium-union-note';"
                    + "d.innerHTML='<p><b>V.</b> Dios mío, ven en mi auxilio.<br><b>R.</b> Señor, date prisa en socorrerme.</p><p>Gloria al Padre, y al Hijo, y al Espíritu Santo. Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p>';document.body.insertBefore(d,document.body.firstChild);}window.scrollTo(0,0);})("
                    + (getIntent().getBooleanExtra(EXTRA_SKIP_HYMN, false)
                    || psalmodyOnly ? "true" : "false") + ")";
        } else if ("hour_canticle".equals(segment)) {
            script = "(function(){function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var a=Array.prototype.slice.call(document.body.children),start=-1,end=a.length;for(var i=0;i<a.length;i++){var t=n(a[i].textContent);"
                    + "if(start<0&&t==='CANTICO EVANGELICO')start=i;else if(start>=0&&t==='PRECES'){end=i;break;}}"
                    + "if(start<0)return;for(var j=0;j<start;j++)a[j].style.display='none';for(var k=end;k<a.length;k++)a[k].style.display='none';window.scrollTo(0,0);})()";
        } else if ("mass_greeting".equals(segment)) {
            script = "(function(){function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var rows=Array.prototype.slice.call(document.querySelectorAll('tr')),end=rows.length;for(var i=0;i<rows.length;i++){if(n(rows[i].textContent)==='ACTO PENITENCIAL'){end=i;break;}}"
                    + "for(var j=end;j<rows.length;j++)rows[j].style.display='none';window.scrollTo(0,0);})()";
        } else if ("mass_kyrie".equals(segment)
                || "mass_intro_a".equals(segment) || "mass_intro_b".equals(segment)) {
            boolean modeB = "mass_intro_b".equals(segment);
            script = "(function(modeB){function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var rows=Array.prototype.slice.call(document.querySelectorAll('tr')),k=-1,pen=-1,end=rows.length;for(var i=0;i<rows.length;i++){var t=n(rows[i].textContent);"
                    + "if(k<0&&t==='KYRIE')k=i;if(pen<0&&t==='ACTO PENITENCIAL')pen=i;if(rows[i].querySelector('#AntesColecta')||t==='LITURGIA DE LA PALABRA'){end=i;break;}}"
                    + "if(!modeB)for(var a=0;a<k;a++)rows[a].style.display='none';else if(pen>=0&&k>pen)for(var b=pen;b<k;b++)rows[b].style.display='none';"
                    + "for(var c=end;c<rows.length;c++)rows[c].style.display='none';window.scrollTo(0,0);})("
                    + (modeB ? "true" : "false") + ")";
        } else if ("mass_eucharist".equals(segment)) {
            script = "(function(){function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var rows=Array.prototype.slice.call(document.querySelectorAll('tr')),start=-1,end=rows.length;for(var i=0;i<rows.length;i++){"
                    + "if(start<0&&n(rows[i].textContent)==='CREDO')start=i;if(rows[i].querySelector('#AntesDespuesComunion')){end=i;break;}}"
                    + "if(start<0)start=0;for(var a=0;a<start;a++)rows[a].style.display='none';for(var b=end;b<rows.length;b++)rows[b].style.display='none';window.scrollTo(0,0);})()";
        } else return;
        webView.evaluateJavascript(script, null);
    }

    private void injectIntentions() {
        if (!showIntentions) return;
        List<String> intentions = IntentionsStore.get(this);
        StringBuilder html = new StringBuilder(
                "<section id=\"ministerium-intentions\"><h3>Tus intenciones de oración</h3>");
        if (intentions.isEmpty()) {
            html.append("<p>No has añadido intenciones personales.</p>");
        } else {
            html.append("<ul>");
            for (String intention : intentions) {
                html.append("<li>").append(escapeHtml(intention)).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("<p><a href=\"ministerium://intentions\">Gestionar intenciones</a></p></section>");

        String script = "(function(){var old=document.getElementById('ministerium-intentions');"
                + "if(old)old.remove();var nodes=document.querySelectorAll('p,span,h1,h2,h3,h4');"
                + "var marker=null,fallback=null;for(var i=0;i<nodes.length;i++){var t=(nodes[i].textContent||'')"
                + ".normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ')"
                + ".trim().toUpperCase();if(t.indexOf('SE PUEDEN ANADIR ALGUNAS INTENCIONES LIBRES')>=0)"
                + "{marker=nodes[i];break;}if(t==='PRECES')fallback=nodes[i];}"
                + "marker=marker||fallback;if(!marker)return;marker=marker.closest('p')||marker;"
                + "marker.insertAdjacentHTML('afterend',"
                + quoteForJavaScript(html.toString()) + ");})()";
        webView.evaluateJavascript(script, null);
    }

    private void scrollToPendingFragment() {
        if (pendingFragment.isEmpty()) return;
        String fragment = pendingFragment;
        pendingFragment = "";
        String script = "(function(){var e=document.getElementById("
                + quoteForJavaScript(fragment) + ");if(e){e.scrollIntoView();"
                + "window.scrollBy(0,-12);}})()";
        webView.evaluateJavascript(script, null);
    }

    private void scrollToDictionaryTerm() {
        if (dictionaryTerm.isEmpty()) return;
        String wanted = dictionaryTerm;
        dictionaryTerm = "";
        String script = "(function(){function n(v){return (v||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').toLowerCase().replace(/\\.$/,'')"
                + ".replace(/\\s+/g,' ').trim();}var wanted=n("
                + quoteForJavaScript(wanted) + ");var words=document.querySelectorAll('span.masnegrita');"
                + "for(var i=0;i<words.length;i++){if(n(words[i].textContent)===wanted){"
                + "var target=words[i].closest('p')||words[i];"
                + "target.classList.add('ministerium-dictionary-hit');target.scrollIntoView();"
                + "window.scrollBy(0,-12);break;}}})()";
        webView.evaluateJavascript(script, null);
    }

    private void scrollToFindText() {
        if (pendingFindText.isEmpty()) return;
        String wanted = pendingFindText;
        pendingFindText = "";
        String script = "(function(q){var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),n;"
                + "while(n=w.nextNode()){var i=n.nodeValue.toLocaleLowerCase('es').indexOf(q.toLocaleLowerCase('es'));"
                + "if(i>=0){var r=document.createRange();r.setStart(n,i);r.setEnd(n,i+q.length);"
                + "var m=document.createElement('mark');m.className='ministerium-dictionary-hit';"
                + "m.appendChild(r.extractContents());r.insertNode(m);m.scrollIntoView({block:'center'});return true;}}return false;})("
                + quoteForJavaScript(wanted) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static String quoteForJavaScript(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", "").replace("\n", "\\n") + "'";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String sourceKey() {
        return directMode ? "hours:" + volume.id + ":" + directFilePath + ":" + directFragment
                : "hours:" + volume.id + ":toc:" + position;
    }

    private ReaderContext context() {
        String title = directMode ? directTitle
                : entries != null && !entries.isEmpty() ? entries.get(position).title : volume.title;
        return new ReaderContext(volume.title, sourceKey(), title, title,
                HoursRepository.isReference(volume) ? "Documentos/libros" : "Liturgia",
                !HoursRepository.isReference(volume));
    }

    private void bindReaderActions() {
        UniversalSelectionMenu.attach(this, webView, context());
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private void goBackOrFinish() {
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null && showIntentions) injectIntentions();
    }

    @Override protected void onPause() {
        try {
            JSONObject extras = new JSONObject();
            android.os.Bundle bundle = getIntent().getExtras();
            if (bundle != null) for (String key : bundle.keySet()) {
                Object item = bundle.get(key);
                if (item instanceof String || item instanceof Integer
                        || item instanceof Long || item instanceof Boolean) extras.put(key, item);
            }
            if (!directMode) extras.put(EXTRA_TOC_INDEX, position);
            ContinueReadingStore.save(this,
                    HoursRepository.isReference(volume) ? "Biblioteca" : "Liturgia",
                    context().title, HoursReaderActivity.class, extras, webView.getScrollY());
        } catch (Exception ignored) {}
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) { WebViewCleanup.destroy(webView); webView = null; }
        super.onDestroy();
    }
}
