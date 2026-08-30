package com.fabri.ministerium;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.json.JSONArray;

/**
 * Bilingual reader WebView that synchronizes to the equivalent liturgical
 * block instead of copying a global scroll percentage.
 *
 * Clean ES/LAT packages already expose data-ministerium-block. Older pages
 * can still use data-ministerium-align-key as a compatibility fallback.
 */
public class BilingualSyncWebView extends MinisteriumWebView {
    private boolean receivingSync;
    private boolean requestPending;

    public BilingualSyncWebView(Context context) { super(context); }
    public BilingualSyncWebView(Context context, AttributeSet attrs) { super(context, attrs); }
    public BilingualSyncWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** The old activity installs a proportional listener; semantic sync supersedes it. */
    @Override
    public void setOnScrollChangeListener(View.OnScrollChangeListener listener) {
        // Intentionally ignored only for this dedicated bilingual WebView.
    }

    /**
     * 3.1.1 used temporary spacer DIVs to force both documents to the same
     * vertical coordinates. On real devices those spacers produced large blank
     * areas whenever ES and LAT paragraphs had different lengths. Keep the
     * measurement pass for backward-compatible anchors, but never insert the
     * artificial blank space. Semantic block sync below handles correspondence.
     */
    @Override
    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        if (script != null && script.contains("d.className='ministerium-align-spacer'")) {
            if (resultCallback != null) resultCallback.onReceiveValue("null");
            return;
        }
        super.evaluateJavascript(script, resultCallback);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (receivingSync || requestPending || getVisibility() != View.VISIBLE) return;
        WebView peer = peer();
        if (peer == null || peer.getVisibility() != View.VISIBLE) return;
        requestPending = true;
        evaluateJavascript(sourceStateScript(), stateValue -> {
            try {
                JSONArray state = new JSONArray(stateValue);
                String key = state.optString(0, "");
                double within = state.optDouble(1, 0d);
                double global = state.optDouble(2, 0d);
                peer.evaluateJavascript(targetPositionScript(key, within, global), targetValue -> {
                    int y = parseIntResult(targetValue);
                    if (y >= 0 && peer instanceof BilingualSyncWebView) {
                        BilingualSyncWebView target = (BilingualSyncWebView) peer;
                        target.receivingSync = true;
                        target.scrollTo(target.getScrollX(), y);
                        target.postDelayed(() -> target.receivingSync = false, 90L);
                    }
                    requestPending = false;
                });
            } catch (Exception error) {
                requestPending = false;
            }
        });
    }

    private WebView peer() {
        View root = getRootView();
        if (root == null) return null;
        int peerId = getId() == R.id.spanishWebView ? R.id.latinWebView : R.id.spanishWebView;
        View value = root.findViewById(peerId);
        return value instanceof WebView ? (WebView) value : null;
    }

    private static String sourceStateScript() {
        return "(function(){var max=Math.max(1,document.documentElement.scrollHeight-innerHeight);"
                + "var global=Math.max(0,Math.min(1,scrollY/max));"
                + "var a=[].slice.call(document.querySelectorAll('[data-ministerium-block],[data-ministerium-align-key]'));"
                + "if(!a.length)return ['',0,global];var y=scrollY+Math.min(80,innerHeight*.16);"
                + "var index=0;for(var i=0;i<a.length;i++){var top=a[i].getBoundingClientRect().top+scrollY;"
                + "if(top<=y)index=i;else break;}var current=a[index];var top=current.getBoundingClientRect().top+scrollY;"
                + "var next=index+1<a.length?a[index+1].getBoundingClientRect().top+scrollY:document.documentElement.scrollHeight;"
                + "var within=next>top?Math.max(0,Math.min(1,(y-top)/(next-top))):0;"
                + "var key=current.getAttribute('data-ministerium-block')||current.getAttribute('data-ministerium-align-key')||'';"
                + "return [key,within,global];})()";
    }

    private static String targetPositionScript(String key, double within, double global) {
        String safeKey = org.json.JSONObject.quote(key == null ? "" : key);
        return "(function(key,within,global){var max=Math.max(0,document.documentElement.scrollHeight-innerHeight);"
                + "var a=[].slice.call(document.querySelectorAll('[data-ministerium-block],[data-ministerium-align-key]'));"
                + "var e=null,index=-1;for(var i=0;i<a.length;i++){var k=a[i].getAttribute('data-ministerium-block')"
                + "||a[i].getAttribute('data-ministerium-align-key')||'';if(k===key){e=a[i];index=i;break;}}"
                + "if(!e)return Math.round(global*max);var top=e.getBoundingClientRect().top+scrollY;"
                + "var next=index>=0&&index+1<a.length?a[index+1].getBoundingClientRect().top+scrollY:document.documentElement.scrollHeight;"
                + "var y=top+Math.max(0,Math.min(1,within))*Math.max(0,next-top)-Math.min(80,innerHeight*.16);"
                + "return Math.round(Math.max(0,Math.min(max,y)));})("
                + safeKey + "," + within + "," + global + ")";
    }

    private static int parseIntResult(String value) {
        if (value == null || "null".equals(value)) return -1;
        try { return (int) Math.round(Double.parseDouble(value.replace("\"", ""))); }
        catch (Exception ignored) { return -1; }
    }
}
