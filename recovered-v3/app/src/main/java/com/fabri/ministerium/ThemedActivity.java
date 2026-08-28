package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Base temática, barra superior estable y ciclo de vida del modo de oración. */
public abstract class ThemedActivity extends Activity {
    private boolean prayerFocusEntered;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeUtils.wrap(newBase));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Las pantallas basadas en ScrollView conservan su barra superior visible.
        // Los lectores tienen además su protección específica en ReaderChrome.
        StaticTopBarController.attach(this);
    }

    /**
     * Lectores y celebraciones que deben activar No molestar cuando el usuario
     * habilitó "Modo oración" en Preferencias de lectura. La lista central evita
     * que una pantalla nueva olvide entrar/salir de la sesión de enfoque.
     */
    protected boolean usesPrayerFocus() {
        String name = getClass().getSimpleName();
        if ("PrayerReaderActivity".equals(name)
                || "HoursReaderActivity".equals(name)
                || "ComplineReaderActivity".equals(name)
                || "CombinedHoursActivity".equals(name)
                || "LatinHoursReaderActivity".equals(name)
                || "MassReadingReaderActivity".equals(name)
                || "MissalSectionReaderActivity".equals(name)
                || "CombinedMassActivity".equals(name)
                || "RitualReaderActivity".equals(name)) return true;
        if ("BibleReaderActivity".equals(name)) {
            Intent intent = getIntent();
            return intent != null && intent.getStringExtra("plan_id") != null
                    && !intent.getStringExtra("plan_id").trim().isEmpty();
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (usesPrayerFocus() && !prayerFocusEntered) {
            PrayerFocusController.enter(this);
            prayerFocusEntered = true;
        }
    }

    @Override
    protected void onStop() {
        if (prayerFocusEntered) {
            PrayerFocusController.exit(this);
            prayerFocusEntered = false;
        }
        super.onStop();
    }
}
