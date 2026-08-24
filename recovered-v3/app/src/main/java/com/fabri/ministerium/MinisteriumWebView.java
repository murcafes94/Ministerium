package com.fabri.ministerium;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * WebView de lectura que conserva la selección nativa de Android y permite a
 * Ministerium añadir acciones propias al ActionMode sin usar APIs inexistentes
 * de WebView como setCustomSelectionActionModeCallback().
 */
public class MinisteriumWebView extends WebView {
    public interface SelectionActionHandler {
        void populate(Menu menu);
        boolean handle(MenuItem item);
    }

    private SelectionActionHandler selectionActionHandler;

    public MinisteriumWebView(Context context) {
        super(context);
    }

    public MinisteriumWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MinisteriumWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelectionActionHandler(SelectionActionHandler handler) {
        selectionActionHandler = handler;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return super.startActionMode(wrap(callback));
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        return super.startActionMode(wrap(callback), type);
    }

    private ActionMode.Callback wrap(ActionMode.Callback original) {
        if (original instanceof SelectionCallback) return original;
        return new SelectionCallback(original);
    }

    private final class SelectionCallback implements ActionMode.Callback {
        private final ActionMode.Callback original;

        SelectionCallback(ActionMode.Callback original) {
            this.original = original;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            boolean created = original == null || original.onCreateActionMode(mode, menu);
            if (created && selectionActionHandler != null) {
                selectionActionHandler.populate(menu);
            }
            return created;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean changed = original != null && original.onPrepareActionMode(mode, menu);
            if (selectionActionHandler != null) {
                selectionActionHandler.populate(menu);
                changed = true;
            }
            return changed;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (selectionActionHandler != null && selectionActionHandler.handle(item)) {
                mode.finish();
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
