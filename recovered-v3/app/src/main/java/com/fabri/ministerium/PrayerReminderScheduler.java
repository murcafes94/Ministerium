package com.fabri.ministerium;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public final class PrayerReminderScheduler {
    public static final String EXTRA_HOUR_INDEX = "prayer_hour_index";
    public static final String[] HOUR_LABELS = {
            "Oficio de lecturas", "Laudes", "Tercia", "Sexta",
            "Nona", "Vísperas", "Completas"
    };
    private static final int[] DEFAULT_HOURS = {7, 6, 9, 12, 15, 18, 21};
    private static final String PREFS = "prayer_reminder";
    private static final String MULTI_MIGRATED = "multi_reminders_migrated";
    private static final String SEVEN_MIGRATED = "seven_reminders_migrated";
    private static final String LEGACY_ENABLED = "enabled";
    private static final String LEGACY_HOUR_INDEX = "hour_index";
    private static final String LEGACY_CLOCK_HOUR = "clock_hour";
    private static final String LEGACY_CLOCK_MINUTE = "clock_minute";
    private static final int OLD_REQUEST_CODE_BASE = 830;
    private static final int REQUEST_CODE_BASE = 930;

    private PrayerReminderScheduler() {}

    public static int count() {
        return HOUR_LABELS.length;
    }

    public static boolean isEnabled(Context context, int index) {
        migrateLegacy(context);
        return preferences(context).getBoolean(enabledKey(validIndex(index)), false);
    }

    public static int clockHour(Context context, int index) {
        migrateLegacy(context);
        int safe = validIndex(index);
        return preferences(context).getInt(hourKey(safe), DEFAULT_HOURS[safe]);
    }

    public static int clockMinute(Context context, int index) {
        migrateLegacy(context);
        return preferences(context).getInt(minuteKey(validIndex(index)), 0);
    }

    public static String label(int index) {
        return HOUR_LABELS[validIndex(index)];
    }

    public static boolean isValidIndex(int index) {
        return index >= 0 && index < HOUR_LABELS.length;
    }

    public static void setReminder(Context context, int index, boolean enabled,
                                   int clockHour, int clockMinute) {
        migrateLegacy(context);
        int safe = validIndex(index);
        preferences(context).edit()
                .putBoolean(enabledKey(safe), enabled)
                .putInt(hourKey(safe), Math.max(0, Math.min(23, clockHour)))
                .putInt(minuteKey(safe), Math.max(0, Math.min(59, clockMinute)))
                .apply();
        if (enabled) scheduleNext(context, safe);
        else cancel(context, safe);
    }

    public static void restore(Context context) {
        migrateLegacy(context);
        for (int index = 0; index < HOUR_LABELS.length; index++) {
            if (isEnabled(context, index)) scheduleNext(context, index);
            else cancel(context, index);
        }
    }

    public static void scheduleNext(Context context, int index) {
        int safe = validIndex(index);
        if (!isEnabled(context, safe)) return;
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, clockHour(context, safe));
        next.set(Calendar.MINUTE, clockMinute(context, safe));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);

        AlarmManager alarms = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);
        if (alarms != null) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    next.getTimeInMillis(), reminderIntent(context, safe));
        }
    }

    public static void cancel(Context context, int index) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(reminderIntent(context, validIndex(index)));
    }

    private static PendingIntent reminderIntent(Context context, int index) {
        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        intent.putExtra(EXTRA_HOUR_INDEX, index);
        return PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void migrateLegacy(Context context) {
        SharedPreferences values = preferences(context);
        if (values.getBoolean(SEVEN_MIGRATED, false)) return;

        boolean[] enabled = new boolean[HOUR_LABELS.length];
        int[] hours = DEFAULT_HOURS.clone();
        int[] minutes = new int[HOUR_LABELS.length];
        if (values.getBoolean(MULTI_MIGRATED, false)) {
            for (int index = 0; index < HOUR_LABELS.length; index++) {
                int oldIndex = index + 1;
                enabled[index] = values.getBoolean(enabledKey(oldIndex), false);
                hours[index] = values.getInt(hourKey(oldIndex), DEFAULT_HOURS[index]);
                minutes[index] = values.getInt(minuteKey(oldIndex), 0);
            }
        } else if (values.getBoolean(LEGACY_ENABLED, false)) {
            int oldIndex = Math.max(0, Math.min(7,
                    values.getInt(LEGACY_HOUR_INDEX, 2)));
            if (oldIndex > 0) {
                int index = oldIndex - 1;
                enabled[index] = true;
                hours[index] = values.getInt(LEGACY_CLOCK_HOUR, DEFAULT_HOURS[index]);
                minutes[index] = values.getInt(LEGACY_CLOCK_MINUTE, 0);
            }
        }

        SharedPreferences.Editor editor = values.edit()
                .putBoolean(MULTI_MIGRATED, true)
                .putBoolean(SEVEN_MIGRATED, true);
        for (int index = 0; index < HOUR_LABELS.length; index++) {
            editor.putBoolean(enabledKey(index), enabled[index])
                    .putInt(hourKey(index), hours[index])
                    .putInt(minuteKey(index), minutes[index]);
        }
        editor.remove(enabledKey(7)).remove(hourKey(7)).remove(minuteKey(7));
        editor.apply();
        cancelOldAlarms(context);
    }

    private static void cancelOldAlarms(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);
        if (alarms == null) return;
        for (int index = 0; index < 8; index++) {
            Intent intent = new Intent(context, PrayerReminderReceiver.class);
            PendingIntent pending = PendingIntent.getBroadcast(context,
                    OLD_REQUEST_CODE_BASE + index, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarms.cancel(pending);
            pending.cancel();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String enabledKey(int index) {
        return "enabled_" + index;
    }

    private static String hourKey(int index) {
        return "clock_hour_" + index;
    }

    private static String minuteKey(int index) {
        return "clock_minute_" + index;
    }

    private static int validIndex(int value) {
        return Math.max(0, Math.min(HOUR_LABELS.length - 1, value));
    }
}
