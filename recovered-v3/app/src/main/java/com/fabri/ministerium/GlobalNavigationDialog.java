package com.fabri.ministerium;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/** Panel lateral derecho. Se abre únicamente desde su icono. */
public final class GlobalNavigationDialog {
    private static final String[] LABELS = {
            "Inicio", "Biblia", "Lecturas del día", "Liturgia de las Horas",
            "Liturgia bilingüe", "Misal", "Calendario", "Derecho Canónico",
            "Catecismo y Magisterio", "Rituales y Bendicional", "Devocionario",
            "Oraciones", "Mi estudio", "Configuración"
    };

    private GlobalNavigationDialog() {}

    public static void show(Activity activity) {
        Dialog dialog = new Dialog(activity);
        ListView list = new ListView(activity);
        int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        list.setPadding(pad, pad, pad, pad);
        list.setDividerHeight(0);
        list.setBackgroundColor(ThemeUtils.isDark(activity)
                ? Color.rgb(38, 33, 30) : Color.rgb(255, 250, 241));
        list.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1, LABELS));
        list.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            open(activity, position);
        });
        dialog.setContentView(list);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.END);
            window.setLayout(Math.round(activity.getResources().getDisplayMetrics().widthPixels
                    * .84f), ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.setOnShowListener(ignored -> {
            Window current = dialog.getWindow();
            if (current != null) current.setLayout(Math.round(
                    activity.getResources().getDisplayMetrics().widthPixels * .84f),
                    ViewGroup.LayoutParams.MATCH_PARENT);
        });
        dialog.show();
    }

    private static void open(Activity activity, int position) {
        Class<?> target;
        switch (position) {
            case 1: target = BibleActivity.class; break;
            case 2: target = MassReadingsActivity.class; break;
            case 3: target = HoursTodayActivity.class; break;
            case 4: target = BilingualHoursActivity.class; break;
            case 5: target = MissalActivity.class; break;
            case 6: target = LiturgicalCalendarActivity.class; break;
            case 7: target = CanonLawActivity.class; break;
            case 8: target = MagisteriumActivity.class; break;
            case 9: target = PastoralActivity.class; break;
            case 10: target = DevotionalHubActivity.class; break;
            case 11: target = BasicPrayersActivity.class; break;
            case 12: target = MyStudyActivity.class; break;
            case 13: target = SettingsActivity.class; break;
            default: target = MainActivity.class; break;
        }
        Intent intent = new Intent(activity, target);
        if (target == HoursTodayActivity.class || target == MissalActivity.class
                || target == MassReadingsActivity.class) {
            java.util.Calendar now = java.util.Calendar.getInstance();
            intent.putExtra("year", now.get(java.util.Calendar.YEAR));
            intent.putExtra("month", now.get(java.util.Calendar.MONTH));
            intent.putExtra("day", now.get(java.util.Calendar.DAY_OF_MONTH));
        }
        if (target == MainActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        activity.startActivity(intent);
    }
}
