package com.fabri.ministerium;

import android.app.Activity;
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

        ((TextView) findViewById(R.id.txtTitle)).setText("Atención pastoral");
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
            Intent intent;
            if (RitualRepository.BAPTISM_ID.equals(selected.id)) {
                intent = new Intent(this, RitualReaderActivity.class);
                intent.putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID, selected.id);
                intent.putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX, 0);
            } else {
                intent = new Intent(this, RitualCatalogActivity.class);
                intent.putExtra(RitualCatalogActivity.EXTRA_DOCUMENT_ID, selected.id);
            }
            startActivity(intent);
        });
    }
}
