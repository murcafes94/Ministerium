package com.fabri.ministerium;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Misal 3.1. La UI ya no navega por el antiguo Misal EPUB: cada entrada abre
 * contenido generado desde los PDF de Liturgia Papal México/latín.
 */
public class MissalActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "missal_year";
    public static final String EXTRA_MONTH = "missal_month";
    public static final String EXTRA_DAY = "missal_day";
    public static final String EXTRA_OPEN_PART = "missal_open_part";
    public static final String EXTRA_RETURN_TO_CALLER = "missal_return_to_caller";
    public static final String EXTRA_LANGUAGE = "missal_language";

    private final List<Item> items = Arrays.asList(
            new Item("Ordinario de la Misa", "Ritos iniciales y Liturgia de la Palabra", "initial"),
            new Item("Oración colecta", "Propia de la celebración del día", "collect"),
            new Item("Liturgia de la Palabra", "Ordinario y lecturas del Leccionario", "word"),
            new Item("Liturgia eucarística", "Dones, prefacio y plegaria eucarística", "eucharist"),
            new Item("Oración sobre las ofrendas", "Propia de la celebración del día", "offerings"),
            new Item("Prefacios", "PDF de Prefacios de Liturgia Papal", "prefaces"),
            new Item("Plegarias eucarísticas", "Plegarias I–IV", "prayers"),
            new Item("Rito de la comunión", "Padrenuestro, paz, fracción y comunión", "communion"),
            new Item("Antífona de comunión", "Propia de la celebración del día", "communion_antiphon"),
            new Item("Oración después de la comunión", "Propia de la celebración del día", "post_communion"),
            new Item("Rito de conclusión", "Bendición y despedida", "conclusion"),
            new Item("Comunes", "Formularios comunes de santos y santas", "commons"),
            new Item("Por diversas necesidades", "Iglesia, sociedad y necesidades particulares", "needs"),
            new Item("Misas votivas", "Formularios votivos", "votive"),
            new Item("Misas de difuntos", "Exequias, aniversarios y otras ocasiones", "dead"),
            new Item("Propio de los santos", "Celebraciones del santoral", "saints")
    );

    private Calendar selectedDate;
    private LiturgicalDay liturgicalDay;
    private TextView dateLabel;
    private TextView celebration;
    private TextView details;
    private Spinner modeSpinner;
    private Spinner languageSpinner;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missal);

        Calendar now = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        selectedDate.clear();
        selectedDate.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);

        dateLabel = findViewById(R.id.txtMissalDate);
        celebration = findViewById(R.id.txtMissalCelebration);
        details = findViewById(R.id.txtMissalDetails);
        modeSpinner = findViewById(R.id.spinnerMassMode);
        languageSpinner = findViewById(R.id.spinnerMassLanguage);
        configureSelectors();

        findViewById(R.id.btnBack).setOnClickListener(v -> exitToHome());
        findViewById(R.id.btnPreviousDay).setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> moveDate(1));
        findViewById(R.id.btnChooseDate).setOnClickListener(v -> chooseDate());
        dateLabel.setOnClickListener(v -> chooseDate());
        findViewById(R.id.btnDailyMissal).setOnClickListener(v -> openDay());
        findViewById(R.id.btnMissalReadings).setOnClickListener(v -> openReadings());

        List<Map<String, String>> rows = new ArrayList<>();
        for (Item item : items) rows.add(Rows.row(item.title, item.subtitle));
        ListView list = findViewById(R.id.listMissalSections);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> openSection(items.get(position).section));

        showDate();
        String requestedPart = getIntent().getStringExtra(EXTRA_OPEN_PART);
        if (requestedPart != null && !requestedPart.isEmpty()) {
            list.post(() -> openRequestedPart(requestedPart));
        }
    }

    private void configureSelectors() {
        String[] modes = {"Misa", "Misa + Laudes", "Misa + Vísperas"};
        String[] languages = {"Español", "Latín–Español"};
        modeSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, modes));
        languageSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages));
        languageSpinner.setSelection("lat_es".equals(
                getIntent().getStringExtra(EXTRA_LANGUAGE)) ? 1 : 0);
    }

    private void showDate() {
        String label = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(selectedDate.getTime());
        dateLabel.setText(label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1));
        try {
            liturgicalDay = LiturgicalResolver.resolve(this, selectedDate);
            celebration.setText(liturgicalDay.celebration);
            String detail = liturgicalDay.liturgicalColor;
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
            if (ordinaryWeek > 0) {
                if (!detail.isEmpty()) detail += " · ";
                detail += "Semana " + roman(ordinaryWeek) + " del Tiempo Ordinario";
            }
            details.setText((detail.isEmpty() ? "" : detail + "\n")
                    + "Misal: Liturgia Papal · versión de México");
        } catch (Exception error) {
            liturgicalDay = null;
            celebration.setText("Celebración del día");
            details.setText("Misal Liturgia Papal disponible sin conexión tras generar el paquete.");
        }
    }

    private void openDay() {
        if (modeSpinner.getSelectedItemPosition() > 0) {
            Intent intent = new Intent(this, CombinedMassActivity.class);
            putDate(intent);
            intent.putExtra(CombinedMassActivity.EXTRA_HOUR,
                    modeSpinner.getSelectedItemPosition() == 1 ? "lauds" : "vespers");
            intent.putExtra(CombinedMassActivity.EXTRA_LANGUAGE, selectedLanguage());
            startActivity(intent);
            return;
        }
        openSection("day");
    }

    private void openSection(String section) {
        Intent intent = new Intent(this, MissalSectionReaderActivity.class);
        intent.putExtra(MissalSectionReaderActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(MissalSectionReaderActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(MissalSectionReaderActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
        intent.putExtra(MissalSectionReaderActivity.EXTRA_SECTION, section);
        intent.putExtra(MissalSectionReaderActivity.EXTRA_LANGUAGE, selectedLanguage());
        startActivity(intent);
    }

    private void putDate(Intent intent) {
        intent.putExtra(CombinedMassActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(CombinedMassActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(CombinedMassActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
    }

    private void openReadings() {
        Intent intent = new Intent(this, MassReadingsActivity.class);
        intent.putExtra(MassReadingsActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
        intent.putExtra(MassReadingsActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
        intent.putExtra(MassReadingsActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
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

    private void exitToHome() {
        if (getIntent().getBooleanExtra(EXTRA_RETURN_TO_CALLER, false)) {
            finish();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override public void onBackPressed() { exitToHome(); }

    private String selectedLanguage() {
        return languageSpinner != null && languageSpinner.getSelectedItemPosition() == 1
                ? "lat_es" : "es";
    }

    private void openRequestedPart(String part) {
        if ("collect".equals(part)) openSection("collect");
        else if ("after_communion".equals(part)) openSection("post_communion");
        else if ("offerings".equals(part)) openSection("offerings");
        else openSection("day");
    }

    private static String roman(int value) {
        int[] numbers = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = value;
        for (int i = 0; i < numbers.length; i++) {
            while (remaining >= numbers[i]) {
                result.append(symbols[i]);
                remaining -= numbers[i];
            }
        }
        return result.toString();
    }

    private static final class Item {
        final String title;
        final String subtitle;
        final String section;
        Item(String title, String subtitle, String section) {
            this.title = title;
            this.subtitle = subtitle;
            this.section = section;
        }
    }
}
