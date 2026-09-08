package com.fabri.ministerium;

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
    private String celebration;
    private MassSection semanticSection;
    private MissalDayPresentation presentation;
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
        celebration = value(EXTRA_CELEBRATION, "Celebración del día");
        semanticSection = MissalV5Semantic.section(sectionId);
        presentation = MissalDisplayRules.resolve(selectedDate, celebration);
        setContentView(buildScreen());
        if (!"word".equals(sectionId)) loadDailyProper();
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

        TextView context = text(dayLabel() + "\n" + celebration, 13, R.color.muted, false);
        context.setPadding(0, 0, 0, dp(8));
        root.addView(context);

        TextView rules = text(presentation.getReason(), 12, R.color.muted, false);
        rules.setPadding(0, 0, 0, dp(8));
        root.addView(rules);

        String initialStatus = "word".equals(sectionId)
                ? "Lecturas: comprobando contenido local…"
                : "Propios: comprobando caché local…";
        sourceStatus = text(initialStatus, 12, R.color.muted, false);
        sourceStatus.setPadding(0, 0, 0, dp(18));
        root.addView(sourceStatus);

        TextView badge = text("MISAL · LECTOR NATIVO", 11, R.color.wine, true);
        badge.setLetterSpacing(.10f);
        badge.setPadding(0, 0, 0, dp(20));
        root.addView(badge);

        if ("initial".equals(sectionId)) renderInitial();
        else if ("word".equals(sectionId)) renderWord();
        else if ("eucharist".equals(sectionId)) renderEucharist();
        else if ("communion".equals(sectionId)) renderCommunion();
        else if ("conclusion".equals(sectionId)) renderConclusion();
        else renderOther();
        return scroll;
    }

    private void renderInitial() {
        heading(sectionTitle("Ritos iniciales"));
        entranceBody = semanticBlock("entrance_antiphon", PENDING);
        semanticBlock("greeting", "Celebrante, respuesta de la asamblea y rúbrica se representarán como elementos distintos.");
        semanticBlock("penitential_act", "Elige una fórmula. Solo una queda activa.");
        semanticSelectable("penitential_act", "Fórmula penitencial");
        if (presentation.getShowGloria()) {
            semanticBlock("gloria", "Gloria activo para esta celebración. El texto verificado se integrará como elemento propio.");
        }
        collectBody = semanticBlock("collect", PENDING);
    }

    private void renderWord() {
        heading(sectionTitle("Liturgia de la Palabra"));
        MissalV5WordContent content = MissalV5Readings.load(getApplicationContext(), selectedDate);
        if (content == null) {
            sourceStatus.setText("Lecturas: esta fecha no está sincronizada en el dispositivo");
            roleBlock("LECTURAS", "Contenido no disponible sin conexión",
                    "Ministerium no mostrará lecturas supuestas. Sincroniza el Leccionario desde Ajustes → Actualizaciones y vuelve a abrir esta fecha.");
            actionCard("Abrir Actualizaciones", () -> startActivity(new Intent(this, UpdateCenterActivity.class)));
            return;
        }

        sourceStatus.setText("Lecturas: " + content.getSourceLabel());
        readingBlock(content, "first_reading");
        readingBlock(content, "psalm");
        readingBlock(content, "second_reading");
        readingBlock(content, "acclamation");
        readingBlock(content, "gospel");

        if (presentation.getShowCreed()) {
            semanticBlock("creed", "El Credo corresponde a esta celebración. El texto fijo se mantendrá como oración litúrgica separada de las lecturas.");
        }
        semanticBlock("universal_prayer",
                "Se conserva como elemento propio de la celebración; las intenciones no se inventan cuando no existe un formulario verificado.");
    }

    private void readingBlock(MissalV5WordContent content, String id) {
        MissalV5Reading reading = content.reading(id);
        if (reading == null) return;
        MassElement element = MissalV5Semantic.element("word", id);
        String title = element == null || element.getTitle().trim().isEmpty()
                ? reading.getTitle() : element.getTitle();
        String body = reading.displayText();
        if (body.trim().isEmpty()) return;
        roleBlock("LECTURA", title, body);
    }

    private void renderEucharist() {
        heading(sectionTitle("Liturgia eucarística"));
        semanticBlock("gifts", "Rúbricas, invitaciones y respuestas se representarán con roles propios.");
        offeringsBody = semanticBlock("offerings", PENDING);
        semanticBlock("preface", "Se resolverá por celebración, tiempo litúrgico y formulario.");
        semanticBlock("eucharistic_prayer", "Elige una plegaria. La IV conservará su relación propia con el prefacio.");
        semanticSelectable("eucharistic_prayer", "Plegaria eucarística");
    }

    private void renderCommunion() {
        heading(sectionTitle("Rito de la comunión"));
        semanticBlock("our_father", "Texto, embolismo y respuesta se mantendrán como elementos semánticos separados.");
        semanticBlock("peace", "Rúbrica y fórmulas separadas.");
        semanticBlock("fraction", "Agnus Dei y gestos rituales como elementos distintos.");
        communionAntiphonBody = semanticBlock("communion_antiphon", PENDING);
        postCommunionBody = semanticBlock("post_communion", PENDING);
    }

    private void renderConclusion() {
        heading(sectionTitle("Rito de conclusión"));
        semanticBlock("blessing", "Bendición simple o solemne según corresponda.");
        semanticBlock("dismissal", "Fórmula de despedida y respuesta de la asamblea.");
    }

    private void renderOther() {
        heading(sectionTitle("Otros formularios"));
        block("Comunes", "Pastores, mártires, vírgenes, santos y santas.");
        block("Por diversas necesidades", "Formularios para la Iglesia, sociedad y necesidades particulares.");
        block("Misas votivas", "Formularios votivos organizados por tema.");
        block("Misas de difuntos", "Exequias, aniversarios y otras ocasiones.");
        block("Propio de los santos", "Santoral por fecha, aislado por celebración para evitar contaminaciones entre santos.");
    }

    private TextView semanticBlock(String elementId, String body) {
        MassElement element = MissalV5Semantic.element(sectionId, elementId);
        String title = element == null ? elementId : element.getTitle();
        MassElementType type = element == null ? MassElementType.PRAYER : element.getType();
        return roleBlock(roleLabel(type), title, body);
    }

    private String roleLabel(MassElementType type) {
        switch (type) {
            case RUBRIC: return "RÚBRICA";
            case CELEBRANT: return "CELEBRANTE";
            case ASSEMBLY: return "ASAMBLEA";
            case ANTIPHON: return "ANTÍFONA";
            case READING_REFERENCE: return "LECTURA";
            case OPTION: return "OPCIÓN";
            case PRAYER: return "ORACIÓN";
            default: return "ELEMENTO";
        }
    }

    private TextView roleBlock(String role, String title, String body) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);
        TextView roleView = text(role, 10, R.color.muted, true);
        roleView.setLetterSpacing(.10f);
        card.addView(roleView);
        TextView titleView = text(title, 18, R.color.wine, true);
        titleView.setPadding(0, dp(3), 0, 0);
        card.addView(titleView);
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

    private void actionCard(String label, Runnable action) {
        TextView button = text(label, 16, R.color.wine, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(13), dp(14), dp(13));
        button.setBackgroundResource(R.drawable.bg_button_secondary);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        root.addView(button, lp);
    }

    private void semanticSelectable(String elementId, String fallbackTitle) {
        MassElement element = MissalV5Semantic.element(sectionId, elementId);
        if (element == null || element.getOptions().isEmpty()) return;
        List<String> options = element.getOptions();
        String[] labels = options.toArray(new String[0]);
        String[] states = new String[labels.length];
        for (int i = 0; i < labels.length; i++) states[i] = element.getTitle() + " " + labels[i] + " seleccionada";
        selectable(element.getTitle().isEmpty() ? fallbackTitle : element.getTitle(), labels, states);
    }

    private String sectionTitle(String fallback) {
        return semanticSection == null || semanticSection.getTitle().trim().isEmpty() ? fallback : semanticSection.getTitle();
    }

    private void loadDailyProper() {
        DailyMassProperRepository.ProperDay cached = DailyMassProperRepository.cached(getApplicationContext(), selectedDate);
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
            DailyMassProperRepository.ProperDay proper = DailyMassProperRepository.getOrSync(getApplicationContext(), requestDate);
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

    private TextView block(String title, String body) { return roleBlock("SECCIÓN", title, body); }

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
        return new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "EC")).format(selectedDate.getTime());
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
