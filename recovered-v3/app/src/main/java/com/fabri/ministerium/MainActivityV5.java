package com.fabri.ministerium;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/** Ministerium 5 preview shell. */
public class MainActivityV5 extends ThemedActivity {
    private boolean dark;
    private int bg;
    private int card;
    private int ink;
    private int muted;
    private int wine;
    private int gold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        dark = ThemeUtils.isDark(this);
        bg = Color.parseColor(dark ? "#12100F" : "#F7F2EA");
        card = Color.parseColor(dark ? "#211C1A" : "#FFFDF9");
        ink = Color.parseColor(dark ? "#F5EEE8" : "#2B211E");
        muted = Color.parseColor(dark ? "#BFAFA8" : "#756863");
        wine = Color.parseColor(dark ? "#D89AA3" : "#6D1E2B");
        gold = Color.parseColor("#D2A84A");
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(6), dp(4), dp(18));
        root.addView(header, matchWrap());

        TextView mark = text("✠", 28, wine, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(gold, 16));
        header.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(14), 0, 0, 0);
        header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titles.addView(text("MINISTERIUM", 24, ink, Typeface.BOLD));
        titles.addView(text("Versión 5 · reconstrucción nativa", 12, muted, Typeface.NORMAL));

        TextView theme = text(dark ? "☀" : "◐", 22, ink, Typeface.NORMAL);
        theme.setGravity(Gravity.CENTER);
        theme.setOnClickListener(v -> {
            ThemeUtils.setMode(this, dark ? ThemeUtils.LIGHT : ThemeUtils.DARK);
            recreate();
        });
        header.addView(theme, new LinearLayout.LayoutParams(dp(48), dp(48)));

        root.addView(text("Lo esencial, primero", 26, ink, Typeface.BOLD), matchWrap());
        TextView intro = text(
                "La nueva base usa navegación nativa y migra cada módulo sin arrastrar los lectores heredados.",
                14, muted, Typeface.NORMAL);
        intro.setPadding(0, dp(6), 0, dp(14));
        root.addView(intro, matchWrap());

        root.addView(card("B", "Biblia", "Se mantiene offline y sin cambios de contenido", () ->
                startActivity(new Intent(this, BibleActivity.class))));

        root.addView(card("M", "Misal", "Ya abre la primera estructura nativa de Ministerium 5", () ->
                startActivity(new Intent(this, MissalV5Activity.class))));

        root.addView(card("☀", "Liturgia de las Horas", "Acceso diario; será el siguiente módulo estructurado", this::openToday));

        root.addView(card("✠", "Rituales y Bendicional", "Acceso pastoral conservado para su migración nativa", () ->
                startActivity(new Intent(this, PastoralActivity.class))));

        root.addView(card("M", "Magisterio y Derecho", "Se conserva mientras definimos contenido online/descargable", () ->
                startActivity(new Intent(this, MagisteriumActivity.class))));

        root.addView(card("⚙", "Ajustes", "Tema, lectura, márgenes y preferencias", () ->
                startActivity(new Intent(this, SettingsActivity.class))));

        TextView note = text("Preview 5.0 · la versión anterior sigue intacta en la rama 4.1.", 12, muted, Typeface.NORMAL);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note, matchWrap());
        return scroll;
    }

    private View card(String icon, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(15), dp(16), dp(15));
        row.setBackground(rounded(card, 16));
        row.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        row.setLayoutParams(lp);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());

        TextView glyph = text(icon, 20, wine, Typeface.BOLD);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(rounded(dark ? Color.parseColor("#332821") : Color.parseColor("#F2E2B9"), 14));
        row.addView(glyph, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), 0, dp(8), 0);
        row.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(text(title, 17, ink, Typeface.BOLD));
        TextView sub = text(subtitle, 13, muted, Typeface.NORMAL);
        sub.setPadding(0, dp(3), 0, 0);
        body.addView(sub);

        row.addView(text("›", 28, gold, Typeface.NORMAL), wrapWrap());
        return row;
    }

    private void openToday() {
        Calendar today = Calendar.getInstance();
        Intent intent = new Intent(this, HoursTodayActivity.class);
        intent.putExtra(HoursTodayActivity.EXTRA_YEAR, today.get(Calendar.YEAR));
        intent.putExtra(HoursTodayActivity.EXTRA_MONTH, today.get(Calendar.MONTH));
        intent.putExtra(HoursTodayActivity.EXTRA_DAY, today.get(Calendar.DAY_OF_MONTH));
        startActivity(intent);
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (color == card) d.setStroke(dp(1), dark ? Color.parseColor("#3A312E") : Color.parseColor("#E8DED4"));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
