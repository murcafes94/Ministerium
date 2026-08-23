package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public final class ThemeUtils {
    private static final String PREFS = "ministerium_preferences";
    private static final String MODE = "theme_mode";
    private static final String LEGACY_DARK = "dark_theme";
    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String SEPIA = "sepia";
    public static final String DARK = "dark";

    private ThemeUtils() {}

    public static void apply(Activity activity) {
        boolean dark = isDark(activity);
        activity.setTheme(dark ? R.style.Theme_Ministerium_Dark : R.style.Theme_Ministerium);
    }

    public static Context wrap(Context base) {
        String mode = getMode(base);
        if (SYSTEM.equals(mode)) return base;
        Configuration configuration = new Configuration(
                base.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode
                & ~Configuration.UI_MODE_NIGHT_MASK)
                | (DARK.equals(mode) ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO);
        return base.createConfigurationContext(configuration);
    }

    public static boolean isDark(Context context) {
        String mode = getMode(context);
        if (DARK.equals(mode)) return true;
        if (LIGHT.equals(mode) || SEPIA.equals(mode)) return false;
        return systemUsesDarkTheme(context);
    }

    public static String getMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        if (preferences.contains(MODE)) {
            String mode = preferences.getString(MODE, SYSTEM);
            if (LIGHT.equals(mode) || SEPIA.equals(mode) || DARK.equals(mode)) return mode;
            return SYSTEM;
        }
        if (preferences.contains(LEGACY_DARK)) {
            return preferences.getBoolean(LEGACY_DARK, false) ? DARK : LIGHT;
        }
        return SYSTEM;
    }

    public static void setMode(Context context, String mode) {
        String selected = LIGHT.equals(mode) || SEPIA.equals(mode) || DARK.equals(mode)
                ? mode : SYSTEM;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(MODE, selected)
                .remove(LEGACY_DARK)
                .apply();
    }

    private static boolean systemUsesDarkTheme(Context context) {
        int night = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }
}
