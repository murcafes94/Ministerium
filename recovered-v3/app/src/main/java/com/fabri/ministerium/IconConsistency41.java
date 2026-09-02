package com.fabri.ministerium;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * Conserva la iconografía original de Ministerium y limita la normalización a
 * geometría segura. Así los glifos/íconos históricos mantienen su aspecto,
 * mientras los controles táctiles siguen respetando tamaños de teléfono/tablet.
 */
public final class IconConsistency41 {
    private IconConsistency41() {}

    public static void apply(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        View root = activity.getWindow().getDecorView();
        if (root == null) return;
        normalizeSizing(activity, root);
    }

    private static void normalizeSizing(Activity activity, View view) {
        if (view instanceof ImageButton) {
            ImageButton button = (ImageButton) view;
            button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int padding = dimension(activity, R.dimen.reader_icon_padding, 12);
            button.setPadding(padding, padding, padding, padding);
            ensureTouchTarget(activity, button);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                normalizeSizing(activity, group.getChildAt(i));
            }
        }
    }

    private static void ensureTouchTarget(Activity activity, View view) {
        int min = dimension(activity, R.dimen.reader_icon_touch, 48);
        view.setMinimumWidth(Math.max(view.getMinimumWidth(), min));
        view.setMinimumHeight(Math.max(view.getMinimumHeight(), min));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return;
        boolean changed = false;
        if (params.width > 0 && params.width < min) {
            params.width = min;
            changed = true;
        }
        if (params.height > 0 && params.height < min) {
            params.height = min;
            changed = true;
        }
        if (changed) view.setLayoutParams(params);
    }

    private static int dimension(Activity activity, int resourceId, int fallbackDp) {
        try {
            return activity.getResources().getDimensionPixelSize(resourceId);
        } catch (Exception ignored) {
            return dp(activity, fallbackDp);
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
