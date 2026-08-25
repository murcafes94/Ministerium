package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class LiturgicalCalendarActivity extends ThemedActivity {
    private final List<Calendar> dates = new ArrayList<>();
    private Calendar month;
    private final Set<Integer> requestedYears = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liturgical_calendar);

        month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPreviousMonth).setOnClickListener(v -> moveMonth(-1));
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> moveMonth(1));
        showMonth();
    }

    private void moveMonth(int amount) {
        month.add(Calendar.MONTH, amount);
        showMonth();
    }

    private void showMonth() {
        ((TextView) findViewById(R.id.txtCalendarMonth)).setText(
                new SimpleDateFormat("MMMM 'de' yyyy", new Locale("es", "EC"))
                        .format(month.getTime()).toUpperCase(Locale.ROOT));
        dates.clear();
        List<Map<String, String>> rows = new ArrayList<>();
        int maximum = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        try {
            for (int day = 1; day <= maximum; day++) {
                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, day);
                dates.add(date);
                List<LiturgicalEvent> events = LiturgicalCalendarRepository.eventsFor(
                        this, date);
                String title = day + " · " + (events.isEmpty()
                        ? "Feria del día" : events.get(0).summary);
                StringBuilder subtitle = new StringBuilder();
                for (LiturgicalEvent event : events) {
                    if (subtitle.length() > 0) subtitle.append(" · ");
                    subtitle.append(event.rankLabel());
                    if (!event.color.isEmpty()) subtitle.append(" · ").append(event.color);
                }
                if (subtitle.length() == 0) subtitle.append("Calendario romano");
                subtitle.append(" · toca para abrir el oficio o las lecturas");
                rows.add(Rows.row(title, subtitle.toString()));
            }
            ListView list = findViewById(R.id.listCalendarDays);
            list.setAdapter(Rows.adapter(this, rows));
            list.setOnItemClickListener((parent, view, position, id) ->
                    chooseDestination(dates.get(position)));
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo leer el calendario litúrgico.",
                    Toast.LENGTH_LONG).show();
        }
        updateCalendarYear(month.get(Calendar.YEAR));
    }

    private void updateCalendarYear(int year) {
        if (!requestedYears.add(year)) return;
        LiturgicalCalendarRepository.updateYear(this, year, updated -> runOnUiThread(() -> {
            if (isFinishing()) return;
            if (updated && month.get(Calendar.YEAR) == year) {
                Toast.makeText(this, "Calendario litúrgico de " + year + " actualizado.",
                        Toast.LENGTH_SHORT).show();
                showMonth();
            } else if (!updated && LiturgicalCalendarRepository.hasCalendar(this, year)) {
                Toast.makeText(this,
                        "Calendario " + year + " disponible sin conexión. No se pudo comprobar una versión más reciente en Internet.",
                        Toast.LENGTH_LONG).show();
            } else if (!updated) {
                Toast.makeText(this, "Sin conexión: se usará el cálculo romano general para "
                        + year + ".", Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void chooseDestination(Calendar date) {
        String label = LiturgicalCalendarRepository.dateLabel(date);
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(new String[]{
                        "Liturgia de las Horas",
                        "Lecturas de la Misa"
                }, (dialog, which) -> {
                    if (which == 0) openHours(date);
                    else openMassReadings(date);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void openHours(Calendar date) {
        Intent intent = new Intent(this, HoursTodayActivity.class);
        intent.putExtra(HoursTodayActivity.EXTRA_YEAR, date.get(Calendar.YEAR));
        intent.putExtra(HoursTodayActivity.EXTRA_MONTH, date.get(Calendar.MONTH));
        intent.putExtra(HoursTodayActivity.EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private void openMassReadings(Calendar date) {
        Intent intent = new Intent(this, MassReadingsActivity.class);
        intent.putExtra(MassReadingsActivity.EXTRA_YEAR, date.get(Calendar.YEAR));
        intent.putExtra(MassReadingsActivity.EXTRA_MONTH, date.get(Calendar.MONTH));
        intent.putExtra(MassReadingsActivity.EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }
}
