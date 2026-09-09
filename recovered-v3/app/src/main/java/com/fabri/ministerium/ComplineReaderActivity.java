package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;

import java.util.Calendar;

/** Compatibility entry point: Compline now uses the Ministerium 5 native reader. */
public class ComplineReaderActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "compline_year";
    public static final String EXTRA_MONTH = "compline_month";
    public static final String EXTRA_DAY = "compline_day";

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);

        Calendar now = Calendar.getInstance();
        Intent nativeReader = new Intent(this, HoursV5ComplineActivity.class);
        nativeReader.putExtra(HoursV5ComplineActivity.EXTRA_YEAR,
                getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)));
        nativeReader.putExtra(HoursV5ComplineActivity.EXTRA_MONTH,
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)));
        nativeReader.putExtra(HoursV5ComplineActivity.EXTRA_DAY,
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)));
        startActivity(nativeReader);
        finish();
    }
}
