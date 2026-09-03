package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Acciones directas sobre anotaciones visibles en la Biblia.
 *
 * Un toque sobre un resaltado no abre una ficha modal: muestra una barra breve
 * dentro del propio lector con Nota, Marcador y Eliminar. Los indicadores de
 * nota/marcador ofrecen eliminación directa desde el texto.
 */
public final class BibleInlineAnnotationController {
    private static final String BRIDGE = "MinisteriumBibleInline";

    private BibleInlineAnnotationController() {}

    public static void attach(Activity activity, WebView webView) {
        if (webView == null) return;
        webView.addJavascriptInterface(new Bridge(activity, webView), BRIDGE);
    }

    /** Se puede llamar tras cada carga; el instalador JS es idempotente. */
    public static void install(WebView webView) {
        if (webView == null) return;
        String script = "(function(){"
                + "if(window.__ministeriumBibleInlineInstalled)return;"
                + "if(!window." + BRIDGE + ")return;"
                + "window.__ministeriumBibleInlineInstalled=true;"
                + "var s=document.getElementById('ministerium-bible-inline-css');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-bible-inline-css';"
                + "s.textContent='#ministerium-bible-inline-actions{position:fixed;left:50%;bottom:18px;"
                + "transform:translateX(-50%);z-index:2147483646;display:flex;gap:6px;align-items:center;"
                + "max-width:calc(100vw - 24px);padding:7px;border-radius:14px;background:rgba(38,33,30,.96);"
                + "box-shadow:0 5px 20px rgba(0,0,0,.28);font:600 14px sans-serif!important;}"
                + "#ministerium-bible-inline-actions button{min-height:38px;padding:7px 11px;border:0;"
                + "border-radius:10px;background:#FFFDF7;color:#2A2521;-webkit-text-fill-color:#2A2521;"
                + "font:600 14px sans-serif!important;white-space:nowrap;}"
                + "#ministerium-bible-inline-actions button.danger{background:#6E1D2A;color:#fff;"
                + "-webkit-text-fill-color:#fff;}';document.head.appendChild(s);}"
                + "function close(){var old=document.getElementById('ministerium-bible-inline-actions');if(old)old.remove();}"
                + "function button(bar,label,id,action,danger){var b=document.createElement('button');"
                + "b.type='button';b.textContent=label;if(danger)b.className='danger';"
                + "b.addEventListener('click',function(e){e.preventDefault();e.stopPropagation();close();"
                + "window." + BRIDGE + ".action(id,action);});bar.appendChild(b);}"
                + "function show(id,kind){close();var bar=document.createElement('div');"
                + "bar.id='ministerium-bible-inline-actions';bar.setAttribute('role','toolbar');"
                + "bar.setAttribute('aria-label','Acciones de anotación');"
                + "if(kind==='highlight'){button(bar,'Nota',id,'note',false);"
                + "button(bar,'Marcador',id,'bookmark',false);button(bar,'Eliminar',id,'delete',true);}"
                + "else{button(bar,'Eliminar',id,'delete',true);}document.body.appendChild(bar);}"
                + "document.addEventListener('click',function(ev){"
                + "var target=ev.target&&ev.target.closest?ev.target.closest('.ministerium-study-highlight,.ministerium-study-marker'):null;"
                + "if(!target){if(!ev.target.closest||!ev.target.closest('#ministerium-bible-inline-actions'))close();return;}"
                + "var id=target.getAttribute('data-study-id');if(!id)return;"
                + "ev.preventDefault();ev.stopPropagation();if(ev.stopImmediatePropagation)ev.stopImmediatePropagation();"
                + "show(id,target.classList.contains('ministerium-study-highlight')?'highlight':'marker');"
                + "},true);"
                + "document.addEventListener('scroll',close,true);"
                + "})()";
        webView.evaluateJavascript(script, null);
    }

    private static StudyEntry find(Activity activity, String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (StudyEntry entry : StudyStore.all(activity)) {
            if (id.equals(entry.id)) return entry;
        }
        return null;
    }

    private static void handle(Activity activity, WebView webView, String id, String action) {
        StudyEntry entry = find(activity, id);
        if (entry == null) {
            Toast.makeText(activity, "La anotación ya no existe.", Toast.LENGTH_SHORT).show();
            removeFromPage(webView, id);
            return;
        }
        if ("delete".equals(action)) {
            StudyStore.delete(activity, entry.id);
            removeFromPage(webView, entry.id);
            Toast.makeText(activity,
                    StudyEntry.HIGHLIGHT.equals(entry.type)
                            ? "Subrayado eliminado." : "Marcador eliminado.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!StudyEntry.HIGHLIGHT.equals(entry.type)) return;
        if ("bookmark".equals(action)) {
            StudyEntry bookmark = copyAnchor(entry, StudyEntry.BOOKMARK);
            bookmark.id = UUID.randomUUID().toString();
            bookmark.icon = "bookmark";
            bookmark.color = value(entry.color, "yellow");
            StudyStore.save(activity, bookmark);
            UniversalSelectionMenu.restoreHighlights(activity, webView, entry.sourceKey);
            Toast.makeText(activity, "Marcador añadido.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("note".equals(action)) editLinkedNote(activity, webView, entry);
    }

    private static void editLinkedNote(Activity activity, WebView webView, StudyEntry highlight) {
        StudyEntry note = linkedNote(activity, highlight);
        final boolean isNew = note == null;
        if (note == null) {
            note = copyAnchor(highlight, StudyEntry.NOTE);
            note.id = UUID.randomUUID().toString();
            note.icon = "note";
            note.color = value(highlight.color, "yellow");
        }
        StudyEntry target = note;
        EditText editor = new EditText(activity);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setMinLines(5);
        editor.setMaxLines(12);
        editor.setHint("Escribe tu nota…");
        editor.setText(target.body == null ? "" : target.body);
        editor.setSelection(editor.getText().length());
        new AlertDialog.Builder(activity)
                .setTitle(isNew ? "Nota sobre el subrayado" : "Editar nota")
                .setMessage(shortQuote(highlight.quote))
                .setView(editor)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String body = editor.getText().toString().trim();
                    if (body.isEmpty()) {
                        Toast.makeText(activity, "La nota está vacía.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    target.body = body;
                    StudyStore.save(activity, target);
                    UniversalSelectionMenu.restoreHighlights(activity, webView,
                            highlight.sourceKey);
                    Toast.makeText(activity, "Nota guardada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private static StudyEntry linkedNote(Activity activity, StudyEntry highlight) {
        for (StudyEntry value : StudyStore.forSource(activity, highlight.sourceKey)) {
            if (!StudyEntry.NOTE.equals(value.type)) continue;
            if (!highlight.semanticUnitId.isEmpty()
                    && highlight.semanticUnitId.equals(value.semanticUnitId)
                    && highlight.startOffset == value.startOffset
                    && highlight.endOffset == value.endOffset) return value;
            if (!highlight.anchorText.isEmpty()
                    && highlight.anchorText.equals(value.anchorText)) return value;
        }
        return null;
    }

    private static StudyEntry copyAnchor(StudyEntry source, String type) {
        StudyEntry target = new StudyEntry();
        target.type = type;
        target.category = source.category;
        target.source = source.source;
        target.sourceKey = source.sourceKey;
        target.contentId = source.contentId;
        target.title = source.title;
        target.reference = source.reference;
        target.quote = source.quote;
        target.anchorText = source.anchorText;
        target.semanticUnitId = source.semanticUnitId;
        target.startOffset = source.startOffset;
        target.endOffset = source.endOffset;
        target.prefix = source.prefix;
        target.suffix = source.suffix;
        target.anchorVersion = source.anchorVersion;
        return target;
    }

    private static void removeFromPage(WebView webView, String id) {
        String script = "(function(id){var e=document.querySelector('[data-study-id=\\\"'+id+'\\\"]');"
                + "if(!e)return;if(e.tagName==='MARK'){var p=e.parentNode;"
                + "while(e.firstChild)p.insertBefore(e.firstChild,e);p.removeChild(e);p.normalize();}"
                + "else e.remove();})(" + JSONObject.quote(id) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static String shortQuote(String value) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return text.length() > 180 ? text.substring(0, 177) + "…" : text;
    }

    private static String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static final class Bridge {
        private final Activity activity;
        private final WebView webView;

        Bridge(Activity activity, WebView webView) {
            this.activity = activity;
            this.webView = webView;
        }

        @JavascriptInterface public void action(String id, String action) {
            activity.runOnUiThread(() -> handle(activity, webView, id, action));
        }
    }
}
