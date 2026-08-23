package com.fabri.ministerium;

import android.content.Context;

/** Datos personales opcionales que nunca son necesarios para usar la aplicación. */
public final class PersonalPreferences {
    private static final String PREFS = "personal_settings_v3";
    private static final String NAME = "signature_name";

    private PersonalPreferences() {}

    public static String signatureName(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(NAME, "").trim();
    }

    public static void setSignatureName(Context context, String value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(NAME, value == null ? "" : value.trim()).apply();
    }
}
