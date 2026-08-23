package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RitualCatalogActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT_ID = "ritual_document_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        RitualDocument document = RitualRepository.find(
                getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        if (document == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtTitle)).setText(document.title);
        ((TextView) findViewById(R.id.txtSubtitle)).setText(document.subtitle);
        ((TextView) findViewById(R.id.txtIntro)).setText(
                document.sourceName + " · toca una sección para abrir el texto completo.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        List<Map<String, String>> rows = new ArrayList<>();
        for (RitualEntry entry : document.entries) {
            rows.add(Rows.row(entry.title, entry.category));
        }
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, RitualReaderActivity.class);
            intent.putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, document.id);
            intent.putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, position);
            startActivity(intent);
        });
    }
}
