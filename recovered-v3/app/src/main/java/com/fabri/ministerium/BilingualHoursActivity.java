package com.fabri.ministerium;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BilingualHoursActivity extends ThemedActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, HourEntry> spanishHours = new HashMap<>();
    private final Map<String, Integer> cards = new LinkedHashMap<>();
    private Calendar selectedDate;
    private LiturgicalDay spanishDay;
    private LatinContentManager.LatinDay latinDay;
    private HoursLink selectedProper;
    private List<CommonOfficeChoice> commonChoices = Collections.emptyList();
    private CommonOfficeChoice selectedCommon;
    private ProgressBar progress;
    private View content;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bilingual_hours);
        selectedDate = Calendar.getInstance();
        progress = findViewById(R.id.bilingualProgress);
        content = findViewById(R.id.bilingualContent);
        cards.put("invitatory", R.id.cardInvitatory);
        cards.put("office", R.id.cardOfficeReadings);
        cards.put("lauds", R.id.cardLauds);
        cards.put("terce", R.id.cardTerce);
        cards.put("sext", R.id.cardSext);
        cards.put("none", R.id.cardNone);
        cards.put("vespers", R.id.cardVespers);
        cards.put("compline", R.id.cardCompline);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCalendar).setOnClickListener(v -> chooseDate());
        findViewById(R.id.btnPreviousDay).setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> moveDate(1));
        findViewById(R.id.btnChooseOffice).setOnClickListener(v -> chooseOffice());
        findViewById(R.id.btnManageLatin).setOnClickListener(v ->
                startActivity(new Intent(this, LatinHoursActivity.class)));
        loadDate();
    }

    private void loadDate() {
        final Calendar request = (Calendar) selectedDate.clone();
        final String requestKey = key(request);
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.INVISIBLE);
        executor.submit(() -> {
            try {
                LiturgicalDay resolved = LiturgicalResolver.resolve(getApplicationContext(), request);
                List<HourEntry> hours = DailyHoursRepository.hoursFor(
                        getApplicationContext(), resolved.temporalOffice, request);
                LatinContentManager.LatinDay latin = LatinContentManager.day(
                        getApplicationContext(), request);
                runOnUiThread(() -> {
                    if (requestKey.equals(key(selectedDate))) showDay(resolved, hours, latin);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!requestKey.equals(key(selectedDate))) return;
                    progress.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.txtBilingualDate)).setText(longDate(selectedDate));
                    ((TextView) findViewById(R.id.txtBilingualCelebration)).setText(
                            "Contenido bilingüe no disponible para este año");
                    ((TextView) findViewById(R.id.txtBilingualStatus)).setText(
                            "Instala o importa primero el EPUB latino del año seleccionado.");
                    for (int id : cards.values()) {
                        findViewById(id).setEnabled(false);
                        findViewById(id).setAlpha(0.42f);
                    }
                });
            }
        });
    }

    private void showDay(LiturgicalDay resolved, List<HourEntry> hours,
                         LatinContentManager.LatinDay latin) {
        spanishDay = resolved;
        latinDay = latin;
        selectedProper = null;
        selectedCommon = null;
        commonChoices = Collections.emptyList();
        spanishHours.clear();
        for (HourEntry hour : hours) spanishHours.put(hour.key, hour);
        for (HoursLink saint : resolved.saintOffices) {
            if (saint.requiresProperOffice()) {
                selectedProper = saint;
                loadCommonChoices(false);
                break;
            }
        }
        ((TextView) findViewById(R.id.txtBilingualDate)).setText(longDate(selectedDate));
        ((TextView) findViewById(R.id.txtBilingualStatus)).setText(
                "Español de Ecuador · " + latin.dateTitle + " · ambas ediciones sin conexión");
        updateCelebration();
        for (Map.Entry<String, Integer> card : cards.entrySet()) bind(card.getKey(), card.getValue());
        findViewById(R.id.btnChooseOffice).setEnabled(!resolved.saintOffices.isEmpty());
        findViewById(R.id.btnChooseOffice).setAlpha(
                resolved.saintOffices.isEmpty() ? 0.55f : 1f);
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    private void bind(String key, int id) {
        HourEntry spanish = spanishHours.get(key);
        String latin = latinDay == null ? null : latinDay.hours.get(key);
        View card = findViewById(id);
        boolean available = spanish != null && latin != null;
        card.setEnabled(available);
        card.setAlpha(available ? 1f : 0.42f);
        card.setOnClickListener(available ? v -> openHour(spanish, latin) : null);
    }

    private void updateCelebration() {
        if (spanishDay == null) return;
        String celebration = selectedProper == null ? spanishDay.celebration : selectedProper.title;
        ((TextView) findViewById(R.id.txtBilingualCelebration)).setText(celebration);
        String latin = latinDay == null ? "" : latinDay.celebration;
        ((TextView) findViewById(R.id.txtBilingualDetails)).setText(
                (selectedProper == null ? spanishDay.psalterWeek : selectedProper.subtitle)
                        + (latin.isEmpty() ? "" : " · LA: " + latin));
    }

    private void chooseOffice() {
        if (spanishDay == null || spanishDay.saintOffices.isEmpty()) return;
        List<String> labels = new ArrayList<>();
        labels.add("Oficio de la feria · " + spanishDay.celebration);
        for (HoursLink saint : spanishDay.saintOffices) {
            labels.add(saint.subtitle + " · " + saint.title);
        }
        int checked = selectedProper == null ? 0 : spanishDay.saintOffices.indexOf(selectedProper) + 1;
        new AlertDialog.Builder(this).setTitle("Elegir oficio para la columna española")
                .setSingleChoiceItems(labels.toArray(new String[0]), checked,
                        (dialog, which) -> {
                            selectedProper = which == 0 ? null : spanishDay.saintOffices.get(which - 1);
                            dialog.dismiss();
                            if (selectedProper == null) {
                                selectedCommon = null;
                                commonChoices = Collections.emptyList();
                                updateCelebration();
                            } else loadCommonChoices(true);
                        }).setNegativeButton("Cancelar", null).show();
    }

    private void loadCommonChoices(boolean showChooser) {
        try {
            commonChoices = SaintOfficeRepository.commonChoices(this, selectedProper);
        } catch (Exception error) {
            commonChoices = Collections.emptyList();
        }
        selectedCommon = commonChoices.isEmpty() ? null : commonChoices.get(0);
        if (!showChooser || commonChoices.size() <= 1) {
            updateCelebration();
            return;
        }
        String[] labels = new String[commonChoices.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = commonChoices.get(i).title;
        new AlertDialog.Builder(this).setTitle("Elegir formulario de la celebración")
                .setSingleChoiceItems(labels, 0, (dialog, which) -> {
                    selectedCommon = commonChoices.get(which);
                    updateCelebration();
                    dialog.dismiss();
                }).setNegativeButton("Usar el primero", null).show();
    }

    private void openHour(HourEntry entry, String latinPath) {
        Intent intent = new Intent(this, BilingualHoursReaderActivity.class);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_SPANISH_VOLUME, entry.volume.id);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_SPANISH_PATH, entry.filePath);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_SPANISH_FRAGMENT, entry.fragment);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_SPANISH_SCROLL, entry.scrollText);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_TITLE, entry.title);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_LATIN_YEAR, latinDay.year);
        intent.putExtra(BilingualHoursReaderActivity.EXTRA_LATIN_PATH, latinPath);

        boolean sundayFirstVespers = "vespers".equals(entry.key)
                && selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
        Calendar reference = (Calendar) selectedDate.clone();
        if (sundayFirstVespers) reference.add(Calendar.DATE, 1);
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(reference);
        if (ordinaryWeek > 0) {
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_ORDINARY_WEEK, ordinaryWeek);
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_CYCLE,
                    LiturgicalResolver.lectionaryCycle(reference));
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_READINGS_YEAR,
                    reference.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
        }
        if (selectedProper != null && !sundayFirstVespers
                && ("invitatory".equals(entry.key) || "office".equals(entry.key)
                || "lauds".equals(entry.key) || "vespers".equals(entry.key)
                || selectedProper.isFeastOrSolemnity()
                && ("terce".equals(entry.key) || "sext".equals(entry.key)
                || "none".equals(entry.key)))) {
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_VOLUME,
                    selectedProper.volume.id);
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_INDEX,
                    selectedProper.tocIndex);
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_TITLE,
                    selectedProper.title);
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_RANK,
                    selectedProper.liturgicalRank);
            intent.putExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_HOUR, entry.key);
            if (selectedCommon != null) {
                intent.putExtra(BilingualHoursReaderActivity.EXTRA_COMMON_FILE,
                        selectedCommon.filePath);
                intent.putExtra(BilingualHoursReaderActivity.EXTRA_COMMON_FRAGMENT,
                        selectedCommon.fragment);
                intent.putExtra(BilingualHoursReaderActivity.EXTRA_COMMON_TITLE,
                        selectedCommon.title);
            }
        }
        startActivity(intent);
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.clear();
            selectedDate.set(year, month, day, 12, 0, 0);
            loadDate();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void moveDate(int amount) { selectedDate.add(Calendar.DATE, amount); loadDate(); }
    private static String longDate(Calendar date) {
        String value = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(date.getTime());
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
    private static String key(Calendar date) {
        return String.format(Locale.US, "%04d%02d%02d", date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
