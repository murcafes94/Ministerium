package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MyStudyActivity extends ThemedActivity {
    private String type = StudyEntry.MEDITATION;
    private List<StudyEntry> visible = new ArrayList<>();
    private ListView list;
    private TextView empty;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_my_study);
        list = findViewById(R.id.listStudyEntries);
        empty = findViewById(R.id.txtStudyEmpty);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStudyHighlights).setOnClickListener(v -> select(StudyEntry.HIGHLIGHT));
        findViewById(R.id.btnStudyNotes).setOnClickListener(v -> select(StudyEntry.NOTE));
        findViewById(R.id.btnStudyMeditations).setOnClickListener(v -> select(StudyEntry.MEDITATION));
        findViewById(R.id.btnNewMeditation).setOnClickListener(v -> startActivity(
                new Intent(this, StudyEditorActivity.class)
                        .putExtra(StudyEditorActivity.EXTRA_TYPE, StudyEntry.MEDITATION)
                        .putExtra(StudyEditorActivity.EXTRA_CATEGORY, "Libres")));
        list.setOnItemClickListener((parent, view, position, id) -> show(visible.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(visible.get(position));
            return true;
        });
        select(StudyEntry.MEDITATION);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void select(String selected) {
        type = selected;
        refresh();
        alpha(R.id.btnStudyHighlights, StudyEntry.HIGHLIGHT.equals(type));
        alpha(R.id.btnStudyNotes, StudyEntry.NOTE.equals(type));
        alpha(R.id.btnStudyMeditations, StudyEntry.MEDITATION.equals(type));
    }

    private void alpha(int id, boolean active) {
        findViewById(id).setAlpha(active ? 1f : .62f);
    }

    private void refresh() {
        visible = StudyStore.ofType(this, type);
        List<Map<String, String>> rows = new ArrayList<>();
        for (StudyEntry entry : visible) {
            String subtitle = entry.category;
            if (!entry.reference.isEmpty()) subtitle += " · " + entry.reference;
            if (entry.updatedAt > 0) subtitle += "\n" + DateFormat.getDateInstance(
                    DateFormat.MEDIUM).format(new Date(entry.updatedAt));
            String preview = !entry.body.isEmpty() ? entry.body : entry.quote;
            if (!preview.isEmpty()) subtitle += "\n" + preview;
            rows.add(Rows.row(entry.title.isEmpty() ? entry.reference : entry.title, subtitle));
        }
        list.setAdapter(Rows.adapter(this, rows));
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(StudyEntry.HIGHLIGHT.equals(type) ? "Aún no tienes resaltados."
                : StudyEntry.NOTE.equals(type) ? "Aún no tienes notas."
                : "Aún no tienes meditaciones.\nToca + Nueva meditación para comenzar.");
    }

    private void show(StudyEntry entry) {
        String text = entry.body;
        if (!entry.quote.isEmpty()) text = "“" + entry.quote + "”\n\n" + text;
        new AlertDialog.Builder(this).setTitle(entry.title.isEmpty()
                ? entry.reference : entry.title).setMessage(text)
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> confirmDelete(entry)).show();
    }

    private void confirmDelete(StudyEntry entry) {
        new AlertDialog.Builder(this).setTitle("Eliminar de Mi estudio")
                .setMessage("Podrás deshacer esta acción inmediatamente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    StudyEntry deleted = StudyStore.delete(this, entry.id);
                    refresh();
                    if (deleted != null) new AlertDialog.Builder(this)
                            .setMessage("Elemento eliminado")
                            .setNegativeButton("Cerrar", null)
                            .setPositiveButton("Deshacer", (undo, button) -> {
                                StudyStore.save(this, deleted);
                                refresh();
                            }).show();
                }).show();
    }
}
