package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public final class BiblePlanStore {
    private static final String PREFS = "bible_reading_plan";
    private static final String ACTIVE = "active_plan";
    private static final String START = "start_date";
    private static final String COMPLETED = "completed_days";
    private BiblePlanStore() {}

    public static String activeId(Context context) {
        return values(context).getString(ACTIVE, "");
    }

    public static BiblePlanRepository.Plan active(Context context) {
        return BiblePlanRepository.find(context, activeId(context));
    }

    public static void start(Context context, String planId) {
        values(context).edit().putString(ACTIVE, planId)
                .putLong(START, startOfToday()).putInt(COMPLETED, 0).apply();
    }

    public static int currentDay(Context context) {
        BiblePlanRepository.Plan plan = active(context);
        if (plan == null) return 1;
        long start = values(context).getLong(START, startOfToday());
        int elapsed = (int) Math.round((startOfToday() - start) / 86400000d);
        return Math.max(1, Math.min(plan.days, elapsed + 1));
    }

    public static int completedDays(Context context) {
        return values(context).getInt(COMPLETED, 0);
    }

    public static void completeToday(Context context) {
        values(context).edit().putInt(COMPLETED,
                Math.max(completedDays(context), currentDay(context))).apply();
    }

    public static boolean completeChapter(Context context, String planId, int day,
                                          String chapterKey, int required) {
        if (!activeId(context).equals(planId) || day != currentDay(context)) return false;
        String key = "session_" + planId + "_" + day;
        Set<String> values = new HashSet<>(values(context).getStringSet(key,
                java.util.Collections.emptySet()));
        values.add(chapterKey);
        values(context).edit().putStringSet(key, values).apply();
        if (values.size() >= required) {
            int before = completedDays(context);
            completeToday(context);
            return completedDays(context) > before;
        }
        return false;
    }

    public static int sessionCompleted(Context context, String planId, int day) {
        return values(context).getStringSet("session_" + planId + "_" + day,
                java.util.Collections.emptySet()).size();
    }

    public static void cancel(Context context) {
        SharedPreferences values = values(context);
        SharedPreferences.Editor editor = values.edit().remove(ACTIVE).remove(START)
                .remove(COMPLETED);
        for (String key : values.getAll().keySet()) if (key.startsWith("session_")) {
            editor.remove(key);
        }
        editor.apply();
    }

    private static long startOfToday() {
        Calendar value = Calendar.getInstance();
        value.set(Calendar.HOUR_OF_DAY, 0);
        value.set(Calendar.MINUTE, 0);
        value.set(Calendar.SECOND, 0);
        value.set(Calendar.MILLISECOND, 0);
        return value.getTimeInMillis();
    }

    private static SharedPreferences values(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
