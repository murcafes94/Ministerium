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

public class GospelReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "daily_gospel";
    private static final int NOTIFICATION_ID = 760;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!GospelReminderScheduler.isEnabled(context)) return;
        createChannel(context);
        final Calendar today = Calendar.getInstance();
        if (!MassReadingsRepository.has(context, today)
                && MassReadingsRepository.isCurrentMonth(today)) {
            final PendingResult pending = goAsync();
            final Context application = context.getApplicationContext();
            new Thread(() -> {
                try {
                    MassReadingsRepository.syncDay(application, today);
                } catch (Exception ignored) {
                    // Sin red se conserva el aviso genérico y se vuelve a intentar al día siguiente.
                }
                postNotification(application, today);
                GospelReminderScheduler.scheduleNext(application);
                pending.finish();
            }, "ministerium-gospel").start();
            return;
        }
        postNotification(context, today);
        GospelReminderScheduler.scheduleNext(context);
    }

    private void postNotification(Context context, Calendar today) {
        Intent open;
        if (MassReadingsRepository.has(context, today)) {
            open = new Intent(context, MassReadingReaderActivity.class);
            open.putExtra(MassReadingReaderActivity.EXTRA_YEAR, today.get(Calendar.YEAR));
            open.putExtra(MassReadingReaderActivity.EXTRA_MONTH, today.get(Calendar.MONTH));
            open.putExtra(MassReadingReaderActivity.EXTRA_DAY,
                    today.get(Calendar.DAY_OF_MONTH));
        } else {
            open = new Intent(context, MassReadingsActivity.class);
        }
        PendingIntent content = PendingIntent.getActivity(context, NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        String summary = MassReadingsRepository.gospelSummary(context, today);
        String message = summary.isEmpty()
                ? "Abre las lecturas de hoy en Ministerium."
                : summary;
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Evangelio del día")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification);
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Evangelio del día", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Aviso matutino con una frase del Evangelio del día");
        manager.createNotificationChannel(channel);
    }
}
