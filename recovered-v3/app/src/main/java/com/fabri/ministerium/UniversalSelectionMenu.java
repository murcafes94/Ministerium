package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;
import java.util.UUID;

/**
 * Menú de selección común de Ministerium.
 *
 * Los subrayados, notas y marcadores son objetos editables: pueden tocarse
 * después de creados, cambiar de estilo, compartirse, ampliarse y eliminarse.
 * Las notas se muestran mediante un indicador junto al texto y se editan sin
 * abandonar el lector.
 */
public final class UniversalSelectionMenu {
    private static final int HIGHLIGHT = 8901;
    private static final int NOTE = 8902;
    private static final int BOOKMARK = 8903;
    private static final int DICTIONARY = 8904;
    private static final int COMMENTARY = 8905;
    private static final int TRANSLATE = 8906;
    private static final int READ_ALOUD = 8907;
    private static final int SHARE = 8908;
    private static final int MEDITATION = 8909;
    private static final int MORE = 8910;

    private UniversalSelectionMenu() {}

    public static void attach(Activity activity, WebView webView, ReaderContext context) {
        if (!(webView instanceof MinisteriumWebView)) return;
        MinisteriumWebView reader = (MinisteriumWebView) webView;
        webView.addJavascriptInterface(new StudyBridge(activity, webView, context), "MinisteriumStudy");
        reader.setSelectionActionHandler(new MinisteriumWebView.SelectionActionHandler() {
            @Override public void populate(Menu menu) {
                add(menu, HIGHLIGHT, "Resaltar");
                add(menu, NOTE, "Nota");
                add(menu, BOOKMARK, "Marcador");
                add(menu, DICTIONARY, "Diccionario");
                SubMenu more = menu.addSubMenu(Menu.NONE, MORE, 120, "Más");
                more.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                add(more, SHARE, "Compartir");
                add(more, MEDITATION, "Reflexión");
                if ("Biblia".equalsIgnoreCase(context.category)) add(more, COMMENTARY, "Comentario");
                add(more, TRANSLATE, "Traducir");
                if (context.allowTts) add(more, READ_ALOUD, "Leer");
            }

            @Override public boolean handle(ActionMode mode, MenuItem item) {
                int id = item.getItemId();
                if (id == HIGHLIGHT) {
                    capture(webView, selection -> {
                        mode.finish();
                        chooseHighlight(activity, webView, context, selection, null);
                    });
                    return true;
                }
                if (id == NOTE) {
                    capture(webView, selection -> {
                        mode.finish();
                        createNote(activity, webView, context, selection);
                    });
                    return true;
                }
                if (id == BOOKMARK) {
                    capture(webView, selection -> {
                        mode.finish();
                        createBookmark(activity, webView, context, selection);
                    });
                    return true;
                }
                if (id == SHARE) {
                    capture(webView, selection -> {
                        mode.finish();
                        share(activity, selection.text, context.reference);
                    });
                    return true;
                }
                if (id == MEDITATION) {
                    capture(webView, selection -> {
                        mode.finish();
                        openMeditationEditor(activity, context, selection);
                    });
                    return true;
                }
                if (id == DICTIONARY) {
                    capture(webView, selection -> {
                        mode.finish();
                        openDictionary(activity, selection.text);
                    });
                    return true;
                }
                if (id == COMMENTARY) {
                    openNearestBibleCommentary(activity, webView, mode);
                    return true;
                }
                if (id == TRANSLATE) {
                    capture(webView, selection -> {
                        mode.finish();
                        openTranslation(activity, selection.text);
                    });
                    return true;
                }
                if (id == READ_ALOUD && context.allowTts) {
                    capture(webView, selection -> {
                        mode.finish();
                        ReaderTtsController.speakSelection(activity, selection.text);
                    });
                    return true;
                }
                return false;
            }
        });
    }

    private static void add(Menu menu, int id, String title) {
        if (menu.findItem(id) != null) return;
        menu.add(Menu.NONE, id, 90 + id - HIGHLIGHT, title)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    private static void capture(WebView webView, SelectionCallback callback) {
        String script = "(function(){var s=window.getSelection&&window.getSelection();"
                + "if(!s||!s.rangeCount||!s.toString().trim())return '';var r=s.getRangeAt(0);"
                + "function unit(root){if(!root)return '';var id=root.getAttribute&&"
                + "(root.getAttribute('data-semantic-id')||root.getAttribute('data-block'));if(id)return id;"
                + "if(root.matches&&root.matches('section.ministerium-section')){var h=root.querySelector('h2');"
                + "var t=h?(h.textContent||'').replace(/\\s+/g,' ').trim().toLowerCase():'';return t?'combined:'+t:'';}return '';}"
                + "function flat(root){var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),x,t='';"
                + "while(x=w.nextNode()){if(x.parentNode&&x.parentNode.tagName!='SCRIPT'&&x.parentNode.tagName!='STYLE'"
                + "&&!x.parentNode.classList.contains('ministerium-study-marker'))t+=x.nodeValue;}return t;}"
                + "function off(root,node,o){if(!root||!node||node.nodeType!==3)return -1;"
                + "var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),x,t=0;while(x=w.nextNode()){"
                + "if(x.parentNode&&x.parentNode.tagName!='SCRIPT'&&x.parentNode.tagName!='STYLE'"
                + "&&!x.parentNode.classList.contains('ministerium-study-marker')){if(x===node)return t+o;t+=x.nodeValue.length;}}return -1;}"
                + "var n=r.commonAncestorContainer.nodeType===1?r.commonAncestorContainer:r.commonAncestorContainer.parentElement;"
                + "var b=n&&n.closest?n.closest('[data-semantic-id],[data-block],section.ministerium-section'):null;"
                + "var root=b||(n&&n.closest?n.closest('p,li,blockquote,section,div'):null)||document.body;"
                + "var u=unit(b),a=-1,z=-1;if(root.contains(r.startContainer)&&root.contains(r.endContainer)){"
                + "a=off(root,r.startContainer,r.startOffset);z=off(root,r.endContainer,r.endOffset);}"
                + "var all=flat(root),pre='',suf='';if(a>=0&&z>=a){pre=all.substring(Math.max(0,a-64),a);"
                + "suf=all.substring(z,Math.min(all.length,z+64));}"
                + "return JSON.stringify({text:s.toString(),anchorText:s.toString(),semanticUnitId:u,startOffset:a,endOffset:z,prefix:pre,suffix:suf});})()";
        webView.evaluateJavascript(script, raw -> {
            SelectionSnapshot selection = decodeSelection(raw);
            if (selection.text.isEmpty()) {
                Toast.makeText(webView.getContext(),
                        "Selecciona primero una palabra o un fragmento.", Toast.LENGTH_SHORT).show();
                return;
            }
            callback.onSelection(selection);
        });
    }

    private static void chooseHighlight(Activity activity, WebView webView,
                                        ReaderContext context, SelectionSnapshot selection,
                                        StudyEntry existing) {
        String[] labels = {
                "Resaltado amarillo", "Resaltado verde", "Resaltado azul",
                "Resaltado rojo", "Resaltado anaranjado", "Resaltado violeta",
                "Subrayado azul", "Subrayado doble", "Caja", "Barra lateral",
                "Pregunta", "Importante"
        };
        String[] colors = {"yellow", "green", "blue", "red", "orange", "violet",
                "blue", "red", "yellow", "red", "red", "red"};
        String[] styles = {"fill", "fill", "fill", "fill", "fill", "fill",
                "underline", "double", "box", "margin", "question", "important"};
        new AlertDialog.Builder(activity).setTitle("Estilo de resaltado")
                .setItems(labels, (dialog, which) -> {
                    StudyEntry entry = existing == null
                            ? base(context, selection, StudyEntry.HIGHLIGHT) : existing;
                    if (existing == null) entry.id = UUID.randomUUID().toString();
                    entry.title = context.reference.isEmpty() ? context.title : context.reference;
                    entry.color = colors[which];
                    entry.style = styles[which];
                    if (selection != null) applySelection(entry, selection);
                    StudyStore.save(activity, entry);
                    removeAnnotation(webView, entry.id);
                    injectHighlight(webView, entry);
                    Toast.makeText(activity, existing == null
                                    ? "Resaltado guardado." : "Estilo actualizado.",
                            Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void createNote(Activity activity, WebView webView, ReaderContext context,
                                   SelectionSnapshot selection) {
        StudyEntry entry = base(context, selection, StudyEntry.NOTE);
        entry.id = UUID.randomUUID().toString();
        entry.title = context.reference.isEmpty() ? context.title : context.reference;
        entry.icon = "note";
        entry.color = "yellow";
        showNoteEditor(activity, webView, entry, true);
    }

    private static void createBookmark(Activity activity, WebView webView, ReaderContext context,
                                       SelectionSnapshot selection) {
        StudyEntry entry = base(context, selection, StudyEntry.BOOKMARK);
        entry.id = UUID.randomUUID().toString();
        entry.title = context.reference.isEmpty() ? context.title : context.reference;
        entry.icon = "bookmark";
        entry.color = "yellow";
        chooseMarkerAppearance(activity, webView, entry, true);
    }

    private static void showNoteEditor(Activity activity, WebView webView,
                                       StudyEntry entry, boolean newEntry) {
        EditText editor = new EditText(activity);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setMinLines(5);
        editor.setMaxLines(12);
        editor.setText(entry.body == null ? "" : entry.body);
        editor.setSelection(editor.getText().length());
        new AlertDialog.Builder(activity)
                .setTitle(newEntry ? "Nueva nota" : "Editar nota")
                .setMessage(shortQuote(entry.quote))
                .setView(editor)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    entry.body = editor.getText().toString().trim();
                    if (entry.body.isEmpty()) {
                        Toast.makeText(activity, "La nota está vacía.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StudyStore.save(activity, entry);
                    removeAnnotation(webView, entry.id);
                    injectMarker(webView, entry);
                    Toast.makeText(activity, "Nota guardada.", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Apariencia", (dialog, which) ->
                        chooseMarkerAppearance(activity, webView, entry, !newEntry))
                .setNegativeButton("Cancelar", null).show();
    }

    private static void chooseMarkerAppearance(Activity activity, WebView webView,
                                               StudyEntry entry, boolean saveAfter) {
        String[] labels = {"Nota", "Favorito", "Idea", "Pregunta", "Importante",
                "Oración", "Estudio", "Marcador"};
        String[] icons = {"note", "star", "idea", "question", "important",
                "prayer", "study", "bookmark"};
        new AlertDialog.Builder(activity).setTitle("Icono")
                .setItems(labels, (dialog, which) -> {
                    entry.icon = icons[which];
                    chooseMarkerColor(activity, webView, entry, saveAfter);
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void chooseMarkerColor(Activity activity, WebView webView,
                                          StudyEntry entry, boolean saveAfter) {
        String[] labels = {"Amarillo", "Rojo", "Naranja", "Verde", "Azul", "Violeta", "Gris"};
        String[] colors = {"yellow", "red", "orange", "green", "blue", "violet", "gray"};
        new AlertDialog.Builder(activity).setTitle("Color")
                .setItems(labels, (dialog, which) -> {
                    entry.color = colors[which];
                    if (saveAfter || !StudyEntry.NOTE.equals(entry.type) || !entry.body.isEmpty()) {
                        StudyStore.save(activity, entry);
                        removeAnnotation(webView, entry.id);
                        injectMarker(webView, entry);
                        Toast.makeText(activity, "Marcador actualizado.", Toast.LENGTH_SHORT).show();
                    } else {
                        showNoteEditor(activity, webView, entry, true);
                    }
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void openMeditationEditor(Activity activity, ReaderContext context,
                                             SelectionSnapshot selection) {
        StudyEntry entry = base(context, selection, StudyEntry.MEDITATION);
        Intent intent = new Intent(activity, StudyEditorActivity.class)
                .putExtra(StudyEditorActivity.EXTRA_TYPE, entry.type)
                .putExtra(StudyEditorActivity.EXTRA_CATEGORY, entry.category)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE, entry.source)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE_KEY, entry.sourceKey)
                .putExtra(StudyEditorActivity.EXTRA_CONTENT_ID, entry.contentId)
                .putExtra(StudyEditorActivity.EXTRA_REFERENCE, entry.reference)
                .putExtra(StudyEditorActivity.EXTRA_QUOTE, entry.quote)
                .putExtra(StudyEditorActivity.EXTRA_ANCHOR_TEXT, entry.anchorText)
                .putExtra(StudyEditorActivity.EXTRA_SEMANTIC_UNIT_ID, entry.semanticUnitId)
                .putExtra(StudyEditorActivity.EXTRA_START_OFFSET, entry.startOffset)
                .putExtra(StudyEditorActivity.EXTRA_END_OFFSET, entry.endOffset)
                .putExtra(StudyEditorActivity.EXTRA_PREFIX, entry.prefix)
                .putExtra(StudyEditorActivity.EXTRA_SUFFIX, entry.suffix);
        activity.startActivity(intent);
    }

    private static void showEntry(Activity activity, WebView webView,
                                  ReaderContext context, String id) {
        StudyEntry entry = find(activity, id);
        if (entry == null) return;
        if (StudyEntry.HIGHLIGHT.equals(entry.type)) {
            showHighlightActions(activity, webView, context, entry);
        } else if (StudyEntry.NOTE.equals(entry.type)) {
            showNote(activity, webView, entry);
        } else if (StudyEntry.BOOKMARK.equals(entry.type)) {
            showBookmark(activity, webView, entry);
        }
    }

    private static void showHighlightActions(Activity activity, WebView webView,
                                             ReaderContext context, StudyEntry entry) {
        String[] actions = {"Cambiar estilo", "Añadir o editar nota", "Compartir",
                "Ampliar al párrafo o versículo", "Eliminar"};
        new AlertDialog.Builder(activity)
                .setTitle(entry.reference.isEmpty() ? "Resaltado" : entry.reference)
                .setMessage(shortQuote(entry.quote))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) chooseHighlight(activity, webView, context,
                            snapshot(entry), entry);
                    else if (which == 1) linkedNote(activity, webView, context, entry);
                    else if (which == 2) share(activity, entry.quote, entry.reference);
                    else if (which == 3) expandHighlight(activity, webView, entry);
                    else confirmDeleteHighlight(activity, webView, entry);
                }).setNegativeButton("Cerrar", null).show();
    }

    private static void linkedNote(Activity activity, WebView webView,
                                   ReaderContext context, StudyEntry highlight) {
        StudyEntry note = linkedNote(activity, highlight);
        if (note == null) {
            note = base(context, snapshot(highlight), StudyEntry.NOTE);
            note.id = UUID.randomUUID().toString();
            note.title = highlight.title;
            note.icon = "note";
            note.color = highlight.color;
        }
        showNoteEditor(activity, webView, note, note.body == null || note.body.isEmpty());
    }

    private static void showNote(Activity activity, WebView webView, StudyEntry entry) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle((entry.reference.isEmpty() ? "Nota" : entry.reference)
                        + "  " + markerGlyph(entry.icon))
                .setMessage((entry.body == null ? "" : entry.body)
                        + (entry.quote.isEmpty() ? "" : "\n\n“" + shortQuote(entry.quote) + "”"))
                .setPositiveButton("Editar", null)
                .setNeutralButton("Compartir", null)
                .setNegativeButton("Más", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                dialog.dismiss();
                showNoteEditor(activity, webView, entry, false);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    share(activity, entry.body + "\n\n" + entry.quote, entry.reference));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                dialog.dismiss();
                new AlertDialog.Builder(activity).setTitle("Nota")
                        .setItems(new String[]{"Cambiar icono o color", "Eliminar nota"},
                                (d, which) -> {
                                    if (which == 0) chooseMarkerAppearance(activity, webView, entry, true);
                                    else confirmDelete(activity, webView, entry, "¿Eliminar esta nota?");
                                }).setNegativeButton("Cancelar", null).show();
            });
        });
        dialog.show();
    }

    private static void showBookmark(Activity activity, WebView webView, StudyEntry entry) {
        new AlertDialog.Builder(activity)
                .setTitle("Marcador " + markerGlyph(entry.icon))
                .setMessage(shortQuote(entry.quote)
                        + (entry.reference.isEmpty() ? "" : "\n\n" + entry.reference))
                .setItems(new String[]{"Cambiar icono o color", "Compartir", "Eliminar"},
                        (dialog, which) -> {
                            if (which == 0) chooseMarkerAppearance(activity, webView, entry, true);
                            else if (which == 1) share(activity, entry.quote, entry.reference);
                            else confirmDelete(activity, webView, entry, "¿Eliminar este marcador?");
                        }).setNegativeButton("Cerrar", null).show();
    }

    private static void expandHighlight(Activity activity, WebView webView, StudyEntry entry) {
        String script = "(function(id){var m=document.querySelector('[data-study-id=\\\"'+id+'\\\"]');"
                + "if(!m)return '';var p=m.closest('.verse,p,li,blockquote');if(!p)return '';"
                + "var t=(p.textContent||'').replace(/\\s+/g,' ').trim();"
                + "var b=p.closest('[data-semantic-id],[data-block],section.ministerium-section');"
                + "var u=b?(b.getAttribute('data-semantic-id')||b.getAttribute('data-block')||''):'';"
                + "return JSON.stringify({text:t,semanticUnitId:u});})('" + js(entry.id) + "')";
        webView.evaluateJavascript(script, raw -> {
            try {
                String decoded = decode(raw);
                if (decoded.isEmpty()) {
                    Toast.makeText(activity, "No hay un bloque mayor disponible.", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject value = new JSONObject(decoded);
                String text = value.optString("text").trim();
                if (text.isEmpty()) return;
                entry.quote = text;
                entry.anchorText = text;
                entry.semanticUnitId = value.optString("semanticUnitId", entry.semanticUnitId);
                entry.startOffset = -1;
                entry.endOffset = -1;
                entry.prefix = "";
                entry.suffix = "";
                entry.anchorVersion = StudyEntry.CURRENT_ANCHOR_VERSION;
                StudyStore.save(activity, entry);
                removeAnnotation(webView, entry.id);
                injectHighlight(webView, entry);
                Toast.makeText(activity, "Resaltado ampliado al bloque.", Toast.LENGTH_SHORT).show();
            } catch (Exception error) {
                Toast.makeText(activity, "No se pudo ampliar el resaltado.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void confirmDeleteHighlight(Activity activity, WebView webView, StudyEntry entry) {
        StudyEntry note = linkedNote(activity, entry);
        if (note == null) {
            confirmDelete(activity, webView, entry, "¿Eliminar este resaltado?");
            return;
        }
        StudyEntry linked = note;
        new AlertDialog.Builder(activity).setTitle("Eliminar resaltado")
                .setItems(new String[]{"Quitar solo el resaltado", "Eliminar resaltado y nota"},
                        (dialog, which) -> {
                            StudyStore.delete(activity, entry.id);
                            removeAnnotation(webView, entry.id);
                            if (which == 1) {
                                StudyStore.delete(activity, linked.id);
                                removeAnnotation(webView, linked.id);
                            }
                        }).setNegativeButton("Cancelar", null).show();
    }

    private static void confirmDelete(Activity activity, WebView webView,
                                      StudyEntry entry, String message) {
        new AlertDialog.Builder(activity).setTitle("Eliminar")
                .setMessage(message).setPositiveButton("Eliminar", (dialog, which) -> {
                    StudyStore.delete(activity, entry.id);
                    removeAnnotation(webView, entry.id);
                }).setNegativeButton("Cancelar", null).show();
    }

    private static StudyEntry linkedNote(Activity activity, StudyEntry highlight) {
        for (StudyEntry value : StudyStore.forSource(activity, highlight.sourceKey)) {
            if (!StudyEntry.NOTE.equals(value.type)) continue;
            if (!highlight.semanticUnitId.isEmpty()
                    && highlight.semanticUnitId.equals(value.semanticUnitId)
                    && highlight.startOffset == value.startOffset
                    && highlight.endOffset == value.endOffset) return value;
            if (!highlight.anchorText.isEmpty() && highlight.anchorText.equals(value.anchorText)) return value;
        }
        return null;
    }

    private static StudyEntry find(Activity activity, String id) {
        if (id == null || id.isEmpty()) return null;
        for (StudyEntry entry : StudyStore.all(activity)) if (id.equals(entry.id)) return entry;
        return null;
    }

    private static StudyEntry base(ReaderContext context, SelectionSnapshot selection, String type) {
        StudyEntry entry = new StudyEntry();
        entry.type = type;
        entry.category = context.category.isEmpty() ? "Documentos/libros" : context.category;
        entry.source = context.source;
        entry.sourceKey = context.sourceKey;
        entry.contentId = context.contentId;
        entry.reference = context.reference;
        applySelection(entry, selection);
        return entry;
    }

    private static void applySelection(StudyEntry entry, SelectionSnapshot selection) {
        if (selection == null) return;
        entry.quote = selection.text;
        entry.anchorText = selection.anchorText;
        entry.semanticUnitId = selection.semanticUnitId;
        entry.startOffset = selection.startOffset;
        entry.endOffset = selection.endOffset;
        entry.prefix = selection.prefix;
        entry.suffix = selection.suffix;
        entry.anchorVersion = StudyEntry.CURRENT_ANCHOR_VERSION;
    }

    public static void restoreHighlights(Activity activity, WebView webView, String sourceKey) {
        ensureStudyCss(webView);
        for (StudyEntry entry : StudyStore.forSource(activity, sourceKey)) {
            if (StudyEntry.HIGHLIGHT.equals(entry.type)) injectHighlight(webView, entry);
            else if (StudyEntry.NOTE.equals(entry.type) || StudyEntry.BOOKMARK.equals(entry.type)) {
                injectMarker(webView, entry);
            }
        }
    }

    private static void ensureStudyCss(WebView webView) {
        String script = "(function(){if(document.getElementById('ministerium-study-css'))return;"
                + "var s=document.createElement('style');s.id='ministerium-study-css';s.textContent='"
                + ".ministerium-study-highlight{cursor:pointer;border-radius:3px;padding:0 1px;}"
                + ".ministerium-study-marker{display:inline-flex!important;align-items:center;justify-content:center;"
                + "min-width:24px!important;height:24px!important;margin-left:5px!important;padding:0 5px!important;"
                + "border:0!important;border-radius:12px!important;font:700 14px sans-serif!important;vertical-align:middle!important;"
                + "box-shadow:0 1px 4px rgba(0,0,0,.22)!important;cursor:pointer!important;-webkit-tap-highlight-color:transparent;}';"
                + "document.head.appendChild(s);})()";
        webView.evaluateJavascript(script, null);
    }

    private static void injectHighlight(WebView webView, StudyEntry entry) {
        ensureStudyCss(webView);
        String css = highlightCss(entry);
        String script = "(function(id,q,u,preferred,pre,suf,css){"
                + "if(document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return true;"
                + "function root(){if(u){var all=document.querySelectorAll('[data-semantic-id],[data-block],section.ministerium-section');"
                + "for(var i=0;i<all.length;i++){var x=all[i].getAttribute('data-semantic-id')||all[i].getAttribute('data-block')||'';"
                + "if(!x&&all[i].matches('section.ministerium-section')){var h=all[i].querySelector('h2');x=h?'combined:'+(h.textContent||'').replace(/\\s+/g,' ').trim().toLowerCase():'';}"
                + "if(x===u)return all[i];}}return document.body;}"
                + "function collect(r){var w=document.createTreeWalker(r,NodeFilter.SHOW_TEXT),n,a=[],t='';while(n=w.nextNode()){"
                + "if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&n.parentNode.tagName!='STYLE'"
                + "&&!n.parentNode.classList.contains('ministerium-study-marker')){a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});t+=n.nodeValue;}}return {a:a,t:t};}"
                + "function locate(t){if(preferred>=0&&t.substr(preferred,q.length)===q)return preferred;var best=-1,score=-1,p=0;"
                + "while((p=t.indexOf(q,p))>=0){var z=0;if(pre&&t.substring(Math.max(0,p-pre.length),p)===pre)z+=2;"
                + "if(suf&&t.substring(p+q.length,p+q.length+suf.length)===suf)z+=2;if(z>score){score=z;best=p;}p+=Math.max(1,q.length);}return best;}"
                + "var r=root(),c=collect(r),at=locate(c.t);if(at<0)return false;var end=at+q.length,sn=null,en=null,so=0,eo=0;"
                + "for(var i=0;i<c.a.length;i++){var x=c.a[i];if(sn===null&&at>=x.s&&at<=x.e){sn=x.n;so=Math.max(0,at-x.s);}"
                + "if(end>=x.s&&end<=x.e){en=x.n;eo=Math.max(0,end-x.s);break;}}if(!sn||!en)return false;"
                + "try{var range=document.createRange();range.setStart(sn,so);range.setEnd(en,eo);var mark=document.createElement('mark');"
                + "mark.className='ministerium-study-highlight';mark.setAttribute('data-study-id',id);mark.setAttribute('style',css);"
                + "var frag=range.extractContents();mark.appendChild(frag);range.insertNode(mark);mark.addEventListener('click',function(ev){"
                + "ev.preventDefault();ev.stopPropagation();if(window.MinisteriumStudy)MinisteriumStudy.openEntry(id);});return true;}catch(e){return false;}})("
                + quote(entry.id) + "," + quote(value(entry.anchorText, entry.quote)) + ","
                + quote(entry.semanticUnitId) + "," + entry.startOffset + ","
                + quote(entry.prefix) + "," + quote(entry.suffix) + "," + quote(css) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static void injectMarker(WebView webView, StudyEntry entry) {
        ensureStudyCss(webView);
        String glyph = markerGlyph(entry.icon);
        String color = markerColor(entry.color);
        String text = markerTextColor(entry.color);
        String script = "(function(id,q,u,preferred,g,bg,fg){if(document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return true;"
                + "function root(){if(u){var all=document.querySelectorAll('[data-semantic-id],[data-block],section.ministerium-section');"
                + "for(var i=0;i<all.length;i++){var x=all[i].getAttribute('data-semantic-id')||all[i].getAttribute('data-block')||'';if(x===u)return all[i];}}return document.body;}"
                + "function collect(r){var w=document.createTreeWalker(r,NodeFilter.SHOW_TEXT),n,a=[],t='';while(n=w.nextNode()){"
                + "if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&n.parentNode.tagName!='STYLE'"
                + "&&!n.parentNode.classList.contains('ministerium-study-marker')){a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});t+=n.nodeValue;}}return {a:a,t:t};}"
                + "var r=root(),c=collect(r),at=(preferred>=0&&c.t.substr(preferred,q.length)===q)?preferred:c.t.indexOf(q);if(at<0)return false;"
                + "var end=at+q.length,en=null,eo=0;for(var i=0;i<c.a.length;i++){var x=c.a[i];if(end>=x.s&&end<=x.e){en=x.n;eo=Math.max(0,end-x.s);break;}}"
                + "if(!en)return false;var range=document.createRange();range.setStart(en,eo);range.collapse(true);var m=document.createElement('button');"
                + "m.type='button';m.className='ministerium-study-marker';m.setAttribute('data-study-id',id);m.textContent=g;"
                + "m.style.backgroundColor=bg;m.style.color=fg;m.style.webkitTextFillColor=fg;m.addEventListener('click',function(ev){"
                + "ev.preventDefault();ev.stopPropagation();if(window.MinisteriumStudy)MinisteriumStudy.openEntry(id);});range.insertNode(m);return true;})("
                + quote(entry.id) + "," + quote(value(entry.anchorText, entry.quote)) + ","
                + quote(entry.semanticUnitId) + "," + entry.startOffset + "," + quote(glyph)
                + "," + quote(color) + "," + quote(text) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static void removeAnnotation(WebView webView, String id) {
        String script = "(function(id){var e=document.querySelector('[data-study-id=\\\"'+id+'\\\"]');if(!e)return;"
                + "if(e.tagName==='MARK'){var p=e.parentNode;while(e.firstChild)p.insertBefore(e.firstChild,e);p.removeChild(e);p.normalize();}"
                + "else e.remove();})(" + quote(id) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static String highlightCss(StudyEntry entry) {
        String color = markerColor(entry.color);
        String translucent = highlightColor(entry.color);
        String style = value(entry.style, "fill");
        if ("underline".equals(style)) return "background:transparent;color:inherit;text-decoration:underline 2px " + color + ";text-underline-offset:3px";
        if ("double".equals(style)) return "background:transparent;color:inherit;border-bottom:4px double " + color;
        if ("box".equals(style)) return "background:transparent;color:inherit;outline:2px solid " + color + ";outline-offset:1px";
        if ("margin".equals(style)) return "background:transparent;color:inherit;border-left:4px solid " + color + ";padding-left:4px";
        if ("question".equals(style)) return "background:transparent;color:inherit;border-bottom:2px dashed " + color;
        if ("important".equals(style)) return "background:" + translucent + ";color:inherit;font-weight:700;border-left:3px solid " + color;
        return "background:" + translucent + ";color:inherit";
    }

    private static String highlightColor(String color) {
        if ("green".equals(color)) return "#9FD79A";
        if ("blue".equals(color)) return "#9CC9F0";
        if ("red".equals(color)) return "#F2AAA7";
        if ("orange".equals(color)) return "#F1B37C";
        if ("violet".equals(color)) return "#C2A4E8";
        if ("gray".equals(color)) return "#C8C8C8";
        return "#F5D56A";
    }

    private static String markerColor(String color) {
        if ("green".equals(color)) return "#4F9A45";
        if ("blue".equals(color)) return "#397DB5";
        if ("red".equals(color)) return "#B84A48";
        if ("orange".equals(color)) return "#B9692E";
        if ("violet".equals(color)) return "#7753A6";
        if ("gray".equals(color)) return "#686868";
        return "#E0A91A";
    }

    private static String markerTextColor(String color) {
        return "yellow".equals(color) ? "#2A2521" : "#FFFFFF";
    }

    private static String markerGlyph(String icon) {
        if ("star".equals(icon)) return "★";
        if ("idea".equals(icon)) return "✦";
        if ("question".equals(icon)) return "?";
        if ("important".equals(icon)) return "!";
        if ("prayer".equals(icon)) return "✝";
        if ("study".equals(icon)) return "A";
        if ("bookmark".equals(icon)) return "◆";
        return "●";
    }

    private static void openDictionary(Activity activity, String selected) {
        String query = selected.replaceAll("[\\r\\n]+", " ").trim();
        if (query.length() > 80) query = query.substring(0, 80).trim();
        String finalQuery = query;
        if (!query.contains(" ")) {
            new Thread(() -> {
                try {
                    List<BibleDictionaryRepository.QuickResult> results =
                            BibleDictionaryRepository.quickLookup(activity, finalQuery);
                    activity.runOnUiThread(() -> {
                        StringBuilder html = new StringBuilder();
                        for (BibleDictionaryRepository.QuickResult result : results) html.append(result.html);
                        html.append(RaeOnlineRepository.actionCard(finalQuery));
                        if (results.isEmpty()) {
                            html.insert(0, "<article class=\"dictionary-card\"><h2>Diccionarios offline</h2>"
                                    + "<p>No hubo coincidencia exacta. Puedes abrir el catálogo completo desde Diccionarios o consultar RAE en línea.</p></article>");
                        }
                        ReaderOverlayDialog.show(activity, "Diccionario · " + finalQuery,
                                html.toString());
                    });
                } catch (Exception error) {
                    activity.runOnUiThread(() -> ReaderOverlayDialog.show(activity,
                            "Diccionario · " + finalQuery,
                            RaeOnlineRepository.actionCard(finalQuery)));
                }
            }).start();
            return;
        }
        openDictionaryChooser(activity, finalQuery);
    }

    private static void openDictionaryChooser(Activity activity, String query) {
        List<BibleDictionaryRepository.Source> sources = BibleDictionaryRepository.sources();
        if (sources.isEmpty()) {
            Toast.makeText(activity, "No hay diccionarios disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) labels[i] = sources.get(i).title;
        new AlertDialog.Builder(activity).setTitle("Buscar en diccionario")
                .setItems(labels, (dialog, which) -> {
                    BibleDictionaryRepository.Source source = sources.get(which);
                    activity.startActivity(new Intent(activity, BibleDictionaryActivity.class)
                            .putExtra(BibleDictionaryActivity.EXTRA_SOURCE_ID, source.id)
                            .putExtra(BibleDictionaryActivity.EXTRA_QUERY, query));
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void openNearestBibleCommentary(Activity activity, WebView webView,
                                                    ActionMode mode) {
        String script = "(function(){var s=window.getSelection&&window.getSelection();"
                + "if(!s||!s.rangeCount)return '';var r=s.getRangeAt(0);"
                + "var n=r.startContainer.nodeType===1?r.startContainer:r.startContainer.parentNode;"
                + "var p=n&&n.closest?n.closest('p,.verse,.reading-section'):null;"
                + "var a=p&&p.querySelector?p.querySelector('a[href*=\\\"#\\\"]'):null;"
                + "if(!a&&n&&n.parentNode&&n.parentNode.querySelector)a=n.parentNode.querySelector('a[href*=\\\"#\\\"]');"
                + "return a?a.href:'';})()";
        webView.evaluateJavascript(script, raw -> {
            String href = decode(raw);
            if (href.isEmpty()) {
                Toast.makeText(activity, "No hay un comentario o nota enlazado a esta selección.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mode.finish();
            webView.loadUrl(href);
        });
    }

    private static void openTranslation(Activity activity, String selected) {
        try {
            Uri uri = Uri.parse("https://translate.google.com/?sl=auto&tl=es&op=translate&text="
                    + Uri.encode(selected));
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            Toast.makeText(activity, "No se pudo abrir el traductor.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void share(Activity activity, String text, String reference) {
        String value = (text == null ? "" : text.trim())
                + (reference == null || reference.trim().isEmpty() ? "" : "\n\n" + reference.trim());
        try {
            activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain").putExtra(Intent.EXTRA_TEXT, value), "Compartir"));
        } catch (Exception error) {
            Toast.makeText(activity, "No se pudo compartir este texto.", Toast.LENGTH_SHORT).show();
        }
    }

    private static SelectionSnapshot snapshot(StudyEntry entry) {
        SelectionSnapshot value = new SelectionSnapshot();
        value.text = entry.quote;
        value.anchorText = value(entry.anchorText, entry.quote);
        value.semanticUnitId = entry.semanticUnitId;
        value.startOffset = entry.startOffset;
        value.endOffset = entry.endOffset;
        value.prefix = entry.prefix;
        value.suffix = entry.suffix;
        return value;
    }

    private static SelectionSnapshot decodeSelection(String raw) {
        SelectionSnapshot result = new SelectionSnapshot();
        try {
            String decoded = decode(raw);
            if (decoded.isEmpty()) return result;
            JSONObject value = new JSONObject(decoded);
            result.text = value.optString("text").trim();
            result.anchorText = value.optString("anchorText", result.text);
            result.semanticUnitId = value.optString("semanticUnitId");
            result.startOffset = value.optInt("startOffset", -1);
            result.endOffset = value.optInt("endOffset", -1);
            result.prefix = value.optString("prefix");
            result.suffix = value.optString("suffix");
        } catch (Exception ignored) {}
        return result;
    }

    private static String decode(String raw) {
        if (raw == null || "null".equals(raw)) return "";
        try {
            Object value = new JSONTokener(raw).nextValue();
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            return raw.replaceFirst("^\\\"", "").replaceFirst("\\\"$", "")
                    .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        }
    }

    private static String shortQuote(String text) {
        String value = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return value.length() <= 220 ? value : value.substring(0, 217) + "…";
    }

    private static String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String quote(String value) { return JSONObject.quote(value == null ? "" : value); }
    private static String js(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'"); }

    private interface SelectionCallback { void onSelection(SelectionSnapshot selection); }

    private static final class SelectionSnapshot {
        String text = "";
        String anchorText = "";
        String semanticUnitId = "";
        int startOffset = -1;
        int endOffset = -1;
        String prefix = "";
        String suffix = "";
    }

    private static final class StudyBridge {
        private final Activity activity;
        private final WebView webView;
        private final ReaderContext context;

        StudyBridge(Activity activity, WebView webView, ReaderContext context) {
            this.activity = activity;
            this.webView = webView;
            this.context = context;
        }

        @JavascriptInterface public void openEntry(String id) {
            activity.runOnUiThread(() -> showEntry(activity, webView, context, id));
        }
    }
}
