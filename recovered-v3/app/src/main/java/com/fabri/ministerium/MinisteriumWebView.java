package com.fabri.ministerium;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * WebView de lectura que conserva los tiradores nativos de selección y añade
 * las acciones de Ministerium al toolbar flotante de Android.
 */
public class MinisteriumWebView extends WebView {
    public interface SelectionActionHandler {
        void populate(Menu menu);
        boolean handle(ActionMode mode, MenuItem item);
    }

    private SelectionActionHandler selectionActionHandler;

    public MinisteriumWebView(Context context) { super(context); }
    public MinisteriumWebView(Context context, AttributeSet attrs) { super(context, attrs); }
    public MinisteriumWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelectionActionHandler(SelectionActionHandler handler) {
        selectionActionHandler = handler;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Fuerza el mismo patrón visual de los lectores modernos: menú
            // contextual junto a la selección, no una barra fija arriba.
            return super.startActionMode(wrap(callback), ActionMode.TYPE_FLOATING);
        }
        return super.startActionMode(wrap(callback));
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        int resolvedType = type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resolvedType = ActionMode.TYPE_FLOATING;
        }
        return super.startActionMode(wrap(callback), resolvedType);
    }

    private ActionMode.Callback wrap(ActionMode.Callback original) {
        if (original instanceof SelectionCallback) return original;
        return new SelectionCallback(original);
    }

    /**
     * Mantiene visibles las herramientas esenciales. Las secundarias quedan en
     * el desbordamiento «Más», de modo que el popup no se convierta en una fila
     * interminable. Los iconos se dejan al sistema para que respeten tema y escala.
     */
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

    private final class SelectionCallback implements ActionMode.Callback {
        private final ActionMode.Callback original;

        SelectionCallback(ActionMode.Callback original) { this.original = original; }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            boolean created = original == null || original.onCreateActionMode(mode, menu);
            if (created && selectionActionHandler != null) {
                selectionActionHandler.populate(menu);
                promoteCoreActions(menu);
            }
            return created;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean changed = original != null && original.onPrepareActionMode(mode, menu);
            if (selectionActionHandler != null) {
                selectionActionHandler.populate(menu);
                promoteCoreActions(menu);
                changed = true;
            }
            return changed;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (selectionActionHandler != null && selectionActionHandler.handle(mode, item)) {
                return true;
            }
            return original != null && original.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (original != null) original.onDestroyActionMode(mode);
        }
    }
}
