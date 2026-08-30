package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.UUID;

public final class ReflectionUtils {
    public interface Factory { ReflectionEntry create(String quote, String reflection); }
    private ReflectionUtils() {}

    public static void selectAndCreate(Activity activity, WebView webView, Factory factory) {
        webView.evaluateJavascript("(window.getSelection?window.getSelection().toString():'').trim()", value -> {
            String quote = decode(value).trim();
            if (quote.isEmpty()) {
                Toast.makeText(activity, "Mantén pulsado y selecciona primero una frase.", Toast.LENGTH_LONG).show();
                return;
            }
            showEditor(activity, webView, quote, factory);
        });
    }

    private static void showEditor(Activity activity, WebView webView, String quote, Factory factory) {
        int pad = (int) (18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView selected = new TextView(activity);
        selected.setText("“" + quote + "”");
        selected.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        selected.setTextSize(16);
        EditText input = new EditText(activity);
        input.setHint("Escribe aquí tu reflexión…");
        input.setMinLines(5);
        input.setGravity(android.view.Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(selected, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Subrayar y reflexionar").setView(box)
                .setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String reflection = input.getText().toString().trim();
                    if (reflection.isEmpty()) {
                        input.setError("Escribe una reflexión");
                        return;
                    }
                    ReflectionEntry entry = factory.create(quote, reflection);
                    entry.id = UUID.randomUUID().toString();
                    ReflectionStore.save(activity, entry);
                    dialog.dismiss();
                    Toast.makeText(activity, "Reflexión guardada en este dispositivo.",
                            Toast.LENGTH_SHORT).show();
                    injectHighlights(activity, webView, entry.sourceKey);
                }));
        dialog.show();
    }

    public static void injectHighlights(Activity activity, WebView webView, String sourceKey) {
        List<ReflectionEntry> entries = ReflectionStore.forSource(activity, sourceKey);
        for (ReflectionEntry entry : entries) {
            String quote = JSONObject.quote(entry.quote);
            String script = "(function(q){var old=document.querySelectorAll('mark.ministerium-highlight');"
                    + "for(var z=0;z<old.length;z++){if(old[z].getAttribute('data-quote')==q)return;}"
                    + "var w=document.createTreeWalker(document.body,"
                    + "NodeFilter.SHOW_TEXT),n,a=[],t='';while(n=w.nextNode()){if(n.parentNode"
                    + "&&n.parentNode.tagName!='SCRIPT'&&n.parentNode.tagName!='STYLE'){"
                    + "a.push({n:n,s:t.length,e:t.length+n.nodeValue.length});t+=n.nodeValue;}}"
                    + "var i=t.indexOf(q);if(i<0)return;var j=i+q.length,st,en,so=0,eo=0;"
                    + "for(var k=0;k<a.length;k++){if(!st&&i>=a[k].s&&i<=a[k].e){st=a[k].n;"
                    + "so=i-a[k].s;}if(j>=a[k].s&&j<=a[k].e){en=a[k].n;eo=j-a[k].s;break;}}"
                    + "if(!st||!en)return;try{var r=document.createRange();r.setStart(st,so);"
                    + "r.setEnd(en,eo);var m=document.createElement('mark');"
                    + "m.className='ministerium-highlight';m.setAttribute('data-quote',q);m.appendChild(r.extractContents());"
                    + "r.insertNode(m);}catch(e){}})(" + quote + ")";
            webView.evaluateJavascript(script, null);
        }
    }

    private static String decode(String value) {
        try { return new JSONArray("[" + value + "]").getString(0); }
        catch (Exception ignored) { return ""; }
    }
}
