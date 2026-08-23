package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT = "document_id";
    public static final String EXTRA_INITIAL_QUERY = "initial_query";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String documentId;
    private EditText input;
    private ProgressBar progress;
    private TextView status;
    private ListView list;
    private List<SearchResult> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT);
        input = findViewById(R.id.inputQuery);
        progress = findViewById(R.id.searchProgress);
        status = findViewById(R.id.txtSearchStatus);
        list = findViewById(R.id.listResults);

        if (documentId != null) {
            DocumentInfo document = ContentRepository.document(documentId);
            ((TextView) findViewById(R.id.txtSearchTitle)).setText(
                    document == null ? "Buscar" : "Buscar en " + document.title);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRunSearch).setOnClickListener(v -> runSearch());
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            SearchResult result = results.get(position);
            if (result.isPrayer()) {
                Intent intent = new Intent(this, PrayerReaderActivity.class);
                intent.putExtra(PrayerReaderActivity.EXTRA_PRAYER_ID, result.prayer.id);
                startActivity(intent);
                return;
            }
            if (result.isRitual()) {
                Intent intent = new Intent(this, RitualReaderActivity.class);
                intent.putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID,
                        result.ritualDocument.id);
                intent.putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX,
                        result.ritualIndex);
                startActivity(intent);
                return;
            }
            if (result.isLiturgicalDate()) {
                Intent intent = new Intent(this, HoursTodayActivity.class);
                intent.putExtra(HoursTodayActivity.EXTRA_YEAR, result.dateYear);
                intent.putExtra(HoursTodayActivity.EXTRA_MONTH, result.dateMonth);
                intent.putExtra(HoursTodayActivity.EXTRA_DAY, result.dateDay);
                startActivity(intent);
                return;
            }
            if (result.isHours()) {
                Intent intent = new Intent(this, HoursReaderActivity.class);
                intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, result.hoursVolume.id);
                intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, result.hoursIndex);
                startActivity(intent);
                return;
            }
            Intent intent = new Intent(this, TextReaderActivity.class);
            intent.putExtra(TextReaderActivity.EXTRA_DOCUMENT, result.document.id);
            intent.putExtra(TextReaderActivity.EXTRA_PAGE, result.pageIndex);
            startActivity(intent);
        });
        status.setText("Busca oraciones, ritos, santos o una fecha de la Liturgia de las Horas.");
        String initial = getIntent().getStringExtra(EXTRA_INITIAL_QUERY);
        if (initial != null && !initial.trim().isEmpty()) {
            input.setText(initial.trim());
            input.post(this::runSearch);
        }
    }

    private void runSearch() {
        String query = input.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Escribe al menos dos caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }
        ReferenceParser.Target reference = ReferenceParser.parse(this, query);
        if (reference != null) {
            openReference(reference);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        status.setText("Buscando…");
        findViewById(R.id.btnRunSearch).setEnabled(false);

        executor.submit(() -> {
            try {
                List<SearchResult> found = ContentRepository.search(
                        getApplicationContext(), query, documentId, 150);
                runOnUiThread(() -> display(found));
            } catch (IOException error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    findViewById(R.id.btnRunSearch).setEnabled(true);
                    status.setText("No se pudo leer el contenido local.");
                });
            }
        });
    }

    private void openReference(ReferenceParser.Target target) {
        if (target.kind == ReferenceParser.Kind.CANON) {
            startActivity(new Intent(this, CanonLawActivity.class)
                    .putExtra(CanonLawActivity.EXTRA_CANON, target.number));
            return;
        }
        if (target.kind == ReferenceParser.Kind.BIBLE) {
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
        status.setText("Localizando CEC " + target.number + "…");
        executor.submit(() -> {
            try {
                CatechismLocator.Target found = CatechismLocator.find(
                        getApplicationContext(), target.number);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (found == null) {
                        status.setText("No se pudo localizar ese numeral en la edición local.");
                        return;
                    }
                    startActivity(new Intent(this, HoursReaderActivity.class)
                            .putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, "catechism")
                            .putExtra(HoursReaderActivity.EXTRA_FILE_PATH, found.filePath)
                            .putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, "CEC " + target.number)
                            .putExtra(HoursReaderActivity.EXTRA_FIND_TEXT, found.searchText));
                });
            } catch (Exception error) {
                runOnUiThread(() -> { progress.setVisibility(View.GONE);
                    status.setText("No se pudo abrir el Catecismo local."); });
            }
        });
    }

    private void display(List<SearchResult> found) {
        results = found;
        List<Map<String, String>> rows = new ArrayList<>();
        for (SearchResult result : found) {
            String source;
            if (result.isPrayer()) {
                source = "Oraciones básicas";
            } else if (result.isRitual()) {
                source = result.ritualDocument.title + " · "
                        + result.ritualDocument.sourceName;
            } else if (result.isLiturgicalDate()) {
                source = "Calendario litúrgico de Ecuador";
            } else if (result.isHours()) {
                source = HoursRepository.isDevotional(result.hoursVolume)
                        ? "Devocionario · Opus Dei"
                        : "Liturgia de las Horas · " + result.hoursVolume.title;
            } else {
                source = result.document.title + " · texto localizado";
            }
            rows.add(Rows.row(result.title, source + "\n" + result.snippet));
        }
        list.setAdapter(Rows.adapter(this, rows));
        progress.setVisibility(View.GONE);
        findViewById(R.id.btnRunSearch).setEnabled(true);
        status.setText(found.isEmpty()
                ? "No se encontraron coincidencias."
                : found.size() + (found.size() == 1 ? " resultado" : " resultados")
                    + (found.size() == 150 ? " (límite mostrado)" : ""));
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
