package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;

public abstract class ThemedActivity extends Activity {
    private boolean prayerFocusEntered;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeUtils.wrap(newBase));
    }

    /**
     * Política central para no activar No molestar en documentos, diccionarios o
     * navegación. Las subclases pueden sobrescribirla si aparece un nuevo tipo de
     * celebración.
     */
    protected boolean usesPrayerFocus() {
        String name = getClass().getSimpleName();
        if ("CombinedMassActivity".equals(name)
                || "CombinedHoursActivity".equals(name)
                || "BilingualHoursReaderActivity".equals(name)
                || "LatinHoursReaderActivity".equals(name)
                || "RitualReaderActivity".equals(name)) {
            return true;
        }
        if ("BibleReaderActivity".equals(name)) {
            String plan = getIntent() == null ? null : getIntent().getStringExtra("plan_id");
            return plan != null && !plan.trim().isEmpty();
        }
        if ("HoursReaderActivity".equals(name) && getIntent() != null) {
            String volume = getIntent().getStringExtra("volume_id");
            return "advent".equals(volume) || "christmas".equals(volume)
                    || "lent".equals(volume) || "easter".equals(volume)
                    || "ordinary".equals(volume) || "sanctoral".equals(volume)
                    || "roman_missal".equals(volume) || "latin_2026".equals(volume);
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (usesPrayerFocus()) {
            PrayerFocusController.enter(this);
            prayerFocusEntered = PrayerFocusController.isEnabled(this)
                    && PrayerFocusController.hasPolicyAccess(this);
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
