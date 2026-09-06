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
import java.util.List;
import java.util.Locale;

/** Native section reader for the Ministerium 5 Missal. */
public class MissalV5SectionActivity extends ThemedActivity {
    public static final String EXTRA_SECTION = "v5_missal_section";
    public static final String EXTRA_TITLE = "v5_missal_title";
    public static final String EXTRA_YEAR = "v5_missal_year";
    public static final String EXTRA_MONTH = "v5_missal_month";
    public static final String EXTRA_DAY = "v5_missal_day";
    public static final String EXTRA_CELEBRATION = "v5_missal_celebration";

    private static final String PENDING = "Contenido propio pendiente de una fuente verificada.";

    private LinearLayout root;
    private Calendar selectedDate;
    private String sectionId;
    private MassSection semanticSection;
    private TextView sourceStatus;
    private TextView entranceBody;
    private TextView collectBody;
    private TextView offeringsBody;
    private TextView communionAntiphonBody;
    private TextView postCommunionBody;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        selectedDate = selectedDate();
        sectionId = value(EXTRA_SECTION, "initial");
        semanticSection = MissalV5Semantic.section(sectionId);
        setContentView(buildScreen());
        loadDailyProper();
    }

    private View buildScreen() {
        String fallbackTitle = value(EXTRA_TITLE, "Misal");
        String title = semanticSection == null ? fallbackTitle : semanticSection.getTitle();

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
        context.setPadding(0, 0, 0, dp(8));
        root.addView(context);

        sourceStatus = text("Propios: comprobando caché local…", 12, R.color.muted, false);
        sourceStatus.setPadding(0, 0, 0, dp(18));
        root.addView(sourceStatus);

        TextView badge = text("MISAL 5 · MODELO KOTLIN", 11, R.color.wine, true);
        badge.setLetterSpacing(.10f);
        badge.setPadding(0, 0, 0, dp(20));
        root.addView(badge);

        if ("initial".equals(sectionId)) renderInitial();
        else if ("eucharist".equals(sectionId)) renderEucharist();
        else if ("communion".equals(sectionId)) renderCommunion();
        else if ("conclusion".equals(sectionId)) renderConclusion();
        else renderOther();
        return scroll;
    }

    private void renderInitial() {
        heading(sectionTitle("Ritos iniciales"));
        entranceBody = block(elementTitle("entrance_antiphon", "Antífona de entrada"), PENDING);
        block(elementTitle("greeting", "Saludo"), "Celebrante, respuesta de la asamblea y rúbrica se representarán como elementos distintos.");
        block(elementTitle("penitential_act", "Acto penitencial"), "Elige una fórmula. Solo una queda activa.");
        semanticSelectable("penitential_act", "Fórmula penitencial");
        block(elementTitle("gloria", "Gloria"), "Se mostrará únicamente cuando corresponda según el calendario litúrgico.");
        collectBody = block(elementTitle("collect", "Oración colecta"), PENDING);
    }

    private void renderEucharist() {
        heading(sectionTitle("Liturgia eucarística"));
        block(elementTitle("gifts", "Preparación de los dones"), "Rúbricas, invitaciones y respuestas se representarán con roles propios.");
        offeringsBody = block(elementTitle("offerings", "Oración sobre las ofrendas"), PENDING);
        block(elementTitle("preface", "Prefacio"), "Se resolverá por celebración, tiempo litúrgico y formulario.");
        block(elementTitle("eucharistic_prayer", "Plegaria eucarística"), "Elige una plegaria. La IV conservará su relación propia con el prefacio.");
        semanticSelectable("eucharistic_prayer", "Plegaria eucarística");
    }

    private void renderCommunion() {
        heading(sectionTitle("Rito de la comunión"));
        block(elementTitle("our_father", "Padrenuestro"), "Texto, embolismo y respuesta se mantendrán como elementos semánticos separados.");
        block(elementTitle("peace", "Rito de la paz"), "Rúbrica y fórmulas separadas.");
        block(elementTitle("fraction", "Fracción del pan"), "Agnus Dei y gestos rituales como elementos distintos.");
        communionAntiphonBody = block(elementTitle("communion_antiphon", "Antífona de comunión"), PENDING);
        postCommunionBody = block(elementTitle("post_communion", "Oración después de la comunión"), PENDING);
    }

    private void renderConclusion() {
        heading(sectionTitle("Rito de conclusión"));
        block(elementTitle("blessing", "Bendición"), "Bendición simple o solemne según corresponda.");
        block(elementTitle("dismissal", "Despedida"), "Fórmula de despedida y respuesta de la asamblea.");
    }

    private void renderOther() {
        heading(sectionTitle("Otros formularios"));
        block("Comunes", "Pastores, mártires, vírgenes, santos y santas.");
        block("Por diversas necesidades", "Formularios para la Iglesia, sociedad y necesidades particulares.");
        block("Misas votivas", "Formularios votivos organizados por tema.");
        block("Misas de difuntos", "Exequias, aniversarios y otras ocasiones.");
        block("Propio de los santos", "Santoral por fecha, aislado por celebración para evitar contaminaciones entre santos.");
    }

    private void semanticSelectable(String elementId, String fallbackTitle) {
        MassElement element = MissalV5Semantic.element(sectionId, elementId);
        if (element == null || element.getOptions().isEmpty()) return;
        List<String> options = element.getOptions();
        String[] labels = options.toArray(new String[0]);
        String[] states = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            states[i] = element.getTitle() + " " + labels[i] + " seleccionada";
        }
        selectable(element.getTitle().isEmpty() ? fallbackTitle : element.getTitle(), labels, states);
    }

    private String sectionTitle(String fallback) {
        return semanticSection == null || semanticSection.getTitle().trim().isEmpty()
                ? fallback : semanticSection.getTitle();
    }

    private String elementTitle(String id, String fallback) {
        MassElement element = MissalV5Semantic.element(sectionId, id);
        return element == null || element.getTitle().trim().isEmpty() ? fallback : element.getTitle();
    }

    private void loadDailyProper() {
        DailyMassProperRepository.ProperDay cached =
                DailyMassProperRepository.cached(getApplicationContext(), selectedDate);
        if (cached != null) {
            applyProper(cached);
            sourceStatus.setText("Propios: Arquidiócesis de Guadalajara · caché local");
            if (cached.isComplete()) return;
        } else {
            sourceStatus.setText(MassReadingsRepository.isCurrentMonth(selectedDate)
                    ? "Propios: buscando fuente verificada…"
                    : "Propios: no disponibles todavía para esta fecha");
        }

        if (!MassReadingsRepository.isCurrentMonth(selectedDate)) return;
        final Calendar requestDate = (Calendar) selectedDate.clone();
        new Thread(() -> {
            DailyMassProperRepository.ProperDay proper =
                    DailyMassProperRepository.getOrSync(getApplicationContext(), requestDate);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (proper == null) {
                    sourceStatus.setText("Propios: fuente verificada no disponible; no se mostrará texto supuesto");
                    return;
                }
                applyProper(proper);
                sourceStatus.setText("Propios: Arquidiócesis de Guadalajara · guardados localmente");
            });
        }, "ministerium-v5-proper").start();
    }

    private void applyProper(DailyMassProperRepository.ProperDay proper) {
        if (proper == null) return;
        setProperText(entranceBody, proper.entrance);
        setProperText(collectBody, proper.collect);
        setProperText(offeringsBody, proper.offerings);
        setProperText(communionAntiphonBody, proper.communionAntiphon);
        setProperText(postCommunionBody, proper.postCommunion);
    }

    private void setProperText(TextView view, String value) {
        if (view == null) return;
        String clean = value == null ? "" : value.trim();
        view.setText(clean.isEmpty() ? PENDING : clean);
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

    private TextView block(String title, String body) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);
        card.addView(text(title, 18, R.color.wine, true));
        TextView p = text(body, 15, R.color.ink, false);
        p.setPadding(0, dp(6), 0, 0);
        p.setLineSpacing(0, 1.12f);
        p.setTextIsSelectable(true);
        card.addView(p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        root.addView(card, lp);
        return p;
    }

    private Calendar selectedDate() {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        return c;
    }

    private String dayLabel() {
        return new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "EC"))
                .format(selectedDate.getTime());
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
