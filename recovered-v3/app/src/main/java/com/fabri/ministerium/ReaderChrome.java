package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;

/** Gestos, cabecera fija y herramientas comunes del lector. */
public final class ReaderChrome {
    public interface Navigator {
        boolean canPrevious();
        boolean canNext();
        void previous();
        void next();
    }

    private ReaderChrome() {}

    public static void attach(Activity activity, WebView webView, View header,
                              ReaderContext context, Navigator navigator,
                              boolean preserveBibleTypeface) {
        UniversalSelectionMenu.attach(activity, webView, context);
        ReaderPreferences.apply(activity, webView, preserveBibleTypeface);
        attachGestures(activity, webView, navigator);
        // La cabecera forma parte del layout superior y permanece fija. No se traslada ni
        // se desvanece durante el scroll para evitar saltos visuales y mantener siempre
        // accesibles Volver, búsqueda, tema y menú.
        if (header != null) {
            header.setTranslationY(0f);
            header.setAlpha(1f);
        }
    }

    public static void bindTheme(Activity activity, View button) {
        if (button == null) return;
        button.setOnClickListener(v -> {
            ThemeUtils.setMode(activity, ThemeUtils.isDark(activity)
                    ? ThemeUtils.LIGHT : ThemeUtils.DARK);
            activity.recreate();
        });
    }

    public static void bindGlobalMenu(Activity activity, View button) {
        if (button != null) button.setOnClickListener(v -> GlobalNavigationDialog.show(activity));
    }

    public static void bindMore(Activity activity, View button, WebView webView,
                                ReaderContext context) {
        if (button == null) return;
        button.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(context.title.isEmpty() ? "Lectura" : context.title)
                .setItems(new String[]{"Leer en voz alta", "Mesa de estudio",
                                "Compartir", "Información del texto"},
                        (dialog, which) -> {
                            if (which == 0) ReaderTtsController.start(activity, webView,
                                    context.omitRubricsInTts);
                            else if (which == 1) activity.startActivity(new Intent(activity,
                                    StudyDeskActivity.class).putExtra(
                                    StudyDeskActivity.EXTRA_QUERY, context.reference));
                            else if (which == 2) share(activity, context);
                            else new AlertDialog.Builder(activity)
                                    .setTitle("Información del texto")
                                    .setMessage(context.source + (context.reference.isEmpty()
                                            ? "" : "\n" + context.reference)
                                            + "\nContenido local de Ministerium 3.0")
                                    .setPositiveButton("Cerrar", null).show();
                        }).show());
    }

    private static void attachGestures(Activity activity, WebView webView,
                                       Navigator navigator) {
        final float[] scale = {1f};
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(activity,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        scale[0] *= detector.getScaleFactor();
                        if (scale[0] > 1.12f) {
                            ReaderPreferences.changeTextZoom(activity, 5);
                            webView.getSettings().setTextZoom(ReaderPreferences.textZoom(activity));
                            scale[0] = 1f;
                        } else if (scale[0] < .89f) {
                            ReaderPreferences.changeTextZoom(activity, -5);
                            webView.getSettings().setTextZoom(ReaderPreferences.textZoom(activity));
                            scale[0] = 1f;
                        }
                        return true;
                    }

                    @Override public void onScaleEnd(ScaleGestureDetector detector) {
                        scale[0] = 1f;
                    }
                });
        GestureDetector gestures = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }

                    @Override public boolean onFling(MotionEvent first, MotionEvent last,
                                                     float velocityX, float velocityY) {
                        if (first == null || last == null || navigator == null) return false;
                        float dx = last.getX() - first.getX();
                        float dy = last.getY() - first.getY();
                        if (Math.abs(dx) < 120 || Math.abs(dx) < Math.abs(dy) * 1.7f
                                || Math.abs(velocityX) < 350) return false;
                        if (dx < 0 && navigator.canNext()) navigator.next();
                        else if (dx > 0 && navigator.canPrevious()) navigator.previous();
                        else return false;
                        return true;
                    }
                });
        webView.setOnTouchListener((view, event) -> {
            scaleDetector.onTouchEvent(event);
            if (!scaleDetector.isInProgress()) gestures.onTouchEvent(event);
            return scaleDetector.isInProgress();
        });
    }

    private static void share(Activity activity, ReaderContext context) {
        String text = context.reference.isEmpty() ? context.title
                : context.title + "\n" + context.reference;
        activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                .setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir"));
    }
}
