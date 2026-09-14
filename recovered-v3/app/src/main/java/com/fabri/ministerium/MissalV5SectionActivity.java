package com.fabri.ministerium;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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

    private static final String PROPER_UNAVAILABLE =
            "Propio no disponible localmente para esta celebración. Ministerium no mostrará un formulario supuesto.";
    private static final String MISSAL_UNAVAILABLE =
            "El paquete local verificado del Misal no está disponible. Vuelve a compilar o actualizar el contenido de Ministerium.";

    private final TextView[] prayerButtons = new TextView[4];
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
    private TextView communionRiteBody;
    private TextView postCommunionBody;
    private TextView eucharisticPrayerBody;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        selectedDate = selectedDate();
        sectionId = value(EXTRA_SECTION, "initial");
        celebration = value(EXTRA_CELEBRATION, "Celebración del día");
        semanticSection = MissalV5Semantic.section(sectionId);
        presentation = MissalDisplayRules.resolve(this, selectedDate, celebration);
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

        sourceStatus = text("word".equals(sectionId)
                ? "Lecturas: comprobando contenido local…"
                : "Propios: comprobando contenido local…", 12, R.color.muted, false);
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
        entranceBody = semanticBlock("entrance_antiphon", PROPER_UNAVAILABLE);

        String ordinary = missalComponent("initial");
        if (!presentation.getShowGloria()) ordinary = omitGloria(ordinary);
        roleBlock("ORDINARIO", "Ritos iniciales · edición de México", ordinary);

        collectBody = semanticBlock("collect", PROPER_UNAVAILABLE);
    }

    private void renderWord() {
        heading(sectionTitle("Liturgia de la Palabra"));
        MissalV5WordContent content = MissalV5Readings.load(getApplicationContext(), selectedDate);
        if (content == null) {
            sourceStatus.setText("Lecturas: esta fecha no está sincronizada en el dispositivo");
            roleBlock("LECTURAS", "Contenido no disponible sin conexión",
                    "Ministerium no mostrará lecturas supuestas. Sincroniza el Leccionario desde Ajustes → Actualizaciones y vuelve a abrir esta fecha.");
            actionCard("Abrir Actualizaciones", () ->
                    startActivity(new Intent(this, UpdateCenterActivity.class)));
            return;
        }

        sourceStatus.setText("Lecturas: " + content.getSourceLabel());
        readingBlock(content, "first_reading");
        readingBlock(content, "psalm");
        if (presentation.getShowSecondReading()) readingBlock(content, "second_reading");
        readingBlock(content, "acclamation");
        readingBlock(content, "gospel");

        if (presentation.getShowCreed()) {
            semanticBlock("creed", assetText("prayers/credo_niceno.txt",
                    "El Credo corresponde a esta celebración, pero su texto local no pudo abrirse."));
        }
        semanticBlock("universal_prayer",
                "Se hace la oración universal según la celebración y las necesidades de la Iglesia y del mundo. Ministerium no genera intenciones litúrgicas cuando no existe un formulario verificado.");
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
        roleBlock("ORDINARIO", "Preparación de los dones",
                missalHtml(() -> LiturgiaPapalMissalRepository.preparationHtml(this, "es")));
        offeringsBody = semanticBlock("offerings", PROPER_UNAVAILABLE);
        roleBlock("CELEBRANTE Y ASAMBLEA", "Diálogo del prefacio",
                missalHtml(() -> LiturgiaPapalMissalRepository.prefaceDialogueHtml(this, "es")));
        eucharisticPrayerSelector();
    }

    private void renderCommunion() {
        heading(sectionTitle("Rito de la comunión"));
        communionRiteBody = roleBlock("ORDINARIO", "Rito de la comunión", communionText(null));
        postCommunionBody = semanticBlock("post_communion", PROPER_UNAVAILABLE);
    }

    private void renderConclusion() {
        heading(sectionTitle("Rito de conclusión"));
        roleBlock("ORDINARIO", "Bendición y despedida",
                missalHtml(() -> LiturgiaPapalMissalRepository.conclusionHtml(this, "es")));
    }

    private void renderOther() {
        heading(sectionTitle("Otros formularios"));
        roleBlock("FORMULARIOS", "Índice nativo en transición",
                "Los formularios de comunes, necesidades, votivas, difuntos y santoral aún no tienen un índice nativo V5 completo. Para evitar botones simulados o formularios incompletos, esta pantalla abre el Misal completo ya funcional.");
        actionCard("Abrir formularios del Misal completo", () ->
                startActivity(new Intent(this, MissalActivity.class)));
    }

    private TextView semanticBlock(String elementId, CharSequence body) {
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

    private TextView roleBlock(String role, String title, CharSequence body) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);
        TextView roleView = text(role, 10, R.color.muted, true);
        roleView.setLetterSpacing(.10f);
        card.addView(roleView);
        TextView titleView = text(title, 18, R.color.wine, true);
        titleView.setPadding(0, dp(3), 0, 0);
        card.addView(titleView);
        TextView value = text("", 15, R.color.ink, false);
        value.setText(body == null ? "" : body);
        value.setPadding(0, dp(6), 0, 0);
        value.setLineSpacing(0, 1.12f);
        value.setTextIsSelectable(true);
        card.addView(value);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        root.addView(card, lp);
        return value;
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

    private void eucharisticPrayerSelector() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        String[] labels = {"I", "II", "III", "IV"};
        for (int i = 0; i < labels.length; i++) {
            final int prayer = i + 1;
            TextView button = text(labels[i], 16, R.color.wine, true);
            prayerButtons[i] = button;
            button.setGravity(Gravity.CENTER);
            button.setBackgroundResource(R.drawable.bg_button_secondary);
            boolean enabled = prayer != 4 || presentation.getAllowEucharisticPrayerIV();
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1f : .42f);
            if (enabled) button.setOnClickListener(v -> showEucharisticPrayer(prayer));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(button, lp);
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        root.addView(row, rowLp);

        if (!presentation.getAllowEucharisticPrayerIV()) {
            TextView note = text(
                    "La Plegaria Eucarística IV no se ofrece en esta celebración porque posee prefacio propio e invariable.",
                    12, R.color.muted, false);
            note.setPadding(dp(2), 0, dp(2), dp(10));
            root.addView(note);
        }

        eucharisticPrayerBody = roleBlock("PLEGARIA EUCARÍSTICA", "Plegaria eucarística II",
                eucharisticPrayerText(2));
        updatePrayerButtons(2);
    }

    private void showEucharisticPrayer(int number) {
        if (number == 4 && !presentation.getAllowEucharisticPrayerIV()) return;
        if (eucharisticPrayerBody == null) return;
        eucharisticPrayerBody.setText(eucharisticPrayerText(number));
        View parent = (View) eucharisticPrayerBody.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout card = (LinearLayout) parent;
            if (card.getChildCount() > 1 && card.getChildAt(1) instanceof TextView) {
                ((TextView) card.getChildAt(1)).setText("Plegaria eucarística " + roman(number));
            }
        }
        updatePrayerButtons(number);
    }

    private void updatePrayerButtons(int selected) {
        for (int i = 0; i < prayerButtons.length; i++) {
            TextView button = prayerButtons[i];
            if (button == null) continue;
            boolean active = i + 1 == selected;
            button.setTypeface(button.getTypeface(), active ? Typeface.BOLD : Typeface.NORMAL);
            button.setContentDescription("Plegaria eucarística " + roman(i + 1)
                    + (active ? ", seleccionada" : ""));
        }
    }

    private CharSequence eucharisticPrayerText(int number) {
        return missalHtml(() -> LiturgiaPapalMissalRepository.eucharisticPrayerHtml(this, "es", number));
    }

    private CharSequence communionText(String antiphon) {
        String antiphonHtml = "";
        if (antiphon != null && !antiphon.trim().isEmpty()) {
            antiphonHtml = "<p><b>Antífona de la comunión</b><br>"
                    + Html.escapeHtml(antiphon.trim()).replace("\n", "<br>") + "</p>";
        }
        final String properHtml = antiphonHtml;
        return missalHtml(() -> LiturgiaPapalMissalRepository.communionHtml(this, "es", properHtml));
    }

    private CharSequence missalHtml(MissalHtmlSupplier supplier) {
        try {
            if (!LiturgiaPapalMissalRepository.isAvailable(this, "es")) return MISSAL_UNAVAILABLE;
            String html = supplier.get();
            if (html == null || html.trim().isEmpty()) return MISSAL_UNAVAILABLE;
            return fromHtml(html);
        } catch (Exception error) {
            return MISSAL_UNAVAILABLE;
        }
    }

    private String missalComponent(String id) {
        try {
            if (!LiturgiaPapalMissalRepository.isAvailable(this, "es")) return MISSAL_UNAVAILABLE;
            String value = LiturgiaPapalMissalRepository.component(this, "es", id);
            return value == null || value.trim().isEmpty() ? MISSAL_UNAVAILABLE : value.trim();
        } catch (Exception error) {
            return MISSAL_UNAVAILABLE;
        }
    }

    private String omitGloria(String initial) {
        if (initial == null || initial.equals(MISSAL_UNAVAILABLE)) return initial;
        String lower = initial.toLowerCase(new Locale("es", "EC"));
        int start = lower.indexOf("gloria a dios en el cielo");
        if (start < 0) return initial;
        int end = lower.indexOf("acabado el himno", start);
        if (end < 0) end = lower.indexOf("oremos", start);
        if (end < 0 || end <= start) return initial;
        return (initial.substring(0, start)
                + "En esta celebración se omite el Gloria.\n\n"
                + initial.substring(end)).trim();
    }

    private void loadDailyProper() {
        final boolean localOrdinary = applyLocalOrdinaryProper();
        DailyMassProperRepository.ProperDay cached =
                DailyMassProperRepository.cached(getApplicationContext(), selectedDate);
        if (cached != null) {
            applyProper(cached);
            sourceStatus.setText("Propios: Arquidiócesis de Guadalajara · caché local");
            if (cached.isComplete()) return;
        } else if (localOrdinary) {
            sourceStatus.setText("Propios: Misal local verificado · Tiempo Ordinario");
        } else {
            sourceStatus.setText(MassReadingsRepository.isCurrentMonth(selectedDate)
                    ? "Propios: buscando fuente verificada…"
                    : "Propios: no disponibles localmente para esta fecha");
        }

        if (!MassReadingsRepository.isCurrentMonth(selectedDate)) return;
        final Calendar requestDate = (Calendar) selectedDate.clone();
        new Thread(() -> {
            DailyMassProperRepository.ProperDay proper =
                    DailyMassProperRepository.getOrSync(getApplicationContext(), requestDate);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (proper == null) {
                    sourceStatus.setText(localOrdinary
                            ? "Propios: Misal local verificado · sin actualización de red"
                            : "Propios: fuente verificada no disponible; no se mostrará texto supuesto");
                    return;
                }
                applyProper(proper);
                sourceStatus.setText("Propios: Arquidiócesis de Guadalajara · guardados localmente");
            });
        }, "ministerium-v5-proper").start();
    }

    private boolean applyLocalOrdinaryProper() {
        if (!mayUseOrdinaryProper()) return false;
        try {
            String entrance = localOrdinaryPart(LiturgiaPapalMissalRepository.ENTRANCE);
            String collect = localOrdinaryPart(LiturgiaPapalMissalRepository.COLLECT);
            String offerings = localOrdinaryPart(LiturgiaPapalMissalRepository.OFFERINGS);
            String communion = localOrdinaryPart(LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON);
            String post = localOrdinaryPart(LiturgiaPapalMissalRepository.POST_COMMUNION);
            boolean any = false;
            any |= setProperText(entranceBody, entrance);
            any |= setProperText(collectBody, collect);
            any |= setProperText(offeringsBody, offerings);
            if (communionRiteBody != null && communion != null && !communion.trim().isEmpty()) {
                communionRiteBody.setText(communionText(communion));
                any = true;
            }
            any |= setProperText(postCommunionBody, post);
            return any;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean mayUseOrdinaryProper() {
        if (LiturgicalResolver.ordinaryWeekNumber(selectedDate) <= 0) return false;
        if (selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true;
        try {
            LiturgicalEvent primary = LiturgicalResolver.primaryEvent(
                    LiturgicalCalendarRepository.eventsFor(this, selectedDate));
            return primary == null || (!primary.isMandatoryMemorial()
                    && !primary.isFeast() && !primary.isSolemnity());
        } catch (Exception ignored) {
            String normalized = normalize(celebration);
            return normalized.contains("feria") || normalized.contains("tiempo ordinario");
        }
    }

    private String localOrdinaryPart(String part) throws Exception {
        String html = LiturgiaPapalMissalRepository.ordinaryProperPartHtml(this, selectedDate, part);
        if (html == null || html.trim().isEmpty()) return "";
        return fromHtml(html).toString().trim();
    }

    private void applyProper(DailyMassProperRepository.ProperDay proper) {
        if (proper == null) return;
        setProperText(entranceBody, proper.entrance);
        setProperText(collectBody, proper.collect);
        setProperText(offeringsBody, proper.offerings);
        if (communionRiteBody != null && proper.communionAntiphon != null
                && !proper.communionAntiphon.trim().isEmpty()) {
            communionRiteBody.setText(communionText(proper.communionAntiphon));
        }
        setProperText(postCommunionBody, proper.postCommunion);
    }

    private boolean setProperText(TextView view, String value) {
        if (view == null) return false;
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return false;
        view.setText(clean);
        return true;
    }

    private String assetText(String path, String fallback) {
        try (InputStream input = getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String value = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @SuppressWarnings("deprecation")
    private CharSequence fromHtml(String html) {
        Spanned spanned = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                : Html.fromHtml(html);
        return spanned == null ? "" : spanned;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String roman(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return String.valueOf(number);
        }
    }

    private String sectionTitle(String fallback) {
        return semanticSection == null || semanticSection.getTitle().trim().isEmpty()
                ? fallback : semanticSection.getTitle();
    }

    private void heading(String value) {
        TextView title = text(value, 28, R.color.ink, true);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);
    }

    private Calendar selectedDate() {
        Calendar now = Calendar.getInstance();
        Calendar value = Calendar.getInstance();
        value.clear();
        value.set(getIntent().getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR)),
                getIntent().getIntExtra(EXTRA_MONTH, now.get(Calendar.MONTH)),
                getIntent().getIntExtra(EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH)), 12, 0, 0);
        return value;
    }

    private String dayLabel() {
        return new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "EC"))
                .format(selectedDate.getTime());
    }

    private String value(String key, String fallback) {
        String raw = getIntent().getStringExtra(key);
        return raw == null || raw.trim().isEmpty() ? fallback : raw;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return layout;
    }

    private TextView text(String value, int sp, int colorRes, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color(colorRes));
        if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }

    @SuppressWarnings("deprecation")
    private int color(int res) { return getResources().getColor(res); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private interface MissalHtmlSupplier {
        String get() throws Exception;
    }
}
