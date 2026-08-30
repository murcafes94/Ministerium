package com.fabri.ministerium;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Normaliza la iconografía heredada que todavía estaba construida con glifos
 * Unicode/emoji. Esos glifos dependen de la fuente del fabricante y pueden
 * verse cortados, sobrepuestos o descentrados. En 4.1 se sustituyen al cargar
 * cada pantalla por vectores de 24dp y se garantiza una zona táctil mínima de
 * 48dp, sin obligar a reescribir todos los layouts históricos a la vez.
 */
public final class IconConsistency41 {
    private static final int ICON_DP = 24;
    private static final int TOUCH_DP = 48;

    private IconConsistency41() {}

    public static void apply(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        View root = activity.getWindow().getDecorView();
        if (root == null) return;
        normalize(activity, root);
    }

    private static void normalize(Activity activity, View view) {
        if (view instanceof ImageButton) {
            ImageButton button = (ImageButton) view;
            button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ensureTouchTarget(activity, button);
        }
        if (view instanceof TextView) {
            normalizeTextIcon(activity, (TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                normalize(activity, group.getChildAt(i));
            }
        }
    }

    private static void normalizeTextIcon(Activity activity, TextView view) {
        String id = entryName(view);
        String text = view.getText() == null ? "" : view.getText().toString().trim();

        // Controles conocidos: se prioriza el significado del botón, no el glifo.
        if ("btnNotifications".equals(id)) {
            setIcon(activity, view, R.drawable.ic_bell_41, true, false);
            return;
        }
        if ("btnTheme".equals(id) || "btnReaderTheme".equals(id)) {
            setIcon(activity, view, R.drawable.ic_theme, true, false);
            return;
        }
        if ("btnCalendar".equals(id) || "btnChooseDate".equals(id)) {
            setIcon(activity, view, R.drawable.ic_calendar, true, false);
            return;
        }
        if ("btnPreviousDay".equals(id)) {
            setIcon(activity, view, R.drawable.ic_chevron_left, true, false);
            return;
        }
        if ("btnNextDay".equals(id)) {
            setIcon(activity, view, R.drawable.ic_chevron_right, true, false);
            return;
        }
        if ("btnBack".equals(id) && isPureIcon(text)) {
            setIcon(activity, view, R.drawable.ic_nav_back, true, false);
            return;
        }
        if ("btnSearch".equals(id) && !text.isEmpty()) {
            setIcon(activity, view, R.drawable.ic_search, false, true);
            return;
        }

        // Iconos de tarjetas que antes eran letras. Solo se reemplazan cuando
        // el contenedor permite identificar inequívocamente el módulo.
        String parent = entryName(view.getParent() instanceof View ? (View) view.getParent() : null);
        if ("B".equals(text) && parent.contains("Bible")) {
            setIcon(activity, view, R.drawable.ic_book_41, true, false);
            return;
        }
        if ("M".equals(text) && parent.contains("Magisterium")) {
            setIcon(activity, view, R.drawable.ic_document_41, true, false);
            return;
        }
        if ("M".equals(text) && parent.contains("Missal")) {
            setIcon(activity, view, R.drawable.ic_book_41, true, false);
            return;
        }
        if ("31".equals(text) && parent.toLowerCase().contains("calendar")) {
            setIcon(activity, view, R.drawable.ic_calendar, true, false);
            return;
        }

        // Glifos heredados usados como iconos en Inicio, Horas y módulos.
        // ℣ se conserva: es un signo litúrgico real y no debe reinterpretarse
        // globalmente como un icono de interfaz.
        int drawable = glyphDrawable(text);
        if (drawable != 0) setIcon(activity, view, drawable, true, false);
    }

    private static int glyphDrawable(String value) {
        if ("↻".equals(value)) return R.drawable.ic_refresh_41;
        if ("✎".equals(value)) return R.drawable.ic_edit_41;
        if ("☀".equals(value)) return R.drawable.ic_sun_41;
        if ("✝".equals(value) || "☩".equals(value) || "✠".equals(value)
                || "✚".equals(value)) return R.drawable.ic_cross_41;
        if ("✦".equals(value)) return R.drawable.ic_star_41;
        if ("⚙".equals(value)) return R.drawable.ic_settings_41;
        if ("›".equals(value)) return R.drawable.ic_chevron_right;
        if ("‹".equals(value)) return R.drawable.ic_chevron_left;
        if ("▣".equals(value)) return R.drawable.ic_calendar;
        if ("🔔".equals(value)) return R.drawable.ic_bell_41;
        if ("◐".equals(value)) return R.drawable.ic_theme;
        return 0;
    }

    private static boolean isPureIcon(String value) {
        return glyphDrawable(value) != 0 || "←".equals(value) || "<".equals(value);
    }

    private static void setIcon(Activity activity, TextView view, int drawableId,
                                boolean clearText, boolean atStart) {
        Drawable drawable = activity.getDrawable(drawableId);
        if (drawable == null) return;
        drawable = drawable.mutate();
        drawable.setColorFilter(view.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
        int size = dp(activity, ICON_DP);
        drawable.setBounds(0, 0, size, size);
        if (clearText) {
            view.setText("");
            view.setCompoundDrawables(drawable, null, null, null);
            view.setCompoundDrawablePadding(0);
            view.setGravity(Gravity.CENTER);
            view.setIncludeFontPadding(false);
            ensureTouchTarget(activity, view);
        } else if (atStart) {
            view.setCompoundDrawables(drawable, null, null, null);
            view.setCompoundDrawablePadding(dp(activity, 8));
        }
    }

    private static void ensureTouchTarget(Activity activity, View view) {
        int min = dp(activity, TOUCH_DP);
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

    private static String entryName(View view) {
        if (view == null || view.getId() == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
