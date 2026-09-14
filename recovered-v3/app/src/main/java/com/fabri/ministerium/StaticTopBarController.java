package com.fabri.ministerium;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

/**
 * Mantiene fija la barra superior de las pantallas cuyo layout raíz es un
 * ScrollView. No reestructura XML heredado: compensa el scroll mediante
 * translationY y eleva la cabecera para que el contenido pase por debajo.
 *
 * Los lectores WebView/TextView tienen su propio contrato en ReaderChrome y
 * TextViewReaderChrome; este controlador cubre las demás pestañas/pantallas.
 */
public final class StaticTopBarController {
    private StaticTopBarController() {}

    public static void attach(Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        ViewGroup host = (ViewGroup) content;
        if (host.getChildCount() == 0) return;
        View root = host.getChildAt(0);
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        scroll.post(() -> attachRootScroll(activity, scroll));
    }

    private static void attachRootScroll(Activity activity, ScrollView scroll) {
        if (scroll.getChildCount() == 0) return;
        View bodyView = scroll.getChildAt(0);
        if (!(bodyView instanceof ViewGroup)) return;
        ViewGroup body = (ViewGroup) bodyView;
        if (body.getChildCount() < 2) return;
        View header = body.getChildAt(0);
        if (!isInteractiveHeader(header)) return;

        int maxHeader = dp(activity, 180);
        if (header.getHeight() <= 0 || header.getHeight() > maxHeader) return;

        // Las cabeceras V5 construidas por código son transparentes por defecto.
        // Al fijarlas, el contenido del ScrollView quedaría visible por debajo.
        // Solo heredamos el fondo del scroll si la cabecera no tiene uno propio,
        // conservando intactos los encabezados XML heredados (vino, tarjetas, etc.).
        if (header.getBackground() == null && scroll.getBackground() != null) {
            Drawable background = scroll.getBackground();
            Drawable.ConstantState state = background.getConstantState();
            header.setBackground(state != null
                    ? state.newDrawable(activity.getResources()).mutate()
                    : background.mutate());
        }

        header.animate().cancel();
        header.setAlpha(1f);
        header.setTranslationY(scroll.getScrollY());
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            header.setElevation(dp(activity, 8));
            header.setTranslationZ(dp(activity, 2));
        }
        scroll.setClipToPadding(false);
        scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
            header.setTranslationY(y);
            header.setAlpha(1f);
        });
    }

    /**
     * Las pantallas heredadas usan Button en su cabecera. Ministerium 5 usa
     * controles de texto clicables para volver y para acciones compactas como
     * el cambio de tema. Ambos patrones representan una barra superior real.
     */
    private static boolean isInteractiveHeader(View view) {
        if (view == null) return false;
        if (view instanceof Button || view.isClickable()) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (isInteractiveHeader(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
