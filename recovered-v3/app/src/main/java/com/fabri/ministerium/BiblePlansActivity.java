package com.fabri.ministerium;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;
import java.util.Locale;

public class BiblePlansActivity extends ThemedActivity {
    private TextView status;
    private TextView today;
    private TextView progress;
    private TextView reminderTime;
    private Button open;
    private Button complete;
    private Button cancel;
    private Switch reminder;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible_plans);
        status = findViewById(R.id.txtPlanStatus);
        today = findViewById(R.id.txtPlanToday);
        progress = findViewById(R.id.txtPlanProgress);
        reminderTime = findViewById(R.id.txtPlanReminderTime);
        open = findViewById(R.id.btnOpenPlanReading);
        complete = findViewById(R.id.btnCompletePlanDay);
        cancel = findViewById(R.id.btnCancelPlan);
        reminder = findViewById(R.id.switchPlanReminder);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPlanBible365).setOnClickListener(v -> startPlan("bible_365"));
        findViewById(R.id.btnPlanGospels89).setOnClickListener(v -> startPlan("gospels_89"));
        findViewById(R.id.btnPlanBook).setOnClickListener(v -> chooseBook());
        findViewById(R.id.btnPlanCustom).setOnClickListener(v -> chooseWholeBibleDays());
        open.setOnClickListener(v -> openToday());
        complete.setOnClickListener(v -> {
            BiblePlanStore.completeToday(this);
            refresh();
            Toast.makeText(this, "Lectura de hoy marcada como completada.",
                    Toast.LENGTH_SHORT).show();
        });
        cancel.setOnClickListener(v -> cancelPlan());
        reminder.setOnCheckedChangeListener((button, enabled) -> {
            if (button.isPressed()) {
                BiblePlanReminderScheduler.set(this, enabled,
                        BiblePlanReminderScheduler.hour(this),
                        BiblePlanReminderScheduler.minute(this));
                refreshReminder();
            }
        });
        findViewById(R.id.rowPlanReminder).setOnClickListener(v -> chooseTime());
        BiblePlanReminderReceiver.createChannel(this);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void startPlan(String id) {
        BiblePlanRepository.Plan plan = BiblePlanRepository.find(this, id);
        if (plan == null) return;
        new AlertDialog.Builder(this).setTitle(plan.title)
                .setMessage(plan.subtitle + "\n\nEl progreso anterior se sustituirá por este plan.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Comenzar hoy", (dialog, which) -> {
                    BiblePlanStore.start(this, id);
                    BiblePlanReminderScheduler.restore(this);
                    refresh();
                }).show();
    }

    private void chooseBook() {
        try {
            java.util.List<BibleRepository.Book> books = BibleRepository.books(this);
            String[] titles = new String[books.size()];
            for (int i = 0; i < books.size(); i++) {
                BibleRepository.Book book = books.get(i);
                titles[i] = book.title + " · " + book.chapters.size() + " capítulos";
            }
            new AlertDialog.Builder(this)
                    .setTitle("Elige el libro que deseas completar")
                    .setItems(titles, (dialog, which) -> chooseBookDays(which,
                            books.get(which).title, books.get(which).chapters.size()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar la lista de libros.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void chooseBookDays(int bookIndex, String title, int chapters) {
        chooseDays("¿En cuántos días quieres leer " + title + "?", chapters,
                days -> startPlan("book_" + bookIndex + "_days_" + days));
    }

    private void chooseWholeBibleDays() {
        try {
            int chapters = 0;
            for (BibleRepository.Book book : BibleRepository.books(this)) {
                chapters += book.chapters.size();
            }
            chooseDays("¿En cuántos días quieres leer la Biblia?", chapters,
                    days -> startPlan("custom_bible_" + days));
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar el plan personalizado.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void chooseDays(String title, int maximum, DaysCallback callback) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1–" + maximum + " días");
        input.setText(String.valueOf(Math.min(maximum, 90)));
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, 0, pad, 0);
        new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> {
                    try {
                        int days = Integer.parseInt(input.getText().toString().trim());
                        if (days < 1 || days > maximum) throw new NumberFormatException();
                        callback.selected(days);
                    } catch (NumberFormatException error) {
                        Toast.makeText(this, "Elige entre 1 y " + maximum + " días.",
                                Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void refresh() {
        BiblePlanRepository.Plan plan = BiblePlanStore.active(this);
        boolean active = plan != null;
        open.setEnabled(active);
        complete.setEnabled(active);
        cancel.setVisibility(active ? View.VISIBLE : View.GONE);
        reminder.setEnabled(active);
        if (!active) {
            status.setText("Elige un plan para comenzar.");
            today.setText("La lectura diaria aparecerá aquí.");
            progress.setText("Progreso: 0 días");
            refreshReminder();
            return;
        }
        int day = BiblePlanStore.currentDay(this);
        status.setText(plan.title);
        try {
            BiblePlanRepository.DayReading reading = BiblePlanRepository.reading(this, plan, day);
            today.setText("Día " + day + " · " + reading.citation);
        } catch (Exception error) {
            today.setText("No se pudo preparar la lectura de hoy.");
        }
        progress.setText("Progreso: " + BiblePlanStore.completedDays(this)
                + " de " + plan.days + " días completados");
        refreshReminder();
    }

    private void openToday() {
        BiblePlanRepository.Plan plan = BiblePlanStore.active(this);
        if (plan == null) return;
        try {
            BiblePlanRepository.DayReading reading = BiblePlanRepository.reading(this, plan,
                    BiblePlanStore.currentDay(this));
            Intent intent = new Intent(this, BibleReaderActivity.class);
            intent.putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, reading.bookIndex);
            intent.putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, reading.chapterIndex);
            intent.putExtra(BibleReaderActivity.EXTRA_PLAN_ID, plan.id);
            intent.putExtra(BibleReaderActivity.EXTRA_PLAN_DAY,
                    BiblePlanStore.currentDay(this));
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir la lectura del plan.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void cancelPlan() {
        BiblePlanRepository.Plan plan = BiblePlanStore.active(this);
        if (plan == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Cancelar plan de lectura")
                .setMessage("Se eliminará el progreso de «" + plan.title
                        + "» y se desactivará su recordatorio.")
                .setNegativeButton("Conservar plan", null)
                .setPositiveButton("Cancelar plan", (dialog, which) -> {
                    BiblePlanStore.cancel(this);
                    BiblePlanReminderScheduler.set(this, false,
                            BiblePlanReminderScheduler.hour(this),
                            BiblePlanReminderScheduler.minute(this));
                    refresh();
                    Toast.makeText(this, "Plan de lectura cancelado.",
                            Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void chooseTime() {
        if (BiblePlanStore.active(this) == null) return;
        new TimePickerDialog(this, (view, hour, minute) -> {
            BiblePlanReminderScheduler.set(this, true, hour, minute);
            refreshReminder();
        }, BiblePlanReminderScheduler.hour(this),
                BiblePlanReminderScheduler.minute(this), true).show();
    }

    private void refreshReminder() {
        boolean enabled = BiblePlanReminderScheduler.isEnabled(this)
                && BiblePlanStore.active(this) != null;
        reminder.setChecked(enabled);
        reminderTime.setText(enabled
                ? String.format(Locale.US, "Todos los días · %02d:%02d",
                BiblePlanReminderScheduler.hour(this),
                BiblePlanReminderScheduler.minute(this))
                : "Recordatorio desactivado");
    }

    private interface DaysCallback { void selected(int days); }
}
