package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

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
        ReaderPagination.apply(activity, webView, context);
        attachGestures(activity, webView, context, navigator);
        attachAutoHideHeader(webView, header);
    }

    /**
     * Oculta la cabecera con histéresis al avanzar y la recupera en cuanto el
     * lector vuelve hacia arriba. La vista conserva su espacio, evitando saltos
     * de paginación o cambios en la posición del texto.
     */
    private static void attachAutoHideHeader(WebView webView, View header) {
        if (webView == null || header == null) return;
        header.animate().cancel();
        header.setVisibility(View.VISIBLE);
        header.setAlpha(1f);
        header.setTranslationY(0f);
        final boolean[] hidden = {false};
        webView.setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
            if (ReaderPagination.PAGE.equals(ReaderPagination.mode(view.getContext()))) {
                if (hidden[0]) {
                    hidden[0] = false;
                    header.animate().cancel();
                    header.animate().translationY(0f).alpha(1f)
                            .setDuration(120).start();
                }
                return;
            }
            int delta = y - oldY;
            int height = Math.max(1, header.getHeight());
            if (!hidden[0] && delta > 18 && y > height * 2) {
                hidden[0] = true;
                header.animate().cancel();
                header.animate().translationY(-height).alpha(.12f)
                        .setDuration(180).start();
            } else if (hidden[0] && (delta < -12 || y < height)) {
                hidden[0] = false;
                header.animate().cancel();
                header.animate().translationY(0f).alpha(1f)
                        .setDuration(150).start();
            }
        });
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
        button.setOnClickListener(v -> {
            boolean pagination = ReaderPagination.supports(context);
            String[] items = pagination
                    ? new String[]{"Leer en voz alta", "Mesa de estudio", "Compartir",
                    "Información del texto", "Modo de lectura · " + ReaderPagination.label(activity)}
                    : new String[]{"Leer en voz alta", "Mesa de estudio",
                    "Compartir", "Información del texto"};
            new AlertDialog.Builder(activity)
                    .setTitle(context.title.isEmpty() ? "Lectura" : context.title)
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) ReaderTtsController.start(activity, webView,
                                context.omitRubricsInTts);
                        else if (which == 1) activity.startActivity(new Intent(activity,
                                StudyDeskActivity.class).putExtra(
                                StudyDeskActivity.EXTRA_QUERY, context.reference));
                        else if (which == 2) share(activity, context);
                        else if (which == 3) new AlertDialog.Builder(activity)
                                .setTitle("Información del texto")
                                .setMessage(context.source + (context.reference.isEmpty()
                                        ? "" : "\n" + context.reference)
                                        + "\nContenido local de Ministerium 4.1")
                                .setPositiveButton("Cerrar", null).show();
                        else if (which == 4 && pagination) chooseReadingMode(
                                activity, webView, context);
                    }).show();
        });
    }

    private static void chooseReadingMode(Activity activity, WebView webView,
                                          ReaderContext context) {
        String current = ReaderPagination.mode(activity);
        int checked = ReaderPagination.PAGE.equals(current) ? 1 : 0;
        new AlertDialog.Builder(activity)
                .setTitle("Modo de lectura")
                .setSingleChoiceItems(new String[]{"Desplazamiento", "Página"}, checked,
                        (dialog, which) -> {
                            ReaderPagination.setMode(activity,
                                    which == 1 ? ReaderPagination.PAGE : ReaderPagination.SCROLL);
                            ReaderPreferences.apply(activity, webView, false);
                            ReaderPagination.apply(activity, webView, context);
                            Toast.makeText(activity, which == 1
                                            ? "Modo Página activado. Desliza a izquierda o derecha para avanzar."
                                            : "Modo Desplazamiento activado.",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancelar", null).show();
    }

    private static void attachGestures(Activity activity, WebView webView,
                                       ReaderContext context, Navigator navigator) {
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
                        ReaderPagination.apply(activity, webView, context);
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
                        if (ReaderPagination.isPageMode(activity, context)) {
                            ReaderPagination.step(activity, webView, context, navigator,
                                    dx < 0 ? 1 : -1);
                            return true;
                        }
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
