package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;

public abstract class ThemedActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeUtils.wrap(newBase));
    }
}
