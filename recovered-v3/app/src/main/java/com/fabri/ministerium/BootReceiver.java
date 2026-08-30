package com.fabri.ministerium;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrayerReminderScheduler.restore(context);
        GospelReminderScheduler.restore(context);
        BiblePlanReminderScheduler.restore(context);
    }
}
