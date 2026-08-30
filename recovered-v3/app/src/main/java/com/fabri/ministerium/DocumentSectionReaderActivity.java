package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class DocumentSectionReaderActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT = "document_id";
    public static final String EXTRA_ENTRY_INDEX = "entry_index";

    private DocumentInfo document;
    private List<CatalogEntry> entries;
    private int position;
    private TextView content;
    private Button favorite;

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
        entries = ContentRepository.catalog(document.id);
        position = Math.max(0, Math.min(entries.size() - 1,
                getIntent().getIntExtra(EXTRA_ENTRY_INDEX, 0)));
        content = findViewById(R.id.txtContent);
        favorite = findViewById(R.id.btnFavorite);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOriginal).setVisibility(View.VISIBLE);
        findViewById(R.id.btnOriginal).setOnClickListener(v -> openOriginal());
        favorite.setOnClickListener(v -> {
            FavoritesStore.toggleItem(this, favoriteKey());
            updateFavorite();
        });
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));

        showSection();
        ReaderContext context = context();
        TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return position > 0; }
                    @Override public boolean canNext() { return position < entries.size() - 1; }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                });
        TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), content, context);
    }

    private void showSection() {
        CatalogEntry entry = entries.get(position);
        int lastPage = position + 1 < entries.size()
                ? Math.max(entry.pdfPageIndex, entries.get(position + 1).pdfPageIndex - 1)
                : document.pageCount - 1;

        ((TextView) findViewById(R.id.txtReaderTitle)).setText(entry.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                document.title + " · " + entry.section);
        ((TextView) findViewById(R.id.txtSource)).setText(
                "Texto continuo reorganizado · el libro original permanece disponible");
        try {
            content.setText(ContentRepository.catalogSectionText(
                    this, document, entry, lastPage));
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir esta sección.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        updateFavorite();
    }

    private void move(int delta) {
        int next = position + delta;
        if (next < 0 || next >= entries.size()) return;
        position = next;
        getIntent().putExtra(EXTRA_ENTRY_INDEX, position);
        recreate();
    }

    private void openOriginal() {
        Intent intent = new Intent(this, PdfReaderActivity.class);
        intent.putExtra(PdfReaderActivity.EXTRA_DOCUMENT, document.id);
        intent.putExtra(PdfReaderActivity.EXTRA_PAGE, entries.get(position).pdfPageIndex);
        startActivity(intent);
    }

    private String favoriteKey() {
        return "section:" + document.id + ":" + position;
    }

    private void updateFavorite() {
        favorite.setText(FavoritesStore.containsItem(this, favoriteKey()) ? "★" : "☆");
    }

    private ReaderContext context() {
        CatalogEntry entry = entries.get(position);
        return new ReaderContext(document.title, "document:" + document.id + ":" + position,
                entry.title, entry.section, "Documentos/libros", false);
    }
}
