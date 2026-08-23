package com.fabri.ministerium;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MissalActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "missal_year";
    public static final String EXTRA_MONTH = "missal_month";
    public static final String EXTRA_DAY = "missal_day";
    public static final String EXTRA_OPEN_PART = "missal_open_part";
    public static final String EXTRA_RETURN_TO_CALLER = "missal_return_to_caller";
    public static final String EXTRA_LANGUAGE = "missal_language";

    private final List<Item> items = Arrays.asList(
            new Item("Ordinario de la Misa", "Ritos iniciales y Liturgia de la Palabra", "Inicio", null),
            new Item("Oración colecta", "Propia de la celebración del día", "Colecta", MissalProperRepository.Part.COLLECT),
            new Item("Liturgia eucarística", "Preparación de los dones y oración eucarística", "L.Eucarística", null),
            new Item("Oración sobre las ofrendas", "Propia de la celebración del día", "Or.Ofrendas", MissalProperRepository.Part.OFFERINGS),
            new Item("Prefacios", "Prefacios propios y comunes", "Prefacio", null),
            new Item("Plegarias eucarísticas", "Textos completos del Ordinario", "Plegarias Eucarísticas", null),
            new Item("Rito de la comunión", "Padrenuestro, paz, fracción y comunión", "RitoComunión", null),
            new Item("Antífona de comunión", "Propia de la celebración del día", "Ant.Comunión", MissalProperRepository.Part.COMMUNION_ANTIPHON),
            new Item("Oración después de la comunión", "Propia de la celebración del día", "Or.DespuésCom", MissalProperRepository.Part.AFTER_COMMUNION),
            new Item("Rito de conclusión", "Bendición y despedida", "RitoConclusión", null),
            new Item("Comunes", "Formularios comunes de santos y santas", "Misas comunes", null),
            new Item("Por diversas necesidades", "Iglesia, sociedad y necesidades particulares", "Misas por diversas necesidades", null),
            new Item("Misas votivas", "Formularios votivos", "Misas votivas", null),
            new Item("Misas de difuntos", "Exequias, aniversarios y otras ocasiones", "Misas de difuntos", null),
            new Item("Propio de los santos", "Celebraciones del santoral", "Propio de los santos", null)
    );

    private Calendar selectedDate;
    private LiturgicalDay liturgicalDay;
    private List<EpubTocEntry> toc;
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
        try {
            toc = EpubUtils.tableOfContents(this, HoursRepository.ROMAN_MISSAL);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar el Misal local.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (Item item : items) rows.add(Rows.row(item.title, item.subtitle));
        ListView list = findViewById(R.id.listMissalSections);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Item item = items.get(position);
            if (item.properPart != null) openProper(item);
            else openTitle(item.tocTitle);
        });
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
            if (!liturgicalDay.psalterWeek.isEmpty()) {
                if (!detail.isEmpty()) detail += " · ";
                detail += "Semana " + liturgicalDay.psalterWeek + " del salterio";
            }
            details.setText(detail.isEmpty() ? liturgicalDay.sourceNote
                    : detail + "\n" + liturgicalDay.sourceNote);
        } catch (Exception error) {
            liturgicalDay = null;
            celebration.setText("Celebración del día");
            details.setText("Misal disponible sin conexión");
        }
    }

    private void openDay() {
        if (modeSpinner.getSelectedItemPosition() > 0) {
            Intent intent = new Intent(this, CombinedMassActivity.class);
            intent.putExtra(CombinedMassActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
            intent.putExtra(CombinedMassActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
            intent.putExtra(CombinedMassActivity.EXTRA_DAY,
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            intent.putExtra(CombinedMassActivity.EXTRA_HOUR,
                    modeSpinner.getSelectedItemPosition() == 1 ? "lauds" : "vespers");
            intent.putExtra(CombinedMassActivity.EXTRA_LANGUAGE, selectedLanguage());
            startActivity(intent);
            return;
        }
        if (openProperTarget(MissalProperRepository.Part.DAY, "Celebración del día")) return;
        int index = closestCelebration();
        if (index < 0) index = find(fallbackTitle());
        if (index < 0) index = find("Propios de la Misa");
        openIndex(index);
    }

    private int closestCelebration() {
        if (liturgicalDay == null) return -1;
        String wanted = normalize(liturgicalDay.celebration);
        if (wanted.isEmpty()) return -1;
        int best = -1;
        int bestScore = 0;
        for (int i = 0; i < toc.size(); i++) {
            String candidate = normalize(toc.get(i).title);
            if (candidate.isEmpty()) continue;
            if (candidate.contains(wanted) || wanted.contains(candidate)) return i;
            int score = 0;
            for (String token : wanted.split(" ")) {
                if (token.length() >= 5 && !commonToken(token) && candidate.contains(token)) score++;
            }
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return bestScore >= 2 ? best : -1;
    }

    private String fallbackTitle() {
        String season = liturgicalDay != null && liturgicalDay.temporalOffice != null
                && liturgicalDay.temporalOffice.volume != null
                ? liturgicalDay.temporalOffice.volume.id : "";
        if ("advent".equals(season)) return "Adviento";
        if ("christmas".equals(season)) return "Navidad";
        if ("lent".equals(season)) return "Cuaresma";
        if ("easter".equals(season)) return "Pascua";
        if (selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return "Domingos T. Ordinario";
        }
        return selectedDate.get(Calendar.YEAR) % 2 == 0
                ? "Tiempo Ordinario Año Par" : "Tiempo Ordinario Año Impar";
    }

    private void openTitle(String title) { openIndex(find(title)); }

    private void openProper(Item item) {
        if (openProperTarget(item.properPart, item.title)) return;
        int celebrationIndex = closestCelebration();
        if (celebrationIndex >= 0) {
            EpubTocEntry entry = toc.get(celebrationIndex);
            openDirect(entry.filePath, item.properPart.fallbackFragment, item.title);
            return;
        }
        openTitle(item.tocTitle);
    }

    private boolean openProperTarget(MissalProperRepository.Part part, String title) {
        if (liturgicalDay == null) return false;
        try {
            MissalProperRepository.Target target = MissalProperRepository.resolve(
                    this, selectedDate, liturgicalDay.celebration, part);
            if (target == null) return false;
            openDirect(target.filePath, target.fragment,
                    title + " · " + target.title);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void openDirect(String filePath, String fragment, String title) {
        Intent intent = new Intent(this, HoursReaderActivity.class);
        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, HoursRepository.ROMAN_MISSAL.id);
        intent.putExtra(HoursReaderActivity.EXTRA_FILE_PATH, filePath);
        intent.putExtra(HoursReaderActivity.EXTRA_FRAGMENT, fragment == null ? "" : fragment);
        intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, title);
        intent.putExtra(HoursReaderActivity.EXTRA_MISSAL_LANGUAGE, selectedLanguage());
        startActivity(intent);
    }

    private int find(String title) {
        String wanted = normalize(title);
        for (int i = 0; i < toc.size(); i++) {
            String candidate = normalize(toc.get(i).title);
            if (candidate.equals(wanted) || candidate.contains(wanted)
                    || wanted.contains(candidate)) return i;
        }
        return -1;
    }

    private void openIndex(int index) {
        if (index < 0 || index >= toc.size()) {
            Toast.makeText(this, "No se encontró esa sección en el Misal.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, HoursReaderActivity.class);
        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, HoursRepository.ROMAN_MISSAL.id);
        intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, index);
        intent.putExtra(HoursReaderActivity.EXTRA_MISSAL_LANGUAGE, selectedLanguage());
        startActivity(intent);
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
        if ("collect".equals(part)) {
            openProper(new Item("Oración colecta", "", "Colecta",
                    MissalProperRepository.Part.COLLECT));
        } else if ("after_communion".equals(part)) {
            openProper(new Item("Oración después de la comunión", "", "Or.DespuésCom",
                    MissalProperRepository.Part.AFTER_COMMUNION));
        } else if ("offerings".equals(part)) {
            openProper(new Item("Oración sobre las ofrendas", "", "Or.Ofrendas",
                    MissalProperRepository.Part.OFFERINGS));
        }
    }

    private static boolean commonToken(String value) {
        return "santo".equals(value) || "santa".equals(value)
                || "senor".equals(value) || "virgen".equals(value)
                || "memoria".equals(value) || "fiesta".equals(value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static final class Item {
        final String title;
        final String subtitle;
        final String tocTitle;
        final MissalProperRepository.Part properPart;
        Item(String title, String subtitle, String tocTitle,
             MissalProperRepository.Part properPart) {
            this.title = title;
            this.subtitle = subtitle;
            this.tocTitle = tocTitle;
            this.properPart = properPart;
        }
    }
}
