package com.fabri.ministerium;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/**
 * Gestiona una sesión de "No molestar" mientras se reza o se celebra.
 *
 * Se conserva el filtro que el usuario tenía antes de entrar y se restaura al
 * salir de la última pantalla litúrgica. Se usa PRIORITY para respetar las
 * excepciones que el propio usuario haya configurado en Android (alarmas,
 * contactos prioritarios, etc.).
 */
public final class PrayerFocusController {
    private static final String PREFS = "prayer_focus";
    private static final String ENABLED = "enabled";
    private static final String SESSION_ACTIVE = "session_active";
    private static final String PREVIOUS_FILTER = "previous_filter";

    private static int activeScreens;
    private static Integer processPreviousFilter;

    private PrayerFocusController() {}

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(ENABLED, enabled).apply();
        if (!enabled) restoreIfPossible(context);
    }

    public static boolean hasPolicyAccess(Context context) {
        NotificationManager manager = manager(context);
        return manager != null && manager.isNotificationPolicyAccessGranted();
    }

    public static void requestPolicyAccess(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }

    /** Recupera una sesión que quedó activa por cierre forzado o muerte del proceso. */
    public static synchronized void recoverStaleSession(Context context) {
        if (activeScreens > 0) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SESSION_ACTIVE, false)) return;
        if (!hasPolicyAccess(context)) {
            prefs.edit().putBoolean(SESSION_ACTIVE, false).remove(PREVIOUS_FILTER).apply();
            return;
        }
        int previous = prefs.getInt(PREVIOUS_FILTER,
                NotificationManager.INTERRUPTION_FILTER_ALL);
        NotificationManager manager = manager(context);
        if (manager != null) {
            try { manager.setInterruptionFilter(previous); } catch (Exception ignored) {}
        }
        prefs.edit().putBoolean(SESSION_ACTIVE, false).remove(PREVIOUS_FILTER).apply();
    }

    public static synchronized void enter(Context context) {
        if (!isEnabled(context) || !hasPolicyAccess(context)) return;
        NotificationManager manager = manager(context);
        if (manager == null) return;
        if (activeScreens == 0) {
            processPreviousFilter = manager.getCurrentInterruptionFilter();
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(SESSION_ACTIVE, true)
                    .putInt(PREVIOUS_FILTER, processPreviousFilter).apply();
            try {
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            } catch (Exception ignored) {
                processPreviousFilter = null;
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putBoolean(SESSION_ACTIVE, false).remove(PREVIOUS_FILTER).apply();
                return;
            }
        }
        activeScreens++;
    }

    public static synchronized void exit(Context context) {
        if (activeScreens <= 0) return;
        activeScreens--;
        if (activeScreens > 0) return;
        restoreIfPossible(context);
    }

    private static synchronized void restoreIfPossible(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        int previous = processPreviousFilter != null
                ? processPreviousFilter
                : prefs.getInt(PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL);
        if (hasPolicyAccess(context)) {
            NotificationManager manager = manager(context);
            if (manager != null) {
                try { manager.setInterruptionFilter(previous); } catch (Exception ignored) {}
            }
        }
        activeScreens = 0;
        processPreviousFilter = null;
        prefs.edit().putBoolean(SESSION_ACTIVE, false).remove(PREVIOUS_FILTER).apply();
    }

    private static NotificationManager manager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
