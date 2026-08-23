package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONObject;

public class BibleReaderActivity extends ThemedActivity {
    public static final String EXTRA_BOOK_INDEX = "book_index";
    public static final String EXTRA_CHAPTER_INDEX = "chapter_index";
    public static final String EXTRA_SCROLL_QUOTE = "scroll_quote";
    public static final String EXTRA_FIND_TEXT = "find_text";
    public static final String EXTRA_SCROLL_VERSE = "scroll_verse";
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_PLAN_DAY = "plan_day";
    private List<BibleRepository.Book> books;
    private BibleRepository.Book book;
    private int bookIndex, chapterIndex;
    private WebView webView;
    private File extractedRoot;
    private String pendingQuote = "";
    private String pendingFind = "";
    private String pendingVerse = "";
    private int pendingScrollY;
    private String planId = "";
    private int planDay;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this); super.onCreate(savedInstanceState); setContentView(R.layout.activity_bible_reader);
        try {
            books = BibleRepository.books(this);
            bookIndex = Math.max(0, Math.min(books.size() - 1, getIntent().getIntExtra(EXTRA_BOOK_INDEX, 0)));
            book = books.get(bookIndex);
            chapterIndex = Math.max(0, Math.min(book.chapters.size() - 1, getIntent().getIntExtra(EXTRA_CHAPTER_INDEX, 0)));
            pendingQuote = getIntent().getStringExtra(EXTRA_SCROLL_QUOTE);
            if (pendingQuote == null) pendingQuote = "";
            pendingFind = value(getIntent().getStringExtra(EXTRA_FIND_TEXT));
            pendingVerse = value(getIntent().getStringExtra(EXTRA_SCROLL_VERSE));
            pendingScrollY = getIntent().getIntExtra("restore_scroll_y", 0);
            planId = value(getIntent().getStringExtra(EXTRA_PLAN_ID));
            planDay = getIntent().getIntExtra(EXTRA_PLAN_DAY, 0);
            extractedRoot = EpubUtils.ensureExtracted(this, HoursRepository.BIBLE);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir la Biblia local.", Toast.LENGTH_LONG).show(); finish(); return;
        }
        webView = findViewById(R.id.bibleWebView);
        webView.getSettings().setAllowFileAccess(true); webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        if (!planId.isEmpty()) webView.addJavascriptInterface(
                new PlanProgressBridge(), "MinisteriumPlan");
        webView.setBackgroundColor(Color.parseColor(ThemeUtils.isDark(this) ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleLink(url); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return handleLink(request.getUrl().toString()); }
            @Override public void onPageFinished(WebView view, String url) {
                applyStyle();
                ReaderPreferences.apply(BibleReaderActivity.this, webView, true);
                ReflectionUtils.injectHighlights(BibleReaderActivity.this, webView, sourceKey());
                ReadingMarkerUtils.injectHighlights(BibleReaderActivity.this, webView, sourceKey());
                UniversalSelectionMenu.restoreHighlights(BibleReaderActivity.this, webView, sourceKey());
                attachPlanProgress();
                if (!pendingQuote.isEmpty()) {
                    ReadingMarkerUtils.scrollToQuote(webView, pendingQuote);
                    pendingQuote = "";
                }
                if (!pendingVerse.isEmpty()) {
                    scrollToVerse(pendingVerse);
                    pendingVerse = "";
                } else if (!pendingFind.isEmpty()) {
                    scrollToText(pendingFind);
                    pendingFind = "";
                } else if (pendingScrollY > 0) {
                    final int y = pendingScrollY;
                    pendingScrollY = 0;
                    webView.postDelayed(() -> webView.scrollTo(0, y), 180);
                }
            }
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new Intent(this, BibleSearchActivity.class)));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), context(),
                new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return chapterIndex > 0 || bookIndex > 0; }
                    @Override public boolean canNext() { return chapterIndex + 1 < book.chapters.size() || bookIndex + 1 < books.size(); }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                }, true);
        showChapter();
    }

    private BibleRepository.Chapter current() { return book.chapters.get(chapterIndex); }
    private String sourceKey() { return "bible:" + bookIndex + ":" + current().number; }
    private void showChapter() {
        BibleRepository.Chapter chapter = current();
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(book.title + " " + chapter.number);
        String subtitle = book.testament + " · notas integradas · sin conexión";
        if (!planId.isEmpty()) {
            BiblePlanRepository.Plan plan = BiblePlanRepository.find(this, planId);
            if (plan != null) {
                try {
                    BiblePlanRepository.DayReading reading = BiblePlanRepository.reading(
                            this, plan, planDay);
                    subtitle = "Plan · día " + planDay + " · "
                            + BiblePlanStore.sessionCompleted(this, planId, planDay)
                            + "/" + reading.chapterCount + " capítulos leídos";
                } catch (Exception ignored) {}
            }
        }
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(subtitle);
        BibleHistoryStore.record(this, bookIndex, chapterIndex,
                BibleRepository.citationAbbreviation(book) + " " + chapter.number,
                book.title);
        UniversalSelectionMenu.attach(this, webView, context());
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
        File target = new File(extractedRoot, chapter.file);
        try {
            BibleRepository.Chapter next = chapterIndex + 1 < book.chapters.size()
                    ? book.chapters.get(chapterIndex + 1) : null;
            String chapterDocument = BibleChapterDocument.from(
                    extractedRoot, chapter.file, chapter.fragment, chapter.number,
                    next == null ? null : next.file,
                    next == null ? null : next.fragment,
                    next == null ? -1 : next.number);
            webView.loadDataWithBaseURL(Uri.fromFile(target).toString(), chapterDocument,
                    "text/html", "UTF-8", null);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo separar este capítulo.",
                    Toast.LENGTH_LONG).show();
            return;
        }
    }
    private void move(int delta) {
        if (delta < 0 && chapterIndex == 0 && bookIndex > 0) { bookIndex--; book = books.get(bookIndex); chapterIndex = book.chapters.size() - 1; }
        else if (delta > 0 && chapterIndex + 1 == book.chapters.size() && bookIndex + 1 < books.size()) { bookIndex++; book = books.get(bookIndex); chapterIndex = 0; }
        else { int next = chapterIndex + delta; if (next < 0 || next >= book.chapters.size()) return; chapterIndex = next; }
        showChapter();
    }
    private void scrollToVerse(String value) {
        String script = "(function(v){var a=[].slice.call(document.querySelectorAll('#ministerium-chapter sup[id]'));"
                + "for(var i=0;i<a.length;i++){if(a[i].textContent.trim()==v){"
                + "a[i].scrollIntoView({block:'center'});a[i].style.background='#F6E58D';return true;}}return false;})("
                + org.json.JSONObject.quote(value) + ")";
        webView.evaluateJavascript(script, result -> { if (!"true".equals(result)) Toast.makeText(this, "No se encontró ese versículo en el capítulo.", Toast.LENGTH_SHORT).show(); });
    }
    private void scrollToText(String value) {
        String script = "(function(q){var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),n;"
                + "while(n=w.nextNode()){var i=n.nodeValue.toLocaleLowerCase('es').indexOf(q.toLocaleLowerCase('es'));"
                + "if(i>=0){var r=document.createRange();r.setStart(n,i);r.setEnd(n,i+q.length);"
                + "var m=document.createElement('mark');m.className='ministerium-highlight';"
                + "m.appendChild(r.extractContents());r.insertNode(m);m.scrollIntoView({block:'center'});return true;}}return false;})("
                + JSONObject.quote(value) + ")";
        webView.evaluateJavascript(script, null);
    }
    private String selectionCitation(ReadingSelectionUtils.Selection selection) {
        String base = BibleRepository.citationAbbreviation(book) + " " + current().number;
        String start = verseNumber(selection.startVerse);
        String end = verseNumber(selection.endVerse);
        if (start.isEmpty()) return base;
        if (end.isEmpty() || start.equals(end)) return base + ", " + start;
        return base + ", " + start + "–" + end;
    }
    private String verseNumber(String id) {
        if (id == null || id.length() < 2 || id.charAt(0) != 'v') return "";
        String value = id.substring(1);
        String chapter = String.valueOf(current().number);
        if (value.startsWith(chapter)) value = value.substring(chapter.length());
        return value;
    }
    private boolean handleLink(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url); String fragment = uri.getFragment();
        if (fragment == null || (!(fragment.endsWith("ref")) && !fragment.matches("m\\d+"))) return false;
        try {
            File target = new File(uri.getPath());
            if (!target.getCanonicalPath().startsWith(extractedRoot.getCanonicalPath())) return true;
            String html = readText(target);
            showIntegratedNote(fragment.matches("m\\d+") ? "Referencias relacionadas" : "Comentario", extractBlock(html, fragment));
        } catch (Exception error) { Toast.makeText(this, "No se pudo abrir esta nota.", Toast.LENGTH_SHORT).show(); }
        return true;
    }
    private String extractBlock(String html, String fragment) {
        int marker = html.indexOf("id=\"" + fragment + "\""); if (marker < 0) return "Nota no encontrada.";
        int start = html.lastIndexOf("<p", marker); if (start < 0) start = marker;
        int end;
        if (fragment.matches("m\\d+")) {
            int next = html.indexOf("<p class=\"lead\"", marker + fragment.length());
            end = next > 0 ? next : html.indexOf("</body>", marker);
        } else { end = html.indexOf("</p>", marker); if (end > 0) end += 4; }
        if (end < 0) end = Math.min(html.length(), marker + 1800);
        return html.substring(start, end);
    }
    private String readText(File file) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
    private void showIntegratedNote(String title, String html) {
        Spanned content = Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(html);
        TextView text = new TextView(this); int pad = (int) (20 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad); text.setText(content); text.setTextSize(17); text.setTextColor(getResources().getColor(R.color.ink));
        ScrollView scroll = new ScrollView(this); scroll.addView(text, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Cerrar", null).show();
    }
    private void applyStyle() {
        boolean dark = ThemeUtils.isDark(this); String bg = dark ? "#26211E" : "#FFFDF7"; String ink = dark ? "#F3EDE4" : "#2A2521"; String gold = dark ? "#E1C57A" : "#772233";
        String css = "html,body{background:" + bg + "!important;color:" + ink + "!important;width:100%;max-width:none}body{font-family:serif;line-height:1.65;padding:24px;margin:0;box-sizing:border-box;overflow-wrap:anywhere}body *{color:" + ink + "!important;-webkit-text-fill-color:" + ink + "!important;max-width:100%;box-sizing:border-box}img,table{max-width:100%!important;height:auto!important}a,a *{color:" + gold + "!important;-webkit-text-fill-color:" + gold + "!important}h1,h2,h3,.capital,.salmocapital1{color:" + gold + "!important;-webkit-text-fill-color:" + gold + "!important}.ministerium-highlight{background:#F6E58D!important;color:#231F1B!important;-webkit-text-fill-color:#231F1B!important;padding:1px 2px;border-radius:2px}@media(min-width:700px){body{padding-left:48px;padding-right:48px}}@media(min-width:1100px){body{padding-left:64px;padding-right:64px}}";
        webView.evaluateJavascript("(function(){var s=document.getElementById('ministerium-style');if(!s){s=document.createElement('style');s.id='ministerium-style';document.head.appendChild(s);}s.innerHTML=" + org.json.JSONObject.quote(css) + ";})()", null);
    }
    private ReaderContext context() {
        String reference = BibleRepository.citationAbbreviation(book) + " " + current().number;
        return new ReaderContext("Biblia de Jerusalén", sourceKey(), book.title,
                reference, "Biblia", false);
    }
    private void attachPlanProgress() {
        if (planId.isEmpty() || planDay < 1) return;
        webView.evaluateJavascript("(function(){if(window.__ministeriumPlan)return;"
                + "window.__ministeriumPlan=true;var done=false;window.addEventListener('scroll',function(){"
                + "if(!done&&window.scrollY+window.innerHeight>=document.documentElement.scrollHeight-40){"
                + "done=true;MinisteriumPlan.reachedEnd();}});})()", null);
    }

    private final class PlanProgressBridge {
        @JavascriptInterface public void reachedEnd() {
            runOnUiThread(() -> {
                try {
                    BiblePlanRepository.Plan plan = BiblePlanRepository.find(
                            BibleReaderActivity.this, planId);
                    if (plan == null) return;
                    BiblePlanRepository.DayReading reading = BiblePlanRepository.reading(
                            BibleReaderActivity.this, plan, planDay);
                    if (!reading.contains(bookIndex, chapterIndex)) return;
                    boolean completed = BiblePlanStore.completeChapter(
                            BibleReaderActivity.this, planId, planDay, sourceKey(),
                            reading.chapterCount);
                    showChapterProgress(reading, completed);
                } catch (Exception ignored) {}
            });
        }
    }

    private void showChapterProgress(BiblePlanRepository.DayReading reading,
                                     boolean completed) {
        int count = BiblePlanStore.sessionCompleted(this, planId, planDay);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText("Plan · día " + planDay
                + " · " + Math.min(count, reading.chapterCount) + "/"
                + reading.chapterCount + " capítulos leídos");
        Toast.makeText(this, completed ? "Sesión del día completada."
                : "Capítulo registrado en la sesión.", Toast.LENGTH_SHORT).show();
    }
    private static String value(String value) { return value == null ? "" : value.trim(); }
    @Override protected void onPause() {
        try {
            ContinueReadingStore.save(this, "Biblia",
                    book.title + " " + current().number, BibleReaderActivity.class,
                    new JSONObject().put(EXTRA_BOOK_INDEX, bookIndex)
                            .put(EXTRA_CHAPTER_INDEX, chapterIndex), webView.getScrollY());
        } catch (Exception ignored) {}
        super.onPause();
    }
    @Override protected void onDestroy() { if (webView != null) webView.destroy(); super.onDestroy(); }
}
