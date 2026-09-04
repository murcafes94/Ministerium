package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Runtime presentation safeguards for the Missal renderer.
 *
 * The generated Liturgia Papal document remains the source of truth. This layer keeps the
 * celebration usable on phones and tablets: one Eucharistic Prayer at a time, no duplicated
 * preface scaffold and no developer placeholders. Spanish and Latin remain separate readers;
 * no side-by-side renderer is reintroduced here.
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
                "details.eucharistic-prayer>summary{display:none!important}";

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
                // Upgrade any prayer <details> blocks to a single one-of-four selector.
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
                // Remove an immediate duplicate prayer heading inside the active block.
                + "function dedupePrayerHeading(id){var box=document.getElementById(id);if(!box)return;var hs=box.querySelectorAll('h1,h2,h3,h4,h5,h6,p');if(hs.length<2)return;var first=n(hs[0].textContent),second=n(hs[1].textContent);if(first&&first===second&&first.indexOf('plegaria eucaristica')===0)hs[1].remove();}"
                + "dedupePrayerHeading('prayer1');dedupePrayerHeading('prayer2');dedupePrayerHeading('prayer3');dedupePrayerHeading('prayer4');"
                // One and only one Eucharistic Prayer is visible. IV has its own preface.
                + "window.setPrayer=function(num){num=parseInt(num,10);if(!(num>=1&&num<=4))num=2;for(var p=1;p<=4;p++){var el=document.getElementById('prayer'+p),bt=document.getElementById('prayerButton'+p),on=p===num;if(el){el.hidden=!on;el.classList.toggle('hidden',!on);el.classList.toggle('ministerium-prayer-active',on);el.setAttribute('aria-hidden',on?'false':'true');if(el.tagName==='DETAILS')el.open=on;}if(bt&&!bt.disabled){bt.classList.toggle('selected',on);bt.setAttribute('aria-pressed',on?'true':'false');}}var cp=document.getElementById('ministerium-common-preface');if(cp)cp.hidden=num===4;try{sessionStorage.setItem('ministerium-prayer',String(num));}catch(e){}};"
                + "var chosen=2;try{var saved=parseInt(sessionStorage.getItem('ministerium-prayer'),10);if(saved>=1&&saved<=4)chosen=saved;}catch(e){}window.setPrayer(chosen);"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
