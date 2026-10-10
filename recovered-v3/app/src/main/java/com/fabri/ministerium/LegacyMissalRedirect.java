package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import java.util.Calendar;

/** Keep old saved links usable after withdrawing the full Missal. */
public final class LegacyMissalRedirect {
    private LegacyMissalRedirect() {}
    public static boolean open(Activity activity) {
        Calendar now = Calendar.getInstance();
        Intent old = activity.getIntent();
        Intent intent = new Intent(activity, MassReadingsActivity.class);
        intent.putExtra(MassReadingsActivity.EXTRA_YEAR, old.getIntExtra("v5_missal_year", old.getIntExtra("missal_year", now.get(Calendar.YEAR))));
        intent.putExtra(MassReadingsActivity.EXTRA_MONTH, old.getIntExtra("v5_missal_month", old.getIntExtra("missal_month", now.get(Calendar.MONTH))));
        intent.putExtra(MassReadingsActivity.EXTRA_DAY, old.getIntExtra("v5_missal_day", old.getIntExtra("missal_day", now.get(Calendar.DAY_OF_MONTH))));
        activity.startActivity(intent);
        activity.finish();
        return true;
    }
}
