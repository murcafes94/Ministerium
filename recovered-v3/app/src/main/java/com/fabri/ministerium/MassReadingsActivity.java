package com.fabri.ministerium;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Offline reader entry point for Mass readings.
 *
 * This screen never downloads content on entry. Lectionary synchronization is
 * centralized in Settings -> Updates -> Lectionary so opening today's readings
 * remains immediate and predictable.
 */
public class MassReadingsActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "mass_year";
    public static final String EXTRA_MONTH = "mass_month";
    public static final String EXTRA_DAY = "mass_day";

    private Calendar selectedDate;
    private TextView dateLabel;
    private TextView status;
    private ProgressBar progress;
    private Button readButton;
    private Button syncButton;
    private TextView celebrationLabel;
    private TextView liturgicalDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mass_readings);

        Calendar now = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        if (getIntent().hasExtra(EXTRA_YEAR)) {
            selectedDate.clear();
            selectedDate.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                    getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                    getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)),
                    12, 0, 0);
        }

        dateLabel = findViewById(R.id.txtMassDate);
        status = findViewById(R.id.txtReadingsStatus);
        progress = findViewById(R.id.readingsProgress);
        readButton = findViewById(R.id.btnTodayReadings);
        syncButton = findViewById(R.id.btnSyncReadings);
        celebrationLabel = findViewById(R.id.txtMassCelebration);
        liturgicalDetails = findViewById(R.id.txtMassLiturgicalDetails);

        progress.setVisibility(View.GONE);
        findViewById(R.id.btnBack).setOnClickListener(v -> back());
        findViewById(R.id.btnPreviousDay).setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> moveDate(1));
        findViewById(R.id.btnChooseDate).setOnClickListener(v -> chooseDate());
        dateLabel.setOnClickListener(v -> chooseDate());
        findViewById(R.id.btnOpenMissal).setOnClickListener(v -> openMissal());
        readButton.setOnClickListener(v -> openReading());
        syncButton.setText("Sincronizar desde Ajustes");
        syncButton.setOnClickListener(v -> openUpdates());
        findViewById(R.id.btnUsccbCalendar).setOnClickListener(v ->
                UsccbLinks.open(this, UsccbLinks.calendar()));
        findViewById(R.id.btnUsccbDate).setOnClickListener(v ->
                UsccbLinks.open(this, UsccbLinks.readings(selectedDate)));
        findViewById(R.id.btnLocalCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, LiturgicalCalendarActivity.class)));

        showDate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dateLabel != null) showDate();
    }

    private void showDate() {
        String label = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(selectedDate.getTime());
        dateLabel.setText(label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1));

        try {
            LiturgicalDay liturgicalDay = LiturgicalResolver.resolve(this, selectedDate);
            celebrationLabel.setText(liturgicalDay.celebration);
            String detail = liturgicalDay.liturgicalColor;
            if (!liturgicalDay.sourceNote.isEmpty()) {
                if (!detail.isEmpty()) detail += " · ";
                detail += liturgicalDay.sourceNote;
            }
            liturgicalDetails.setText(detail.isEmpty() ? "Leccionario" : detail);
        } catch (Exception error) {
            celebrationLabel.setText("Lecturas de la celebración del día");
            liturgicalDetails.setText("Leccionario");
        }

        boolean saved = MassReadingsRepository.has(this, selectedDate);
        int cached = MassReadingsRepository.cachedDays(this, selectedDate);
        readButton.setText(saved ? "Leer las lecturas sin conexión" : "Lecturas no sincronizadas");
        readButton.setEnabled(saved);
        readButton.setAlpha(saved ? 1f : 0.55f);

        if (saved) {
            status.setText("Lecturas guardadas en este dispositivo · no necesitan Internet.");
        } else if (cached > 0) {
            status.setText(cached + " días de este mes están sincronizados, pero esta fecha no. "
                    + "Actualiza el Leccionario desde Ajustes.");
        } else {
            status.setText("Esta fecha no está guardada. Sincroniza el Leccionario desde "
                    + "Ajustes → Actualizaciones → Leccionario.");
        }

        syncButton.setEnabled(true);
        syncButton.setAlpha(1f);
    }

    private void openReading() {
        if (MassReadingsRepository.has(this, selectedDate)) openLocalReading();
    }

    private void openUpdates() {
        startActivity(new Intent(this, UpdateCenterActivity.class));
    }

    private void openLocalReading() {
        Intent intent = new Intent(this, MassReadingReaderActivity.class);
        intent.putExtra(MassReadingReaderActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(MassReadingReaderActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(MassReadingReaderActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private void openMissal() {
        Intent intent = new Intent(this, MissalActivity.class);
        intent.putExtra(MissalActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(MissalActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(MissalActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.clear();
            selectedDate.set(year, month, day, 12, 0, 0);
            showDate();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void moveDate(int amount) {
        selectedDate.add(Calendar.DATE, amount);
        showDate();
    }

    private void back() { finish(); }

    @Override public void onBackPressed() { back(); }
}
