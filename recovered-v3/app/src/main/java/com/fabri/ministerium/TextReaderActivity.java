package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import org.json.JSONObject;

public class TextReaderActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT = "document_id";
    public static final String EXTRA_PAGE = "page_index";

    private DocumentInfo document;
    private String[] pages;
    private int pageIndex;
    private TextView titleView;
    private TextView pageView;
    private TextView contentView;
    private ScrollView scrollView;
    private Button favoriteButton;
    private int pendingScrollY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_reader);

        document = ContentRepository.document(getIntent().getStringExtra(EXTRA_DOCUMENT));
        if (document == null) {
            finish();
            return;
        }

        pageIndex = clamp(getIntent().getIntExtra(EXTRA_PAGE, 0), 0, document.pageCount - 1);
        pendingScrollY = getIntent().getIntExtra("restore_scroll_y", 0);
        titleView = findViewById(R.id.txtReaderTitle);
        pageView = findViewById(R.id.txtReaderSubtitle);
        contentView = findViewById(R.id.txtContent);
        scrollView = findViewById(R.id.readerScroll);
        favoriteButton = findViewById(R.id.btnFavorite);

        try {
            pages = ContentRepository.pages(this, document);
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir el texto local.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOriginal).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.btnOriginal).setOnClickListener(v -> openOriginal());
        favoriteButton.setOnClickListener(v -> {
            FavoritesStore.toggle(this, document.id, pageIndex);
            updateFavoriteIcon();
        });
        showPage();
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        bindChrome();
    }

    private void showPage() {
        String text = pages[pageIndex] == null ? "" : pages[pageIndex].trim();
        titleView.setText(ContentRepository.firstUsefulLine(text));
        pageView.setText(document.title + " · página PDF " + (pageIndex + 1) + " de " + document.pageCount);
        contentView.setText(text.isEmpty()
                ? "Esta página no contiene texto extraíble. Usa «Página original» para verla."
                : text);
        int restore = pendingScrollY;
        pendingScrollY = 0;
        scrollView.post(() -> scrollView.scrollTo(0, restore));
        updateFavoriteIcon();
        bindChrome();
    }

    private void move(int delta) {
        int next = clamp(pageIndex + delta, 0, document.pageCount - 1);
        if (next != pageIndex) {
            pageIndex = next;
            showPage();
        }
    }

    private void openOriginal() {
        Intent intent = new Intent(this, PdfReaderActivity.class);
        intent.putExtra(PdfReaderActivity.EXTRA_DOCUMENT, document.id);
        intent.putExtra(PdfReaderActivity.EXTRA_PAGE, pageIndex);
        startActivity(intent);
    }

    private void bindChrome() {
        ReaderContext context = new ReaderContext(document.title,
                "page:" + document.id + ":" + pageIndex, titleView.getText().toString(),
                "Página " + (pageIndex + 1), "Documentos/libros", false);
        TextViewReaderChrome.attach(this, contentView, scrollView,
                findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return pageIndex > 0; }
                    @Override public boolean canNext() { return pageIndex < document.pageCount - 1; }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                });
        TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore),
                contentView, context);
    }

    private void updateFavoriteIcon() {
        favoriteButton.setText(FavoritesStore.contains(this, document.id, pageIndex) ? "★" : "☆");
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override protected void onPause() {
        try {
            ContinueReadingStore.save(this, document.title, titleView.getText().toString(),
                    TextReaderActivity.class,
                    new JSONObject().put(EXTRA_DOCUMENT, document.id)
                            .put(EXTRA_PAGE, pageIndex), scrollView.getScrollY());
        } catch (Exception ignored) {}
        super.onPause();
    }
}
