package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudyDeskActivity extends ThemedActivity {
    public static final String EXTRA_QUERY = "study_query";
    private static final int EXPORT_STUDY = 91;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText input;
    private TextView status;
    private ProgressBar progress;
    private ListView list;
    private List<DeskRow> rows = new ArrayList<>();
    private boolean exportMarkdown;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_study_desk);
        input = findViewById(R.id.inputStudyQuery);
        status = findViewById(R.id.txtStudyDeskStatus);
        progress = findViewById(R.id.studyDeskProgress);
        list = findViewById(R.id.listStudyDeskResults);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStudyDeskRun).setOnClickListener(v -> search());
        findViewById(R.id.btnStudyExport).setOnClickListener(v -> chooseExport());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });
        list.setOnItemClickListener((parent, view, position, id) -> open(rows.get(position)));
        String initial = getIntent().getStringExtra(EXTRA_QUERY);
        if (initial != null && !initial.trim().isEmpty()) {
            input.setText(initial);
            search();
        } else {
            int count = StudyStore.all(this).size();
            status.setText("Busca un tema, referencia o etiqueta. Mi estudio contiene "
                    + count + (count == 1 ? " anotación." : " anotaciones."));
        }
    }

    private void search() {
        String query = input.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Escribe al menos dos caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        status.setText("Reuniendo relaciones verificables…");
        executor.submit(() -> {
            List<DeskRow> found = new ArrayList<>();
            try {
                for (SearchResult result : ContentRepository.search(
                        getApplicationContext(), query, null, 120)) {
                    found.add(new DeskRow(source(result), "Coincidencia textual explícita",
                            result.title, result.snippet, result, null));
                }
            } catch (Exception ignored) {}
            String wanted = normalize(query);
            for (StudyEntry entry : StudyStore.all(getApplicationContext())) {
                StringBuilder personal = new StringBuilder(entry.title).append(' ')
                        .append(entry.reference).append(' ').append(entry.quote).append(' ')
                        .append(entry.body).append(' ').append(entry.contentId);
                for (String tag : entry.tags) personal.append(' ').append(tag);
                if (!normalize(personal.toString()).contains(wanted)) continue;
                found.add(new DeskRow("Mi estudio · " + entry.category,
                        "Coincide con una nota, reflexión, etiqueta o subrayado personal",
                        entry.title.isEmpty() ? entry.reference : entry.title,
                        entry.body.isEmpty() ? entry.quote : entry.body, null, entry));
            }
            found.sort(Comparator.comparing(value -> value.source));
            runOnUiThread(() -> display(found));
        });
    }

    private void display(List<DeskRow> found) {
        rows = found;
        List<Map<String, String>> display = new ArrayList<>();
        for (DeskRow row : rows) display.add(Rows.row(row.title,
                row.source + "\n" + row.reason + "\n" + row.snippet));
        list.setAdapter(Rows.adapter(this, display));
        progress.setVisibility(View.GONE);
        status.setText(rows.isEmpty() ? "No se encontraron relaciones verificables."
                : rows.size() + (rows.size() == 1 ? " resultado" : " resultados")
                + " agrupados por fuente");
    }

    private void open(DeskRow row) {
        if (row.study != null) {
            StringBuilder message = new StringBuilder();
            if (!row.study.quote.isEmpty()) message.append('“').append(row.study.quote)
                    .append("”\n\n");
            if (!row.study.body.isEmpty()) message.append(row.study.body);
            if (!row.study.tags.isEmpty()) {
                message.append("\n\nEtiquetas: ");
                for (int i = 0; i < row.study.tags.size(); i++) {
                    if (i > 0) message.append(", ");
                    message.append(row.study.tags.get(i));
                }
            }
            if (!row.study.contentId.isEmpty()) {
                message.append("\n\nID: ").append(row.study.contentId);
            }
            new AlertDialog.Builder(this).setTitle(row.title)
                    .setMessage(message.toString()).setPositiveButton("Cerrar", null).show();
            return;
        }
        SearchResult result = row.result;
        if (result.isPrayer()) {
            startActivity(new Intent(this, PrayerReaderActivity.class)
                    .putExtra(PrayerReaderActivity.EXTRA_PRAYER_ID, result.prayer.id));
        } else if (result.isRitual()) {
            startActivity(new Intent(this, RitualReaderActivity.class)
                    .putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, result.ritualDocument.id)
                    .putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, result.ritualIndex));
        } else if (result.isLiturgicalDate()) {
            startActivity(new Intent(this, HoursTodayActivity.class)
                    .putExtra(HoursTodayActivity.EXTRA_YEAR, result.dateYear)
                    .putExtra(HoursTodayActivity.EXTRA_MONTH, result.dateMonth)
                    .putExtra(HoursTodayActivity.EXTRA_DAY, result.dateDay));
        } else if (result.isHours()) {
            startActivity(new Intent(this, HoursReaderActivity.class)
                    .putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, result.hoursVolume.id)
                    .putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, result.hoursIndex));
        } else {
            startActivity(new Intent(this, TextReaderActivity.class)
                    .putExtra(TextReaderActivity.EXTRA_DOCUMENT, result.document.id)
                    .putExtra(TextReaderActivity.EXTRA_PAGE, result.pageIndex));
        }
    }

    private void chooseExport() {
        new AlertDialog.Builder(this).setTitle("Exportar Mi estudio")
                .setItems(new String[]{"Markdown (.md)", "JSON portable (.json)"},
                        (dialog, which) -> launchExport(which == 0))
                .setNegativeButton("Cancelar", null).show();
    }

    private void launchExport(boolean markdown) {
        exportMarkdown = markdown;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(markdown ? "text/markdown" : "application/json");
        intent.putExtra(Intent.EXTRA_TITLE,
                markdown ? "ministerium-mi-estudio.md" : "ministerium-mi-estudio.json");
        startActivityForResult(intent, EXPORT_STUDY);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != EXPORT_STUDY || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        try {
            byte[] bytes = exportMarkdown ? StudyExport.markdown(this) : StudyExport.json(this);
            try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "w")) {
                if (output == null) throw new IllegalStateException("No se pudo abrir el destino.");
                output.write(bytes);
                output.flush();
            }
            Toast.makeText(this, "Mi estudio se exportó correctamente.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo exportar Mi estudio.", Toast.LENGTH_LONG).show();
        }
    }

    private static String source(SearchResult result) {
        if (result.isPrayer()) return "Oraciones";
        if (result.isRitual()) return "Rituales · " + result.ritualDocument.title;
        if (result.isLiturgicalDate()) return "Calendario";
        if (result.isHours()) {
            if ("catechism".equals(result.hoursVolume.id)) return "Catecismo";
            if (HoursRepository.isReference(result.hoursVolume)) return "Magisterio";
            return HoursRepository.isDevotional(result.hoursVolume)
                    ? "Devocionario" : "Liturgia";
        }
        return result.document == null ? "Biblioteca" : result.document.title;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private static final class DeskRow {
        final String source, reason, title, snippet;
        final SearchResult result;
        final StudyEntry study;
        DeskRow(String source, String reason, String title, String snippet,
                SearchResult result, StudyEntry study) {
            this.source = source; this.reason = reason; this.title = title;
            this.snippet = snippet; this.result = result; this.study = study;
        }
    }
}
