package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

/** Presenta Completas como un flujo continuo usando datos semánticos externos al código. */
public final class ComplineEnhancer {
    private ComplineEnhancer() {}

    public static void inject(WebView webView, boolean ordained, boolean easterSeason) {
        try {
            JSONObject data = ComplineContentRepository.load(webView.getContext());
            JSONArray penance = ComplineContentRepository.penitentialFormulas(data);
            JSONArray hymns = ComplineContentRepository.hymns(data);
            JSONArray marian = ComplineContentRepository.marianAntiphons(data, easterSeason);
            String conclusion = ComplineContentRepository.conclusion(data);
            String penitentialConclusion = ComplineContentRepository.penitentialConclusion(data);

            // La conclusión de Completas es propia de la Hora. ordained se conserva en
            // la firma para compatibilidad con los llamadores existentes.
            String script = "(function(p,h,m,conclusion,penitentialConclusion){"
                    + "if(document.getElementById('ministerium-compline-start'))return;"
                    + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "function findLabel(q){var a=document.querySelectorAll('p,h1,h2,h3,h4');for(var i=0;i<a.length;i++){var t=n(a[i].textContent);if(t===q||t.indexOf(q+' ')===0)return a[i];}return null;}"
                    + "function theme(){var probe=findLabel('HIMNO')||document.querySelector('h3,h2,h1')||document.body;var accent=getComputedStyle(probe).color||'#6E1D2A';"
                    + "var rgb=accent.match(/\\d+/g)||[110,29,42];var lum=(parseInt(rgb[0])*299+parseInt(rgb[1])*587+parseInt(rgb[2])*114)/1000;"
                    + "document.documentElement.style.setProperty('--ministerium-accent',accent);document.documentElement.style.setProperty('--ministerium-active-ink',lum>155?'#2A2521':'#FFFFFF');}"
                    + "function buttons(items,target,id){if(!items||!items.length)return null;var box=document.createElement('section');box.className='ministerium-flow';"
                    + "var nav=document.createElement('div');nav.className='ministerium-options';var out=document.createElement('p');out.id=id;out.className='ministerium-selected';"
                    + "function select(index){out.textContent=items[index].text||'';var bs=nav.querySelectorAll('button');for(var j=0;j<bs.length;j++){var active=j===index;bs[j].classList.toggle('ministerium-option-active',active);bs[j].setAttribute('aria-pressed',active?'true':'false');}}"
                    + "for(var i=0;i<items.length;i++){(function(index){var b=document.createElement('button');b.type='button';b.textContent=items[index].title||('Opción '+(index+1));b.setAttribute('aria-pressed','false');b.onclick=function(){select(index);};nav.appendChild(b);})(i);}"
                    + "box.appendChild(nav);box.appendChild(out);target.parentNode.insertBefore(box,target.nextSibling);select(0);return box;}"
                    + "function sourceFiltered(items,holder){if(!holder||!holder.querySelectorAll)return items;var links=holder.querySelectorAll('a');if(!links.length)return items;var wanted=[];for(var i=0;i<links.length;i++)wanted.push(n(links[i].textContent));"
                    + "var out=[];for(var j=0;j<wanted.length;j++){for(var k=0;k<items.length;k++){if(n(items[k].title)===wanted[j]){out.push(items[k]);break;}}}return out.length?out:items;}"
                    + "theme();var exam=findLabel('EXAMEN DE CONCIENCIA');var hymn=findLabel('HIMNO');"
                    + "if(exam){var start=document.createElement('section');start.id='ministerium-compline-start';start.className='ministerium-compline-start';"
                    + "start.innerHTML='<h3>Invocación inicial</h3><p class=\"ministerium-dialogue\"><b>℣.</b> Dios mío, ven en mi auxilio.<br><b>℟.</b> Señor, date prisa en socorrerme.</p><p>Gloria al Padre, y al Hijo, y al Espíritu Santo. Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p>';exam.parentNode.insertBefore(start,exam);}"
                    + "if(hymn){var penanceTitle=findLabel('ACTO PENITENCIAL');if(!penanceTitle){penanceTitle=document.createElement('h3');penanceTitle.id='ministerium-penance-title';penanceTitle.textContent='Acto penitencial';hymn.parentNode.insertBefore(penanceTitle,hymn);}else{penanceTitle.id='ministerium-penance-title';var stale=penanceTitle.nextElementSibling;while(stale&&stale!==hymn){var next=stale.nextElementSibling;stale.style.display='none';stale=next;}}"
                    + "var penanceBox=buttons(p,penanceTitle,'ministerium-penance');if(penanceBox){var rubric=document.createElement('p');rubric.className='ministerium-rubric';rubric.textContent='Si preside la celebración un ministro, él solo dice la conclusión siguiente; en caso contrario, la dicen todos:';"
                    + "var pc=document.createElement('p');pc.className='ministerium-penitential-conclusion';pc.textContent=penitentialConclusion;pc.style.whiteSpace='pre-line';penanceBox.parentNode.insertBefore(rubric,penanceBox.nextSibling);rubric.parentNode.insertBefore(pc,rubric.nextSibling);}"
                    + "var old=hymn.nextElementSibling;var actual=sourceFiltered(h,old);if(actual.length){if(old&&old.querySelector&&old.querySelector('a'))old.style.display='none';buttons(actual,hymn,'ministerium-hymn');}}"
                    + "var final=findLabel('ANTIFONA FINAL');if(final){var links=final.nextElementSibling;if(links)links.style.display='none';buttons(m,final,'ministerium-marian');}"
                    + "var end=findLabel('CONCLUSION');if(end&&end.nextElementSibling&&conclusion){end.nextElementSibling.textContent=conclusion;end.nextElementSibling.style.whiteSpace='pre-line';}"
                    + "var s=document.createElement('style');s.textContent='"
                    + ".ministerium-compline-start{margin:0 0 18px;}"
                    + ".ministerium-flow{margin:12px 0 16px;padding:14px;border-left:4px solid var(--ministerium-accent,#6E1D2A);background:rgba(201,165,92,.13);border-radius:0 8px 8px 0;}"
                    + ".ministerium-options{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px;}"
                    + ".ministerium-options button{border:1px solid var(--ministerium-accent,#6E1D2A);border-radius:18px;padding:8px 12px;background:transparent;color:var(--ministerium-accent,#6E1D2A)!important;-webkit-text-fill-color:var(--ministerium-accent,#6E1D2A)!important;line-height:1.2;}"
                    + ".ministerium-options button.ministerium-option-active{background:var(--ministerium-accent,#6E1D2A);color:var(--ministerium-active-ink,#FFFFFF)!important;-webkit-text-fill-color:var(--ministerium-active-ink,#FFFFFF)!important;}"
                    + ".ministerium-selected{white-space:pre-line;line-height:1.65;margin:0;}"
                    + ".ministerium-rubric{margin:12px 0 8px;font-size:.92em;font-style:italic;color:var(--ministerium-accent,#6E1D2A)!important;-webkit-text-fill-color:var(--ministerium-accent,#6E1D2A)!important;}"
                    + ".ministerium-penitential-conclusion{white-space:pre-line;line-height:1.65;margin:0 0 24px;}"
                    + ".ministerium-dialogue{line-height:1.75;}';document.head.appendChild(s);})("
                    + penance.toString() + "," + hymns.toString() + "," + marian.toString()
                    + "," + JSONObject.quote(conclusion) + "," + JSONObject.quote(penitentialConclusion) + ")";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {}
    }
}
