package com.fabri.ministerium;

import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RitualReaderActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT_ID = "ritual_document_id";
    public static final String EXTRA_ENTRY_INDEX = "ritual_entry_index";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RitualDocument document;
    private int position;
    private int loadGeneration;
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
        configureReadingTypography(content);
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

    private void configureReadingTypography(TextView view) {
        view.setLineSpacing(0f, 1.12f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
            view.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Las líneas litúrgicas breves terminan como párrafos independientes,
            // por lo que permanecen naturales; el beneficio se aplica al cuerpo largo.
            view.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
    }

    private void showEntry() {
        final int requestedPosition = position;
        final int generation = ++loadGeneration;
        RitualEntry entry = document.entries.get(requestedPosition);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(entry.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(
                document.title + " · " + entry.category);
        ((TextView) findViewById(R.id.txtSource)).setText(
                document.sourceName + " · texto estructurado para consulta offline");
        content.setText("Cargando ritual…");
        updateFavorite();

        executor.submit(() -> {
            try {
                String source = RitualRepository.readSection(
                        getApplicationContext(), document, requestedPosition);
                CharSequence formatted = RitualTextFormatter.format(
                        getApplicationContext(), source);
                runOnUiThread(() -> {
                    if (generation != loadGeneration || isFinishing()) return;
                    content.setText(formatted);
                    ReaderContext context = context();
                    TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                            findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                                @Override public boolean canPrevious() { return position > 0; }
                                @Override public boolean canNext() {
                                    return position < document.entries.size() - 1;
                                }
                                @Override public void previous() { move(-1); }
                                @Override public void next() { move(1); }
                            });
                    TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore),
                            content, context);
                    updateFavorite();
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    if (generation != loadGeneration || isFinishing()) return;
                    content.setText("No se pudo abrir este texto.");
                    Toast.makeText(this, "No se pudo abrir este texto.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
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

    @Override
    protected void onDestroy() {
        loadGeneration++;
        executor.shutdownNow();
        super.onDestroy();
    }
}
