package com.fabri.ministerium;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

/**
 * WebView de lectura que conserva los tiradores y la geometría nativa de
 * selección. Ministerium añade acciones y solicita el ActionMode flotante
 * para que el toolbar aparezca junto al texto seleccionado en Android 6+.
 *
 * Chromium normalmente aporta un rectángulo exacto mediante Callback2. En
 * algunos WebView/dispositivos devuelve, sin embargo, un rectángulo casi tan
 * grande como toda la vista. En ese caso usamos como ancla secundaria la
 * posición del gesto que inició/ajustó la selección. Es el mismo principio
 * empleado por lectores como Calibre: anclar la barra al contenido y dejar
 * que el sistema resuelva colisiones con bordes y tiradores.
 */
public class MinisteriumWebView extends WebView {
    public interface SelectionActionHandler {
        void populate(Menu menu);
        boolean handle(ActionMode mode, MenuItem item);
    }

    private interface WrappedSelectionCallback {}

    private SelectionActionHandler selectionActionHandler;
    private float lastSelectionX = -1f;
    private float lastSelectionY = -1f;

    public MinisteriumWebView(Context context) { super(context); }
    public MinisteriumWebView(Context context, AttributeSet attrs) { super(context, attrs); }
    public MinisteriumWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelectionActionHandler(SelectionActionHandler handler) {
        selectionActionHandler = handler;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_MOVE) {
                lastSelectionX = event.getX();
                lastSelectionY = event.getY();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return super.startActionMode(wrap(callback), ActionMode.TYPE_FLOATING);
        }
        return super.startActionMode(wrap(callback));
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return super.startActionMode(wrap(callback), ActionMode.TYPE_FLOATING);
        }
        return super.startActionMode(wrap(callback), type);
    }

    private ActionMode.Callback wrap(ActionMode.Callback original) {
        if (original instanceof WrappedSelectionCallback) return original;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new SelectionCallback2(original);
        }
        return new SelectionCallback(original);
    }

    private void promoteCoreActions(Menu menu) {
        if (menu == null) return;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item == null || item.getTitle() == null) continue;
            String title = item.getTitle().toString();
            if ("Subrayar".equals(title)) {
                item.setIcon(android.R.drawable.ic_menu_edit);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else if ("Nota".equals(title)) {
                item.setIcon(android.R.drawable.ic_menu_save);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else if ("Diccionario".equals(title)) {
                item.setIcon(android.R.drawable.ic_menu_search);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else if ("Reflexión".equals(title) || "Comentario".equals(title)) {
                item.setIcon(android.R.drawable.ic_menu_info_details);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else if ("Leer".equals(title)) {
                item.setIcon(android.R.drawable.ic_lock_silent_mode_off);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
    }

    private boolean onCreateSelectionMode(ActionMode.Callback original, ActionMode mode, Menu menu) {
        boolean created = original == null || original.onCreateActionMode(mode, menu);
        if (created && selectionActionHandler != null) {
            selectionActionHandler.populate(menu);
            promoteCoreActions(menu);
        }
        return created;
    }

    private boolean onPrepareSelectionMode(ActionMode.Callback original, ActionMode mode, Menu menu) {
        boolean changed = original != null && original.onPrepareActionMode(mode, menu);
        if (selectionActionHandler != null) {
            selectionActionHandler.populate(menu);
            promoteCoreActions(menu);
            changed = true;
        }
        return changed;
    }

    private boolean onSelectionItemClicked(ActionMode.Callback original, ActionMode mode, MenuItem item) {
        if (selectionActionHandler != null && selectionActionHandler.handle(mode, item)) {
            return true;
        }
        return original != null && original.onActionItemClicked(mode, item);
    }

    private void onDestroySelectionMode(ActionMode.Callback original, ActionMode mode) {
        if (original != null) original.onDestroyActionMode(mode);
    }

    private void touchFallbackRect(View view, Rect outRect) {
        if (view == null || outRect == null) return;
        float density = getResources().getDisplayMetrics().density;
        int halfWidth = Math.max(20, Math.round(28f * density));
        int halfHeight = Math.max(14, Math.round(20f * density));
        int x = lastSelectionX >= 0 ? Math.round(lastSelectionX) : view.getWidth() / 2;
        int y = lastSelectionY >= 0 ? Math.round(lastSelectionY) : view.getHeight() / 2;
        int left = Math.max(0, x - halfWidth);
        int top = Math.max(0, y - halfHeight);
        int right = Math.min(view.getWidth(), x + halfWidth);
        int bottom = Math.min(view.getHeight(), y + halfHeight);
        if (right <= left) right = Math.min(view.getWidth(), left + 1);
        if (bottom <= top) bottom = Math.min(view.getHeight(), top + 1);
        outRect.set(left, top, right, bottom);
    }

    private boolean unusableContentRect(View view, Rect rect) {
        if (view == null || rect == null || rect.isEmpty()) return true;
        long viewArea = (long) Math.max(1, view.getWidth()) * Math.max(1, view.getHeight());
        long rectArea = (long) Math.max(0, rect.width()) * Math.max(0, rect.height());
        if (rectArea * 100L > viewArea * 55L) return true;
        return rect.right < 0 || rect.bottom < 0
                || rect.left > view.getWidth() || rect.top > view.getHeight();
    }

    private final class SelectionCallback implements ActionMode.Callback, WrappedSelectionCallback {
        private final ActionMode.Callback original;

        SelectionCallback(ActionMode.Callback original) { this.original = original; }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return onCreateSelectionMode(original, mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return onPrepareSelectionMode(original, mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return onSelectionItemClicked(original, mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onDestroySelectionMode(original, mode);
        }
    }

    /**
     * Desde Android 6 el floating toolbar usa Callback2.onGetContentRect()
     * para anclarse a la selección. Se conserva el rectángulo de Chromium si
     * es razonable y sólo se aplica el ancla táctil cuando ese dato es vacío o
     * claramente representa casi toda la página.
     */
    private final class SelectionCallback2 extends ActionMode.Callback2
            implements WrappedSelectionCallback {
        private final ActionMode.Callback original;

        SelectionCallback2(ActionMode.Callback original) { this.original = original; }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return onCreateSelectionMode(original, mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return onPrepareSelectionMode(original, mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return onSelectionItemClicked(original, mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onDestroySelectionMode(original, mode);
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (original instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) original).onGetContentRect(mode, view, outRect);
            } else {
                outRect.setEmpty();
            }
            if (unusableContentRect(view, outRect)) touchFallbackRect(view, outRect);
        }
    }
}
