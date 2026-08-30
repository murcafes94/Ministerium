package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DevotionalHubActivity extends ThemedActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Devocionario");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Oración personal y sacramental");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Elige el devocionario completo, una selección complementaria o un examen de conciencia adaptado a la edad.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(Rows.row("Devocionario completo", "Oraciones en español y latín · sin conexión"));
        rows.add(Rows.row("Devociones adicionales", "Oraciones tradicionales en español"));
        rows.add(Rows.row("Examen de conciencia", "Adultos, jóvenes y niños"));
        rows.add(Rows.row("Mis oraciones", "Crea y guarda tus oraciones personales"));
        rows.add(Rows.row("Viacrucis de Joseph Ratzinger (2005)",
                "Catorce estaciones del futuro Benedicto XVI · sin conexión"));
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                Intent intent = new Intent(this, HoursTocActivity.class);
                intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID, HoursRepository.DEVOTIONAL.id);
                startActivity(intent);
            } else if (position == 1) {
                startActivity(new Intent(this, AdditionalPrayersActivity.class));
            } else if (position == 2) {
                startActivity(new Intent(this, ConscienceActivity.class));
            } else if (position == 3) {
                startActivity(new Intent(this, PersonalPrayersActivity.class));
            } else {
                Intent intent = new Intent(this, HoursTocActivity.class);
                intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID,
                        HoursRepository.RATZINGER_WAY_OF_CROSS.id);
                startActivity(intent);
            }
        });
    }
}
