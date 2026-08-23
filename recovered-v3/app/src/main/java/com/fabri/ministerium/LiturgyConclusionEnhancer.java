package com.fabri.ministerium;

import android.webkit.WebView;

/** Selector legítimo de conclusión para Laudes y Vísperas rezadas separadamente. */
public final class LiturgyConclusionEnhancer {
    private LiturgyConclusionEnhancer() {}

    public static void inject(WebView webView, boolean ordained) {
        String script = "(function(ordained){if(document.getElementById('ministerium-conclusion-choice'))return;"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "var a=document.querySelectorAll('p,h1,h2,h3,h4'),h=null;for(var i=0;i<a.length;i++){if(n(a[i].textContent)==='CONCLUSION'){h=a[i];break;}}"
                + "if(!h){h=document.createElement('h3');h.textContent='Conclusión';document.body.appendChild(h);var created=document.createElement('p');document.body.appendChild(created);}"
                + "var p=h.nextElementSibling;if(!p){p=document.createElement('p');h.parentNode.insertBefore(p,h.nextSibling);}var lay='V. El Señor nos bendiga, nos guarde de todo mal y nos lleve a la vida eterna.\\nR. Amén.';"
                + "var minister='V. El Señor esté con ustedes.\\nR. Y con tu espíritu.\\nV. La bendición de Dios todopoderoso, Padre, Hijo y Espíritu Santo, descienda sobre ustedes.\\nR. Amén.';"
                + "var box=document.createElement('div');box.id='ministerium-conclusion-choice';box.className='ministerium-options';"
                + "function add(t,v){var b=document.createElement('button');b.type='button';b.textContent=t;b.onclick=function(){p.textContent=v;p.style.whiteSpace='pre-line';};box.appendChild(b);}"
                + "add('Laico / individual',lay);add('Ministro ordenado',minister);h.parentNode.insertBefore(box,p);p.textContent=ordained?minister:lay;p.style.whiteSpace='pre-line';"
                + "var s=document.createElement('style');s.textContent='#ministerium-conclusion-choice{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0}.ministerium-options button{border:1px solid #6E1D2A;border-radius:18px;padding:8px 12px;background:transparent;color:#6E1D2A}';document.head.appendChild(s);})("
                + (ordained ? "true" : "false") + ")";
        webView.evaluateJavascript(script, null);
    }
}
