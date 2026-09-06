package com.fabri.ministerium;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native section reader for the Ministerium 5 Missal prototype. */
public class MissalV5SectionActivity extends ThemedActivity {
    public static final String EXTRA_SECTION = "v5_missal_section";
    public static final String EXTRA_TITLE = "v5_missal_title";

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        String section = getIntent().getStringExtra(EXTRA_SECTION);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (section == null) section = "initial";
        if (title == null) title = "Misal";

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));

        LinearLayout root = column();
        root.setPadding(dp(22), dp(18), dp(22), dp(32));
        scroll.addView(root);

        TextView back = text("‹  " + title, 23, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(6));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        TextView badge = text("MISAL 5 · LECTOR NATIVO", 11, R.color.wine, true);
        badge.setLetterSpacing(.10f);
        badge.setPadding(0, 0, 0, dp(20));
        root.addView(badge);

        if ("initial".equals(section)) renderInitial(root);
        else if ("eucharist".equals(section)) renderEucharist(root);
        else if ("communion".equals(section)) renderCommunion(root);
        else if ("conclusion".equals(section)) renderConclusion(root);
        else renderOther(root);
        return scroll;
    }

    private void renderInitial(LinearLayout root) {
        heading(root, "Ritos iniciales");
        block(root, "Entrada", "Aquí irá la antífona o canto de entrada correspondiente a la celebración.");
        block(root, "Saludo", "Componente litúrgico nativo: celebrante, respuesta de la asamblea y rúbrica se mostrarán con estilos distintos.");
        block(root, "Acto penitencial", "Selector único de fórmulas I, II y III. No habrá botones duplicados ni alternativas superpuestas.");
        selector(root, new String[]{"I", "II", "III"});
        block(root, "Gloria", "Se mostrará solamente cuando corresponda según el día litúrgico.");
        block(root, "Oración colecta", "La oración propia se cargará desde el formulario estructurado del día; si falta, la app lo indicará sin inventar texto.");
    }

    private void renderEucharist(LinearLayout root) {
        heading(root, "Liturgia eucarística");
        block(root, "Preparación de los dones", "Rúbricas, invitación y respuesta en componentes separados.");
        block(root, "Oración sobre las ofrendas", "Propia del día o formulario seleccionado.");
        block(root, "Prefacio", "El prefacio se resolverá por celebración y tiempo litúrgico.");
        block(root, "Plegaria eucarística", "Selector nativo I–IV. Se mostrará una sola plegaria a la vez.");
        selector(root, new String[]{"I", "II", "III", "IV"});
    }

    private void renderCommunion(LinearLayout root) {
        heading(root, "Rito de la comunión");
        block(root, "Padrenuestro", "Texto y respuestas se presentarán con jerarquía litúrgica clara.");
        block(root, "Rito de la paz", "Rúbrica y fórmulas separadas.");
        block(root, "Fracción del pan", "Agnus Dei y gestos rituales como elementos distintos.");
        block(root, "Comunión", "Antífona propia y rito de comunión.");
        block(root, "Oración después de la comunión", "Propia del día o del formulario seleccionado.");
    }

    private void renderConclusion(LinearLayout root) {
        heading(root, "Rito de conclusión");
        block(root, "Bendición", "Bendición simple o solemne según corresponda.");
        block(root, "Despedida", "Fórmula de despedida y respuesta de la asamblea.");
    }

    private void renderOther(LinearLayout root) {
        heading(root, "Otros formularios");
        block(root, "Comunes", "Pastores, mártires, vírgenes, santos y santas.");
        block(root, "Por diversas necesidades", "Formularios para la Iglesia, sociedad y necesidades particulares.");
        block(root, "Misas votivas", "Formularios votivos organizados por tema.");
        block(root, "Misas de difuntos", "Exequias, aniversarios y otras ocasiones.");
        block(root, "Propio de los santos", "Santoral por fecha, sin mezclar celebraciones entre sí.");
    }

    private void heading(LinearLayout root, String value) {
        TextView v = text(value, 28, R.color.ink, true);
        v.setPadding(0, 0, 0, dp(16));
        root.addView(v);
    }

    private void block(LinearLayout root, String title, String body) {
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

    private void selector(LinearLayout root, String[] labels) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 0, 0, dp(12));
        for (String label : labels) {
            TextView button = text(label, 16, R.color.wine, true);
            button.setGravity(Gravity.CENTER);
            button.setBackgroundResource(R.drawable.bg_button_secondary);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(button, lp);
        }
        root.addView(row);
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
