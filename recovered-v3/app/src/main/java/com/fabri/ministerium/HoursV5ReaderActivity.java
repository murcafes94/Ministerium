package com.fabri.ministerium;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native reader for Ministerium 5 Liturgia de las Horas. No WebView. */
public class HoursV5ReaderActivity extends ThemedActivity {
    public static final String EXTRA_VOLUME_ID = "v5_hours_reader_volume";
    public static final String EXTRA_FILE_PATH = "v5_hours_reader_file";
    public static final String EXTRA_FRAGMENT = "v5_hours_reader_fragment";
    public static final String EXTRA_TITLE = "v5_hours_reader_title";
    public static final String EXTRA_SUBTITLE = "v5_hours_reader_subtitle";
    public static final String EXTRA_SCROLL_TEXT = "v5_hours_reader_scroll";
    public static final String EXTRA_HOUR_KEY = "v5_hours_reader_hour_key";
    public static final String EXTRA_OFFICE_RANK = "v5_hours_reader_office_rank";
    public static final String EXTRA_TEMPORAL_VOLUME_ID = "v5_hours_reader_temporal_volume";
    public static final String EXTRA_TEMPORAL_FILE_PATH = "v5_hours_reader_temporal_file";
    public static final String EXTRA_TEMPORAL_FRAGMENT = "v5_hours_reader_temporal_fragment";
    public static final String EXTRA_TEMPORAL_SCROLL_TEXT = "v5_hours_reader_temporal_scroll";
    public static final String EXTRA_COMMON_VOLUME_ID = "v5_hours_reader_common_volume";
    public static final String EXTRA_COMMON_FILE_PATH = "v5_hours_reader_common_file";
    public static final String EXTRA_COMMON_FRAGMENT = "v5_hours_reader_common_fragment";
    public static final String EXTRA_COMMON_TITLE = "v5_hours_reader_common_title";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private ProgressBar progress;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        loadDocument();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.cream));

        LinearLayout root = column();
        root.setPadding(dp(22), dp(18), dp(22), dp(34));
        scroll.addView(root);

        String title = value(EXTRA_TITLE, "Liturgia de las Horas");
        TextView back = text("‹  " + title, 23, R.color.wine, true);
        back.setPadding(0, dp(6), 0, dp(5));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        String subtitle = value(EXTRA_SUBTITLE, "");
        if (!subtitle.isEmpty()) {
            TextView sub = text(subtitle, 13, R.color.muted, false);
            sub.setPadding(0, 0, 0, dp(12));
            root.addView(sub);
        }

        status = text("Preparando el oficio…", 12, R.color.muted, false);
        status.setPadding(0, 0, 0, dp(14));
        root.addView(status);

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(32), dp(32));
        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.setMargins(0, dp(6), 0, dp(16));
        root.addView(progress, p);

        content = column();
        root.addView(content);
        return scroll;
    }

    private void loadDocument() {
        final String volumeId = value(EXTRA_VOLUME_ID, "");
        final String filePath = value(EXTRA_FILE_PATH, "");
        final String fragment = value(EXTRA_FRAGMENT, "");
        final String title = value(EXTRA_TITLE, "Liturgia de las Horas");
        final String scrollText = value(EXTRA_SCROLL_TEXT, "");
        final String hourKey = value(EXTRA_HOUR_KEY, "");
        final String rank = value(EXTRA_OFFICE_RANK, "");
        final String temporalVolume = value(EXTRA_TEMPORAL_VOLUME_ID, "");
        final String temporalFile = value(EXTRA_TEMPORAL_FILE_PATH, "");
        final String temporalFragment = value(EXTRA_TEMPORAL_FRAGMENT, "");
        final String temporalScroll = value(EXTRA_TEMPORAL_SCROLL_TEXT, "");
        final String commonVolume = value(EXTRA_COMMON_VOLUME_ID, "");
        final String commonFile = value(EXTRA_COMMON_FILE_PATH, "");
        final String commonFragment = value(EXTRA_COMMON_FRAGMENT, "");
        final String commonTitle = value(EXTRA_COMMON_TITLE, "Común");

        executor.submit(() -> {
            try {
                HoursNativeDocument primary = loadNativeDocument(
                        volumeId, filePath, fragment, title, scrollText);
                HoursNativeDocument document = primary;
                boolean composed = false;

                HoursNativeDocument temporal = null;
                if (!temporalVolume.isEmpty() && !temporalFile.isEmpty()) {
                    temporal = loadNativeDocument(
                            temporalVolume, temporalFile, temporalFragment, title, temporalScroll);
                }

                HoursNativeDocument common = null;
                if (!commonVolume.isEmpty() && !commonFile.isEmpty()) {
                    common = loadNativeDocument(
                            commonVolume, commonFile, commonFragment, commonTitle, "");
                }

                if (!rank.isEmpty() && (temporal != null || common != null)) {
                    document = HoursV5Composition.compose(hourKey, rank, temporal, primary, common);
                    composed = true;
                }

                final HoursNativeDocument finalDocument = document;
                final boolean finalComposed = composed;
                final boolean usedCommon = common != null;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    progress.setVisibility(View.GONE);
                    if (finalDocument.getBlocks().isEmpty()) {
                        status.setText("No se encontró contenido estructurado para este oficio.");
                        addEmptyState();
                        return;
                    }
                    if (finalComposed) {
                        status.setText(usedCommon
                                ? "Texto local · temporal/propio/común con procedencia explícita"
                                : "Texto local · composición litúrgica semántica controlada");
                    } else {
                        status.setText("Texto local · lector nativo");
                    }
                    render(finalDocument);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    progress.setVisibility(View.GONE);
                    status.setText("No se pudo abrir este oficio sin conexión.");
                    roleBlock("ESTADO", "Contenido no disponible",
                            "Ministerium no sustituirá este oficio con contenido de otra celebración.");
                });
            }
        });
    }

    private HoursNativeDocument loadNativeDocument(String volumeId, String filePath,
                                                   String fragment, String title,
                                                   String scrollText) throws Exception {
        HoursVolume volume = HoursRepository.find(volumeId);
        if (volume == null || filePath.isEmpty()) {
            throw new IllegalStateException("Ruta del oficio no disponible.");
        }
        File root = EpubUtils.ensureExtracted(getApplicationContext(), volume);
        File file = new File(root, filePath);
        if (!file.isFile()) throw new IllegalStateException("El texto del oficio no está instalado.");
        String html = read(file);
        return HoursV5DocumentParser.parse(title, html, fragment, scrollText);
    }

    private void render(HoursNativeDocument document) {
        content.removeAllViews();
        for (HoursBlock block : document.getBlocks()) {
            String role = role(block.getType());
            String title = block.getTitle();
            String body = block.getBody();
            if (title.isEmpty() && body.isEmpty()) continue;
            roleBlock(role, title, body);
        }
    }

    private String role(HoursBlockType type) {
        switch (type) {
            case HYMN: return "HIMNO";
            case ANTIPHON: return "ANTÍFONA";
            case PSALMODY: return "SALMODIA";
            case READING: return "LECTURA";
            case RESPONSORY: return "RESPONSORIO";
            case CANTICLE: return "CÁNTICO";
            case INTERCESSIONS: return "PRECES";
            case PRAYER: return "ORACIÓN";
            case HEADING: return "SECCIÓN";
            default: return "TEXTO";
        }
    }

    private void roleBlock(String role, String title, String body) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_button_secondary);

        TextView roleView = text(role, 10, R.color.muted, true);
        roleView.setLetterSpacing(.10f);
        card.addView(roleView);

        if (title != null && !title.trim().isEmpty()) {
            TextView titleView = text(title.trim(), 18, R.color.wine, true);
            titleView.setPadding(0, dp(3), 0, 0);
            card.addView(titleView);
        }

        if (body != null && !body.trim().isEmpty()) {
            TextView bodyView = text(body.trim(), 16, R.color.ink, false);
            bodyView.setPadding(0, dp(7), 0, 0);
            bodyView.setLineSpacing(0, 1.18f);
            bodyView.setTextIsSelectable(true);
            card.addView(bodyView);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        content.addView(card, lp);
    }

    private void addEmptyState() {
        roleBlock("ESTADO", "Oficio sin contenido legible",
                "La estructura se mantuvo aislada para evitar mezclar temporal, santoral o comunes incorrectos.");
    }

    private String read(File file) throws Exception {
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String value(String key, String fallback) {
        String value = getIntent().getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
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
