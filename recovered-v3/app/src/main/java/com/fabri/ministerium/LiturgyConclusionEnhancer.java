package com.fabri.ministerium;

import android.webkit.WebView;

/** Selector de conclusión para Laudes y Vísperas rezadas separadamente. */
public final class LiturgyConclusionEnhancer {
    private LiturgyConclusionEnhancer() {}

    public static void inject(WebView webView, boolean ordained) {
        // La elección pertenece a cada celebración; no se toma como una configuración
        // litúrgica global. El parámetro se conserva para mantener compatibilidad con
        // los llamadores existentes mientras se elimina esa dependencia.
        String script = "(function(){if(document.getElementById('ministerium-conclusion-choice'))return;"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "var a=document.querySelectorAll('p,h1,h2,h3,h4'),h=null;for(var i=0;i<a.length;i++){if(n(a[i].textContent)==='CONCLUSION'){h=a[i];break;}}"
                + "if(!h){h=document.createElement('h3');h.textContent='Conclusión';document.body.appendChild(h);}"
                + "var old=h.nextElementSibling;if(old)old.style.display='none';"
                + "var box=document.createElement('section');box.id='ministerium-conclusion-choice';box.className='ministerium-conclusion';"
                + "var accent=getComputedStyle(h).color||'#6E1D2A';box.style.setProperty('--ministerium-accent',accent);"
                + "var nav=document.createElement('div');nav.className='ministerium-conclusion-options';"
                + "var out=document.createElement('p');out.className='ministerium-conclusion-text';"
                + "var choices=["
                + "{title:'Laico / individual',text:'℣. El Señor nos bendiga, nos guarde de todo mal y nos lleve a la vida eterna.\\n℟. Amén.'},"
                + "{title:'Ministro ordenado',text:'℣. El Señor esté con ustedes.\\n℟. Y con tu espíritu.\\n\\n℣. La bendición de Dios todopoderoso, Padre, Hijo y Espíritu Santo, descienda sobre ustedes.\\n℟. Amén.'}];"
                + "function select(index){out.textContent=choices[index].text;var bs=nav.querySelectorAll('button');for(var j=0;j<bs.length;j++){var active=j===index;bs[j].classList.toggle('ministerium-conclusion-active',active);bs[j].setAttribute('aria-pressed',active?'true':'false');}}"
                + "for(var k=0;k<choices.length;k++){(function(index){var b=document.createElement('button');b.type='button';b.textContent=choices[index].title;b.setAttribute('aria-pressed','false');b.onclick=function(){select(index);};nav.appendChild(b);})(k);}"
                + "box.appendChild(nav);box.appendChild(out);h.parentNode.insertBefore(box,old||h.nextSibling);select(0);"
                + "var existing=document.getElementById('ministerium-conclusion-style');if(existing)existing.remove();var s=document.createElement('style');s.id='ministerium-conclusion-style';"
                + "s.textContent='"
                + ".ministerium-conclusion{margin:10px 0 22px;padding:14px;border-left:4px solid var(--ministerium-accent);background:rgba(201,165,92,.12);border-radius:0 8px 8px 0;}"
                + ".ministerium-conclusion-options{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px;}"
                + ".ministerium-conclusion-options button{border:1px solid var(--ministerium-accent);border-radius:18px;padding:8px 12px;background:transparent;color:var(--ministerium-accent)!important;-webkit-text-fill-color:var(--ministerium-accent)!important;line-height:1.2;}"
                + ".ministerium-conclusion-options button.ministerium-conclusion-active{background:var(--ministerium-accent)!important;color:#FFF!important;-webkit-text-fill-color:#FFF!important;}"
                + ".ministerium-conclusion-text{white-space:pre-line;line-height:1.65;margin:0;}';document.head.appendChild(s);})()";
        webView.evaluateJavascript(script, null);
    }
}
