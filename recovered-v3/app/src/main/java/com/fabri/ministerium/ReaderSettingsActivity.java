package com.fabri.ministerium;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** Ajustes globales y persistentes de todos los lectores. */
public class ReaderSettingsActivity extends ThemedActivity {
    private SeekBar size;
    private SeekBar weight;
    private SeekBar line;
    private TextView preview;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_reader_settings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        size = findViewById(R.id.seekReaderSize);
        weight = findViewById(R.id.seekReaderWeight);
        line = findViewById(R.id.seekReaderLine);
        preview = findViewById(R.id.txtReaderPreview);
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
        refresh();
    }

    private void refresh() {
        preview.setTextSize(16f * ReaderPreferences.textZoom(this) / 110f);
        preview.setTypeface(android.graphics.Typeface.create(
                ReaderPreferences.family(this), ReaderPreferences.weight(this) >= 600
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        preview.setLineSpacing(0, ReaderPreferences.lineHeight(this));
        ((TextView) findViewById(R.id.txtReaderValues)).setText(String.format(Locale.US,
                "%d%% · grosor %d · interlineado %.2f",
                ReaderPreferences.textZoom(this), ReaderPreferences.weight(this),
                ReaderPreferences.lineHeight(this)));
    }
}
