package com.fabri.ministerium;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Ministerium 5 hub for Liturgia de las Horas. */
public class HoursV5Activity extends ThemedActivity {
    public static final String EXTRA_YEAR = "v5_hours_year";
    public static final String EXTRA_MONTH = "v5_hours_month";
    public static final String EXTRA_DAY = "v5_hours_day";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, HourEntry> hours = new HashMap<>();
    private Calendar selectedDate;
    private LiturgicalDay currentDay;
    private HoursOfficeSelection officeSelection;
    private HoursOfficeOption selectedOffice;
    private TextView dateView;
    private TextView celebrationView;
    private TextView detailsView;
    private TextView sourceView;
    private TextView officeChoice;
    private LinearLayout hourList;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        selectedDate = selectedDate();
        setContentView(buildScreen());
        loadDate();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));

        LinearLayout root = column();
        root.setPadding(dp(20), dp(18), dp(20), dp(30));
        scroll.addView(root);

        TextView back = text("‹  Liturgia de las Horas", 24, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(12));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = action("‹");
        previous.setOnClickListener(v -> moveDay(-1));
        dateRow.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(52)));
        dateView = text("", 18, R.color.ink, true);
        dateView.setGravity(Gravity.CENTER);
        dateView.setOnClickListener(v -> chooseDate());
        dateRow.addView(dateView, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView next = action("›");
        next.setOnClickListener(v -> moveDay(1));
        dateRow.addView(next, new LinearLayout.LayoutParams(dp(48), dp(52)));
        root.addView(dateRow);

        celebrationView = text("", 25, R.color.wine, true);
        celebrationView.setPadding(0, dp(16), 0, dp(5));
        root.addView(celebrationView);

        detailsView = text("", 14, R.color.muted, false);
        root.addView(detailsView);

        sourceView = text("", 12, R.color.muted, false);
        sourceView.setPadding(0, dp(4), 0, dp(10));
        root.addView(sourceView);

        officeChoice = text("Elegir oficio", 15, R.color.wine, true);
        officeChoice.setGravity(Gravity.CENTER);
        officeChoice.setPadding(dp(12), dp(12), dp(12), dp(12));
        officeChoice.setBackgroundResource(R.drawable.bg_button_secondary);
        officeChoice.setOnClickListener(v -> chooseOffice());
        LinearLayout.LayoutParams choiceLp = new LinearLayout.LayoutParams(-1, -2);
        choiceLp.setMargins(0, 0, 0, dp(14));
        root.addView(officeChoice, choiceLp);

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        progressLp.gravity = Gravity.CENTER_HORIZONTAL;
        progressLp.setMargins(0, dp(8), 0, dp(14));
        root.addView(progress, progressLp);

        hourList = column();
        root.addView(hourList);

        TextView readings = text("Lecturas de la Misa", 16, R.color.wine, true);
        readings.setGravity(Gravity.CENTER);
        readings.setPadding(dp(14), dp(14), dp(14), dp(14));
        readings.setBackgroundResource(R.drawable.bg_button_secondary);
        readings.setOnClickListener(v -> openMassWord());
        LinearLayout.LayoutParams readingsLp = new LinearLayout.LayoutParams(-1, -2);
        readingsLp.setMargins(0, dp(8), 0, 0);
        root.addView(readings, readingsLp);
        return scroll;
    }

    private void loadDate() {
        final Calendar request = (Calendar) selectedDate.clone();
        final String requestKey = key(request);
        renderDate();
        progress.setVisibility(View.VISIBLE);
        hourList.removeAllViews();
        officeChoice.setEnabled(false);
        officeChoice.setAlpha(.55f);
        executor.submit(() -> {
            try {
                LiturgicalDay day = LiturgicalResolver.resolve(getApplicationContext(), request);
                HoursOfficeSelection resolved = HoursV5OfficePolicy.resolve(day);
                HoursOfficeOption chosen = resolved.getDefaultOption();
                List<HourEntry> entries = chosen == null
                        ? java.util.Collections.emptyList()
                        : DailyHoursRepository.hoursFor(getApplicationContext(), chosen.getOffice(), request);
                runOnUiThread(() -> {
                    if (!requestKey.equals(key(selectedDate)) || isFinishing()) return;
                    currentDay = day;
                    officeSelection = resolved;
                    selectedOffice = chosen;
                    applyEntries(entries);
                    renderResolvedDay();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showResolveError(requestKey));
            }
        });
    }

    private void loadSelectedOffice(HoursOfficeOption option) {
        if (option == null || currentDay == null) return;
        final String requestKey = key(selectedDate);
        final Calendar request = (Calendar) selectedDate.clone();
        progress.setVisibility(View.VISIBLE);
        hourList.removeAllViews();
        executor.submit(() -> {
            try {
                List<HourEntry> entries = DailyHoursRepository.hoursFor(
                        getApplicationContext(), option.getOffice(), request);
                runOnUiThread(() -> {
                    if (!requestKey.equals(key(selectedDate)) || isFinishing()) return;
                    selectedOffice = option;
                    applyEntries(entries);
                    renderResolvedDay();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!requestKey.equals(key(selectedDate)) || isFinishing()) return;
                    selectedOffice = option;
                    hours.clear();
                    progress.setVisibility(View.GONE);
                    sourceView.setText("El formulario elegido no tiene contenido local verificable. No se sustituirá con otro oficio.");
                    renderHours();
                    updateOfficeChooser();
                });
            }
        });
    }

    private void applyEntries(List<HourEntry> entries) {
        hours.clear();
        for (HourEntry entry : entries) hours.put(entry.key, entry);
    }

    private void showResolveError(String requestKey) {
        if (!requestKey.equals(key(selectedDate)) || isFinishing()) return;
        currentDay = null;
        officeSelection = null;
        selectedOffice = null;
        hours.clear();
        celebrationView.setText("Oficio del día");
        detailsView.setText("No se pudo resolver el oficio para esta fecha.");
        sourceView.setText("Ministerium no mezclará contenido de otra celebración como sustitución.");
        progress.setVisibility(View.GONE);
        updateOfficeChooser();
        renderHours();
    }

    private void renderDate() {
        String date = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "EC"))
                .format(selectedDate.getTime());
        dateView.setText(date.substring(0, 1).toUpperCase(Locale.ROOT) + date.substring(1));
    }

    private void renderResolvedDay() {
        String celebration = selectedOffice == null ? currentDay.celebration : selectedOffice.getTitle();
        celebrationView.setText(celebration == null || celebration.trim().isEmpty() ? "Oficio del día" : celebration);

        StringBuilder details = new StringBuilder();
        String liturgicalColor = selectedOffice != null && selectedOffice.getOffice().liturgicalColor != null
                && !selectedOffice.getOffice().liturgicalColor.isEmpty()
                ? selectedOffice.getOffice().liturgicalColor : currentDay.liturgicalColor;
        if (liturgicalColor != null && !liturgicalColor.isEmpty()) {
            details.append("Color ").append(liturgicalColor.toLowerCase(Locale.ROOT));
        }
        int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
        if (ordinaryWeek > 0) {
            if (details.length() > 0) details.append(" · ");
            details.append("Semana ").append(ordinaryWeek).append(" del Tiempo Ordinario");
        }
        if (currentDay.psalterWeek != null && !currentDay.psalterWeek.isEmpty()) {
            if (details.length() > 0) details.append(" · ");
            details.append("Salterio ").append(currentDay.psalterWeek);
        }
        detailsView.setText(details.length() == 0 ? "Liturgia de las Horas" : details.toString());

        String source = officeSelection == null ? "Fuente aislada" : officeSelection.getReason();
        if (selectedOffice != null) {
            source += "\n" + (selectedOffice.getSource() == HoursOfficeSource.PROPER
                    ? "Fuente activa: propio del santoral" : "Fuente activa: temporal");
        }
        sourceView.setText(source);
        progress.setVisibility(View.GONE);
        updateOfficeChooser();
        renderHours();
    }

    private void updateOfficeChooser() {
        int count = officeSelection == null ? 0 : officeSelection.getOptions().size();
        officeChoice.setEnabled(count > 1);
        officeChoice.setAlpha(count > 1 ? 1f : .55f);
        if (selectedOffice == null) officeChoice.setText("Oficio no disponible");
        else if (count > 1) officeChoice.setText("Oficio: " + selectedOffice.getTitle() + " · cambiar");
        else officeChoice.setText("Oficio: " + selectedOffice.getTitle());
    }

    private void chooseOffice() {
        if (officeSelection == null || officeSelection.getOptions().size() <= 1) return;
        List<HoursOfficeOption> options = officeSelection.getOptions();
        String[] labels = new String[options.size()];
        int checked = 0;
        for (int i = 0; i < options.size(); i++) {
            HoursOfficeOption option = options.get(i);
            labels[i] = option.getTitle() + "\n" + option.getSubtitle();
            if (selectedOffice != null && option.getOffice() == selectedOffice.getOffice()) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Elegir oficio")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    HoursOfficeOption chosen = options.get(which);
                    dialog.dismiss();
                    loadSelectedOffice(chosen);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void renderHours() {
        hourList.removeAllViews();
        for (HoursV5Item item : HoursV5Semantic.items()) {
            HourEntry entry = hours.get(item.getKey());
            LinearLayout card = column();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackgroundResource(R.drawable.bg_button_secondary);
            card.addView(text(item.getTitle(), 18, entry == null ? R.color.muted : R.color.ink, true));
            String subtitle = entry == null ? "No disponible en el formulario seleccionado" :
                    (entry.subtitle == null || entry.subtitle.trim().isEmpty() ? item.getSummary() : entry.subtitle);
            TextView sub = text(subtitle, 13, R.color.muted, false);
            sub.setPadding(0, dp(4), 0, 0);
            card.addView(sub);
            card.setEnabled(entry != null);
            card.setAlpha(entry == null ? .55f : 1f);
            if (entry != null) card.setOnClickListener(v -> openHour(entry));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, dp(9));
            hourList.addView(card, lp);
        }
    }

    private void openHour(HourEntry entry) {
        if (entry == null) return;
        if ("compline".equals(entry.key)) {
            Intent intent = new Intent(this, ComplineReaderActivity.class);
            intent.putExtra(ComplineReaderActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
            intent.putExtra(ComplineReaderActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
            intent.putExtra(ComplineReaderActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, HoursV5ReaderActivity.class);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_VOLUME_ID, entry.volume.id);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_FILE_PATH, entry.filePath);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_FRAGMENT, entry.fragment);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_TITLE, entry.title);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_SUBTITLE, entry.subtitle);
        intent.putExtra(HoursV5ReaderActivity.EXTRA_SCROLL_TEXT, entry.scrollText);
        startActivity(intent);
    }

    private void openMassWord() {
        Intent intent = new Intent(this, MissalV5SectionActivity.class);
        intent.putExtra(MissalV5SectionActivity.EXTRA_SECTION, "word");
        intent.putExtra(MissalV5SectionActivity.EXTRA_TITLE, "Liturgia de la Palabra");
        intent.putExtra(MissalV5SectionActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(MissalV5SectionActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(MissalV5SectionActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        intent.putExtra(MissalV5SectionActivity.EXTRA_CELEBRATION,
                selectedOffice == null ? (currentDay == null ? "Celebración del día" : currentDay.celebration)
                        : selectedOffice.getTitle());
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

    private void moveDay(int amount) {
        selectedDate.add(Calendar.DATE, amount);
        loadDate();
    }

    private Calendar selectedDate() {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        return c;
    }

    private String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return l;
    }

    private TextView action(String value) {
        TextView v = text(value, 30, R.color.wine, false);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView text(String value, int sp, int colorRes, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color(colorRes));
        if (bold) v.setTypeface(v.getTypeface(), Typeface.BOLD);
        return v;
    }

    @SuppressWarnings("deprecation")
    private int color(int res) { return getResources().getColor(res); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
