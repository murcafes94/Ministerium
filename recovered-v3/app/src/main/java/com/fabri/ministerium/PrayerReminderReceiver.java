package com.fabri.ministerium;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class PrayerReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "prayer_reminders";
    private static final int NOTIFICATION_ID_BASE = 731;

    @Override
    public void onReceive(Context context, Intent receivedIntent) {
        int index = receivedIntent.getIntExtra(
                PrayerReminderScheduler.EXTRA_HOUR_INDEX, -1);
        if (!PrayerReminderScheduler.isValidIndex(index)
                || !PrayerReminderScheduler.isEnabled(context, index)) return;
        showNotification(context, index);
        PrayerReminderScheduler.scheduleNext(context, index);
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Recordatorios de oración", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Avisos diarios para rezar una hora litúrgica");
        manager.createNotificationChannel(channel);
    }

    private static void showNotification(Context context, int index) {
        createChannel(context);
        Calendar today = Calendar.getInstance();
        Intent open = new Intent(context, HoursTodayActivity.class);
        open.putExtra(HoursTodayActivity.EXTRA_YEAR, today.get(Calendar.YEAR));
        open.putExtra(HoursTodayActivity.EXTRA_MONTH, today.get(Calendar.MONTH));
        open.putExtra(HoursTodayActivity.EXTRA_DAY, today.get(Calendar.DAY_OF_MONTH));
        String[] keys = {"office", "lauds", "terce", "sext", "none", "vespers", "compline"};
        if (index >= 0 && index < keys.length) {
            open.putExtra(HoursTodayActivity.EXTRA_HOUR_KEY, keys[index]);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context,
                NOTIFICATION_ID_BASE + index, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Ministerium")
                .setContentText("Es momento de rezar "
                        + PrayerReminderScheduler.label(index) + ".")
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(
                NOTIFICATION_ID_BASE + index, notification);
    }
}
