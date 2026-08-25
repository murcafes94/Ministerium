package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Runtime safety net for the 3.1.1 Missal renderer.
 *
 * The generated document remains the source of truth. This layer only fixes presentation
 * failures that must never depend on another optional enhancement having initialized first:
 * prayer visibility, duplicated preface scaffolding, OCR escape residues and paragraph-level
 * alignment in the ES/LAT view.
 */
public final class MissalRuntimeFixes31 {
    private MissalRuntimeFixes31() {}

    public static void inject(WebView webView) {
        if (webView == null) return;

        String css =
                ".hidden,.eucharistic-prayer[hidden],#ministerium-common-preface[hidden]{display:none!important}" +
                ".parallel.runtime-aligned{display:block!important}" +
                ".parallel-head,.parallel-row{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:18px}" +
                ".parallel-head{font-weight:700;margin:0 0 8px;padding:0 0 6px;border-bottom:1px solid rgba(128,128,128,.35)}" +
                ".parallel-row{align-items:start;padding:.15em 0 .75em;margin:0 0 .35em;border-bottom:1px solid rgba(128,128,128,.14)}" +
                ".parallel-cell{min-width:0;overflow-wrap:anywhere;hyphens:auto}" +
                ".parallel-cell p{margin:.55em 0}" +
                "@media(max-width:760px){.parallel-head{display:none}.parallel-row{display:block;border-bottom:1px solid rgba(128,128,128,.25);padding-bottom:1em}.parallel-cell+.parallel-cell{margin-top:.75em;padding-top:.75em;border-top:1px dashed rgba(128,128,128,.28)}.parallel-cell[data-lang=\"la\"]:before{content:'LAT';display:block;font-weight:700;opacity:.72;margin-bottom:.35em}}";

        String script = "(function(){"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "if(!document.getElementById('ministerium-runtime-fixes-31')){var st=document.createElement('style');st.id='ministerium-runtime-fixes-31';st.textContent=" + JSONObject.quote(css) + ";document.head.appendChild(st);}"
                // Remove extraction/escape artefacts such as the visible '[n' residue.
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),x;while((x=w.nextNode())){if(!x.nodeValue)continue;var v=x.nodeValue;v=v.replace(/\\[n(?=\\s|$)/g,' ').replace(/\\\\n/g,' ');if(v!==x.nodeValue)x.nodeValue=v;}"
                // Replace developer-facing placeholders with reader-facing wording.
                + "var notices=document.querySelectorAll('.pending,.reference-day-hint');for(var i=0;i<notices.length;i++){var nt=n(notices[i].textContent);"
                + "if(nt.indexOf('se incorporara desde su fuente verificada')>=0||nt.indexOf('no se sustituye por el epub')>=0){notices[i].textContent='Prefacio propio o común según la celebración y las rúbricas.';}"
                + "else if(nt.indexOf('todavia no esta normalizado')>=0||nt.indexOf('se bloqueo el fallback')>=0){notices[i].textContent='Este formulario propio todavía no está disponible en el paquete local.';}"
                + "else if(nt.indexOf('no de curas.com.ar')>=0){notices[i].textContent=notices[i].textContent.replace(/El texto se toma[^.]*\\./i,'');}}"
                // The common preface belongs to Prayers I-III. Prayer IV carries its own preface.
                + "function wrapCommonPreface(){if(document.getElementById('ministerium-common-preface'))return;var hs=document.querySelectorAll('h3'),a=null,b=null;"
                + "for(var j=0;j<hs.length;j++){var t=n(hs[j].textContent);if(!a&&t==='prefacio')a=hs[j];else if(a&&t.indexOf('plegarias eucaristicas')===0){b=hs[j];break;}}"
                + "if(!a||!b||a.parentNode!==b.parentNode)return;var box=document.createElement('div');box.id='ministerium-common-preface';a.parentNode.insertBefore(box,a);var cur=a;while(cur&&cur!==b){var next=cur.nextSibling;box.appendChild(cur);cur=next;}}wrapCommonPreface();"
                // Prayer PDFs I-III repeat the preface dialogue/Sanctus scaffold. Keep only the anaphora body.
                + "function trimPrayer(id){var box=document.getElementById(id);if(!box||box.getAttribute('data-trimmed')==='1')return;var ps=Array.prototype.slice.call(box.querySelectorAll('p'));var seen=false,cut=-1;"
                + "for(var k=0;k<ps.length;k++){var t=n(ps[k].textContent);if(t.indexOf('santo santo santo')>=0||t.indexOf('sanctus sanctus sanctus')>=0)seen=true;if(seen&&(t.indexOf('hosanna en el cielo')>=0||t.indexOf('hosanna in excelsis')>=0))cut=k;}"
                + "if(cut>=0){for(var q=0;q<=cut&&q<ps.length;q++)ps[q].remove();}ps=Array.prototype.slice.call(box.querySelectorAll('p'));"
                + "for(var r=0;r<ps.length;r++){var e=n(ps[r].textContent);if(e.indexOf('despues sigue el rito de la comunion')>=0||e.indexOf('deinde sequitur ritus communionis')>=0){for(var z=r;z<ps.length;z++)ps[z].remove();break;}}box.setAttribute('data-trimmed','1');}"
                + "trimPrayer('prayer1');trimPrayer('prayer2');trimPrayer('prayer3');"
                // A selector must work even if another optional enhancement failed during initialization.
                + "window.setPrayer=function(num){for(var p=1;p<=4;p++){var el=document.getElementById('prayer'+p),bt=document.getElementById('prayerButton'+p),on=p===num;if(el){el.hidden=!on;el.classList.toggle('hidden',!on);}if(bt&&!bt.disabled){bt.classList.toggle('selected',on);bt.setAttribute('aria-pressed',on?'true':'false');}}var cp=document.getElementById('ministerium-common-preface');if(cp)cp.hidden=num===4;};"
                + "if(document.getElementById('prayer2'))window.setPrayer(2);"
                // Avoid the duplicated 'Plegarias eucarísticas' / 'Plegaria eucarística' headings.
                + "var groups=document.querySelectorAll('.eucharistic-prayers');for(var g=0;g<groups.length;g++){var prev=groups[g].previousElementSibling;if(prev&&prev.tagName==='H3'&&n(prev.textContent).indexOf('plegarias eucaristicas')===0)prev.remove();}"
                // Rebuild ES/LAT as aligned paragraph rows instead of two independent long columns.
                + "var parallels=document.querySelectorAll('.parallel');for(var m=0;m<parallels.length;m++){var par=parallels[m];if(par.classList.contains('runtime-aligned')||par.children.length<2)continue;var left=par.children[0],right=par.children[1];if(!left.classList.contains('col')||!right.classList.contains('col'))continue;"
                + "var lp=left.querySelectorAll('.liturgia-papal>p'),rp=right.querySelectorAll('.liturgia-papal>p');if(lp.length===0&&rp.length===0)continue;var frag=document.createDocumentFragment();var head=document.createElement('div');head.className='parallel-head';head.innerHTML='<div>ES</div><div>LAT</div>';frag.appendChild(head);var max=Math.max(lp.length,rp.length);"
                + "for(var u=0;u<max;u++){var row=document.createElement('div');row.className='parallel-row';var c1=document.createElement('div'),c2=document.createElement('div');c1.className='parallel-cell';c2.className='parallel-cell';c1.setAttribute('data-lang','es');c2.setAttribute('data-lang','la');if(u<lp.length)c1.appendChild(lp[u].cloneNode(true));if(u<rp.length)c2.appendChild(rp[u].cloneNode(true));row.appendChild(c1);row.appendChild(c2);frag.appendChild(row);}par.innerHTML='';par.appendChild(frag);par.classList.add('runtime-aligned');}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
