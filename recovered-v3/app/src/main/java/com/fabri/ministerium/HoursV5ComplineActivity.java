package com.fabri.ministerium;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Native semantic reader for Compline. No WebView and no EPUB rendering. */
public class HoursV5ComplineActivity extends ThemedActivity {
    public static final String EXTRA_YEAR = "v5_compline_year";
    public static final String EXTRA_MONTH = "v5_compline_month";
    public static final String EXTRA_DAY = "v5_compline_day";

    private LinearLayout content;
    private Calendar selectedDate;
    private JSONArray hymns = new JSONArray();
    private int hymnIndex = 0;
    private LinearLayout hymnContainer;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        selectedDate = selectedDate();
        setContentView(buildScreen());
        load();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));
        content = column();
        content.setPadding(dp(22), dp(18), dp(22), dp(34));
        scroll.addView(content);

        TextView back = text("‹  Completas", 23, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(4));
        back.setOnClickListener(v -> finish());
        content.addView(back);

        TextView date = text(dateLabel(), 13, R.color.muted, false);
        date.setPadding(0, 0, 0, dp(14));
        content.addView(date);
        return scroll;
    }

    private void load() {
        try {
            JSONObject data = ComplineContentRepository.load(this);
            JSONObject form = ComplineContentRepository.formForDay(data,
                    selectedDate.get(Calendar.DAY_OF_WEEK));
            if (form == null) throw new IllegalStateException("Formulario ausente");

            LiturgicalDay day = LiturgicalResolver.resolve(this, selectedDate);
            String season = day.temporalOffice == null || day.temporalOffice.volume == null
                    ? "ordinary" : day.temporalOffice.volume.id;
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(selectedDate);
            data.put("_ordinaryWeek", ordinaryWeek);

            String volume = ComplineContentRepository.liturgicalVolume(season, ordinaryWeek);
            roleBlock("CELEBRACIÓN", day.celebration,
                    "Tomo " + volume + (ordinaryWeek > 0 ? " · Semana " + ordinaryWeek : "")
                            + " · " + form.optString("title", "Completas"));

            JSONObject invocation = ComplineContentRepository.invocation(data);
            String invocationBody = joinNonEmpty(invocation.optString("verse", ""),
                    invocation.optString("response", ""), invocation.optString("doxology", ""));
            if (!"lent".equals(ComplineContentRepository.normalizeSeason(season))) {
                invocationBody = joinNonEmpty(invocationBody,
                        invocation.optString("alleluiaOutsideLent", ""));
            }
            roleBlock("INVOCACIÓN", "Invocación inicial", invocationBody);

            roleBlock("RÚBRICA", "Examen de conciencia",
                    ComplineContentRepository.examinationRubric(data));
            renderPenance(data);

            hymns = ComplineContentRepository.hymnsForSeason(data, season);
            renderHymn();
            renderKindArray("SALMODIA", "Salmodia", form.optJSONArray("psalmody"));

            JSONObject reading = form.optJSONObject("shortReading");
            if (reading != null) roleBlock("LECTURA", reading.optString("reference", "Lectura breve"),
                    reading.optString("text", ""));
            renderStringArray("RESPONSORIO", "Responsorio breve", form.optJSONArray("responsory"));
            renderKindArray("CÁNTICO", "Cántico evangélico", form.optJSONArray("gospelCanticle"));
            roleBlock("ORACIÓN", "Oración conclusiva", form.optString("prayer", ""));
            renderStringArray("CONCLUSIÓN", "Bendición", form.optJSONArray("conclusion"));

            JSONArray marian = ComplineContentRepository.marianAntiphons(data,
                    "easter".equals(ComplineContentRepository.normalizeSeason(season)));
            if (marian.length() > 0) {
                JSONObject item = marian.optJSONObject(0);
                if (item != null) roleBlock("ANTÍFONA MARIANA",
                        item.optString("title", "Antífona final"), item.optString("text", ""));
            }
        } catch (Exception error) {
            roleBlock("ESTADO", "Completas no disponibles",
                    "El paquete semántico local no pudo resolverse. No se cargará contenido de otro día como sustitución.");
        }
    }

    private void renderPenance(JSONObject data) {
        JSONArray formulas = ComplineContentRepository.penitentialFormulas(data);
        if (formulas.length() == 0) return;
        JSONObject first = formulas.optJSONObject(0);
        roleBlock("ACTO PENITENCIAL", first == null ? "Acto penitencial" : first.optString("title", "Acto penitencial"),
                first == null ? "" : first.optString("text", ""));
        String conclusion = ComplineContentRepository.penitentialConclusion(data);
        if (!conclusion.isEmpty()) roleBlock("ORACIÓN", "Conclusión penitencial", conclusion);
    }

    private void renderHymn() {
        if (hymnContainer != null) content.removeView(hymnContainer);
        hymnContainer = column();
        int insertion = Math.min(4, content.getChildCount());
        content.addView(hymnContainer, insertion);
        if (hymns.length() == 0) return;

        JSONObject hymn = hymns.optJSONObject(Math.max(0, Math.min(hymnIndex, hymns.length() - 1)));
        if (hymn == null) return;
        addRoleBlock(hymnContainer, "HIMNO", hymn.optString("title", "Himno"), hymn.optString("text", ""));
        if (hymns.length() > 1) {
            TextView change = text("Cambiar himno", 14, R.color.wine, true);
            change.setGravity(Gravity.CENTER);
            change.setPadding(dp(12), dp(11), dp(12), dp(11));
            change.setBackgroundResource(R.drawable.bg_button_secondary);
            change.setOnClickListener(v -> {
                hymnIndex = (hymnIndex + 1) % hymns.length();
                renderHymn();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, dp(10));
            hymnContainer.addView(change, lp);
        }
    }

    private void renderKindArray(String role, String title, JSONArray array) {
        if (array == null || array.length() == 0) return;
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String text = item.optString("text", "").trim();
            if (text.isEmpty()) continue;
            if (body.length() > 0) body.append("\n\n");
            body.append(text);
        }
        roleBlock(role, title, body.toString());
    }

    private void renderStringArray(String role, String title, JSONArray array) {
        if (array == null || array.length() == 0) return;
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (value.isEmpty()) continue;
            if (body.length() > 0) body.append("\n");
            body.append(value);
        }
        roleBlock(role, title, body.toString());
    }

    private void roleBlock(String role, String title, String body) {
        addRoleBlock(content, role, title, body);
    }

    private void addRoleBlock(LinearLayout parent, String role, String title, String body) {
        if ((title == null || title.trim().isEmpty()) && (body == null || body.trim().isEmpty())) return;
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);
        TextView roleView = text(role, 10, R.color.muted, true);
        roleView.setLetterSpacing(.10f);
        card.addView(roleView);
        if (title != null && !title.trim().isEmpty()) {
            TextView t = text(title.trim(), 18, R.color.wine, true);
            t.setPadding(0, dp(3), 0, 0);
            card.addView(t);
        }
        if (body != null && !body.trim().isEmpty()) {
            TextView b = text(body.trim(), 16, R.color.ink, false);
            b.setPadding(0, dp(7), 0, 0);
            b.setLineSpacing(0, 1.18f);
            b.setTextIsSelectable(true);
            card.addView(b);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        parent.addView(card, lp);
    }

    private String joinNonEmpty(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (result.length() > 0) result.append("\n");
            result.append(value.trim());
        }
        return result.toString();
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

    private String dateLabel() {
        return new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "EC"))
                .format(selectedDate.getTime());
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
