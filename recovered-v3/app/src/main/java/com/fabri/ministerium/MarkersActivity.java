package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Saved highlights are now backed by the same StudyStore used by the reader. */
public class MarkersActivity extends ThemedActivity {
    private List<StudyEntry> entries;
    private ListView list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Subrayados");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Biblia, lecturas y estudio");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.listItems);
        list.setOnItemClickListener((parent, view, position, id) -> open(entries.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(entries.get(position));
            return true;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        entries = StudyStore.ofType(this, StudyEntry.HIGHLIGHT);
        List<Map<String, String>> rows = new ArrayList<>();
        for (StudyEntry entry : entries) {
            String citation = !entry.reference.isEmpty() ? entry.reference
                    : !entry.title.isEmpty() ? entry.title : entry.category;
            String detail = "“" + entry.quote + "”";
            if (!entry.category.isEmpty()) detail += "\n" + entry.category;
            rows.add(Rows.row(citation, detail));
        }
        ((TextView) findViewById(R.id.txtIntro)).setText(entries.isEmpty()
                ? "Todavía no hay subrayados. Selecciona una frase y toca «Subrayar»."
                : "Aquí aparecen los mismos subrayados de Mi estudio. Toca uno para volver al texto; mantén pulsado para eliminarlo.");
        list.setAdapter(Rows.adapter(this, rows));
    }

    private void open(StudyEntry entry) {
        String key = entry.sourceKey == null ? "" : entry.sourceKey;
        if (key.startsWith("bible:")) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                try {
                    int bookIndex = Integer.parseInt(parts[1]);
                    int chapterNumber = Integer.parseInt(parts[2]);
                    int chapterIndex = findChapterIndex(bookIndex, chapterNumber);
                    startActivity(new Intent(this, BibleReaderActivity.class)
                            .putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, bookIndex)
                            .putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, chapterIndex)
                            .putExtra(BibleReaderActivity.EXTRA_SCROLL_QUOTE, entry.quote));
                    return;
                } catch (Exception ignored) {}
            }
        }
        if (key.startsWith("mass:")) {
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                try {
                    startActivity(new Intent(this, MassReadingReaderActivity.class)
                            .putExtra(MassReadingReaderActivity.EXTRA_YEAR, Integer.parseInt(parts[1]))
                            .putExtra(MassReadingReaderActivity.EXTRA_MONTH, Integer.parseInt(parts[2]))
                            .putExtra(MassReadingReaderActivity.EXTRA_DAY, Integer.parseInt(parts[3]))
                            .putExtra(MassReadingReaderActivity.EXTRA_SCROLL_QUOTE, entry.quote));
                    return;
                } catch (Exception ignored) {}
            }
        }
        new AlertDialog.Builder(this).setTitle(entry.reference.isEmpty() ? "Subrayado" : entry.reference)
                .setMessage(entry.quote).setPositiveButton("Cerrar", null).show();
    }

    private int findChapterIndex(int bookIndex, int chapterNumber) throws Exception {
        List<BibleRepository.Book> books = BibleRepository.books(this);
        if (bookIndex < 0 || bookIndex >= books.size()) return 0;
        List<BibleRepository.Chapter> chapters = books.get(bookIndex).chapters;
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).number == chapterNumber) return i;
        }
        return Math.max(0, Math.min(chapters.size() - 1, chapterNumber - 1));
    }

    private void confirmDelete(StudyEntry entry) {
        String label = !entry.reference.isEmpty() ? entry.reference : entry.title;
        new AlertDialog.Builder(this).setTitle("Eliminar subrayado")
                .setMessage(label + "\n\n“" + entry.quote + "”")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    StudyStore.delete(this, entry.id);
                    reload();
                }).show();
    }
}
