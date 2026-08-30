package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicPrayersActivity extends ThemedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Oraciones básicas");
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                "Fórmulas esenciales para la oración cotidiana");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Cada oración aparece como un texto independiente, limpio y fácil de consultar.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        List<PrayerEntry> entries = PrayerRepository.basic();
        List<Map<String, String>> rows = new ArrayList<>();
        for (PrayerEntry entry : entries) {
            rows.add(Rows.row(entry.title, entry.category + " · " + entry.description));
        }

        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, PrayerReaderActivity.class);
            intent.putExtra(PrayerReaderActivity.EXTRA_PRAYER_ID, entries.get(position).id);
            intent.putExtra(PrayerReaderActivity.EXTRA_COLLECTION, "basic");
            startActivity(intent);
        });
    }
}
