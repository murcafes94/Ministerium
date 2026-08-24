package com.fabri.ministerium;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.fabri.ministerium.bible.semantic.BibleBook;
import com.fabri.ministerium.bible.semantic.BibleEdition;
import com.fabri.ministerium.bible.semantic.BibleVerse;
import com.fabri.ministerium.bible.semantic.SemanticBiblePackages;
import com.fabri.ministerium.bible.semantic.SemanticBibleRenderer;
import com.fabri.ministerium.bible.semantic.SqliteBibleRepository;

import java.util.List;

/** Native semantic-package reader. Falls back at the caller level when no package is installed. */
public class SemanticBibleReaderActivity extends ThemedActivity {
    public static final String EXTRA_BOOK_ID = "semantic_book_id";
    public static final String EXTRA_CHAPTER = "semantic_chapter";

    private SqliteBibleRepository repository;
    private BibleEdition edition;
    private List<BibleBook> books;
    private BibleBook book;
    private int bookIndex;
    private int chapter;
    private WebView webView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible_reader);

        try {
            repository = SemanticBiblePackages.openIfInstalled(
                    this, SemanticBiblePackages.DEFAULT_EDITION_ID);
            if (repository == null) throw new IllegalStateException("No semantic package installed");
            edition = repository.getEdition();
            books = repository.listBooks();
            String requestedBook = value(getIntent().getStringExtra(EXTRA_BOOK_ID));
            chapter = Math.max(1, getIntent().getIntExtra(EXTRA_CHAPTER, 1));
            for (int i = 0; i < books.size(); i++) {
                if (books.get(i).getBookId().equals(requestedBook)) {
                    bookIndex = i;
                    book = books.get(i);
                    break;
                }
            }
            if (book == null) throw new IllegalArgumentException("Unknown book: " + requestedBook);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el paquete bíblico semántico.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        webView = findViewById(R.id.bibleWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setTextZoom(ReaderPreferences.textZoom(this));
        webView.setBackgroundColor(Color.parseColor(ThemeUtils.isDark(this) ? "#26211E" : "#FFFDF7"));
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                applyStyle();
                ReaderPreferences.apply(SemanticBibleReaderActivity.this, webView, true);
                ReflectionUtils.injectHighlights(SemanticBibleReaderActivity.this, webView, sourceKey());
                ReadingMarkerUtils.injectHighlights(SemanticBibleReaderActivity.this, webView, sourceKey());
                UniversalSelectionMenu.restoreHighlights(SemanticBibleReaderActivity.this, webView, sourceKey());
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReaderSearch).setOnClickListener(v ->
                startActivity(new Intent(this, BibleSearchActivity.class)));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), context(),
                new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() {
                        return chapter > 1 || bookIndex > 0;
                    }
                    @Override public boolean canNext() {
                        return chapter < book.getChapterCount() || bookIndex + 1 < books.size();
                    }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                }, true);
        showChapter();
    }

    private void showChapter() {
        List<BibleVerse> verses = repository.getChapter(book.getBookId(), chapter);
        if (verses.isEmpty()) {
            Toast.makeText(this, "Este capítulo no está disponible en el paquete instalado.", Toast.LENGTH_LONG).show();
            return;
        }
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(book.getName() + " " + chapter);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                edition.getName() + " · paquete semántico · sin conexión");
        UniversalSelectionMenu.attach(this, webView, context());
        ReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), webView, context());
        String document = SemanticBibleRenderer.chapter(edition, book, chapter, verses);
        webView.loadDataWithBaseURL("https://ministerium.local/bible/", document,
                "text/html", "UTF-8", null);
    }

    private void move(int delta) {
        if (delta < 0 && chapter == 1 && bookIndex > 0) {
            bookIndex--;
            book = books.get(bookIndex);
            chapter = Math.max(1, book.getChapterCount());
        } else if (delta > 0 && chapter >= book.getChapterCount() && bookIndex + 1 < books.size()) {
            bookIndex++;
            book = books.get(bookIndex);
            chapter = 1;
        } else {
            int next = chapter + delta;
            if (next < 1 || next > book.getChapterCount()) return;
            chapter = next;
        }
        showChapter();
    }

    private String sourceKey() {
        return "bible-semantic:" + edition.getEditionId() + ":" + book.getBookId() + ":" + chapter;
    }

    private ReaderContext context() {
        return new ReaderContext(edition.getName(), sourceKey(), book.getName(),
                book.getShortName() + " " + chapter, "Biblia", false);
    }

    private void applyStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String bg = dark ? "#26211E" : "#FFFDF7";
        String ink = dark ? "#F3EDE4" : "#2A2521";
        String accent = dark ? "#E1C57A" : "#772233";
        String css = "html,body{background:" + bg + "!important;color:" + ink
                + "!important;width:100%;max-width:none}body{font-family:serif;line-height:1.65;"
                + "padding:24px;margin:0;box-sizing:border-box;overflow-wrap:anywhere}"
                + "body *{color:" + ink + "!important;-webkit-text-fill-color:" + ink
                + "!important;max-width:100%;box-sizing:border-box}"
                + "h1,h2,h3,sup{color:" + accent + "!important;-webkit-text-fill-color:"
                + accent + "!important}.bible-verse{display:inline}.bible-paragraph{margin:.8em 0}"
                + ".ministerium-highlight{background:#F6E58D!important;color:#231F1B!important;"
                + "-webkit-text-fill-color:#231F1B!important;padding:1px 2px;border-radius:2px}"
                + "@media(min-width:700px){body{padding-left:48px;padding-right:48px}}"
                + "@media(min-width:1100px){body{padding-left:64px;padding-right:64px}}";
        webView.evaluateJavascript("(function(){var s=document.getElementById('ministerium-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + org.json.JSONObject.quote(css) + ";})()", null);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    @Override protected void onDestroy() {
        if (repository != null) repository.close();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
