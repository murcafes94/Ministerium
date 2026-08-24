package com.fabri.ministerium;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Ajustes persistentes de la experiencia de lectura. */
public class ReaderSettingsActivity extends ThemedActivity {
    private SeekBar size;
    private SeekBar weight;
    private SeekBar line;
    private TextView preview;
    private Switch prayerFocus;
    private TextView prayerFocusStatus;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_reader_settings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        size = findViewById(R.id.seekReaderSize);
        weight = findViewById(R.id.seekReaderWeight);
        line = findViewById(R.id.seekReaderLine);
        preview = findViewById(R.id.txtReaderPreview);
        prayerFocus = findViewById(R.id.switchPrayerFocus);
        prayerFocusStatus = findViewById(R.id.txtPrayerFocusStatus);

        RadioGroup family = findViewById(R.id.readerFamilyGroup);
        String selected = ReaderPreferences.family(this);
        family.check(ReaderPreferences.SANS.equals(selected) ? R.id.readerSans
                : ReaderPreferences.MONO.equals(selected) ? R.id.readerMono
                : R.id.readerSerif);
        family.setOnCheckedChangeListener((group, id) -> {
            ReaderPreferences.setFamily(this, id == R.id.readerSans
                    ? ReaderPreferences.SANS : id == R.id.readerMono
                    ? ReaderPreferences.MONO : ReaderPreferences.SERIF);
            refresh();
        });

        RadioGroup margins = findViewById(R.id.readerMarginGroup);
        String margin = ReaderPreferences.margin(this);
        margins.check(ReaderPreferences.MARGIN_WIDE.equals(margin) ? R.id.readerMarginWide
                : ReaderPreferences.MARGIN_NARROW.equals(margin) ? R.id.readerMarginNarrow
                : R.id.readerMarginStandard);
        margins.setOnCheckedChangeListener((group, id) -> {
            ReaderPreferences.setMargin(this, id == R.id.readerMarginWide
                    ? ReaderPreferences.MARGIN_WIDE : id == R.id.readerMarginNarrow
                    ? ReaderPreferences.MARGIN_NARROW : ReaderPreferences.MARGIN_STANDARD);
            refresh();
        });

        size.setMax(100);
        size.setProgress(ReaderPreferences.textZoom(this) - 80);
        weight.setMax(4);
        weight.setProgress((ReaderPreferences.weight(this) - 300) / 100);
        line.setMax(17);
        line.setProgress(Math.round((ReaderPreferences.lineHeight(this) - 1.25f) * 20));
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                ReaderPreferences.setTextZoom(ReaderSettingsActivity.this,
                        size.getProgress() + 80);
                ReaderPreferences.setWeight(ReaderSettingsActivity.this,
                        weight.getProgress() * 100 + 300);
                ReaderPreferences.setLineHeight(ReaderSettingsActivity.this,
                        line.getProgress() / 20f + 1.25f);
                refresh();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        };
        size.setOnSeekBarChangeListener(listener);
        weight.setOnSeekBarChangeListener(listener);
        line.setOnSeekBarChangeListener(listener);
        findViewById(R.id.btnReaderReset).setOnClickListener(v -> {
            ReaderPreferences.reset(this);
            recreate();
        });

        prayerFocus.setChecked(PrayerFocusController.isEnabled(this));
        prayerFocus.setOnCheckedChangeListener((button, enabled) -> {
            PrayerFocusController.setEnabled(this, enabled);
            if (enabled && !PrayerFocusController.hasPolicyAccess(this)) {
                Toast.makeText(this,
                        "Autoriza a Ministerium para usar No molestar. Android abrirá la configuración del sistema.",
                        Toast.LENGTH_LONG).show();
                PrayerFocusController.requestPolicyAccess(this);
            }
            refreshPrayerFocus();
        });
        refresh();
        refreshPrayerFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prayerFocus != null) {
            prayerFocus.setChecked(PrayerFocusController.isEnabled(this));
            refreshPrayerFocus();
        }
    }

    private void refreshPrayerFocus() {
        if (prayerFocusStatus == null) return;
        boolean enabled = PrayerFocusController.isEnabled(this);
        boolean access = PrayerFocusController.hasPolicyAccess(this);
        prayerFocusStatus.setText(!enabled
                ? "Desactivado. No se modifica el modo de sonido del dispositivo."
                : access
                ? "Activo. Se usa en Liturgia, Misa, Ritual y sesiones del plan bíblico; al salir se restaura el estado anterior."
                : "Activado en Ministerium, pero falta autorizar el acceso a No molestar en Android. Toca el interruptor para abrir el permiso.");
    }

    private void refresh() {
        preview.setTextSize(16f * ReaderPreferences.textZoom(this) / 110f);
        preview.setTypeface(android.graphics.Typeface.create(
                ReaderPreferences.family(this), ReaderPreferences.weight(this) >= 600
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        preview.setLineSpacing(0, ReaderPreferences.lineHeight(this));
        int horizontal = Math.round(ReaderPreferences.horizontalPaddingPx(this)
                * getResources().getDisplayMetrics().density / 2f);
        int vertical = Math.round(16 * getResources().getDisplayMetrics().density);
        preview.setPadding(horizontal, vertical, horizontal, vertical);
        String margin = ReaderPreferences.margin(this);
        String marginLabel = ReaderPreferences.MARGIN_WIDE.equals(margin) ? "margen amplio"
                : ReaderPreferences.MARGIN_NARROW.equals(margin) ? "margen estrecho"
                : "margen estándar";
        ((TextView) findViewById(R.id.txtReaderValues)).setText(String.format(Locale.US,
                "%d%% · grosor %d · interlineado %.2f · %s",
                ReaderPreferences.textZoom(this), ReaderPreferences.weight(this),
                ReaderPreferences.lineHeight(this), marginLabel));
    }
}
