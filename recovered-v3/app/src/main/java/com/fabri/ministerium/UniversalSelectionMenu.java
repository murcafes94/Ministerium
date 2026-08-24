package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

            @Override public boolean handle(MenuItem item) {
                int id = item.getItemId();
                if (id == HIGHLIGHT) {
                    capture(webView, selected ->
                            chooseHighlight(activity, webView, context, selected));
                    return true;
                }
                if (id == NOTE) {
                    capture(webView, selected ->
                            openEditor(activity, context, selected, StudyEntry.NOTE));
                    return true;
                }
                if (id == MEDITATION) {
                    capture(webView, selected ->
                            openEditor(activity, context, selected, StudyEntry.MEDITATION));
                    return true;
                }
                if (id == DICTIONARY) {
                    capture(webView, selected -> openDictionary(activity, selected));
                    return true;
                }
                if (id == COMMENTARY) {
                    openNearestBibleCommentary(activity, webView);
                    return true;
                }
                if (id == TRANSLATE) {
                    capture(webView, selected -> openTranslation(activity, selected));
                    return true;
                }
                if (id == READ_ALOUD) {
                    capture(webView, selected ->
                            ReaderTtsController.speakSelection(activity, selected));
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
        webView.evaluateJavascript("(window.getSelection?window.getSelection().toString():'')",
                raw -> {
                    String selected = decode(raw);
                    if (selected.isEmpty()) {
                        Toast.makeText(webView.getContext(),
                                "Selecciona primero una palabra o un fragmento.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callback.onSelection(selected);
                });
    }

    private static void chooseHighlight(Activity activity, WebView webView,
                                        ReaderContext context, String selected) {
        String[] labels = {"Amarillo", "Verde", "Azul", "Rojo", "Gris"};
        String[] colors = {"yellow", "green", "blue", "red", "gray"};
        new AlertDialog.Builder(activity).setTitle("Color del subrayado")
                .setItems(labels, (dialog, which) -> {
                    StudyEntry entry = base(context, selected, StudyEntry.HIGHLIGHT);
                    entry.id = UUID.randomUUID().toString();
                    entry.title = context.reference.isEmpty() ? context.title : context.reference;
                    entry.color = colors[which];
                    StudyStore.save(activity, entry);
                    injectHighlight(webView, selected, entry.color, entry.id);
                    Toast.makeText(activity, "Subrayado guardado en Mi estudio.",
                            Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void openEditor(Activity activity, ReaderContext context,
                                   String selected, String type) {
        StudyEntry entry = base(context, selected, type);
        Intent intent = new Intent(activity, StudyEditorActivity.class)
                .putExtra(StudyEditorActivity.EXTRA_TYPE, entry.type)
                .putExtra(StudyEditorActivity.EXTRA_CATEGORY, entry.category)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE, entry.source)
                .putExtra(StudyEditorActivity.EXTRA_SOURCE_KEY, entry.sourceKey)
                .putExtra(StudyEditorActivity.EXTRA_REFERENCE, entry.reference)
                .putExtra(StudyEditorActivity.EXTRA_QUOTE, entry.quote);
        activity.startActivity(intent);
    }

    private static void openDictionary(Activity activity, String selected) {
        String query = selected.replaceAll("[\\r\\n]+", " ").trim();
        if (query.length() > 80) query = query.substring(0, 80).trim();
        List<BibleDictionaryRepository.Source> sources = BibleDictionaryRepository.sources();
        if (sources.isEmpty()) {
            Toast.makeText(activity, "No hay diccionarios disponibles.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) labels[i] = sources.get(i).title;
        String finalQuery = query;
        new AlertDialog.Builder(activity).setTitle("Buscar en diccionario")
                .setItems(labels, (dialog, which) -> {
                    BibleDictionaryRepository.Source source = sources.get(which);
                    activity.startActivity(new Intent(activity, BibleDictionaryActivity.class)
                            .putExtra(BibleDictionaryActivity.EXTRA_SOURCE_ID, source.id)
                            .putExtra(BibleDictionaryActivity.EXTRA_QUERY, finalQuery));
                }).setNegativeButton("Cancelar", null).show();
    }

    private static void openNearestBibleCommentary(Activity activity, WebView webView) {
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

    private static StudyEntry base(ReaderContext context, String selected, String type) {
        StudyEntry entry = new StudyEntry();
        entry.type = type;
        entry.category = context.category.isEmpty() ? "Documentos/libros" : context.category;
        entry.source = context.source;
        entry.sourceKey = context.sourceKey;
        entry.reference = context.reference;
        entry.quote = selected;
        return entry;
    }

    public static void restoreHighlights(Activity activity, WebView webView,
                                         String sourceKey) {
        for (StudyEntry entry : StudyStore.forSource(activity, sourceKey)) {
            if (StudyEntry.HIGHLIGHT.equals(entry.type)) {
                injectHighlight(webView, entry.quote, entry.color, entry.id);
            }
        }
    }

    private static void injectHighlight(WebView webView, String quote, String color,
                                        String id) {
        String hex;
        if ("green".equals(color)) hex = "#A8D5A2";
        else if ("blue".equals(color)) hex = "#A9C7E8";
        else if ("red".equals(color)) hex = "#E9AAA7";
        else if ("gray".equals(color)) hex = "#C7C7C7";
        else hex = "#F4D77A";
        String script = "(function(q,id,c){if(document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return;"
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),n,a=[],t='';"
                + "while(n=w.nextNode()){if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&"
                + "n.parentNode.tagName!='STYLE'){a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});"
                + "t+=n.nodeValue;}}var i=t.indexOf(q);if(i<0)return;var j=i+q.length,st,en,so=0,eo=0;"
                + "for(var k=0;k<a.length;k++){if(!st&&i>=a[k].s&&i<=a[k].e){st=a[k].n;so=i-a[k].s;}"
                + "if(j>=a[k].s&&j<=a[k].e){en=a[k].n;eo=j-a[k].s;break;}}if(!st||!en)return;"
                + "try{var r=document.createRange();r.setStart(st,so);r.setEnd(en,eo);"
                + "var m=document.createElement('mark');m.setAttribute('data-study-id',id);"
                + "m.style.backgroundColor=c;m.style.color='#1f1b18';m.appendChild(r.extractContents());"
                + "r.insertNode(m);}catch(e){}})(" + JSONObject.quote(quote) + ","
                + JSONObject.quote(id) + "," + JSONObject.quote(hex) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static String decode(String raw) {
        try {
            Object value = new org.json.JSONTokener(raw).nextValue();
            return value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private interface SelectionCallback { void onSelection(String selected); }
}
