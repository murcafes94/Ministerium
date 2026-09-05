package com.fabri.ministerium;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Ministerium 5 Missal shell.
 *
 * This screen intentionally does not render or repair the legacy Missal HTML.
 * It models the Mass as native, semantic sections so each section can be migrated
 * independently to structured content.
 */
public class MissalV5Activity extends ThemedActivity {
    private Calendar selectedDate = Calendar.getInstance();
    private TextView dateView;
    private TextView celebrationView;
    private TextView detailView;
    private LinearLayout sections;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        refreshDay();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));

        LinearLayout root = column();
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(root);

        TextView back = text("‹  Misal Diario Romano", 24, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(8));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        TextView subtitle = text("Ministerium 5 · estructura nativa", 13, R.color.muted, false);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView previous = action("‹");
        previous.setOnClickListener(v -> moveDay(-1));
        dateRow.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(52)));

        dateView = text("", 18, R.color.ink, true);
        dateView.setGravity(Gravity.CENTER);
        dateView.setOnClickListener(v -> chooseDate());
        dateRow.addView(dateView, new LinearLayout.LayoutParams(0, dp(52), 1f));

        TextView next = action("›");
        next.setOnClickListener(v -> moveDay(1));
        dateRow.addView(next, new LinearLayout.LayoutParams(dp(48), dp(52)));
        root.addView(dateRow);

        celebrationView = text("", 25, R.color.wine, true);
        celebrationView.setPadding(0, dp(16), 0, dp(5));
        root.addView(celebrationView);

        detailView = text("", 14, R.color.muted, false);
        detailView.setPadding(0, 0, 0, dp(22));
        root.addView(detailView);

        TextView label = text("CELEBRACIÓN", 12, R.color.wine, true);
        label.setLetterSpacing(.11f);
        label.setPadding(0, 0, 0, dp(10));
        root.addView(label);

        sections = column();
        root.addView(sections);
        return scroll;
    }

    private void refreshDay() {
        String date = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "EC")).format(selectedDate.getTime());
        dateView.setText(date.substring(0, 1).toUpperCase(Locale.ROOT) + date.substring(1));

        try {
            LiturgicalDay day = LiturgicalResolver.resolve(this, selectedDate);
            celebrationView.setText(day.celebration);
            String detail = day.liturgicalColor == null ? "" : day.liturgicalColor;
            int week = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
            if (week > 0) detail += (detail.isEmpty() ? "" : " · ") + "Semana " + week + " del Tiempo Ordinario";
            detailView.setText(detail);
        } catch (Exception e) {
            celebrationView.setText("Celebración del día");
            detailView.setText("Calendario litúrgico");
        }

        renderSections();
    }

    private void renderSections() {
        sections.removeAllViews();
        addSection("Ritos iniciales", "Entrada, saludo, acto penitencial, Gloria y colecta", "initial");
        addSection("Liturgia de la Palabra", "Lecturas del día, salmo, Evangelio, Credo y oración universal", "word");
        addSection("Liturgia eucarística", "Preparación de los dones, prefacio y plegaria eucarística", "eucharist");
        addSection("Rito de la comunión", "Padrenuestro, paz, fracción, comunión y oración", "communion");
        addSection("Rito de conclusión", "Bendición y despedida", "conclusion");
        addSection("Otros formularios", "Comunes, necesidades, votivas, difuntos y santos", "other");
    }

    private void addSection(String title, String subtitle, String id) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_button_secondary);

        TextView t = text(title, 19, R.color.ink, true);
        card.addView(t);
        TextView s = text(subtitle, 14, R.color.muted, false);
        s.setPadding(0, dp(4), 0, 0);
        card.addView(s);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        sections.addView(card, lp);
        card.setOnClickListener(v -> openSection(id, title));
    }

    private void openSection(String id, String title) {
        if ("word".equals(id)) {
            Intent intent = new Intent(this, MassReadingsActivity.class);
            intent.putExtra(MassReadingsActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR));
            intent.putExtra(MassReadingsActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH));
            intent.putExtra(MassReadingsActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH));
            startActivity(intent);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Este apartado ya pertenece al Misal 5 nativo. El contenido se migrará de forma estructurada, sin reutilizar el lector HTML antiguo.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.set(year, month, day, 12, 0, 0);
            refreshDay();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void moveDay(int amount) {
        selectedDate.add(Calendar.DATE, amount);
        refreshDay();
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private TextView action(String value) {
        TextView v = text(value, 30, R.color.wine, false);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView text(String value, int sp, int colorRes, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color(colorRes));
        if (bold) v.setTypeface(v.getTypeface(), Typeface.BOLD);
        return v;
    }

    @SuppressWarnings("deprecation")
    private int color(int res) { return getResources().getColor(res); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
