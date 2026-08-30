package com.fabri.ministerium;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

public final class BiblePlanReminderScheduler {
    private static final String PREFS = "bible_plan_reminder";
    private static final String ENABLED = "enabled";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final int REQUEST_CODE = 1060;
    private BiblePlanReminderScheduler() {}

    public static boolean isEnabled(Context context) {
        return values(context).getBoolean(ENABLED, false);
    }

    public static int hour(Context context) { return values(context).getInt(HOUR, 20); }
    public static int minute(Context context) { return values(context).getInt(MINUTE, 0); }

    public static void set(Context context, boolean enabled, int hour, int minute) {
        values(context).edit().putBoolean(ENABLED, enabled)
                .putInt(HOUR, Math.max(0, Math.min(23, hour)))
                .putInt(MINUTE, Math.max(0, Math.min(59, minute))).apply();
        restore(context);
    }

    public static void restore(Context context) {
        if (isEnabled(context) && BiblePlanStore.active(context) != null) scheduleNext(context);
        else cancel(context);
    }

    public static void scheduleNext(Context context) {
        if (!isEnabled(context)) return;
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour(context));
        next.set(Calendar.MINUTE, minute(context));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DATE, 1);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(), intent(context));
    }

    private static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(intent(context));
    }

    private static PendingIntent intent(Context context) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE,
                new Intent(context, BiblePlanReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
