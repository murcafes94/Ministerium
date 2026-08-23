package com.fabri.ministerium;

import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.EditText;

import java.util.Locale;

public class SettingsActivity extends ThemedActivity {
    public static final String EXTRA_OPEN_REMINDERS = "open_reminders";
    private static final int REQUEST_NOTIFICATIONS = 40;
    private static final int[] ROW_IDS = {
            R.id.rowReminderOffice, R.id.rowReminderLauds, R.id.rowReminderTerce,
            R.id.rowReminderSext, R.id.rowReminderNone,
            R.id.rowReminderVespers, R.id.rowReminderCompline
    };

    private final Switch[] reminderSwitches = new Switch[
            PrayerReminderScheduler.HOUR_LABELS.length];
    private final TextView[] reminderTimes = new TextView[
            PrayerReminderScheduler.HOUR_LABELS.length];
    private Switch gospelSwitch;
    private TextView gospelTime;
    private EditText signatureName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        configureTheme();
        configureLiturgicalRole();
        findViewById(R.id.cardReaderSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ReaderSettingsActivity.class)));
        findViewById(R.id.cardBackup).setOnClickListener(v ->
                startActivity(new Intent(this, BackupActivity.class)));
        findViewById(R.id.cardUpdates).setOnClickListener(v ->
                startActivity(new Intent(this, UpdateCenterActivity.class)));
        findViewById(R.id.cardFeedback).setOnClickListener(v ->
                startActivity(new Intent(this, FeedbackActivity.class)));
        signatureName = findViewById(R.id.inputSignatureName);
        signatureName.setText(PersonalPreferences.signatureName(this));
        configureReminders();
        configureGospelReminder();
        if (getIntent().getBooleanExtra(EXTRA_OPEN_REMINDERS, false)) {
            ScrollView scroll = findViewById(R.id.settingsScroll);
            View target = findViewById(R.id.reminderSectionTitle);
            scroll.post(() -> scroll.smoothScrollTo(0, target.getTop()));
        }
    }

    private void configureTheme() {
        RadioGroup group = findViewById(R.id.themeGroup);
        String mode = ThemeUtils.getMode(this);
        group.check(ThemeUtils.LIGHT.equals(mode) ? R.id.themeLight
                : ThemeUtils.SEPIA.equals(mode) ? R.id.themeSepia
                : ThemeUtils.DARK.equals(mode) ? R.id.themeDark : R.id.themeSystem);
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            String selected = checkedId == R.id.themeLight ? ThemeUtils.LIGHT
                    : checkedId == R.id.themeSepia ? ThemeUtils.SEPIA
                    : checkedId == R.id.themeDark ? ThemeUtils.DARK : ThemeUtils.SYSTEM;
            if (!selected.equals(ThemeUtils.getMode(this))) {
                ThemeUtils.setMode(this, selected);
                recreate();
            }
        });
    }

    private void configureLiturgicalRole() {
        RadioGroup group = findViewById(R.id.ministryRoleGroup);
        group.check(LiturgyPreferences.isOrdained(this)
                ? R.id.roleOrdained : R.id.roleLay);
        group.setOnCheckedChangeListener((radioGroup, checkedId) ->
                LiturgyPreferences.setOrdained(this, checkedId == R.id.roleOrdained));
    }

    private void configureReminders() {
        PrayerReminderScheduler.restore(this);
        for (int index = 0; index < PrayerReminderScheduler.count(); index++) {
            final int reminderIndex = index;
            View row = findViewById(ROW_IDS[index]);
            ((TextView) row.findViewById(R.id.txtReminderRowTitle)).setText(
                    PrayerReminderScheduler.label(index));
            reminderSwitches[index] = row.findViewById(R.id.switchReminderRow);
            reminderTimes[index] = row.findViewById(R.id.txtReminderRowTime);
            reminderSwitches[index].setChecked(
                    PrayerReminderScheduler.isEnabled(this, index));
            refreshReminder(index);
            row.setOnClickListener(v -> chooseTime(reminderIndex));
            reminderTimes[index].setOnClickListener(v -> chooseTime(reminderIndex));
            reminderSwitches[index].setOnCheckedChangeListener((button, enabled) -> {
                PrayerReminderScheduler.setReminder(this, reminderIndex, enabled,
                        PrayerReminderScheduler.clockHour(this, reminderIndex),
                        PrayerReminderScheduler.clockMinute(this, reminderIndex));
                refreshReminder(reminderIndex);
                if (enabled) ensureNotificationPermission();
            });
        }
    }

    private void chooseTime(int index) {
        int hour = PrayerReminderScheduler.clockHour(this, index);
        int minute = PrayerReminderScheduler.clockMinute(this, index);
        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            PrayerReminderScheduler.setReminder(this, index,
                    PrayerReminderScheduler.isEnabled(this, index),
                    selectedHour, selectedMinute);
            refreshReminder(index);
        }, hour, minute, true).show();
    }

    private void configureGospelReminder() {
        GospelReminderScheduler.restore(this);
        View row = findViewById(R.id.rowDailyGospel);
        ((TextView) row.findViewById(R.id.txtReminderRowTitle)).setText(
                "Evangelio del día");
        gospelSwitch = row.findViewById(R.id.switchReminderRow);
        gospelTime = row.findViewById(R.id.txtReminderRowTime);
        gospelSwitch.setChecked(GospelReminderScheduler.isEnabled(this));
        refreshGospelReminder();
        row.setOnClickListener(v -> chooseGospelTime());
        gospelTime.setOnClickListener(v -> chooseGospelTime());
        gospelSwitch.setOnCheckedChangeListener((button, enabled) -> {
            GospelReminderScheduler.setReminder(this, enabled,
                    GospelReminderScheduler.clockHour(this),
                    GospelReminderScheduler.clockMinute(this));
            refreshGospelReminder();
            if (enabled) ensureNotificationPermission();
        });
    }

    private void chooseGospelTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            GospelReminderScheduler.setReminder(this,
                    GospelReminderScheduler.isEnabled(this), hour, minute);
            refreshGospelReminder();
        }, GospelReminderScheduler.clockHour(this),
                GospelReminderScheduler.clockMinute(this), true).show();
    }

    private void refreshGospelReminder() {
        String time = String.format(Locale.US, "%02d:%02d",
                GospelReminderScheduler.clockHour(this),
                GospelReminderScheduler.clockMinute(this));
        gospelTime.setText(GospelReminderScheduler.isEnabled(this)
                ? "Aviso diario a las " + time + " · abre las lecturas"
                : "Desactivado · hora guardada " + time + " · toca para cambiar");
    }

    private void refreshReminder(int index) {
        String time = String.format(Locale.US, "%02d:%02d",
                PrayerReminderScheduler.clockHour(this, index),
                PrayerReminderScheduler.clockMinute(this, index));
        reminderTimes[index].setText(PrayerReminderScheduler.isEnabled(this, index)
                ? "Aviso diario a las " + time + " · toca para cambiar"
                : "Desactivado · hora guardada " + time + " · toca para cambiar");
    }

    private void ensureNotificationPermission() {
        PrayerReminderReceiver.createChannel(this);
        GospelReminderReceiver.createChannel(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},
                    REQUEST_NOTIFICATIONS);
        }
    }

    @Override protected void onPause() {
        if (signatureName != null) PersonalPreferences.setSignatureName(
                this, signatureName.getText().toString());
        super.onPause();
    }
}
