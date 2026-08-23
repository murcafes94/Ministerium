package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class RitualReaderActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT_ID = "ritual_document_id";
    public static final String EXTRA_ENTRY_INDEX = "ritual_entry_index";

    private RitualDocument document;
    private int position;
    private TextView content;
    private Button favorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_reader);

        document = RitualRepository.find(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        if (document == null) {
            finish();
            return;
        }
        position = Math.max(0, Math.min(document.entries.size() - 1,
                getIntent().getIntExtra(EXTRA_ENTRY_INDEX, 0)));
        content = findViewById(R.id.txtContent);
        favorite = findViewById(R.id.btnFavorite);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOriginal).setVisibility(View.GONE);
        favorite.setOnClickListener(v -> {
            FavoritesStore.toggleItem(this, favoriteKey());
            updateFavorite();
        });
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        showEntry();
    }

    private void showEntry() {
        RitualEntry entry = document.entries.get(position);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(entry.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                document.title + " · " + entry.category);
        ((TextView) findViewById(R.id.txtSource)).setText(
                document.sourceName + " · texto guardado para consulta offline");
        try {
            content.setText(RitualRepository.readSectionStyled(this, document, position));
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir este texto.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ReaderContext context = context();
        TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return position > 0; }
                    @Override public boolean canNext() { return position < document.entries.size() - 1; }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                });
        TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), content, context);
        updateFavorite();
    }

    private void move(int delta) {
        int next = position + delta;
        if (next < 0 || next >= document.entries.size()) return;
        position = next;
        getIntent().putExtra(EXTRA_ENTRY_INDEX, position);
        showEntry();
    }

    private String favoriteKey() {
        return "ritual:" + document.id + ":" + position;
    }

    private void updateFavorite() {
        favorite.setText(FavoritesStore.containsItem(this, favoriteKey()) ? "★" : "☆");
    }

    private ReaderContext context() {
        RitualEntry entry = document.entries.get(position);
        return new ReaderContext(document.title, "ritual:" + document.id + ":" + position,
                entry.title, entry.category, "Rituales", true);
    }
}
