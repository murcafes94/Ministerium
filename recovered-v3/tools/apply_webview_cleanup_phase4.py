from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

helper = ROOT / "app/src/main/java/com/fabri/ministerium/WebViewCleanup.java"
text = helper.read_text(encoding="utf-8")
old = '''        try { webView.stopLoading(); } catch (Exception ignored) {}\n        try { webView.loadUrl("about:blank"); } catch (Exception ignored) {}\n        try { webView.setWebChromeClient(null); } catch (Exception ignored) {}\n        try { webView.setWebViewClient(null); } catch (Exception ignored) {}\n        try { webView.removeAllViews(); } catch (Exception ignored) {}\n        try { webView.clearHistory(); } catch (Exception ignored) {}\n        try { webView.destroy(); } catch (Exception ignored) {}\n'''
new = '''        try { webView.stopLoading(); } catch (Exception ignored) {}\n        try { webView.setOnTouchListener(null); } catch (Exception ignored) {}\n        try { webView.setOnLongClickListener(null); } catch (Exception ignored) {}\n        try {\n            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {\n                webView.setOnScrollChangeListener(null);\n            }\n        } catch (Exception ignored) {}\n        try { webView.clearAnimation(); } catch (Exception ignored) {}\n        try { webView.loadUrl("about:blank"); } catch (Exception ignored) {}\n        try { webView.setWebChromeClient(null); } catch (Exception ignored) {}\n        try { webView.setWebViewClient(null); } catch (Exception ignored) {}\n        try { webView.removeAllViews(); } catch (Exception ignored) {}\n        try { webView.clearHistory(); } catch (Exception ignored) {}\n        try { webView.destroy(); } catch (Exception ignored) {}\n'''
if old not in text:
    raise SystemExit("WebViewCleanup contract changed; patch not applied")
helper.write_text(text.replace(old, new, 1), encoding="utf-8")

reader = ROOT / "app/src/main/java/com/fabri/ministerium/BilingualHoursReaderActivity.java"
text = reader.read_text(encoding="utf-8")
old = '''    @Override protected void onDestroy() {\n        if (pendingSync != null) syncHandler.removeCallbacks(pendingSync);\n        if (spanish != null) spanish.destroy();\n        if (latin != null) latin.destroy();\n        super.onDestroy();\n    }\n'''
new = '''    @Override protected void onDestroy() {\n        if (pendingSync != null) syncHandler.removeCallbacks(pendingSync);\n        syncHandler.removeCallbacksAndMessages(null);\n        pendingSync = null;\n        syncingScroll = false;\n        WebViewCleanup.destroy(spanish);\n        WebViewCleanup.destroy(latin);\n        spanish = null;\n        latin = null;\n        panes = null;\n        super.onDestroy();\n    }\n'''
if old not in text:
    raise SystemExit("BilingualHoursReaderActivity teardown contract changed; patch not applied")
reader.write_text(text.replace(old, new, 1), encoding="utf-8")

print("WebView cleanup phase 4 applied")
