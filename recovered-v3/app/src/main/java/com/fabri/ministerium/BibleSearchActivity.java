package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BibleSearchActivity extends ThemedActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean flexible;
    private EditText input;
    private ProgressBar progress;
    private TextView status;
    private ListView list;
    private List<BibleSearchRepository.Hit> hits = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_bible_search);
        input = findViewById(R.id.inputBibleSearch);
        progress = findViewById(R.id.bibleSearchProgress);
        status = findViewById(R.id.txtBibleSearchStatus);
        list = findViewById(R.id.listBibleSearchResults);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBibleSearchRun).setOnClickListener(v -> search());
        findViewById(R.id.btnBibleSearchPrecise).setOnClickListener(v -> mode(false));
        findViewById(R.id.btnBibleSearchFlexible).setOnClickListener(v -> mode(true));
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            BibleSearchRepository.Hit hit = hits.get(position);
            startActivity(new Intent(this, BibleReaderActivity.class)
                    .putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, hit.bookIndex)
                    .putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, hit.chapterIndex)
                    .putExtra(BibleReaderActivity.EXTRA_FIND_TEXT, input.getText().toString().trim()));
        });
        mode(false);
    }

    private void mode(boolean selected) {
        flexible = selected;
        findViewById(R.id.btnBibleSearchPrecise).setAlpha(flexible ? .62f : 1f);
        findViewById(R.id.btnBibleSearchFlexible).setAlpha(flexible ? 1f : .62f);
        status.setText(flexible ? "Flexible: encuentra todas las palabras, aunque estén separadas."
                : "Precisa: escribe una frase o una cita como Jn 3,16 o Mt 27,6.");
    }

    private void search() {
        String query = input.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Escribe al menos dos caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        ReferenceParser.Target target = ReferenceParser.parse(this, query);
        if (target != null && target.kind == ReferenceParser.Kind.BIBLE) {
            Intent intent = new Intent(this, BibleReaderActivity.class)
                    .putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, target.bookIndex)
                    .putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, target.chapterIndex);
            if (target.verseStart > 0) {
                intent.putExtra(BibleReaderActivity.EXTRA_SCROLL_VERSE,
                        String.valueOf(target.verseStart));
            }
            startActivity(intent);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText("Buscando en el índice local…");
        executor.submit(() -> {
            try {
                List<BibleSearchRepository.Hit> found = BibleSearchRepository.search(
                        getApplicationContext(), query, flexible, 150);
                runOnUiThread(() -> display(found));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText("No se pudo completar la búsqueda local.");
                });
            }
        });
    }

    private void display(List<BibleSearchRepository.Hit> found) {
        hits = found;
        List<Map<String, String>> rows = new ArrayList<>();
        for (BibleSearchRepository.Hit hit : hits) rows.add(Rows.row(hit.reference, hit.snippet));
        list.setAdapter(Rows.adapter(this, rows));
        progress.setVisibility(View.GONE);
        status.setText(hits.isEmpty() ? "No se encontraron coincidencias."
                : hits.size() + (hits.size() == 1 ? " resultado" : " resultados"));
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
