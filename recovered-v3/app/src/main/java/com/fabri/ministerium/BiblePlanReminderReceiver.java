package com.fabri.ministerium;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BiblePlanReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "bible_reading_plan";
    private static final int NOTIFICATION_ID = 770;

    @Override public void onReceive(Context context, Intent intent) {
        BiblePlanRepository.Plan plan = BiblePlanStore.active(context);
        if (!BiblePlanReminderScheduler.isEnabled(context) || plan == null) return;
        createChannel(context);
        String message = "Abre la lectura bíblica correspondiente a hoy.";
        Intent open = new Intent(context, BiblePlansActivity.class);
        try {
            BiblePlanRepository.DayReading reading = BiblePlanRepository.reading(context,
                    plan, BiblePlanStore.currentDay(context));
            message = "Día " + reading.day + ": " + reading.citation;
            open = new Intent(context, BibleReaderActivity.class)
                    .putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, reading.bookIndex)
                    .putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, reading.chapterIndex)
                    .putExtra(BibleReaderActivity.EXTRA_PLAN_ID, plan.id)
                    .putExtra(BibleReaderActivity.EXTRA_PLAN_DAY, reading.day);
        } catch (Exception ignored) {}
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        Notification notification = builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Plan de lectura bíblica")
                .setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(content).setAutoCancel(true).build();
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification);
        BiblePlanReminderScheduler.scheduleNext(context);
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Plan de lectura bíblica", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Recordatorio local para continuar el plan bíblico");
        manager.createNotificationChannel(channel);
    }
}
