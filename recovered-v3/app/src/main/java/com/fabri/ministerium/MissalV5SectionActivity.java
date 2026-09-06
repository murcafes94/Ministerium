package com.fabri.ministerium;

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

/** Native section reader for the Ministerium 5 Missal. */
public class MissalV5SectionActivity extends ThemedActivity {
    public static final String EXTRA_SECTION = "v5_missal_section";
    public static final String EXTRA_TITLE = "v5_missal_title";
    public static final String EXTRA_YEAR = "v5_missal_year";
    public static final String EXTRA_MONTH = "v5_missal_month";
    public static final String EXTRA_DAY = "v5_missal_day";
    public static final String EXTRA_CELEBRATION = "v5_missal_celebration";

    private LinearLayout root;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        String section = value(EXTRA_SECTION, "initial");
        String title = value(EXTRA_TITLE, "Misal");

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));

        root = column();
        root.setPadding(dp(22), dp(18), dp(22), dp(32));
        scroll.addView(root);

        TextView back = text("‹  " + title, 23, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(6));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        TextView context = text(dayLabel() + "\n" + value(EXTRA_CELEBRATION, "Celebración del día"),
                13, R.color.muted, false);
        context.setPadding(0, 0, 0, dp(18));
        root.addView(context);

        TextView badge = text("MISAL 5 · LECTOR NATIVO", 11, R.color.wine, true);
        badge.setLetterSpacing(.10f);
        badge.setPadding(0, 0, 0, dp(20));
        root.addView(badge);

        if ("initial".equals(section)) renderInitial();
        else if ("eucharist".equals(section)) renderEucharist();
        else if ("communion".equals(section)) renderCommunion();
        else if ("conclusion".equals(section)) renderConclusion();
        else renderOther();
        return scroll;
    }

    private void renderInitial() {
        heading("Ritos iniciales");
        block("Entrada", "La antífona o canto de entrada se resolverá desde el formulario estructurado de la celebración.");
        block("Saludo", "Celebrante, respuesta de la asamblea y rúbrica serán elementos distintos, no un párrafo HTML reparado.");
        block("Acto penitencial", "Elige una fórmula. Solo una queda activa.");
        selectable("Fórmula penitencial", new String[]{"I", "II", "III"},
                new String[]{"Fórmula I seleccionada", "Fórmula II seleccionada", "Fórmula III seleccionada"});
        block("Gloria", "Se mostrará únicamente cuando corresponda según el calendario litúrgico.");
        block("Oración colecta", "Se cargará la propia del día. Si la fuente no contiene texto verificable, Ministerium lo marcará como pendiente en vez de inventarlo.");
    }

    private void renderEucharist() {
        heading("Liturgia eucarística");
        block("Preparación de los dones", "Rúbricas, invitaciones y respuestas se representarán con roles propios.");
        block("Oración sobre las ofrendas", "Se resolverá desde el formulario propio del día o el formulario elegido.");
        block("Prefacio", "Se resolverá por celebración, tiempo litúrgico y formulario.");
        block("Plegaria eucarística", "Elige una plegaria. La IV conservará su relación propia con el prefacio.");
        selectable("Plegaria eucarística", new String[]{"I", "II", "III", "IV"},
                new String[]{"Plegaria I seleccionada", "Plegaria II seleccionada", "Plegaria III seleccionada", "Plegaria IV seleccionada"});
    }

    private void renderCommunion() {
        heading("Rito de la comunión");
        block("Padrenuestro", "Texto, embolismo y respuesta se mantendrán como elementos semánticos separados.");
        block("Rito de la paz", "Rúbrica y fórmulas separadas.");
        block("Fracción del pan", "Agnus Dei y gestos rituales como elementos distintos.");
        block("Comunión", "Antífona propia y rito de comunión.");
        block("Oración después de la comunión", "Propia del día o del formulario seleccionado.");
    }

    private void renderConclusion() {
        heading("Rito de conclusión");
        block("Bendición", "Bendición simple o solemne según corresponda.");
        block("Despedida", "Fórmula de despedida y respuesta de la asamblea.");
    }

    private void renderOther() {
        heading("Otros formularios");
        block("Comunes", "Pastores, mártires, vírgenes, santos y santas.");
        block("Por diversas necesidades", "Formularios para la Iglesia, sociedad y necesidades particulares.");
        block("Misas votivas", "Formularios votivos organizados por tema.");
        block("Misas de difuntos", "Exequias, aniversarios y otras ocasiones.");
        block("Propio de los santos", "Santoral por fecha, aislado por celebración para evitar contaminaciones entre santos.");
    }

    private void selectable(String title, String[] labels, String[] states) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        LinearLayout statusCard = column();
        statusCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        statusCard.setBackgroundResource(R.drawable.bg_button_secondary);
        TextView statusTitle = text(title, 17, R.color.wine, true);
        TextView status = text(states[0], 14, R.color.ink, false);
        status.setPadding(0, dp(4), 0, 0);
        statusCard.addView(statusTitle);
        statusCard.addView(status);

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView button = text(labels[i], 16, R.color.wine, true);
            button.setGravity(Gravity.CENTER);
            button.setBackgroundResource(R.drawable.bg_button_secondary);
            button.setOnClickListener(v -> status.setText(states[index]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(button, lp);
        }

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        root.addView(row, rowLp);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, 0, 0, dp(12));
        root.addView(statusCard, cardLp);
    }

    private void heading(String value) {
        TextView v = text(value, 28, R.color.ink, true);
        v.setPadding(0, 0, 0, dp(16));
        root.addView(v);
    }

    private void block(String title, String body) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);
        card.addView(text(title, 18, R.color.wine, true));
        TextView p = text(body, 15, R.color.ink, false);
        p.setPadding(0, dp(6), 0, 0);
        p.setLineSpacing(0, 1.12f);
        card.addView(p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        root.addView(card, lp);
    }

    private String dayLabel() {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        return new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "EC")).format(c.getTime());
    }

    private String value(String key, String fallback) {
        String v = getIntent().getStringExtra(key);
        return v == null || v.trim().isEmpty() ? fallback : v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return l;
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
