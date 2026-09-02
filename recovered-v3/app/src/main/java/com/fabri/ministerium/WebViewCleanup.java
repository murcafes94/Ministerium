package com.fabri.ministerium;

import android.webkit.WebView;

/** Small shared teardown helper to avoid retaining reader state after navigation. */
public final class WebViewCleanup {
    private WebViewCleanup() {}

    public static void destroy(WebView webView) {
        if (webView == null) return;
        try { webView.stopLoading(); } catch (Exception ignored) {}
        try { webView.setOnTouchListener(null); } catch (Exception ignored) {}
        try { webView.setOnLongClickListener(null); } catch (Exception ignored) {}
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                webView.setOnScrollChangeListener(null);
            }
        } catch (Exception ignored) {}
        try { webView.clearAnimation(); } catch (Exception ignored) {}
        try { webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        try { webView.setWebChromeClient(null); } catch (Exception ignored) {}
        try { webView.setWebViewClient(null); } catch (Exception ignored) {}
        try { webView.removeAllViews(); } catch (Exception ignored) {}
        try { webView.clearHistory(); } catch (Exception ignored) {}
        try { webView.destroy(); } catch (Exception ignored) {}
    }
}
