package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Runtime presentation safeguards for the 3.1.1 Missal renderer.
 *
 * The generated Liturgia Papal document remains the source of truth. This layer keeps the
 * celebration usable on phones and tablets: one Eucharistic Prayer at a time, no duplicated
 * preface scaffold, no developer placeholders and a readable ES/LAT layout.
 */
public final class MissalRuntimeFixes31 {
    private MissalRuntimeFixes31() {}

    public static void inject(WebView webView) {
        if (webView == null) return;

        String css =
                ".hidden,.eucharistic-prayer[hidden],#ministerium-common-preface[hidden]{display:none!important}" +
                ".eucharistic-prayer{display:none!important}" +
                ".eucharistic-prayer.ministerium-prayer-active{display:block!important}" +
                ".ministerium-prayer-choice{display:flex!important;flex-wrap:wrap;gap:8px;margin:10px 0 16px}" +
                ".ministerium-prayer-choice button{border:1px solid currentColor;border-radius:18px;background:transparent;color:inherit;padding:7px 13px;font:inherit}" +
                ".ministerium-prayer-choice button[aria-pressed=\"true\"],.ministerium-prayer-choice .selected,.prayer-selector button[aria-pressed=\"true\"],.prayer-selector .selected{font-weight:700;outline:2px solid currentColor;outline-offset:1px}" +
                "details.eucharistic-prayer>summary{display:none!important}" +
                ".parallel.runtime-aligned{display:block!important;width:100%!important}" +
                ".parallel-head,.parallel-row{display:grid!important;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:20px;width:100%!important}" +
                ".parallel-head{font-weight:700;margin:0 0 8px;padding:0 0 6px;border-bottom:1px solid rgba(128,128,128,.35)}" +
                ".parallel-row{align-items:start;padding:.15em 0 .75em;margin:0 0 .35em;border-bottom:1px solid rgba(128,128,128,.14)}" +
                ".parallel-cell{min-width:0!important;width:auto!important;max-width:100%!important;overflow-wrap:break-word!important;word-break:normal!important;hyphens:auto}" +
                ".parallel-cell p{margin:.55em 0!important}" +
                "@media(max-width:960px){" +
                ".parallel-head{display:none!important}" +
                ".parallel-row{display:block!important;width:100%!important;padding:.35em 0 1em!important;margin:0 0 .8em!important;border-bottom:1px solid rgba(128,128,128,.25)!important}" +
                ".parallel-cell{display:block!important;width:100%!important;max-width:100%!important;margin:0!important;padding:0!important}" +
                ".parallel-cell+.parallel-cell{margin-top:.7em!important;padding-top:.7em!important;border-top:1px dashed rgba(128,128,128,.28)!important}" +
                ".parallel-cell[data-lang=\"es\"]:before{content:'ES';display:block;font-size:.72em;font-weight:700;opacity:.62;margin-bottom:.3em}" +
                ".parallel-cell[data-lang=\"la\"]:before{content:'LAT';display:block;font-size:.72em;font-weight:700;opacity:.62;margin-bottom:.3em}" +
                "}";

        String script = "(function(){"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "if(!document.getElementById('ministerium-runtime-fixes-31')){var st=document.createElement('style');st.id='ministerium-runtime-fixes-31';st.textContent=" + JSONObject.quote(css) + ";document.head.appendChild(st);}"
                // Remove extraction/escape artefacts.
                + "var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),x;while((x=w.nextNode())){if(!x.nodeValue)continue;var v=x.nodeValue;v=v.replace(/\\[n(?=\\s|$)/g,' ').replace(/\\\\n/g,' ');if(v!==x.nodeValue)x.nodeValue=v;}"
                // Never expose build/developer notes to the reader.
                + "var notices=document.querySelectorAll('.pending,.reference-day-hint,.source-warning');for(var i=0;i<notices.length;i++){var nt=n(notices[i].textContent);"
                + "if(nt.indexOf('se incorporara desde su fuente verificada')>=0||nt.indexOf('no se sustituye por el epub')>=0||nt.indexOf('texto especifico se mostrara cuando')>=0){notices[i].remove();continue;}"
                + "if(nt.indexOf('todavia no esta normalizado')>=0||nt.indexOf('se bloqueo el fallback')>=0){notices[i].textContent='Este formulario propio no está disponible todavía en el paquete local.';}"
                + "if(nt.indexOf('no de curas.com.ar')>=0){notices[i].textContent=notices[i].textContent.replace(/El texto se toma[^.]*\\./i,'');}}"
                // Bilingual renderer originally emitted four <details>. Upgrade them to the same
                // one-of-four selector used in Spanish so only the chosen anaphora exists visually.
                + "function upgradePrayerDetails(){var ds=document.querySelectorAll('details'),found=[];for(var d=0;d<ds.length;d++){var sm=ds[d].querySelector('summary');if(sm&&n(sm.textContent).indexOf('plegaria eucaristica')===0)found.push(ds[d]);}if(found.length<2)return;var bar=document.querySelector('.ministerium-prayer-choice');if(!bar){bar=document.createElement('div');bar.className='choicebar ministerium-prayer-choice';for(var b=1;b<=Math.min(4,found.length);b++){var bt=document.createElement('button');bt.type='button';bt.id='prayerButton'+b;bt.textContent=['I','II','III','IV'][b-1];bt.setAttribute('onclick','setPrayer('+b+')');bt.setAttribute('aria-pressed','false');bar.appendChild(bt);}found[0].parentNode.insertBefore(bar,found[0]);}for(var j=0;j<found.length&&j<4;j++){var box=found[j];box.id='prayer'+(j+1);box.classList.add('eucharistic-prayer');box.open=true;}}upgradePrayerDetails();"
                // Keep the common preface as one block before the selected anaphora.
                + "function wrapCommonPreface(){if(document.getElementById('ministerium-common-preface'))return;var hs=document.querySelectorAll('h3'),a=null,b=null;"
                + "for(var j=0;j<hs.length;j++){var t=n(hs[j].textContent);if(!a&&t==='prefacio')a=hs[j];else if(a&&t.indexOf('plegarias eucaristicas')===0){b=hs[j];break;}}"
                + "if(!a||!b||a.parentNode!==b.parentNode)return;var box=document.createElement('div');box.id='ministerium-common-preface';a.parentNode.insertBefore(box,a);var cur=a;while(cur&&cur!==b){var next=cur.nextSibling;box.appendChild(cur);cur=next;}}wrapCommonPreface();"
                // Prayer source files can repeat preface dialogue + Sanctus. Keep only the anaphora.
                + "function trimPrayer(id){var box=document.getElementById(id);if(!box||box.getAttribute('data-trimmed')==='1')return;var ps=Array.prototype.slice.call(box.querySelectorAll('p'));var seen=false,cut=-1;"
                + "for(var k=0;k<ps.length;k++){var t=n(ps[k].textContent);if(t.indexOf('santo santo santo')>=0||t.indexOf('sanctus sanctus sanctus')>=0)seen=true;if(seen&&(t.indexOf('hosanna en el cielo')>=0||t.indexOf('hosanna in excelsis')>=0))cut=k;}"
                + "if(cut>=0){for(var q=0;q<=cut&&q<ps.length;q++)ps[q].remove();}ps=Array.prototype.slice.call(box.querySelectorAll('p'));"
                + "for(var r=0;r<ps.length;r++){var e=n(ps[r].textContent);if(e.indexOf('despues sigue el rito de la comunion')>=0||e.indexOf('deinde sequitur ritus communionis')>=0){for(var z=r;z<ps.length;z++)ps[z].remove();break;}}box.setAttribute('data-trimmed','1');}"
                + "trimPrayer('prayer1');trimPrayer('prayer2');trimPrayer('prayer3');trimPrayer('prayer4');"
                // Remove duplicated group headings adjacent to the selector.
                + "var groups=document.querySelectorAll('.eucharistic-prayers');for(var g=0;g<groups.length;g++){var prev=groups[g].previousElementSibling;if(prev&&/^H[1-6]$/.test(prev.tagName)&&n(prev.textContent).indexOf('plegarias eucaristicas')===0)prev.remove();}"
                // One and only one Eucharistic Prayer is visible. IV has its own preface.
                + "window.setPrayer=function(num){num=parseInt(num,10);if(!(num>=1&&num<=4))num=2;for(var p=1;p<=4;p++){var el=document.getElementById('prayer'+p),bt=document.getElementById('prayerButton'+p),on=p===num;if(el){el.hidden=!on;el.classList.toggle('hidden',!on);el.classList.toggle('ministerium-prayer-active',on);el.setAttribute('aria-hidden',on?'false':'true');if(el.tagName==='DETAILS')el.open=on;}if(bt&&!bt.disabled){bt.classList.toggle('selected',on);bt.setAttribute('aria-pressed',on?'true':'false');}}var cp=document.getElementById('ministerium-common-preface');if(cp)cp.hidden=num===4;try{sessionStorage.setItem('ministerium-prayer',String(num));}catch(e){}};"
                + "var chosen=2;try{var saved=parseInt(sessionStorage.getItem('ministerium-prayer'),10);if(saved>=1&&saved<=4)chosen=saved;}catch(e){}window.setPrayer(chosen);"
                // Rebuild ES/LAT as matching paragraph rows; on portrait/narrow tablets each
                // semantic pair becomes ES followed immediately by its LAT counterpart.
                + "var parallels=document.querySelectorAll('.parallel');for(var m=0;m<parallels.length;m++){var par=parallels[m];if(par.classList.contains('runtime-aligned')||par.children.length<2)continue;var left=par.children[0],right=par.children[1];if(!left.classList.contains('col')||!right.classList.contains('col'))continue;"
                + "var lp=left.querySelectorAll('.liturgia-papal>p'),rp=right.querySelectorAll('.liturgia-papal>p');if(lp.length===0&&rp.length===0){lp=left.querySelectorAll('p');rp=right.querySelectorAll('p');}if(lp.length===0&&rp.length===0)continue;var frag=document.createDocumentFragment();var head=document.createElement('div');head.className='parallel-head';head.innerHTML='<div>ES</div><div>LAT</div>';frag.appendChild(head);var max=Math.max(lp.length,rp.length);"
                + "for(var u=0;u<max;u++){var row=document.createElement('div');row.className='parallel-row';var c1=document.createElement('div'),c2=document.createElement('div');c1.className='parallel-cell';c2.className='parallel-cell';c1.setAttribute('data-lang','es');c2.setAttribute('data-lang','la');if(u<lp.length)c1.appendChild(lp[u].cloneNode(true));if(u<rp.length)c2.appendChild(rp[u].cloneNode(true));row.appendChild(c1);row.appendChild(c2);frag.appendChild(row);}par.innerHTML='';par.appendChild(frag);par.classList.add('runtime-aligned');}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
