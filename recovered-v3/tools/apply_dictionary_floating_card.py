from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / 'app/src/main/java/com/fabri/ministerium/UniversalSelectionMenu.java'
text = path.read_text(encoding='utf-8')

old = '                        openDictionary(activity, selection.text);'
new = '                        openDictionary(activity, webView, selection);'
if old not in text:
    raise SystemExit('dictionary action anchor not found')
text = text.replace(old, new, 1)

old = "                + \"return JSON.stringify({text:s.toString(),anchorText:s.toString(),semanticUnitId:u,startOffset:a,endOffset:z,prefix:pre,suffix:suf});})()\";"
new = "                + \"var box=r.getBoundingClientRect();return JSON.stringify({text:s.toString(),anchorText:s.toString(),semanticUnitId:u,startOffset:a,endOffset:z,prefix:pre,suffix:suf,rectTop:box.top,rectBottom:box.bottom,viewportHeight:window.innerHeight||document.documentElement.clientHeight||0});})()\";"
if old not in text:
    raise SystemExit('selection geometry anchor not found')
text = text.replace(old, new, 1)

start = text.index('    private static void openDictionary(Activity activity, String selected) {')
end = text.index('    private static void openDictionaryChooser(Activity activity, String query) {', start)
replacement = '''    private static void openDictionary(Activity activity, WebView webView,
                                       SelectionSnapshot selection) {
        DictionaryFloatingCard.show(activity, webView, selection.text,
                selection.rectTop, selection.rectBottom, selection.viewportHeight);
    }

'''
text = text[:start] + replacement + text[end:]

old = '''            result.prefix = value.optString("prefix");
            result.suffix = value.optString("suffix");'''
new = '''            result.prefix = value.optString("prefix");
            result.suffix = value.optString("suffix");
            result.rectTop = (float) value.optDouble("rectTop", 0d);
            result.rectBottom = (float) value.optDouble("rectBottom", result.rectTop);
            result.viewportHeight = (float) value.optDouble("viewportHeight", 0d);'''
if old not in text:
    raise SystemExit('decode selection anchor not found')
text = text.replace(old, new, 1)

old = '''        String prefix = "";
        String suffix = "";
    }'''
new = '''        String prefix = "";
        String suffix = "";
        float rectTop = 0f;
        float rectBottom = 0f;
        float viewportHeight = 0f;
    }'''
if old not in text:
    raise SystemExit('selection snapshot anchor not found')
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('Dictionary floating card integrated into UniversalSelectionMenu.')
