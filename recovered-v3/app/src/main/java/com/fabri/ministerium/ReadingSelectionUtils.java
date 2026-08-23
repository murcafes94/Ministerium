package com.fabri.ministerium;

import android.app.Activity;
import android.webkit.WebView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ReadingSelectionUtils {
    public interface Callback { void onSelection(Selection selection); }

    public static final class Selection {
        public final String quote;
        public final String startVerse;
        public final String endVerse;
        public final String reference;
        public final String section;

        Selection(String quote, String startVerse, String endVerse,
                  String reference, String section) {
            this.quote = quote;
            this.startVerse = startVerse;
            this.endVerse = endVerse;
            this.reference = reference;
            this.section = section;
        }
    }

    private ReadingSelectionUtils() {}

    public static void capture(Activity activity, WebView webView, Callback callback) {
        String script = "(function(){var s=window.getSelection&&window.getSelection();"
                + "if(!s||!s.rangeCount)return '';var q=s.toString().trim();"
                + "if(!q)return '';var r=s.getRangeAt(0),a=document.querySelectorAll('sup[id^=\\\"v\\\"]');"
                + "var sv='',ev='';for(var i=0;i<a.length;i++){try{var c=r.comparePoint(a[i],0);"
                + "if(c<=0){ev=a[i].id;if(!sv)sv=a[i].id;}if(c<0)sv=a[i].id;"
                + "if(c>0)break;}catch(e){}}if(!ev)ev=sv;"
                + "var n=r.startContainer.nodeType==1?r.startContainer:r.startContainer.parentNode;"
                + "var sec=n&&n.closest?n.closest('.reading-section'):null;"
                + "var ref=sec&&sec.querySelector('.reading-reference');var h=sec&&sec.querySelector('h2');"
                + "return JSON.stringify({q:q,s:sv,e:ev,r:ref?ref.textContent.trim():'',"
                + "h:h?h.textContent.trim():''});})()";
        webView.evaluateJavascript(script, value -> {
            try {
                String decoded = new JSONArray("[" + value + "]").getString(0);
                if (decoded.trim().isEmpty()) throw new IllegalStateException();
                JSONObject data = new JSONObject(decoded);
                callback.onSelection(new Selection(data.optString("q"),
                        data.optString("s"), data.optString("e"),
                        data.optString("r"), data.optString("h")));
            } catch (Exception ignored) {
                Toast.makeText(activity,
                        "Mantén pulsado y selecciona primero una frase.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
