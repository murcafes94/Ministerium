package com.fabri.ministerium;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MassReadingsActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "mass_year";
    public static final String EXTRA_MONTH = "mass_month";
    public static final String EXTRA_DAY = "mass_day";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Calendar selectedDate;
    private TextView dateLabel;
    private TextView status;
    private ProgressBar progress;
    private Button readButton;
    private Button syncButton;
    private TextView celebrationLabel;
    private TextView liturgicalDetails;
    private boolean syncing;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> back());
        findViewById(R.id.btnPreviousDay).setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> moveDate(1));
        findViewById(R.id.btnChooseDate).setOnClickListener(v -> chooseDate());
        dateLabel.setOnClickListener(v -> chooseDate());
        findViewById(R.id.btnOpenMissal).setOnClickListener(v -> openMissal());
        readButton.setOnClickListener(v -> openOrSync());
        syncButton.setOnClickListener(v -> syncMonth(false));
        findViewById(R.id.btnUsccbCalendar).setOnClickListener(v ->
                UsccbLinks.open(this, UsccbLinks.calendar()));
        findViewById(R.id.btnUsccbDate).setOnClickListener(v ->
                UsccbLinks.open(this, UsccbLinks.readings(selectedDate)));
        findViewById(R.id.btnLocalCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, LiturgicalCalendarActivity.class)));

        showDate();
        if ((!MassReadingsRepository.has(this, selectedDate)
                || MassReadingsRepository.monthNeedsFormattingUpdate(this, selectedDate))
                && MassReadingsRepository.isCurrentMonth(selectedDate)) syncMonth(false);
    }

    private void showDate() {
        String label = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(selectedDate.getTime());
        dateLabel.setText(label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1));
        try {
            LiturgicalDay liturgicalDay = LiturgicalResolver.resolve(this, selectedDate);
            celebrationLabel.setText(liturgicalDay.celebration);
            String detail = liturgicalDay.liturgicalColor;
            if (!liturgicalDay.psalterWeek.isEmpty()) {
                if (!detail.isEmpty()) detail += " · ";
                detail += "Semana " + liturgicalDay.psalterWeek + " del salterio";
            }
            liturgicalDetails.setText(detail.isEmpty() ? liturgicalDay.sourceNote
                    : detail + " · " + liturgicalDay.sourceNote);
        } catch (Exception error) {
            celebrationLabel.setText("Lecturas de la celebración del día");
            liturgicalDetails.setText("Leccionario");
        }
        boolean saved = MassReadingsRepository.has(this, selectedDate);
        int cached = MassReadingsRepository.cachedDays(this, selectedDate);
        readButton.setText(saved ? "Leer las lecturas sin conexión"
                : MassReadingsRepository.isCurrentMonth(selectedDate)
                ? "Descargar el mes y leer" : "Ver esta fecha en USCCB");
        status.setText(saved
                ? "Lecturas guardadas en este dispositivo · no necesitan Internet."
                : cached > 0
                ? cached + " lecturas guardadas para este mes; esta fecha aún no está disponible."
                : MassReadingsRepository.isCurrentMonth(selectedDate)
                ? "El mes todavía no está guardado. Ministerium puede actualizarlo ahora."
                : "Esta fecha no fue guardada anteriormente; puedes consultarla en USCCB.");
        syncButton.setEnabled(MassReadingsRepository.isCurrentMonth(selectedDate) && !syncing);
        syncButton.setAlpha(syncButton.isEnabled() ? 1f : 0.55f);
    }

    private void openOrSync() {
        if (MassReadingsRepository.has(this, selectedDate)) openLocalReading();
        else if (MassReadingsRepository.isCurrentMonth(selectedDate)) syncMonth(true);
        else UsccbLinks.open(this, UsccbLinks.readings(selectedDate));
    }

    private void syncMonth(boolean openAfter) {
        if (syncing || !MassReadingsRepository.isCurrentMonth(selectedDate)) return;
        syncing = true;
        progress.setVisibility(View.VISIBLE);
        syncButton.setEnabled(false);
        status.setText("Conectando con la fuente de las lecturas…");
        final Calendar requested = (Calendar) selectedDate.clone();
        executor.submit(() -> {
            try {
                MassReadingsRepository.SyncResult result =
                        MassReadingsRepository.syncCurrentMonth(getApplicationContext(), requested,
                                (completed, total) -> runOnUiThread(() -> {
                                    progress.setMax(total);
                                    progress.setProgress(completed);
                                    status.setText("Actualizando lecturas: " + completed + " de " + total);
                                }));
                runOnUiThread(() -> {
                    syncing = false;
                    progress.setVisibility(View.GONE);
                    showDate();
                    Toast.makeText(this, result.saved + " de " + result.total
                            + " días guardados para este mes.", Toast.LENGTH_LONG).show();
                    if (openAfter && MassReadingsRepository.has(this, selectedDate)) openLocalReading();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    syncing = false;
                    progress.setVisibility(View.GONE);
                    showDate();
                    Toast.makeText(this,
                            "No se pudo actualizar el mes. Puedes usar USCCB mientras tanto.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
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

    private void back() {
        finish();
    }

    @Override public void onBackPressed() { back(); }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
