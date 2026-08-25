package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;

/** Base temática. Ministerium no modifica automáticamente No molestar. */
public abstract class ThemedActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeUtils.wrap(newBase));
    }

    /**
     * Se conserva el método por compatibilidad con lectores que lo sobrescribían,
     * pero el modo enfoque fue retirado y ya no activa ninguna política del sistema.
     */
    protected boolean usesPrayerFocus() {
        return false;
    }
}
