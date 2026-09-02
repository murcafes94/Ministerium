package com.fabri.ministerium;

import android.webkit.WebView;

/**
 * Compacta alternativas del Misal sin alterar el texto litúrgico: mantiene una
 * fórmula visible, oculta variantes hasta que se elijan y limpia duplicados
 * editoriales que vienen de la extracción de la fuente.
 */
public final class MissalAlternativeOptions31 {
    private MissalAlternativeOptions31() {}

    public static void inject(WebView webView) {
        if (webView == null) return;
        String script = "(function(){"
                + "if(document.body.getAttribute('data-ministerium-alternatives')==='1')return;"
                + "document.body.setAttribute('data-ministerium-alternatives','1');"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                // Evita glifos ℣/℟ deformados por fuentes del dispositivo.
                + "var tw=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),tx;while((tx=tw.nextNode())){if(!tx.nodeValue)continue;tx.nodeValue=tx.nodeValue.replace(/℣\\.?/g,'V/.').replace(/℟\\.?/g,'R/.');}"
                // Elimina rótulos duplicados consecutivos, p. ej. Oración colecta o Plegaria Eucarística I.
                + "var labels=Array.prototype.slice.call(document.querySelectorAll('h1,h2,h3,h4,h5,h6,p'));"
                + "for(var d=1;d<labels.length;d++){var a=labels[d-1],b=labels[d];if(!a.parentNode||!b.parentNode)continue;var ta=n(a.textContent),tb=n(b.textContent);"
                + "if(ta&&ta===tb&&ta.length<90&&a.parentNode===b.parentNode){b.remove();labels.splice(d,1);d--;}}"
                // Las tres fórmulas del acto penitencial quedan en bloques plegables; solo I abierta.
                + "function compactPenitential(){var nodes=Array.prototype.slice.call(document.querySelectorAll('h1,h2,h3,h4,h5,h6,p'));var marks=[];"
                + "for(var i=0;i<nodes.length;i++){var t=n(nodes[i].textContent);if(/^formula (i|ii|iii)$/.test(t))marks.push(nodes[i]);}if(marks.length<2)return;"
                + "for(var m=0;m<marks.length;m++){var mark=marks[m];if(!mark.parentNode||mark.closest('details.ministerium-penitential-formula'))continue;var parent=mark.parentNode;"
                + "var det=document.createElement('details');det.className='ministerium-penitential-formula';det.open=m===0;var sum=document.createElement('summary');sum.textContent=mark.textContent.trim();det.appendChild(sum);parent.insertBefore(det,mark);var cur=mark;"
                + "while(cur){var next=cur.nextSibling;if(cur!==mark&&cur.nodeType===1){var nt=n(cur.textContent);if(/^formula (i|ii|iii)$/.test(nt)||nt==='gloria'||nt.indexOf('gloria a dios')===0||nt==='kyrie'||nt==='oracion colecta')break;}det.appendChild(cur);cur=next;}}}compactPenitential();"
                + "function marker(t){t=n(t);return t==='o bien:'||t==='o bien'||t==='tambien puede decirse:'||t==='tambien puede decirse'||t==='vel:'||t==='vel';}"
                + "function boundary(el,moved){if(!el||el.nodeType!==1)return false;if(/^H[1-6]$/.test(el.tagName)||el.tagName==='SECTION'||el.tagName==='HR')return true;"
                + "var t=n(el.textContent);if(marker(t))return true;if(moved>0&&(t.indexOf('rito de ')===0||t.indexOf('liturgia ')===0||t.indexOf('oracion despues')===0||t.indexOf('oratio post')===0))return true;return false;}"
                + "var all=Array.prototype.slice.call(document.querySelectorAll('p,div'));"
                + "for(var i=0;i<all.length;i++){var mark=all[i];if(!mark.parentNode||!marker(mark.textContent)||mark.getAttribute('data-alt-done')==='1')continue;"
                + "mark.setAttribute('data-alt-done','1');var parent=mark.parentNode;var button=document.createElement('button');button.type='button';button.className='ministerium-alt-button';"
                + "button.textContent=n(mark.textContent).indexOf('vel')===0?'Altera formula':'Otra fórmula';var box=document.createElement('div');box.className='ministerium-alt-body';box.hidden=true;"
                + "parent.insertBefore(button,mark);parent.insertBefore(box,mark.nextSibling);mark.remove();var moved=0,node=box.nextSibling;"
                + "while(node&&moved<5){var next=node.nextSibling;if(boundary(node,moved))break;if(node.nodeType===1&&n(node.textContent)){box.appendChild(node);moved++;}node=next;}"
                + "if(moved===0){button.remove();box.remove();continue;}button.addEventListener('click',function(ev){var b=ev.currentTarget;var x=b.nextElementSibling;if(!x)return;x.hidden=!x.hidden;b.classList.toggle('selected',!x.hidden);b.textContent=x.hidden?(b.getAttribute('data-latin')==='1'?'Altera formula':'Otra fórmula'):(b.getAttribute('data-latin')==='1'?'Ocultar':'Ocultar alternativa');});"
                + "if(n(button.textContent).indexOf('altera')===0)button.setAttribute('data-latin','1');}"
                + "if(!document.getElementById('ministerium-alt-style')){var st=document.createElement('style');st.id='ministerium-alt-style';st.textContent='"
                + ".ministerium-alt-button{display:inline-flex;align-items:center;min-height:40px;margin:6px 0 10px;padding:6px 13px;border:1px solid currentColor;border-radius:18px;background:transparent;color:inherit;font:600 .88em sans-serif}.ministerium-alt-button.selected{font-weight:700;outline:2px solid currentColor;outline-offset:1px}.ministerium-alt-body{padding:2px 0 8px 12px;border-left:2px solid rgba(128,128,128,.35)}.ministerium-alt-body[hidden]{display:none!important}.ministerium-penitential-formula{margin:.8em 0 1em;border:1px solid rgba(128,128,128,.28);border-radius:10px;padding:.3em .8em}.ministerium-penitential-formula>summary{cursor:pointer;font-weight:700;color:inherit;padding:.35em 0}.ministerium-penitential-formula[open]>summary{margin-bottom:.45em}';document.head.appendChild(st);}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
