package com.fabri.ministerium;

import android.webkit.WebView;

/**
 * Corrige saltos editoriales heredados de PDF/HTML en textos de prosa ritual.
 * No se usa en la Liturgia de las Horas: allí la división de líneas/párrafos
 * forma parte de la presentación orante del texto.
 */
public final class ProseParagraphNormalizer {
    private ProseParagraphNormalizer() {}

    public static void inject(WebView webView) {
        if (webView == null) return;
        String script = "(function(){if(window.__ministeriumProseParagraphsNormalized)return;"
                + "window.__ministeriumProseParagraphsNormalized=true;"
                + "function t(e){return(e&&e.textContent||'').replace(/\\s+/g,' ').trim();}"
                + "function excluded(e){if(!e||e.tagName!=='P')return true;"
                + "if(e.querySelector('button,h1,h2,h3,h4,h5,h6'))return true;"
                + "var c=((e.className||'')+' '+(e.id||'')).toLowerCase();"
                + "return /rubric|rubrica|rúbrica|title|titulo|heading|response|respuesta|assembly|asamblea|minister|celebrant|sacerdote|diacono|diácono|option|alternative/.test(c);}"
                + "var ps=Array.prototype.slice.call(document.querySelectorAll('p'));"
                + "for(var i=0;i<ps.length;i++){var p=ps[i];if(!p.parentNode||excluded(p))continue;"
                + "var n=p.nextElementSibling;if(!n||excluded(n))continue;"
                + "var a=t(p),b=t(n);if(!a||!b)continue;"
                + "if(!/[,:;]$/.test(a)||!/^[a-záéíóúüñ]/.test(b))continue;"
                + "p.appendChild(document.createTextNode(' '));"
                + "while(n.firstChild)p.appendChild(n.firstChild);n.remove();i--;}})()";
        webView.evaluateJavascript(script, null);
    }
}
