package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;

public abstract class ThemedActivity extends Activity {
    private boolean prayerFocusEntered;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeUtils.wrap(newBase));
    }

    /** Las pantallas litúrgicas sobrescriben este método para activar No molestar. */
    protected boolean usesPrayerFocus() {
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
