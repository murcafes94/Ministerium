package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HoursTodayActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_HOUR_KEY = "hour_key";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, HourEntry> hours = new HashMap<>();
    private Calendar selectedDate;
    private LiturgicalDay currentDay;
    private HoursLink selectedProper;
    private List<CommonOfficeChoice> commonChoices = Collections.emptyList();
    private CommonOfficeChoice selectedCommon;
    private ProgressBar progress;
    private View content;
    private String requestedHourKey = "";
    private boolean requestedHourConsumed;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_today);
        progress = findViewById(R.id.dailyProgress);
        content = findViewById(R.id.dailyContent);
        Calendar now = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        selectedDate.clear();
        selectedDate.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        requestedHourKey = getIntent().getStringExtra(EXTRA_HOUR_KEY);
        if (requestedHourKey == null) requestedHourKey = "";
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCalendar).setOnClickListener(v -> chooseDate());
        findViewById(R.id.txtDailyDate).setOnClickListener(v -> chooseDate());
        findViewById(R.id.btnPreviousDay).setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> moveDate(1));
        findViewById(R.id.btnChooseOffice).setOnClickListener(v -> chooseOffice());
        findViewById(R.id.btnCombineOfficeLauds).setOnClickListener(v -> openCombined());
        findViewById(R.id.cardProper).setOnClickListener(v -> openLink(selectedProper));
        findViewById(R.id.btnIntentions).setOnClickListener(v -> startActivity(new Intent(this, PrayerIntentionsActivity.class)));
        findViewById(R.id.btnCommonOffices).setOnClickListener(v -> openNamedSantoral("OFICIOS COMUNES"));
        findViewById(R.id.btnOfficeDead).setOnClickListener(v -> openNamedSantoral("OFICIO DE DIFUNTOS"));
        findViewById(R.id.btnVolumes).setOnClickListener(v -> startActivity(new Intent(this, HoursActivity.class)));
        loadDate();
    }

    private void loadDate() {
        final Calendar request = (Calendar) selectedDate.clone();
        final String requestKey = key(request);
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.INVISIBLE);
        executor.submit(() -> {
            try {
                LiturgicalDay day = LiturgicalResolver.resolve(getApplicationContext(), request);
                List<HourEntry> entries = DailyHoursRepository.hoursFor(getApplicationContext(), day.temporalOffice, request);
                runOnUiThread(() -> { if (requestKey.equals(key(selectedDate))) showDay(day, entries); });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!requestKey.equals(key(selectedDate))) return;
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "No se pudo localizar la Liturgia de las Horas para esta fecha.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showDay(LiturgicalDay day, List<HourEntry> entries) {
        currentDay = day;
        selectedProper = null;
        selectedCommon = null;
        commonChoices = Collections.emptyList();
        hours.clear();
        for (HourEntry entry : entries) hours.put(entry.key, entry);
        for (HoursLink saint : day.saintOffices) {
            if (saint.requiresProperOffice()) {
                selectedProper = saint;
                loadCommonChoices(false);
                break;
            }
        }
        ((TextView) findViewById(R.id.txtDailyDate)).setText(longDate(selectedDate));
        ((TextView) findViewById(R.id.txtDailySource)).setText(day.sourceNote + " · contenido limpio disponible sin conexión");
        bind(R.id.cardInvitatory, "invitatory");
        bind(R.id.cardOfficeReadings, "office");
        bind(R.id.cardLauds, "lauds");
        bind(R.id.cardTerce, "terce");
        bind(R.id.cardSext, "sext");
        bind(R.id.cardNone, "none");
        bind(R.id.cardVespers, "vespers");
        bind(R.id.cardCompline, "compline");
        updateOfficeHeader();
        Button chooser = findViewById(R.id.btnChooseOffice);
        chooser.setEnabled(!day.saintOffices.isEmpty());
        chooser.setAlpha(day.saintOffices.isEmpty() ? 0.65f : 1f);
        chooser.setText(day.saintOffices.isEmpty() ? "Oficio del día"
                : selectedProper != null && selectedProper.requiresProperOffice()
                ? selectedProper.subtitle + " · cambiar formulario" : "Elegir feria o memoria libre");
        View combined = findViewById(R.id.btnCombineOfficeLauds);
        boolean canCombine = hours.containsKey("office") && hours.containsKey("lauds");
        combined.setEnabled(canCombine);
        combined.setAlpha(canCombine ? 1f : .45f);
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        if (!requestedHourConsumed && !requestedHourKey.isEmpty()) {
            HourEntry requested = hours.get(requestedHourKey);
            if (requested != null) {
                requestedHourConsumed = true;
                content.post(() -> openHour(requested));
            }
        }
    }

    private void bind(int viewId, String key) {
        View card = findViewById(viewId);
        HourEntry entry = hours.get(key);
        if (viewId == R.id.cardVespers && entry != null) {
            ((TextView) findViewById(R.id.txtVespersTitle)).setText(entry.title);
            ((TextView) findViewById(R.id.txtVespersSubtitle)).setText(entry.subtitle);
        } else if (viewId == R.id.cardCompline && entry != null) {
            ((TextView) findViewById(R.id.txtComplineSubtitle)).setText(entry.subtitle);
        }
        card.setEnabled(entry != null);
        card.setAlpha(entry == null ? 0.42f : 1f);
        card.setOnClickListener(entry == null ? null : v -> openHour(entry));
    }

    private void updateOfficeHeader() {
        if (currentDay == null) return;
        String celebration = selectedProper == null ? currentDay.celebration : selectedProper.title;
        String color = selectedProper != null && !selectedProper.liturgicalColor.isEmpty()
                ? selectedProper.liturgicalColor : currentDay.liturgicalColor;
        ((TextView) findViewById(R.id.txtDailyCelebration)).setText(celebration);
        List<String> details = new ArrayList<>();
        if (!color.isEmpty()) details.add("Color " + color.toLowerCase(Locale.ROOT));
        String season = currentDay.temporalOffice == null || currentDay.temporalOffice.volume == null
                ? "" : currentDay.temporalOffice.volume.id;
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
        if ("ordinary".equals(season) && ordinaryWeek > 0) {
            details.add("Semana " + roman(ordinaryWeek) + " del Tiempo Ordinario");
        }
        if (!currentDay.psalterWeek.isEmpty()) details.add("Salterio " + currentDay.psalterWeek);
        details.add("Tomo " + ComplineContentRepository.liturgicalVolume(season, ordinaryWeek));
        if (selectedProper != null) details.add(selectedProper.subtitle);
        ((TextView) findViewById(R.id.txtDailyDetails)).setText(join(details));
        findViewById(R.id.liturgicalColorDot).setBackgroundTintList(ColorStateList.valueOf(colorValue(color)));
        View properCard = findViewById(R.id.cardProper);
        if (selectedProper == null) {
            properCard.setVisibility(View.GONE);
        } else {
            properCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.txtProperTitle)).setText("Propio de " + selectedProper.title);
            ((TextView) findViewById(R.id.txtProperSubtitle)).setText(selectedProper.isFeastOrSolemnity()
                    ? "Oficio propio completo; cuando falta un elemento se toma del común correspondiente"
                    : selectedCommon == null
                    ? "Propio de la celebración · para las memorias se conserva la salmodia del salterio, salvo norma explícita"
                    : "Invitatorio, himno y elementos del propio o del común · salmodia del salterio en las memorias");
        }
    }

    private void chooseOffice() {
        if (currentDay == null || currentDay.saintOffices.isEmpty()) return;
        if (selectedProper != null && selectedProper.requiresProperOffice()) { loadCommonChoices(true); return; }
        List<String> labels = new ArrayList<>();
        labels.add("Oficio de la feria · " + currentDay.celebration);
        for (HoursLink saint : currentDay.saintOffices) labels.add(saint.subtitle + " · " + saint.title);
        int checked = selectedProper == null ? 0 : currentDay.saintOffices.indexOf(selectedProper) + 1;
        new AlertDialog.Builder(this).setTitle("Elegir oficio")
                .setSingleChoiceItems(labels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedProper = which == 0 ? null : currentDay.saintOffices.get(which - 1);
                    dialog.dismiss();
                    if (selectedProper == null) {
                        selectedCommon = null;
                        commonChoices = Collections.emptyList();
                        updateOfficeHeader();
                    } else loadCommonChoices(true);
                }).setNegativeButton("Cancelar", null).show();
    }

    private void loadCommonChoices(boolean showChooser) {
        try { commonChoices = SaintOfficeRepository.commonChoices(this, selectedProper); }
        catch (Exception error) { commonChoices = Collections.emptyList(); }
        selectedCommon = commonChoices.isEmpty() ? null : commonChoices.get(0);
        if (!showChooser || commonChoices.size() <= 1) { updateOfficeHeader(); return; }
        String[] labels = new String[commonChoices.size()];
        for (int i = 0; i < commonChoices.size(); i++) labels[i] = commonChoices.get(i).title;
        new AlertDialog.Builder(this).setTitle("Elegir formulario de la celebración")
                .setSingleChoiceItems(labels, 0, (dialog, which) -> {
                    selectedCommon = commonChoices.get(which); updateOfficeHeader(); dialog.dismiss();
                }).setNegativeButton("Usar el primero", (dialog, which) -> updateOfficeHeader())
                .setOnCancelListener(dialog -> updateOfficeHeader()).show();
    }

    private void openHour(HourEntry entry) {
        if ("compline".equals(entry.key)) {
            Intent compline = new Intent(this, ComplineReaderActivity.class);
            compline.putExtra(ComplineReaderActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
            compline.putExtra(ComplineReaderActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
            compline.putExtra(ComplineReaderActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
            startActivity(compline); return;
        }
        Intent intent = new Intent(this, HoursReaderActivity.class);
        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, entry.volume.id);
        intent.putExtra(HoursReaderActivity.EXTRA_FILE_PATH, entry.filePath);
        intent.putExtra(HoursReaderActivity.EXTRA_FRAGMENT, entry.fragment);
        intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, entry.title);
        intent.putExtra(HoursReaderActivity.EXTRA_SCROLL_TEXT, entry.scrollText);
        intent.putExtra(HoursReaderActivity.EXTRA_SHOW_INTENTIONS, entry.showIntentions);
        intent.putExtra(HoursReaderActivity.EXTRA_EASTER_SEASON, currentDay != null && currentDay.temporalOffice != null && "easter".equals(currentDay.temporalOffice.volume.id));
        intent.putExtra(HoursReaderActivity.EXTRA_SUNDAY_OR_SOLEMNITY,
                selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || selectedProper != null && selectedProper.isFeastOrSolemnity());
        boolean sundayFirstVespers = "vespers".equals(entry.key) && selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
        Calendar referenceDate = (Calendar) selectedDate.clone();
        if (sundayFirstVespers) referenceDate.add(Calendar.DATE, 1);
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(referenceDate);
        if (ordinaryWeek > 0) {
            intent.putExtra(HoursReaderActivity.EXTRA_ORDINARY_WEEK, ordinaryWeek);
            intent.putExtra(HoursReaderActivity.EXTRA_LECTIONARY_CYCLE, LiturgicalResolver.lectionaryCycle(referenceDate));
            intent.putExtra(HoursReaderActivity.EXTRA_READINGS_YEAR, referenceDate.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
        }
        if (selectedProper != null && !sundayFirstVespers
                && ("invitatory".equals(entry.key) || "office".equals(entry.key) || "lauds".equals(entry.key)
                || "vespers".equals(entry.key) || selectedProper.isFeastOrSolemnity()
                && ("terce".equals(entry.key) || "sext".equals(entry.key) || "none".equals(entry.key)))) {
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_VOLUME_ID, selectedProper.volume.id);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_TOC_INDEX, selectedProper.tocIndex);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_TITLE, selectedProper.title);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_RANK, selectedProper.liturgicalRank);
            intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_HOUR_KEY, entry.key);
            if (selectedCommon != null) {
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_FILE, selectedCommon.filePath);
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_FRAGMENT, selectedCommon.fragment);
                intent.putExtra(HoursReaderActivity.EXTRA_MEMORY_COMMON_TITLE, selectedCommon.title);
            }
        }
        startActivity(intent);
    }

    private void openCombined() {
        Intent intent = new Intent(this, CombinedHoursActivity.class);
        intent.putExtra(CombinedHoursActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(CombinedHoursActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(CombinedHoursActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        if (selectedProper != null) {
            intent.putExtra(CombinedHoursActivity.EXTRA_SAINT_VOLUME, selectedProper.volume.id);
            intent.putExtra(CombinedHoursActivity.EXTRA_SAINT_INDEX, selectedProper.tocIndex);
            intent.putExtra(CombinedHoursActivity.EXTRA_SAINT_TITLE, selectedProper.title);
            intent.putExtra(CombinedHoursActivity.EXTRA_SAINT_RANK, selectedProper.liturgicalRank);
        }
        if (selectedCommon != null) {
            intent.putExtra(CombinedHoursActivity.EXTRA_COMMON_FILE, selectedCommon.filePath);
            intent.putExtra(CombinedHoursActivity.EXTRA_COMMON_FRAGMENT, selectedCommon.fragment);
            intent.putExtra(CombinedHoursActivity.EXTRA_COMMON_TITLE, selectedCommon.title);
        }
        startActivity(intent);
    }

    private void openLink(HoursLink link) {
        if (link == null) return;
        Intent intent = new Intent(this, HoursReaderActivity.class);
        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, link.volume.id);
        intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, link.tocIndex);
        intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, link.title);
        startActivity(intent);
    }

    private void openNamedSantoral(String title) {
        try {
            HoursVolume santoral = HoursRepository.find("sanctoral");
            int index = EpubUtils.findEntryIndex(this, santoral, title);
            if (index < 0) throw new IllegalStateException();
            startActivity(new Intent(this, HoursReaderActivity.class)
                    .putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, santoral.id)
                    .putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, index));
        } catch (Exception error) {
            Toast.makeText(this, "No se encontró este formulario.", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.set(year, month, day, 12, 0, 0); loadDate();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void moveDate(int amount) { selectedDate.add(Calendar.DATE, amount); loadDate(); }

    private static String roman(int value) {
        int[] numbers = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = Math.max(0, value);
        for (int i = 0; i < numbers.length; i++) while (remaining >= numbers[i]) {
            result.append(symbols[i]); remaining -= numbers[i];
        }
        return result.toString();
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) { if (result.length() > 0) result.append(" · "); result.append(value); }
        return result.toString();
    }

    private static String longDate(Calendar date) {
        return new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "EC")).format(date.getTime());
    }

    private static String key(Calendar date) {
        return date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH) + "-" + date.get(Calendar.DAY_OF_MONTH);
    }

    private static int colorValue(String color) {
        String value = color == null ? "" : color.toLowerCase(Locale.ROOT);
        if (value.contains("blanco")) return Color.parseColor("#E6D9A2");
        if (value.contains("rojo")) return Color.parseColor("#9B1C2D");
        if (value.contains("morado") || value.contains("violeta")) return Color.parseColor("#6D4776");
        if (value.contains("rosa")) return Color.parseColor("#B8647B");
        return Color.parseColor("#3C7A4A");
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
