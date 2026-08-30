package com.fabri.ministerium;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class HoursActivity extends ThemedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Liturgia de las Horas");
        ((TextView) findViewById(R.id.txtSubtitle)).setText("Colección completa sin conexión");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Abre directamente el oficio de hoy, elige otra fecha o consulta un tomo completo.");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        List<HoursVolume> volumes = HoursRepository.all();
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(Rows.row("Liturgia de hoy",
                "Calendario litúrgico de Ecuador · acceso directo al día correspondiente"));
        rows.add(Rows.row("Elegir otra fecha",
                "Busca el oficio y las celebraciones disponibles para un día concreto"));
        for (HoursVolume volume : volumes) {
            rows.add(Rows.row(volume.title, volume.subtitle));
        }
        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                openDate(Calendar.getInstance());
                return;
            }
            if (position == 1) {
                chooseDate();
                return;
            }
            Intent intent = new Intent(this, HoursTocActivity.class);
            intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID, volumes.get(position - 2).id);
            startActivity(intent);
        });
    }

    private void chooseDate() {
        Calendar initial = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.clear();
            selected.set(year, month, dayOfMonth, 12, 0, 0);
            openDate(selected);
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openDate(Calendar date) {
        Intent intent = new Intent(this, HoursTodayActivity.class);
        intent.putExtra(HoursTodayActivity.EXTRA_YEAR, date.get(Calendar.YEAR));
        intent.putExtra(HoursTodayActivity.EXTRA_MONTH, date.get(Calendar.MONTH));
        intent.putExtra(HoursTodayActivity.EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }
}
