#!/usr/bin/env python3
"""Final 3.1 normalization after the legacy project has been reconstructed."""

from pathlib import Path
import json


def patch_bible_visual_line() -> None:
    path = Path("app/src/main/java/com/fabri/ministerium/BibleReaderActivity.java")
    text = path.read_text(encoding="utf-8")
    marker = "// Ministerium 3.1: preserve the Bible EPUB's own graphic line."
    if marker in text:
        return
    start = text.find("    private void applyStyle() {")
    end = text.find("    private ReaderContext context()", start)
    if start < 0 or end <= start:
        raise SystemExit("Bible visual style boundaries not found")
    method = '''    private void applyStyle() {\n        // Ministerium 3.1: preserve the Bible EPUB's own graphic line.\n        boolean dark = ThemeUtils.isDark(this);\n        String bg = dark ? "#26211E" : "#FFFDF7";\n        String ink = dark ? "#F3EDE4" : "#2A2521";\n        String accent = dark ? "#E1C57A" : "#772233";\n        String css = "html,body{background:" + bg + "!important;}"\n                + "body{margin:0!important;padding:18px 20px!important;box-sizing:border-box;max-width:100%!important;overflow-wrap:break-word;}"\n                + "img,table{max-width:100%!important;height:auto!important;}"\n                + ".ministerium-highlight{background:#F6E58D!important;color:#231F1B!important;-webkit-text-fill-color:#231F1B!important;padding:1px 2px;border-radius:2px;}"\n                + (dark ? "body,body p,body li,body span,body div{color:" + ink\n                + "!important;-webkit-text-fill-color:" + ink + "!important;}"\n                + "a,a *{color:" + accent + "!important;-webkit-text-fill-color:" + accent + "!important;}" : "")\n                + "@media(min-width:700px){body{padding-left:42px!important;padding-right:42px!important}}";\n        webView.evaluateJavascript("(function(){var s=document.getElementById('ministerium-style');"\n                + "if(!s){s=document.createElement('style');s.id='ministerium-style';document.head.appendChild(s);}"\n                + "s.innerHTML=" + org.json.JSONObject.quote(css) + ";})()", null);\n    }\n\n'''
    path.write_text(text[:start] + method + text[end:], encoding="utf-8")


def patch_ritual_scope() -> None:
    catalog = Path("app/src/main/java/com/fabri/ministerium/RitualCatalogActivity.java")
    text = catalog.read_text(encoding="utf-8")
    old = '        ((TextView) findViewById(R.id.txtIntro)).setText(\n                document.sourceName + " · toca una sección para abrir el texto completo.");'
    new = '''        String scope = ritualScope(document.sourceName);\n        ((TextView) findViewById(R.id.txtIntro)).setText(\n                document.sourceName + " · " + scope\n                        + " · toca una sección para abrir el texto completo.");'''
    if new not in text:
        if old not in text:
            raise SystemExit("Ritual catalog source label point not found")
        text = text.replace(old, new, 1)
    if "private static String ritualScope" not in text:
        helper = '''\n    private static String ritualScope(String source) {\n        String value = source == null ? "" : source.toLowerCase(java.util.Locale.ROOT);\n        if (value.contains("argentina")) {\n            return "edición regional: comprobar las adaptaciones aprobadas para Ecuador antes de uso celebrativo";\n        }\n        if (value.contains("selección pastoral")) {\n            return "selección para consulta; no sustituye el Bendicional completo";\n        }\n        return "texto local guardado; conviene verificar edición y adaptaciones vigentes";\n    }\n'''
        text = text.rsplit("\n}", 1)[0] + helper + "\n}"
    catalog.write_text(text, encoding="utf-8")

    reader = Path("app/src/main/java/com/fabri/ministerium/RitualReaderActivity.java")
    text = reader.read_text(encoding="utf-8")
    old = '        ((TextView) findViewById(R.id.txtSource)).setText(\n                document.sourceName + " · texto guardado para consulta offline");'
    new = '''        String sourceNotice = document.sourceName != null\n                && document.sourceName.toLowerCase(java.util.Locale.ROOT).contains("argentina")\n                ? " · edición regional; verificar adaptaciones para Ecuador"\n                : " · texto guardado para consulta offline";\n        ((TextView) findViewById(R.id.txtSource)).setText(document.sourceName + sourceNotice);'''
    if new not in text:
        if old not in text:
            raise SystemExit("Ritual reader source label point not found")
        text = text.replace(old, new, 1)
    reader.write_text(text, encoding="utf-8")


def patch_package_manifest() -> None:
    path = Path("app/src/main/assets/package-manifest.json")
    if not path.exists():
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    app = data.setdefault("app", {})
    app["versionName"] = "3.1.0"
    app["versionCode"] = 31
    app["released"] = "2026-08-24"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def patch_distribution_templates() -> None:
    path = Path("distribution/release-manifest.template.json")
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        data["versionCode"] = 31
        data["versionName"] = "3.1.0"
        if isinstance(data.get("apk"), dict):
            apk = data["apk"]
            apk["url"] = str(apk.get("url", "")).replace("3.0.0", "3.1.0")
        if "changelog" in data:
            data["changelog"] = "NOVEDADES-3.1.0.md"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    path = Path("distribution/content-packages.template.json")
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        for package in data.get("packages", []):
            if package.get("id") == "rituals":
                package["version"] = "3.1.0"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    path = Path("distribution/manifests/testing.json")
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        if "version" in data:
            data["version"] = "3.1.0-dev"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    patch_bible_visual_line()
    patch_ritual_scope()
    patch_package_manifest()
    patch_distribution_templates()
    print("Ministerium 3.1 post-apply normalization complete")


if __name__ == "__main__":
    main()
