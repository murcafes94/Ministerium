package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConscienceActivity extends ThemedActivity {
    private static final String[] TITLES = {
            "Examen para adultos", "Examen para jóvenes y adolescentes", "Examen para niños"};
    private static final String[] FILES = {
            "devotions/examen_adultos.txt", "devotions/examen_jovenes.txt",
            "devotions/examen_ninos.txt"};

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        ((TextView) findViewById(R.id.txtTitle)).setText("Examen de conciencia");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Preparación para la Reconciliación");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Estas preguntas son una ayuda para la sinceridad ante Dios; no sustituyen el diálogo con el confesor.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(Rows.row(TITLES[0], "Preguntas para la vida personal, familiar y profesional"));
        rows.add(Rows.row(TITLES[1], "Fe, familia, amistades, estudio y responsabilidad"));
        rows.add(Rows.row(TITLES[2], "Preguntas breves y claras para niños"));
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ConscienceReaderActivity.class);
            intent.putExtra(ConscienceReaderActivity.EXTRA_TITLE, TITLES[position]);
            intent.putExtra(ConscienceReaderActivity.EXTRA_ASSET, FILES[position]);
            startActivity(intent);
        });
    }
}
