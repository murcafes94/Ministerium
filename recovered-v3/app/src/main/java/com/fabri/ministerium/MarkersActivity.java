package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarkersActivity extends ThemedActivity {
    private List<ReadingMarker> entries;
    private ListView list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Subrayados");
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                "Biblia y lecturas de la Misa");
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
        entries = ReadingMarkerStore.all(this);
        List<Map<String, String>> rows = new ArrayList<>();
        for (ReadingMarker marker : entries) {
            rows.add(Rows.row(marker.citation, "“" + marker.quote + "”\n" + marker.subtitle));
        }
        ((TextView) findViewById(R.id.txtIntro)).setText(entries.isEmpty()
                ? "Todavía no hay subrayados. Selecciona una frase en la Biblia o en las lecturas del día y toca «Subrayar»."
                : "Toca una cita para volver al texto. Mantén pulsado para eliminarla.");
        list.setAdapter(Rows.adapter(this, rows));
    }

    private void open(ReadingMarker marker) {
        if ("bible".equals(marker.source)) {
            Intent intent = new Intent(this, BibleReaderActivity.class);
            intent.putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, marker.bookIndex);
            intent.putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, marker.chapterIndex);
            intent.putExtra(BibleReaderActivity.EXTRA_SCROLL_QUOTE, marker.quote);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MassReadingReaderActivity.class);
            intent.putExtra(MassReadingReaderActivity.EXTRA_YEAR, marker.year);
            intent.putExtra(MassReadingReaderActivity.EXTRA_MONTH, marker.month);
            intent.putExtra(MassReadingReaderActivity.EXTRA_DAY, marker.day);
            intent.putExtra(MassReadingReaderActivity.EXTRA_SCROLL_QUOTE, marker.quote);
            startActivity(intent);
        }
    }

    private void confirmDelete(ReadingMarker marker) {
        new AlertDialog.Builder(this).setTitle("Eliminar subrayado")
                .setMessage(marker.citation + "\n\n“" + marker.quote + "”")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    ReadingMarkerStore.delete(this, marker.id);
                    reload();
                }).show();
    }
}
