package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

/**
 * Acciones contextuales explícitas. Seleccionar texto nunca ejecuta una acción
 * por sí solo. Las anclas usan unidad semántica + offsets + texto/contexto para
 * sobrevivir a cambios menores del documento.
 */
public final class UniversalSelectionMenu {
    private static final int HIGHLIGHT = 8901;
    private static final int NOTE = 8902;
    private static final int MEDITATION = 8903;
    private static final int DICTIONARY = 8904;
    private static final int COMMENTARY = 8905;
    private static final int TRANSLATE = 8906;
    private static final int READ_ALOUD = 8907;
    private static final int MORE = 8908;

    private UniversalSelectionMenu() {}

    public static void attach(Activity activity, WebView webView, ReaderContext context) {
        if (!(webView instanceof MinisteriumWebView)) return;
        MinisteriumWebView reader = (MinisteriumWebView) webView;
        reader.setSelectionActionHandler(new MinisteriumWebView.SelectionActionHandler() {
            @Override public void populate(Menu menu) {
                add(menu, HIGHLIGHT, "Subrayar");
                add(menu, NOTE, "Nota");
                add(menu, DICTIONARY, "Diccionario");
                SubMenu more = menu.addSubMenu(Menu.NONE, MORE, 120, "Más");
                more.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
                + "while(x=w.nextNode()){if(x.parentNode&&x.parentNode.tagName!='SCRIPT'&&x.parentNode.tagName!='STYLE')t+=x.nodeValue;}return t;}"
                + "function off(root,node,o){if(!root||!node||node.nodeType!==3)return -1;"
                + "var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),x,t=0;while(x=w.nextNode()){"
                + "if(x.parentNode&&x.parentNode.tagName!='SCRIPT'&&x.parentNode.tagName!='STYLE'){"
                + "if(x===node)return t+o;t+=x.nodeValue.length;}}return -1;}"
                + "var n=r.commonAncestorContainer.nodeType===1?r.commonAncestorContainer:r.commonAncestorContainer.parentElement;"
                + "var b=n&&n.closest?n.closest('[data-semantic-id],[data-block],section.ministerium-section'):null;"
                + "var anchorRoot=b||(n&&n.closest?n.closest('p,li,blockquote,section,div'):null)||document.body;"
                + "var u=unit(b),a=-1,z=-1;if(anchorRoot.contains(r.startContainer)&&anchorRoot.contains(r.endContainer)){"
                + "a=off(anchorRoot,r.startContainer,r.startOffset);z=off(anchorRoot,r.endContainer,r.endOffset);}"
                + "var all=flat(anchorRoot),pre='',suf='';if(a>=0&&z>=a){pre=all.substring(Math.max(0,a-64),a);"
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
                + "if(!a&&n&&n.parentNode&&n.parentNode.querySelector)"
                + "a=n.parentNode.querySelector('a[href*=\\\"#\\\"]');return a?a.href:'';})()";
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
            Toast.makeText(activity, "No se pudo abrir el traductor.", Toast.LENGTH_SHORT).show();
        }
    }

    private static StudyEntry base(ReaderContext context, SelectionSnapshot selection, String type) {
        StudyEntry entry = new StudyEntry();
        entry.type = type;
        entry.category = context.category.isEmpty() ? "Documentos/libros" : context.category;
        entry.source = context.source;
        entry.sourceKey = context.sourceKey;
        entry.contentId = context.contentId;
        entry.reference = context.reference;
        entry.quote = selection.text;
        entry.anchorText = selection.anchorText;
        entry.semanticUnitId = selection.semanticUnitId;
        entry.startOffset = selection.startOffset;
        entry.endOffset = selection.endOffset;
        entry.prefix = selection.prefix;
        entry.suffix = selection.suffix;
        entry.anchorVersion = StudyEntry.CURRENT_ANCHOR_VERSION;
        return entry;
    }

    public static void restoreHighlights(Activity activity, WebView webView, String sourceKey) {
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

        String script = "(function(q,xq,id,c,u,s,e,pre,suf){"
                + "if(document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return true;"
                + "function unit(root){if(!root)return '';var v=root.getAttribute&&"
                + "(root.getAttribute('data-semantic-id')||root.getAttribute('data-block'));if(v)return v;"
                + "if(root.matches&&root.matches('section.ministerium-section')){var h=root.querySelector('h2');"
                + "var t=h?(h.textContent||'').replace(/\\s+/g,' ').trim().toLowerCase():'';return t?'combined:'+t:'';}return '';}"
                + "function collect(root){var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),n,a=[],t='';"
                + "while(n=w.nextNode()){if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&n.parentNode.tagName!='STYLE'){"
                + "a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});t+=n.nodeValue;}}return {a:a,t:t};}"
                + "function score(t,i,len){var z=0;if(pre){var p=t.substring(Math.max(0,i-pre.length),i);"
                + "if(p===pre)z+=4;else if(p.endsWith(pre.substring(Math.max(0,pre.length-24))))z+=2;}"
                + "if(suf){var f=t.substring(i+len,i+len+suf.length);if(f===suf)z+=4;"
                + "else if(f.startsWith(suf.substring(0,Math.min(24,suf.length))))z+=2;}return z;}"
                + "function locate(t,needle,preferred){if(!needle)return -1;"
                + "if(preferred>=0&&preferred+needle.length<=t.length&&t.substr(preferred,needle.length)===needle)return preferred;"
                + "var best=-1,bestScore=-1,p=0;while((p=t.indexOf(needle,p))>=0){var sc=score(t,p,needle.length);"
                + "if(sc>bestScore){best=p;bestScore=sc;}p+=Math.max(1,needle.length);}return best;}"
                + "function mark(root,start,len){var d=collect(root),a=d.a,end=start+len,st=null,en=null,so=0,eo=0;"
                + "for(var k=0;k<a.length;k++){if(st===null&&start>=a[k].s&&start<=a[k].e){st=a[k].n;so=start-a[k].s;}"
                + "if(end>=a[k].s&&end<=a[k].e){en=a[k].n;eo=end-a[k].s;break;}}"
                + "if(!st||!en)return false;try{var r=document.createRange();r.setStart(st,so);r.setEnd(en,eo);"
                + "var m=document.createElement('mark');m.setAttribute('data-study-id',id);m.style.backgroundColor=c;"
                + "m.style.color='#1f1b18';m.appendChild(r.extractContents());r.insertNode(m);return true;}catch(err){return false;}}"
                + "function tryRoot(root,preferred){if(!root)return false;var d=collect(root),needle=xq||q;"
                + "var i=locate(d.t,needle,preferred);if(i<0&&q!==needle)i=locate(d.t,q,preferred);"
                + "return i>=0?mark(root,i,(i>=0&&d.t.substr(i,needle.length)===needle)?needle.length:q.length):false;}"
                + "if(u){var roots=document.querySelectorAll('[data-semantic-id],[data-block],section.ministerium-section');"
                + "for(var h=0;h<roots.length;h++){if(unit(roots[h])===u&&tryRoot(roots[h],s))return true;}}"
                + "return tryRoot(document.body,-1);"
                + "})(" + JSONObject.quote(entry.quote) + ","
                + JSONObject.quote(entry.anchorText.isEmpty() ? entry.quote : entry.anchorText) + ","
                + JSONObject.quote(entry.id) + "," + JSONObject.quote(hex) + ","
                + JSONObject.quote(entry.semanticUnitId) + "," + entry.startOffset + ","
                + entry.endOffset + "," + JSONObject.quote(entry.prefix) + ","
                + JSONObject.quote(entry.suffix) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static SelectionSnapshot decodeSelection(String raw) {
        try {
            Object value = new org.json.JSONTokener(raw).nextValue();
            if (value == null) return SelectionSnapshot.EMPTY;
            JSONObject json = value instanceof JSONObject
                    ? (JSONObject) value : new JSONObject(value.toString());
            String anchorText = json.optString("anchorText", json.optString("text"));
            String text = json.optString("text").replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) return SelectionSnapshot.EMPTY;
            return new SelectionSnapshot(text, anchorText, json.optString("semanticUnitId"),
                    json.optInt("startOffset", -1), json.optInt("endOffset", -1),
                    json.optString("prefix"), json.optString("suffix"));
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
        static final SelectionSnapshot EMPTY = new SelectionSnapshot(
                "", "", "", -1, -1, "", "");
        final String text;
        final String anchorText;
        final String semanticUnitId;
        final int startOffset;
        final int endOffset;
        final String prefix;
        final String suffix;

        SelectionSnapshot(String text, String anchorText, String semanticUnitId,
                          int startOffset, int endOffset, String prefix, String suffix) {
            this.text = text == null ? "" : text;
            this.anchorText = anchorText == null ? this.text : anchorText;
            this.semanticUnitId = semanticUnitId == null ? "" : semanticUnitId;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.prefix = prefix == null ? "" : prefix;
            this.suffix = suffix == null ? "" : suffix;
        }
    }
}
