package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PastoralActivity extends ThemedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Ritual");
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                "Bautismo, enfermos, Viático y difuntos");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Rituales y formularios de consulta pastoral disponibles sin conexión.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        List<RitualDocument> documents = RitualRepository.pastoral();
        List<Map<String, String>> rows = new ArrayList<>();
        for (RitualDocument document : documents) {
            rows.add(Rows.row(document.title,
                    document.subtitle + " · " + document.sourceName));
        }
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            RitualDocument selected = documents.get(position);
            if (selected.entries.size() == 1) {
                startActivity(new Intent(this, RitualReaderActivity.class)
                        .putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, selected.id)
                        .putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, 0));
                return;
            }
            startActivity(new Intent(this, RitualCatalogActivity.class)
                    .putExtra(RitualCatalogActivity.EXTRA_DOCUMENT_ID, selected.id));
        });
    }
}
