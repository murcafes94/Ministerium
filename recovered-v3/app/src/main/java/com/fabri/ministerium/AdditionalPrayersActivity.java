package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdditionalPrayersActivity extends ThemedActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Devociones adicionales");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Oraciones tradicionales en español");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Selección complementaria de fórmulas tradicionales que no duplican las oraciones básicas ni el devocionario principal.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        List<PrayerEntry> entries = PrayerRepository.additional();
        List<Map<String, String>> rows = new ArrayList<>();
        for (PrayerEntry entry : entries) rows.add(Rows.row(entry.title,
                entry.category + " · " + entry.description));
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, PrayerReaderActivity.class);
            intent.putExtra(PrayerReaderActivity.EXTRA_PRAYER_ID, entries.get(position).id);
            intent.putExtra(PrayerReaderActivity.EXTRA_COLLECTION, "additional");
            startActivity(intent);
        });
    }
}
