package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReflectionsActivity extends ThemedActivity {
    private List<ReflectionEntry> entries;
    private ListView list;
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this); super.onCreate(savedInstanceState); setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Mis reflexiones");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Subrayados y oración personal");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); list = findViewById(R.id.listItems);
        list.setOnItemClickListener((parent, view, position, id) -> open(entries.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> { confirmDelete(entries.get(position)); return true; });
    }
    @Override protected void onResume() { super.onResume(); reload(); }
    private void reload() {
        entries = ReflectionStore.all(this); List<Map<String, String>> rows = new ArrayList<>();
        for (ReflectionEntry entry : entries) rows.add(Rows.row(entry.title,
                "“" + entry.quote + "”\n" + entry.reflection));
        ((TextView) findViewById(R.id.txtIntro)).setText(entries.isEmpty()
                ? "Todavía no hay reflexiones. Selecciona una frase en la Biblia o en las lecturas de la Misa y toca «Subrayar y reflexionar»."
                : "Toca una reflexión para volver al texto. Mantén pulsado para eliminarla.");
        list.setAdapter(Rows.adapter(this, rows));
    }
    private void open(ReflectionEntry entry) {
        if ("bible".equals(entry.source)) {
            Intent intent = new Intent(this, BibleReaderActivity.class);
            intent.putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, entry.bookIndex);
            intent.putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, entry.chapterIndex); startActivity(intent);
        } else {
            Intent intent = new Intent(this, MassReadingReaderActivity.class);
            intent.putExtra(MassReadingReaderActivity.EXTRA_YEAR, entry.year);
            intent.putExtra(MassReadingReaderActivity.EXTRA_MONTH, entry.month);
            intent.putExtra(MassReadingReaderActivity.EXTRA_DAY, entry.day); startActivity(intent);
        }
    }
    private void confirmDelete(ReflectionEntry entry) {
        new AlertDialog.Builder(this).setTitle("Eliminar reflexión")
                .setMessage("Se quitará también el subrayado guardado para esta frase.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar", (dialog, which) -> {
                    ReflectionStore.delete(this, entry.id); reload();
                }).show();
    }
}
