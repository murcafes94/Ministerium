package com.fabri.ministerium;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LatinHoursActivity extends ThemedActivity {
    private static final int REQUEST_IMPORT_EPUB = 731;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Integer> cards = new LinkedHashMap<>();
    private Calendar selectedDate;
    private LatinContentManager.LatinDay currentDay;
    private ProgressBar progress;
    private View content;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latin_hours);

        selectedDate = Calendar.getInstance();
        progress = findViewById(R.id.latinProgress);
        content = findViewById(R.id.latinContent);
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
        findViewById(R.id.btnUpdateLatin).setOnClickListener(v -> updateOnline());
        findViewById(R.id.btnImportLatin).setOnClickListener(v -> importEpub());
        findViewById(R.id.btnLatinSource).setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(LatinContentManager.DOWNLOAD_PAGE))));
        loadDate();
    }

    private void loadDate() {
        final Calendar request = (Calendar) selectedDate.clone();
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.INVISIBLE);
        executor.submit(() -> {
            try {
                LatinContentManager.LatinDay day = LatinContentManager.day(
                        getApplicationContext(), request);
                runOnUiThread(() -> showDay(day));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    currentDay = null;
                    progress.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    showUnavailable(request.get(Calendar.YEAR));
                });
            }
        });
    }

    private void showDay(LatinContentManager.LatinDay day) {
        currentDay = day;
        ((TextView) findViewById(R.id.txtLatinDate)).setText(day.dateTitle);
        ((TextView) findViewById(R.id.txtLatinCelebration)).setText(
                day.celebration.isEmpty() ? "Liturgia Horarum" : day.celebration);
        ((TextView) findViewById(R.id.txtLatinRank)).setText(day.rank);
        ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                LatinContentManager.status(this, day.year));
        for (Map.Entry<String, Integer> card : cards.entrySet()) {
            String path = day.hours.get(card.getKey());
            View view = findViewById(card.getValue());
            view.setEnabled(path != null);
            view.setAlpha(path == null ? 0.42f : 1f);
            view.setOnClickListener(path == null ? null : v -> openHour(card.getKey(), path));
        }
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    private void showUnavailable(int year) {
        ((TextView) findViewById(R.id.txtLatinDate)).setText(longDate(selectedDate));
        ((TextView) findViewById(R.id.txtLatinCelebration)).setText(
                "Liturgia Horarum " + year);
        ((TextView) findViewById(R.id.txtLatinRank)).setText("Contenido anual no instalado");
        ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                "Busca la actualización oficial o importa el EPUB latino de " + year + ".");
        for (int id : cards.values()) {
            findViewById(id).setEnabled(false);
            findViewById(id).setAlpha(0.42f);
        }
    }

    private void openHour(String key, String path) {
        Intent intent = new Intent(this, LatinHoursReaderActivity.class);
        intent.putExtra(LatinHoursReaderActivity.EXTRA_YEAR, currentDay.year);
        intent.putExtra(LatinHoursReaderActivity.EXTRA_PATH, path);
        intent.putExtra(LatinHoursReaderActivity.EXTRA_TITLE, title(key));
        startActivity(intent);
    }

    private void updateOnline() {
        final int year = selectedDate.get(Calendar.YEAR);
        progress.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                "Consultando la fuente oficial…");
        executor.submit(() -> {
            try {
                LatinContentManager.updateFromOfficial(getApplicationContext(), year,
                        message -> runOnUiThread(() ->
                                ((TextView) findViewById(R.id.txtLatinStatus)).setText(message)));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Liturgia Horarum " + year + " instalada.",
                            Toast.LENGTH_LONG).show();
                    loadDate();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                            "La descarga automática no estuvo disponible. Puedes importar el EPUB oficial.");
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void importEpub() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/epub+zip", "application/octet-stream", "application/zip"
        });
        startActivityForResult(intent, REQUEST_IMPORT_EPUB);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_EPUB || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final int year = selectedDate.get(Calendar.YEAR);
        progress.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                "Verificando que el EPUB sea latín y corresponda a " + year + "…");
        executor.submit(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalStateException("No se pudo leer el EPUB.");
                LatinContentManager.install(getApplicationContext(), input, year);
                runOnUiThread(() -> {
                    Toast.makeText(this, "EPUB latino verificado e instalado.",
                            Toast.LENGTH_LONG).show();
                    loadDate();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.txtLatinStatus)).setText(
                            "El archivo fue rechazado y el contenido anterior se conservó.");
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.clear();
            selectedDate.set(year, month, day, 12, 0, 0);
            loadDate();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void moveDate(int amount) {
        selectedDate.add(Calendar.DATE, amount);
        loadDate();
    }

    private static String title(String key) {
        if ("invitatory".equals(key)) return "Invitatorium";
        if ("office".equals(key)) return "Officium lectionis";
        if ("lauds".equals(key)) return "Laudes matutinæ";
        if ("terce".equals(key)) return "Tertia";
        if ("sext".equals(key)) return "Sexta";
        if ("none".equals(key)) return "Nona";
        if ("vespers".equals(key)) return "Vesperæ";
        return "Completorium";
    }

    private static String longDate(Calendar date) {
        String value = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(date.getTime());
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
