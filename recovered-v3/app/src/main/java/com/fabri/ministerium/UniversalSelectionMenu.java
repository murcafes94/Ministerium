package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

/** Acciones contextuales explícitas. Seleccionar texto nunca ejecuta una acción por sí solo. */
public final class UniversalSelectionMenu {
    private static final int HIGHLIGHT = 8901;
    private static final int NOTE = 8902;
    private static final int MEDITATION = 8903;
    private static final int DICTIONARY = 8904;
    private static final int COMMENTARY = 8905;
    private static final int TRANSLATE = 8906;
    private static final int READ_ALOUD = 8907;

    private UniversalSelectionMenu() {}

    public static void attach(Activity activity, WebView webView, ReaderContext context) {
        if (!(webView instanceof MinisteriumWebView)) return;
        MinisteriumWebView reader = (MinisteriumWebView) webView;
        reader.setSelectionActionHandler(new MinisteriumWebView.SelectionActionHandler() {
            @Override public void populate(Menu menu) {
                add(menu, HIGHLIGHT, "Subrayar");
                add(menu, NOTE, "Nota");
                add(menu, MEDITATION, "Reflexión");
                add(menu, DICTIONARY, "Diccionario");
                if ("Biblia".equalsIgnoreCase(context.category)) {
                    add(menu, COMMENTARY, "Comentario");
                }
                add(menu, TRANSLATE, "Traducir");
                add(menu, READ_ALOUD, "Leer");
                // Copiar/Compartir se conservan del ActionMode nativo de Android.
            }

            @Override public boolean handle(ActionMode mode, MenuItem item) {
                int id = item.getItemId();
                if (id == HIGHLIGHT) {
                    capture(webView, selection -> {
                        mode.finish();
                        chooseHighlight(activity, webView, context, selection);
                    });
                    return true;
                }
                if (id == NOTE) {
                    capture(webView, selection -> {
                        mode.finish();
                        openEditor(activity, context, selection, StudyEntry.NOTE);
                    });
                    return true;
                }
                if (id == MEDITATION) {
                    capture(webView, selection -> {
                        mode.finish();
                        openEditor(activity, context, selection, StudyEntry.MEDITATION);
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
                if (id == READ_ALOUD) {
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

    /**
     * Captura el texto y, cuando el renderizador expone una unidad semántica,
     * también la unidad estable y los offsets dentro de ella. En la celebración
     * combinada, cada section.ministerium-section se identifica por su encabezado.
     * Los lectores antiguos siguen funcionando con quote como fallback.
     */
    private static void capture(WebView webView, SelectionCallback callback) {
        String script = "(function(){var s=window.getSelection&&window.getSelection();"
                + "if(!s||!s.rangeCount||!s.toString().trim())return '';var r=s.getRangeAt(0);"
                + "function unit(root){if(!root)return '';var id=root.getAttribute&&"
                + "(root.getAttribute('data-semantic-id')||root.getAttribute('data-block'));if(id)return id;"
                + "if(root.matches&&root.matches('section.ministerium-section')){var h=root.querySelector('h2');"
                + "var t=h?(h.textContent||'').replace(/\\s+/g,' ').trim().toLowerCase():'';return t?'combined:'+t:'';}return '';}"
                + "var n=r.commonAncestorContainer.nodeType===1?r.commonAncestorContainer:r.commonAncestorContainer.parentElement;"
                + "var b=n&&n.closest?n.closest('[data-semantic-id],[data-block],section.ministerium-section'):null;"
                + "var u=unit(b);"
                + "function off(root,node,o){if(!root||!node||node.nodeType!==3)return -1;"
                + "var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),x,t=0;"
                + "while(x=w.nextNode()){if(x===node)return t+o;t+=x.nodeValue.length;}return -1;}"
                + "var a=-1,z=-1;if(b&&b.contains(r.startContainer)&&b.contains(r.endContainer)){"
                + "a=off(b,r.startContainer,r.startOffset);z=off(b,r.endContainer,r.endOffset);}"
                + "return JSON.stringify({text:s.toString(),semanticUnitId:u,startOffset:a,endOffset:z});})()";
        webView.evaluateJavascript(script, raw -> {
            SelectionSnapshot selection = decodeSelection(raw);
            if (selection.text.isEmpty()) {
                Toast.makeText(webView.getContext(),
                        "Selecciona primero una palabra o un fragmento.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            callback.onSelection(selection);
        });
    }

    private static void chooseHighlight(Activity activity, WebView webView,
                                        ReaderContext context, SelectionSnapshot selection) {
        String[] labels = {"Amarillo", "Verde", "Azul", "Rojo", "Gris"};
        String[] colors = {"yellow", "green", "blue", "red", "gray"};
        new AlertDialog.Builder(activity).setTitle("Color del subrayado")
                .setItems(labels, (dialog, which) -> {
                    StudyEntry entry = base(context, selection, StudyEntry.HIGHLIGHT);
                    entry.id = UUID.randomUUID().toString();
                    entry.title = context.reference.isEmpty() ? context.title : context.reference;
                    entry.color = colors[which];
                    StudyStore.save(activity, entry);
                    injectHighlight(webView, entry);
                    Toast.makeText(activity, "Subrayado guardado en Mi estudio.",
                            Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void openEditor(Activity activity, ReaderContext context,
                                   SelectionSnapshot selection, String type) {
        StudyEntry entry = base(context, selection, type);
        Intent intent = new Intent(activity, StudyEditorActivity.class)
                .putExtra(StudyEditorActivity.EXTRA_TYPE, entry.type)
                .putExtra(StudyEditorActivity.EXTRA_CATEGORY, entry.category)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE, entry.source)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE_KEY, entry.sourceKey)
                .putExtra(StudyEditorActivity.EXTRA_REFERENCE, entry.reference)
                .putExtra(StudyEditorActivity.EXTRA_QUOTE, entry.quote)
                .putExtra(StudyEditorActivity.EXTRA_SEMANTIC_UNIT_ID, entry.semanticUnitId)
                .putExtra(StudyEditorActivity.EXTRA_START_OFFSET, entry.startOffset)
                .putExtra(StudyEditorActivity.EXTRA_END_OFFSET, entry.endOffset);
        activity.startActivity(intent);
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
                        if (results.isEmpty()) {
                            openDictionaryChooser(activity, finalQuery);
                            return;
                        }
                        StringBuilder html = new StringBuilder();
                        for (BibleDictionaryRepository.QuickResult result : results) {
                            html.append(result.html);
                        }
                        ReaderOverlayDialog.show(activity,
                                "Diccionario · " + finalQuery, html.toString());
                    });
                } catch (Exception error) {
                    activity.runOnUiThread(() -> openDictionaryChooser(activity, finalQuery));
                }
            }).start();
            return;
        }
        openDictionaryChooser(activity, finalQuery);
    }

    private static void openDictionaryChooser(Activity activity, String query) {
        List<BibleDictionaryRepository.Source> sources = BibleDictionaryRepository.sources();
        if (sources.isEmpty()) {
            Toast.makeText(activity, "No hay diccionarios disponibles.",
                    Toast.LENGTH_SHORT).show();
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
                + "if(!a&&n&&n.parentNode&&n.parentNode.querySelector)"
                + "a=n.parentNode.querySelector('a[href*=\\\"#\\\"]');"
                + "return a?a.href:'';})()";
        webView.evaluateJavascript(script, raw -> {
            String href = decode(raw);
            if (href.isEmpty()) {
                Toast.makeText(activity,
                        "No hay un comentario o nota enlazado a esta selección.",
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
            Toast.makeText(activity, "No se pudo abrir el traductor.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static StudyEntry base(ReaderContext context, SelectionSnapshot selection,
                                   String type) {
        StudyEntry entry = new StudyEntry();
        entry.type = type;
        entry.category = context.category.isEmpty() ? "Documentos/libros" : context.category;
        entry.source = context.source;
        entry.sourceKey = context.sourceKey;
        entry.reference = context.reference;
        entry.quote = selection.text;
        entry.semanticUnitId = selection.semanticUnitId;
        entry.startOffset = selection.startOffset;
        entry.endOffset = selection.endOffset;
        return entry;
    }

    public static void restoreHighlights(Activity activity, WebView webView,
                                         String sourceKey) {
        for (StudyEntry entry : StudyStore.forSource(activity, sourceKey)) {
            if (StudyEntry.HIGHLIGHT.equals(entry.type)) injectHighlight(webView, entry);
        }
    }

    private static void injectHighlight(WebView webView, StudyEntry entry) {
        String hex;
        if ("green".equals(entry.color)) hex = "#A8D5A2";
        else if ("blue".equals(entry.color)) hex = "#A9C7E8";
        else if ("red".equals(entry.color)) hex = "#E9AAA7";
        else if ("gray".equals(entry.color)) hex = "#C7C7C7";
        else hex = "#F4D77A";

        String script = "(function(q,id,c,u,s,e){"
                + "if(document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return;"
                + "function unit(root){if(!root)return '';var id=root.getAttribute&&"
                + "(root.getAttribute('data-semantic-id')||root.getAttribute('data-block'));if(id)return id;"
                + "if(root.matches&&root.matches('section.ministerium-section')){var h=root.querySelector('h2');"
                + "var t=h?(h.textContent||'').replace(/\\s+/g,' ').trim().toLowerCase():'';return t?'combined:'+t:'';}return '';}"
                + "function mark(st,so,en,eo){try{var r=document.createRange();r.setStart(st,so);r.setEnd(en,eo);"
                + "var m=document.createElement('mark');m.setAttribute('data-study-id',id);"
                + "m.style.backgroundColor=c;m.style.color='#1f1b18';m.appendChild(r.extractContents());"
                + "r.insertNode(m);return true;}catch(x){return false;}}"
                + "function anchored(){if(!u||s<0||e<=s)return false;"
                + "var roots=document.querySelectorAll('[data-semantic-id],[data-block],section.ministerium-section'),root=null;"
                + "for(var h=0;h<roots.length;h++){if(unit(roots[h])===u){root=roots[h];break;}}"
                + "if(!root)return false;var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),n,t=0,st=null,en=null,so=0,eo=0;"
                + "while(n=w.nextNode()){var l=n.nodeValue.length;if(st===null&&s>=t&&s<=t+l){st=n;so=s-t;}"
                + "if(e>=t&&e<=t+l){en=n;eo=e-t;break;}t+=l;}return st&&en?mark(st,so,en,eo):false;}"
                + "if(anchored())return;var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),n,a=[],t='';"
                + "while(n=w.nextNode()){if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&n.parentNode.tagName!='STYLE'){"
                + "a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});t+=n.nodeValue;}}"
                + "var i=t.indexOf(q);if(i<0)return;var j=i+q.length,st=null,en=null,so=0,eo=0;"
                + "for(var k=0;k<a.length;k++){if(st===null&&i>=a[k].s&&i<=a[k].e){st=a[k].n;so=i-a[k].s;}"
                + "if(j>=a[k].s&&j<=a[k].e){en=a[k].n;eo=j-a[k].s;break;}}if(st&&en)mark(st,so,en,eo);"
                + "})(" + JSONObject.quote(entry.quote) + "," + JSONObject.quote(entry.id) + ","
                + JSONObject.quote(hex) + "," + JSONObject.quote(entry.semanticUnitId) + ","
                + entry.startOffset + "," + entry.endOffset + ")";
        webView.evaluateJavascript(script, null);
    }

    private static SelectionSnapshot decodeSelection(String raw) {
        try {
            Object value = new org.json.JSONTokener(raw).nextValue();
            if (value == null) return SelectionSnapshot.EMPTY;
            JSONObject json = value instanceof JSONObject
                    ? (JSONObject) value : new JSONObject(value.toString());
            String text = json.optString("text").replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) return SelectionSnapshot.EMPTY;
            return new SelectionSnapshot(text, json.optString("semanticUnitId"),
                    json.optInt("startOffset", -1), json.optInt("endOffset", -1));
        } catch (Exception ignored) {
            return SelectionSnapshot.EMPTY;
        }
    }

    private static String decode(String raw) {
        try {
            Object value = new org.json.JSONTokener(raw).nextValue();
            return value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private interface SelectionCallback { void onSelection(SelectionSnapshot selection); }

    private static final class SelectionSnapshot {
        static final SelectionSnapshot EMPTY = new SelectionSnapshot("", "", -1, -1);
        final String text;
        final String semanticUnitId;
        final int startOffset;
        final int endOffset;

        SelectionSnapshot(String text, String semanticUnitId, int startOffset, int endOffset) {
            this.text = text == null ? "" : text;
            this.semanticUnitId = semanticUnitId == null ? "" : semanticUnitId;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
