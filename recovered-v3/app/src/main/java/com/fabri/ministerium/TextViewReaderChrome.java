package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;

/** Contrato 3.0 para lectores basados en TextView. */
public final class TextViewReaderChrome {
    private static final int HIGHLIGHT = 9401, NOTE = 9402, MEDITATION = 9403,
            DICTIONARY = 9404, TRANSLATE = 9405, READ = 9406;

    private TextViewReaderChrome() {}

    public static void attach(Activity activity, TextView content, ScrollView scroll,
                              View header, ReaderContext context,
                              ReaderChrome.Navigator navigator) {
        applyPreferences(activity, content);
        restoreHighlights(activity, content, context.sourceKey);
        attachSelection(activity, content, context);
        attachGestures(activity, content, navigator);
        if (scroll != null && header != null) {
            scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
                if (y - oldY > 12 && y > header.getHeight()) {
                    header.animate().translationY(-header.getHeight()).alpha(.15f)
                            .setDuration(170).start();
                } else if (y - oldY < -8 || y < header.getHeight()) {
                    header.animate().translationY(0).alpha(1f).setDuration(150).start();
                }
            });
        }
    }

    public static void bindMore(Activity activity, View button, TextView content,
                                ReaderContext context) {
        if (button == null) return;
        button.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(context.title)
                .setItems(new String[]{"Leer en voz alta", "Mesa de estudio", "Compartir"},
                        (dialog, which) -> {
                            if (which == 0) ReaderTtsController.speakSelection(activity,
                                    content.getText().toString());
                            else if (which == 1) activity.startActivity(new Intent(activity,
                                    StudyDeskActivity.class).putExtra(
                                    StudyDeskActivity.EXTRA_QUERY, context.reference));
                            else activity.startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_SEND).setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT, context.title + "\n"
                                                    + context.reference), "Compartir"));
                        }).show());
    }

    private static void applyPreferences(Activity activity, TextView content) {
        content.setTextSize(17f * ReaderPreferences.textZoom(activity) / 110f);
        content.setTypeface(Typeface.create(ReaderPreferences.family(activity),
                ReaderPreferences.weight(activity) >= 600 ? Typeface.BOLD : Typeface.NORMAL));
        content.setLineSpacing(0, ReaderPreferences.lineHeight(activity));
    }

    private static void attachGestures(Activity activity, TextView content,
                                       ReaderChrome.Navigator navigator) {
        final float[] accumulated = {1f};
        ScaleGestureDetector scale = new ScaleGestureDetector(activity,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        accumulated[0] *= detector.getScaleFactor();
                        if (accumulated[0] > 1.12f || accumulated[0] < .89f) {
                            ReaderPreferences.changeTextZoom(activity,
                                    accumulated[0] > 1f ? 5 : -5);
                            applyPreferences(activity, content);
                            accumulated[0] = 1f;
                        }
                        return true;
                    }
                });
        GestureDetector gesture = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }
                    @Override public boolean onFling(MotionEvent first, MotionEvent last,
                                                     float vx, float vy) {
                        if (first == null || last == null || navigator == null) return false;
                        float dx = last.getX() - first.getX();
                        float dy = last.getY() - first.getY();
                        if (Math.abs(dx) < 120 || Math.abs(dx) < Math.abs(dy) * 1.7f) return false;
                        if (dx < 0 && navigator.canNext()) navigator.next();
                        else if (dx > 0 && navigator.canPrevious()) navigator.previous();
                        else return false;
                        return true;
                    }
                });
        content.setOnTouchListener((view, event) -> {
            scale.onTouchEvent(event);
            if (!scale.isInProgress()) gesture.onTouchEvent(event);
            return scale.isInProgress();
        });
    }

    private static void attachSelection(Activity activity, TextView content,
                                        ReaderContext context) {
        content.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(Menu.NONE, HIGHLIGHT, 90, "Resaltar");
                menu.add(Menu.NONE, NOTE, 91, "Nota");
                menu.add(Menu.NONE, MEDITATION, 92, "Meditación");
                menu.add(Menu.NONE, DICTIONARY, 93, "Diccionario");
                menu.add(Menu.NONE, TRANSLATE, 94, "Traducir");
                menu.add(Menu.NONE, READ, 95, "Leer en voz alta");
                return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int id = item.getItemId();
                if (id < HIGHLIGHT || id > READ) return false;
                int start = Math.max(0, content.getSelectionStart());
                int end = Math.max(start, content.getSelectionEnd());
                if (end <= start) return true;
                String selected = content.getText().subSequence(start, end).toString().trim();
                if (selected.isEmpty()) return true;
                if (id == HIGHLIGHT) chooseHighlight(activity, content, context, selected);
                else if (id == NOTE || id == MEDITATION) {
                    activity.startActivity(new Intent(activity, StudyEditorActivity.class)
                            .putExtra(StudyEditorActivity.EXTRA_TYPE,
                                    id == NOTE ? StudyEntry.NOTE : StudyEntry.MEDITATION)
                            .putExtra(StudyEditorActivity.EXTRA_CATEGORY, context.category)
                            .putExtra(StudyEditorActivity.EXTRA_SOURCE, context.source)
                            .putExtra(StudyEditorActivity.EXTRA_SOURCE_KEY, context.sourceKey)
                            .putExtra(StudyEditorActivity.EXTRA_REFERENCE, context.reference)
                            .putExtra(StudyEditorActivity.EXTRA_QUOTE, selected));
                } else if (id == DICTIONARY) DictionarySelectionHelper.showDictionary(activity, selected);
                else if (id == TRANSLATE) DictionarySelectionHelper.showTranslator(activity, selected);
                else ReaderTtsController.speakSelection(activity, selected);
                mode.finish();
                return true;
            }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });
    }

    private static void chooseHighlight(Activity activity, TextView content,
                                        ReaderContext context, String selected) {
        String[] names = {"Amarillo", "Verde", "Azul", "Rojo", "Gris"};
        String[] keys = {"yellow", "green", "blue", "red", "gray"};
        new AlertDialog.Builder(activity).setTitle("Color del resaltado")
                .setItems(names, (dialog, which) -> {
                    StudyEntry entry = new StudyEntry();
                    entry.id = UUID.randomUUID().toString();
                    entry.type = StudyEntry.HIGHLIGHT;
                    entry.category = context.category;
                    entry.source = context.source;
                    entry.sourceKey = context.sourceKey;
                    entry.reference = context.reference;
                    entry.title = context.title;
                    entry.quote = selected;
                    entry.color = keys[which];
                    StudyStore.save(activity, entry);
                    applyHighlight(content, selected, color(keys[which]));
                    Toast.makeText(activity, "Resaltado guardado en Mi estudio.",
                            Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void restoreHighlights(Activity activity, TextView content,
                                          String sourceKey) {
        for (StudyEntry entry : StudyStore.forSource(activity, sourceKey)) {
            if (StudyEntry.HIGHLIGHT.equals(entry.type)) {
                applyHighlight(content, entry.quote, color(entry.color));
            }
        }
    }

    private static void applyHighlight(TextView content, String quote, int color) {
        SpannableString text = new SpannableString(content.getText());
        int start = text.toString().indexOf(quote);
        if (start < 0) return;
        text.setSpan(new BackgroundColorSpan(color), start, start + quote.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        content.setText(text);
    }

    private static int color(String key) {
        if ("green".equals(key)) return Color.rgb(168, 213, 162);
        if ("blue".equals(key)) return Color.rgb(169, 199, 232);
        if ("red".equals(key)) return Color.rgb(233, 170, 167);
        if ("gray".equals(key)) return Color.rgb(199, 199, 199);
        return Color.rgb(244, 215, 122);
    }
}
