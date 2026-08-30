package com.fabri.ministerium;

import android.content.Context;

public final class LiturgyPreferences {
    private static final String PREFS = "liturgy_settings_v3";
    private static final String ORDAINED = "ordained_minister";

    private LiturgyPreferences() {}

    public static boolean isOrdained(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ORDAINED, false);
    }

    public static void setOrdained(Context context, boolean ordained) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(ORDAINED, ordained).apply();
    }
}
