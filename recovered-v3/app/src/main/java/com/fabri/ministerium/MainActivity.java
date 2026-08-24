package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends ThemedActivity {
    private String appliedThemeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedThemeMode = ThemeUtils.getMode(this);
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Si Android cerró el proceso mientras Ministerium había activado No molestar,
        // restaura el filtro previo antes de iniciar una nueva sesión.
        PrayerFocusController.recoverStaleSession(this);
        replaceText(findViewById(R.id.cardPastoral), "Atención pastoral", "Ritual");

        findViewById(R.id.btnSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btnFavorites).setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));
        findViewById(R.id.btnTheme).setOnClickListener(v -> toggleTheme());
        findViewById(R.id.btnNotifications).setOnClickListener(v -> openReminders());
        PrayerReminderScheduler.restore(this);
        GospelReminderScheduler.restore(this);
        BiblePlanReminderScheduler.restore(this);
        LiturgicalCalendarRepository.ensureCurrentYear(this);

        findViewById(R.id.cardDevotional).setOnClickListener(v ->
                startActivity(new Intent(this, DevotionalHubActivity.class)));
        findViewById(R.id.cardBible).setOnClickListener(v ->
                startActivity(new Intent(this, BibleActivity.class)));
        findViewById(R.id.cardMagisterium).setOnClickListener(v ->
                startActivity(new Intent(this, MagisteriumActivity.class)));
        findViewById(R.id.cardMyStudy).setOnClickListener(v ->
                startActivity(new Intent(this, MyStudyActivity.class)));
        findViewById(R.id.cardPrayers).setOnClickListener(v ->
                startActivity(new Intent(this, BasicPrayersActivity.class)));
        findViewById(R.id.cardHours).setOnClickListener(v ->
                openToday());
        findViewById(R.id.cardBilingualHours).setOnClickListener(v ->
                startActivity(new Intent(this, BilingualHoursActivity.class)));
        findViewById(R.id.cardMassReadings).setOnClickListener(v ->
                startActivity(new Intent(this, MassReadingsActivity.class)));
        findViewById(R.id.cardMissal).setOnClickListener(v ->
                startActivity(new Intent(this, MissalActivity.class)));
        findViewById(R.id.cardLiturgicalCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, LiturgicalCalendarActivity.class)));
        findViewById(R.id.cardBlessings).setOnClickListener(v -> openRitual(
                RitualRepository.COMMON_BLESSINGS_ID));
        findViewById(R.id.cardPastoral).setOnClickListener(v ->
                startActivity(new Intent(this, PastoralActivity.class)));
        findViewById(R.id.cardSettings).setOnClickListener(v -> openSettings());
        bindContinueReading();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedThemeMode != null
                && !appliedThemeMode.equals(ThemeUtils.getMode(this))) {
            recreate();
            return;
        }
        bindContinueReading();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openReminders() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_OPEN_REMINDERS, true);
        startActivity(intent);
    }

    private void toggleTheme() {
        ThemeUtils.setMode(this, ThemeUtils.isDark(this)
                ? ThemeUtils.LIGHT : ThemeUtils.DARK);
        recreate();
    }

    private void openEpub(String volumeId) {
        Intent intent = new Intent(this, HoursTocActivity.class);
        intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID, volumeId);
        startActivity(intent);
    }

    private void openRitual(String documentId) {
        Intent intent = new Intent(this, RitualCatalogActivity.class);
        intent.putExtra(RitualCatalogActivity.EXTRA_DOCUMENT_ID, documentId);
        startActivity(intent);
    }

    private void openToday() {
        Calendar today = Calendar.getInstance();
        Intent intent = new Intent(this, HoursTodayActivity.class);
        intent.putExtra(HoursTodayActivity.EXTRA_YEAR, today.get(Calendar.YEAR));
        intent.putExtra(HoursTodayActivity.EXTRA_MONTH, today.get(Calendar.MONTH));
        intent.putExtra(HoursTodayActivity.EXTRA_DAY, today.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private void bindContinueReading() {
        View section = findViewById(R.id.sectionContinue);
        ContinueReadingStore.Position entry = ContinueReadingStore.latest(this);
        if (entry == null) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtContinueTitle)).setText("Continuar: " + entry.title);
        ((TextView) findViewById(R.id.txtContinueSubtitle)).setText(
                entry.module + (entry.scrollY > 0 ? " · posición guardada" : ""));
        section.setOnClickListener(v -> {
            if (!ContinueReadingStore.open(this, entry)) section.setVisibility(View.GONE);
        });
    }

    private static void replaceText(View root, String from, String to) {
        if (root == null) return;
        if (root instanceof TextView) {
            TextView text = (TextView) root;
            if (from.contentEquals(text.getText())) text.setText(to);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                replaceText(group.getChildAt(i), from, to);
            }
        }
    }
}
