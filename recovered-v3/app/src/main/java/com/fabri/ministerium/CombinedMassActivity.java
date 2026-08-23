package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

/** Recorrido por bloques para la unión comunitaria de Misa y una Hora (OGLH 93–97). */
public class CombinedMassActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "combined_mass_year";
    public static final String EXTRA_MONTH = "combined_mass_month";
    public static final String EXTRA_DAY = "combined_mass_day";
    public static final String EXTRA_HOUR = "combined_mass_hour";
    public static final String EXTRA_LANGUAGE = "combined_mass_language";

    private Calendar date;
    private String hourKey;
    private String language;
    private HourEntry hour;
    private HoursLink proper;
    private CommonOfficeChoice common;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_mass);
        Calendar now = Calendar.getInstance();
        date = Calendar.getInstance();
        date.clear();
        date.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)),
                12, 0, 0);
        hourKey = "vespers".equals(getIntent().getStringExtra(EXTRA_HOUR))
                ? "vespers" : "lauds";
        language = "lat_es".equals(getIntent().getStringExtra(EXTRA_LANGUAGE))
                ? "lat_es" : "es";
        ((TextView) findViewById(R.id.txtCombinedMassTitle)).setText(
                "Misa + " + hourName());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCombinedStart).setOnClickListener(v -> openStart());
        findViewById(R.id.btnCombinedPsalmody).setOnClickListener(v ->
                openHourSegment("hour_psalmody"));
        findViewById(R.id.btnCombinedKyrie).setOnClickListener(v ->
                openMissalSegment("Inicio", "mass_kyrie", "Kyrie y Gloria"));
        findViewById(R.id.btnCombinedCollect).setOnClickListener(v -> openProper("collect"));
        findViewById(R.id.btnCombinedReadings).setOnClickListener(v -> openReadings());
        findViewById(R.id.btnCombinedEucharist).setOnClickListener(v ->
                openMissalSegment("Credo", "mass_eucharist",
                        "Liturgia de la Palabra y eucarística"));
        findViewById(R.id.btnCombinedCanticle).setOnClickListener(v ->
                openHourSegment("hour_canticle"));
        findViewById(R.id.btnCombinedAfterCommunion).setOnClickListener(v ->
                openProper("after_communion"));
        findViewById(R.id.btnCombinedConclusion).setOnClickListener(v ->
                openMissalSegment("RitoConclusión", "", "Conclusión de la Misa"));
        ((RadioGroup) findViewById(R.id.groupCombinedStart))
                .setOnCheckedChangeListener((group, checkedId) -> updateStartLabel());
        updateStartLabel();
        prepare();
    }

    private void prepare() {
        setEnabled(false);
        new Thread(() -> {
            try {
                LiturgicalDay day = LiturgicalResolver.resolve(getApplicationContext(), date);
                List<HourEntry> hours = DailyHoursRepository.hoursFor(
                        getApplicationContext(), day.temporalOffice, date);
                for (HourEntry entry : hours) if (hourKey.equals(entry.key)) hour = entry;
                for (HoursLink saint : day.saintOffices) {
                    if (saint.requiresProperOffice()) {
                        proper = saint;
                        List<CommonOfficeChoice> choices = SaintOfficeRepository.commonChoices(
                                getApplicationContext(), saint);
                        common = choices.isEmpty() ? null : choices.get(0);
                        break;
                    }
                }
                if (hour == null) throw new IllegalStateException();
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.txtCombinedMassCelebration)).setText(
                            day.celebration + " · " + day.dateLabel);
                    setEnabled(true);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Esta unión no está disponible para la fecha elegida.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void setEnabled(boolean enabled) {
        int[] ids = {R.id.btnCombinedStart, R.id.btnCombinedPsalmody,
                R.id.btnCombinedKyrie, R.id.btnCombinedCollect,
                R.id.btnCombinedReadings, R.id.btnCombinedEucharist,
                R.id.btnCombinedCanticle, R.id.btnCombinedAfterCommunion,
                R.id.btnCombinedConclusion};
        for (int id : ids) {
            View view = findViewById(id);
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : .45f);
        }
    }

    private void updateStartLabel() {
        boolean modeA = ((RadioGroup) findViewById(R.id.groupCombinedStart))
                .getCheckedRadioButtonId() != R.id.startMassEntrance;
        ((TextView) findViewById(R.id.btnCombinedStart)).setText(modeA
                ? "1 · Versículo e himno de " + hourName()
                : "1 · Entrada, procesión y saludo de la Misa");
    }

    private void openStart() {
        boolean massEntrance = ((RadioGroup) findViewById(R.id.groupCombinedStart))
                .getCheckedRadioButtonId() == R.id.startMassEntrance;
        if (massEntrance) openMissalSegment("Inicio", "mass_greeting",
                "Entrada y saludo de la Misa");
        else openHourSegment("hour_hymn");
    }

    private void openHourSegment(String segment) {
        if (hour == null) return;
        Intent intent = new Intent(this, HoursReaderActivity.class);
        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, hour.volume.id);
        intent.putExtra(HoursReaderActivity.EXTRA_FILE_PATH, hour.filePath);
        intent.putExtra(HoursReaderActivity.EXTRA_FRAGMENT, hour.fragment);
        intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE,
                hourName() + " unida a la Misa");
        intent.putExtra(HoursReaderActivity.EXTRA_COMBINED_SEGMENT, segment);
        intent.putExtra(HoursReaderActivity.EXTRA_SHOW_INTENTIONS, false);
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
        if (ordinaryWeek > 0) {
            intent.putExtra(HoursReaderActivity.EXTRA_ORDINARY_WEEK, ordinaryWeek);
            intent.putExtra(HoursReaderActivity.EXTRA_LECTIONARY_CYCLE,
                    LiturgicalResolver.lectionaryCycle(date));
            intent.putExtra(HoursReaderActivity.EXTRA_READINGS_YEAR,
                    date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
        }
        if (proper != null) {
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_VOLUME_ID, proper.volume.id);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_TOC_INDEX, proper.tocIndex);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_TITLE, proper.title);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_RANK,
                    proper.liturgicalRank);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_HOUR_KEY, hour.key);
            if (common != null) {
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_FILE, common.filePath);
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_FRAGMENT,
                        common.fragment);
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_TITLE, common.title);
            }
        }
        startActivity(intent);
    }

    private void openMissalSegment(String title, String segment, String readerTitle) {
        try {
            int index = EpubUtils.findEntryIndex(this, HoursRepository.ROMAN_MISSAL, title);
            if (index < 0) throw new IllegalStateException();
            Intent intent = new Intent(this, HoursReaderActivity.class);
            intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID,
                    HoursRepository.ROMAN_MISSAL.id);
            intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, index);
            intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, readerTitle);
            intent.putExtra(HoursReaderActivity.EXTRA_MISSAL_LANGUAGE, language);
            if (!segment.isEmpty()) intent.putExtra(
                    HoursReaderActivity.EXTRA_COMBINED_SEGMENT, segment);
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "No se encontró este bloque del Ordinario.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openProper(String part) {
        startActivity(new Intent(this, MissalActivity.class)
                .putExtra(MissalActivity.EXTRA_YEAR, date.get(Calendar.YEAR))
                .putExtra(MissalActivity.EXTRA_MONTH, date.get(Calendar.MONTH))
                .putExtra(MissalActivity.EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH))
                .putExtra(MissalActivity.EXTRA_OPEN_PART, part)
                .putExtra(MissalActivity.EXTRA_LANGUAGE, language)
                .putExtra(MissalActivity.EXTRA_RETURN_TO_CALLER, true));
    }

    private void openReadings() {
        Intent intent = new Intent(this, MassReadingsActivity.class)
                .putExtra(MassReadingsActivity.EXTRA_YEAR, date.get(Calendar.YEAR))
                .putExtra(MassReadingsActivity.EXTRA_MONTH, date.get(Calendar.MONTH))
                .putExtra(MassReadingsActivity.EXTRA_DAY, date.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private String hourName() { return "vespers".equals(hourKey) ? "Vísperas" : "Laudes"; }
}
