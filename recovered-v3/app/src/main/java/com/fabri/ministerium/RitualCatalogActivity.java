package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RitualCatalogActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT_ID = "ritual_document_id";

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String requested = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);
        if (RitualRepository.COMMON_BLESSINGS_ID.equals(requested)) {
            showBlessings();
            return;
        }

        RitualDocument document = RitualRepository.find(requested);
        if (document == null) {
            ((TextView) findViewById(R.id.txtTitle)).setText("Rituales");
            ((TextView) findViewById(R.id.txtIntro)).setText("No se encontró el ritual solicitado.");
            return;
        }
        if (document.entries.size() == 1) {
            startActivity(new Intent(this, RitualReaderActivity.class)
                    .putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, document.id)
                    .putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, 0));
            finish();
            return;
        }
        showDocument(document);
    }

    private void showBlessings() {
        ((TextView) findViewById(R.id.txtTitle)).setText("Bendicional");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Bendiciones para diversas circunstancias");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Textos de Liturgia Papal · toca una bendición para abrir su rito.");

        List<RitualDocument> blessings = new ArrayList<>();
        for (RitualDocument document : RitualRepository.all()) {
            if (!document.entries.isEmpty()
                    && "Bendiciones".equalsIgnoreCase(document.entries.get(0).category)) {
                blessings.add(document);
            }
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (RitualDocument document : blessings) {
            rows.add(Rows.row(document.title, document.subtitle));
        }
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            RitualDocument document = blessings.get(position);
            startActivity(new Intent(this, RitualReaderActivity.class)
                    .putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, document.id)
                    .putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, 0));
        });
    }

    private void showDocument(RitualDocument document) {
        ((TextView) findViewById(R.id.txtTitle)).setText(document.title);
        ((TextView) findViewById(R.id.txtSubtitle)).setText(document.subtitle);
        ((TextView) findViewById(R.id.txtIntro)).setText(
                document.sourceName + " · toca una sección para abrir el texto completo.");

        List<Map<String, String>> rows = new ArrayList<>();
        for (RitualEntry entry : document.entries) rows.add(Rows.row(entry.title, entry.category));
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            startActivity(new Intent(this, RitualReaderActivity.class)
                    .putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, document.id)
                    .putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, position));
        });
    }
}
