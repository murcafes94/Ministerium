package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class UsccbLinks {
    private UsccbLinks() {}

    public static String readings(Calendar date) {
        return String.format(Locale.US,
                "https://bible.usccb.org/es/bible/lecturas/%02d%02d%02d.cfm",
                date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH),
                date.get(Calendar.YEAR) % 100);
    }

    public static String calendar() {
        return "https://bible.usccb.org/es/readings/calendar";
    }

    public static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception error) {
            Toast.makeText(activity, "No se encontró un navegador para abrir el enlace.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
