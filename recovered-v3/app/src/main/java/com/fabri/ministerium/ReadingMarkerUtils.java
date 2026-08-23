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
}
