from pathlib import Path

# One-shot deterministic source hardening. The workflow commits only generated Java changes.
ROOT = Path(__file__).resolve().parents[1]


def replace_once(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Expected patch anchor not found in {path}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


bible = 'app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java'
replace_once(
    bible,
    '        StudyStore.save(this, bookmark);\n        UniversalSelectionMenu.restoreHighlights(this, webView, sourceKey());',
    '        StudyStore.save(this, bookmark);\n'
    '        ReadingMarkerUtils.injectHighlights(this, webView, sourceKey());\n'
    '        UniversalSelectionMenu.restoreHighlights(this, webView, sourceKey());'
)
replace_once(
    bible,
    '        if (webView != null && book != null) {\n            UniversalSelectionMenu.restoreHighlights(this, webView, sourceKey());\n        }',
    '        if (webView != null && book != null) {\n'
    '            ReadingMarkerUtils.injectHighlights(this, webView, sourceKey());\n'
    '            UniversalSelectionMenu.restoreHighlights(this, webView, sourceKey());\n'
    '        }'
)
replace_once(
    bible,
    '    @Override protected void onPause() {',
    '    @Override public void onWindowFocusChanged(boolean hasFocus) {\n'
    '        super.onWindowFocusChanged(hasFocus);\n'
    '        if (hasFocus && webView != null && book != null) {\n'
    '            webView.postDelayed(() -> {\n'
    '                if (webView == null || book == null) return;\n'
    '                ReadingMarkerUtils.injectHighlights(this, webView, sourceKey());\n'
    '                UniversalSelectionMenu.restoreHighlights(this, webView, sourceKey());\n'
    '            }, 120);\n'
    '        }\n'
    '    }\n\n'
    '    @Override protected void onPause() {'
)
replace_once(
    bible,
    '    @Override protected void onDestroy() { if (webView != null) webView.destroy(); super.onDestroy(); }',
    '    @Override protected void onDestroy() {\n'
    '        if (webView != null) {\n'
    '            webView.stopLoading();\n'
    '            webView.loadUrl("about:blank");\n'
    '            webView.setWebChromeClient(null);\n'
    '            webView.setWebViewClient(null);\n'
    '            webView.removeAllViews();\n'
    '            webView.destroy();\n'
    '            webView = null;\n'
    '        }\n'
    '        super.onDestroy();\n'
    '    }'
)

print('Quality hardening patches applied.')
