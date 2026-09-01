package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.List;
import java.util.UUID;

public final class ReadingMarkerUtils {
    public interface Factory { ReadingMarker create(ReadingSelectionUtils.Selection selection); }
    private ReadingMarkerUtils() {}

    public static void selectAndSave(Activity activity, WebView webView, Factory factory) {
        ReadingSelectionUtils.capture(activity, webView, selection -> {
            ReadingMarker marker = factory.create(selection);
            showConfirmation(activity, webView, marker);
        });
    }

    private static void showConfirmation(Activity activity, WebView webView,
                                         ReadingMarker marker) {
        int pad = (int) (18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView citation = new TextView(activity);
        citation.setText(marker.citation);
        citation.setTextSize(17);
        citation.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView quote = new TextView(activity);
        quote.setText("“" + marker.quote + "”");
        quote.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        quote.setTextSize(16);
        quote.setPadding(0, pad / 2, 0, 0);
        box.addView(citation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(quote, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(activity).setTitle("Guardar subrayado")
                .setMessage("La cita y el texto aparecerán en «Subrayados».")
                .setView(box).setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    marker.id = UUID.randomUUID().toString();
                    marker.createdAt = System.currentTimeMillis();
                    ReadingMarkerStore.save(activity, marker);
                    injectHighlights(activity, webView, marker.sourceKey);
                    Toast.makeText(activity, "Subrayado guardado en el dispositivo.",
                            Toast.LENGTH_SHORT).show();
                }).show();
    }

    public static void injectHighlights(Activity activity, WebView webView, String sourceKey) {
        List<ReadingMarker> entries = ReadingMarkerStore.forSource(activity, sourceKey);
        for (ReadingMarker entry : entries) injectQuote(webView, entry.quote);
        injectStudyMarkers(activity, webView, sourceKey);
    }

    private static void injectStudyMarkers(Activity activity, WebView webView, String sourceKey) {
        for (StudyEntry entry : StudyStore.forSource(activity, sourceKey)) {
            if (!(StudyEntry.NOTE.equals(entry.type) || StudyEntry.BOOKMARK.equals(entry.type))) continue;
            injectStudyMarker(webView, entry);
        }
    }

    public static void scrollToQuote(WebView webView, String quote) {
        if (quote == null || quote.trim().isEmpty()) return;
        String q = JSONObject.quote(quote);
        String script = "(function(q){var m=document.querySelectorAll('[data-quote]');"
                + "for(var i=0;i<m.length;i++){if(m[i].getAttribute('data-quote')==q){"
                + "m[i].scrollIntoView({block:'center'});return;}}})(" + q + ")";
        webView.evaluateJavascript(script, null);
    }

    private static void injectQuote(WebView webView, String value) {
        String quote = JSONObject.quote(value);
        String script = "(function(q){var old=document.querySelectorAll('mark.ministerium-highlight');"
                + "for(var z=0;z<old.length;z++){if(old[z].getAttribute('data-quote')==q)return;}"
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),n,a=[],t='';"
                + "while(n=w.nextNode()){if(n.parentNode&&n.parentNode.tagName!='SCRIPT'&&"
                + "n.parentNode.tagName!='STYLE'){a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});"
                + "t+=n.nodeValue;}}var i=t.indexOf(q);if(i<0)return;var j=i+q.length,st,en,so=0,eo=0;"
                + "for(var k=0;k<a.length;k++){if(!st&&i>=a[k].s&&i<=a[k].e){st=a[k].n;so=i-a[k].s;}"
                + "if(j>=a[k].s&&j<=a[k].e){en=a[k].n;eo=j-a[k].s;break;}}if(!st||!en)return;"
                + "try{var r=document.createRange();r.setStart(st,so);r.setEnd(en,eo);"
                + "var m=document.createElement('mark');m.className='ministerium-highlight';"
                + "m.setAttribute('data-quote',q);m.appendChild(r.extractContents());r.insertNode(m);}catch(e){}})("
                + quote + ")";
        webView.evaluateJavascript(script, null);
    }

    private static void injectStudyMarker(WebView webView, StudyEntry entry) {
        String id = JSONObject.quote(entry.id == null ? "" : entry.id);
        String quote = JSONObject.quote(entry.anchorText == null || entry.anchorText.trim().isEmpty()
                ? entry.quote == null ? "" : entry.quote : entry.anchorText);
        String glyph = JSONObject.quote(markerGlyph(entry.icon));
        String background = JSONObject.quote(markerColor(entry.color));
        String foreground = JSONObject.quote("yellow".equals(entry.color) ? "#2A2521" : "#FFFFFF");
        String script = "(function(id,q,g,bg,fg){"
                + "if(!id||!q||document.querySelector('[data-study-id=\\\"'+id+'\\\"]'))return;"
                + "var css=document.getElementById('ministerium-study-marker-fallback-css');"
                + "if(!css){css=document.createElement('style');css.id='ministerium-study-marker-fallback-css';"
                + "css.textContent='.ministerium-study-marker{display:inline-flex!important;align-items:center!important;justify-content:center!important;min-width:24px!important;height:24px!important;margin-left:5px!important;padding:0 5px!important;border:0!important;border-radius:12px!important;font:700 14px sans-serif!important;vertical-align:middle!important;box-shadow:0 1px 4px rgba(0,0,0,.22)!important;cursor:pointer!important;}';document.head.appendChild(css);}"
                + "function n(v){return(v||'').replace(/\\s+/g,' ').trim();}"
                + "function marker(host){var m=document.createElement('button');m.type='button';m.className='ministerium-study-marker';m.setAttribute('data-study-id',id);m.textContent=g;m.style.setProperty('background-color',bg,'important');m.style.setProperty('color',fg,'important');m.style.setProperty('-webkit-text-fill-color',fg,'important');m.onclick=function(ev){ev.preventDefault();ev.stopPropagation();if(window.MinisteriumStudy)MinisteriumStudy.openEntry(id);};host.appendChild(m);return true;}"
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),x,a=[],t='';while(x=w.nextNode()){if(x.parentNode&&x.parentNode.tagName!='SCRIPT'&&x.parentNode.tagName!='STYLE'&&!x.parentNode.classList.contains('ministerium-study-marker')){a.push({n:x,s:t.length,e:t.length+x.nodeValue.length});t+=x.nodeValue;}}"
                + "var at=t.indexOf(q);if(at>=0){var end=at+q.length,en=null,eo=0;for(var i=0;i<a.length;i++){var z=a[i];if(end>=z.s&&end<=z.e){en=z.n;eo=Math.max(0,end-z.s);break;}}if(en){try{var r=document.createRange();r.setStart(en,eo);r.collapse(true);var span=document.createElement('span');r.insertNode(span);if(marker(span))return;}catch(e){}}}"
                + "var nq=n(q),blocks=document.querySelectorAll('.verse,p,li,blockquote');for(var j=0;j<blocks.length;j++){var bt=n(blocks[j].textContent);if(bt&&nq&&bt.indexOf(nq)>=0){marker(blocks[j]);return;}}"
                + "for(var k=0;k<blocks.length;k++){var bt2=n(blocks[k].textContent);if(!bt2||!nq)continue;var probe=nq.length>80?nq.substring(0,80):nq;if(bt2.indexOf(probe)>=0){marker(blocks[k]);return;}}"
                + "})(" + id + "," + quote + "," + glyph + "," + background + "," + foreground + ")";
        webView.evaluateJavascript(script, null);
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

    private static String markerColor(String color) {
        if ("green".equals(color)) return "#4F9A45";
        if ("blue".equals(color)) return "#397DB5";
        if ("red".equals(color)) return "#B84A48";
        if ("orange".equals(color)) return "#B9692E";
        if ("violet".equals(color)) return "#7753A6";
        if ("gray".equals(color)) return "#686868";
        return "#E0A91A";
    }
}
