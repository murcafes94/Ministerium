package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Convierte el texto fuente del Misal en una vista celebrativa legible sin
 * cambiar las fórmulas ni los selectores propios del documento.
 */
public final class MissalCompactView {
    private MissalCompactView() {}

    public static void inject(WebView webView) {
        if (webView == null) return;
        String script = "(function(){if(document.getElementById('ministerium-compact-missal-style'))return;"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')"
                + ".replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function rubric(t){return /^(reunido el pueblo|cuando llega|terminado el canto|acabado el canto|tambien pueden usarse|el obispo|el sacerdote|el diacono|el celebrante|con las manos|con la mano|junta las manos|toma la |toma el |eleva |elevando |inclinado |se inclina|genuflex|luego|despues|a continuacion|mientras|si se |si no |si parece oportuno|cuando |entonces |en este momento|todos se |los fieles |el pueblo |terminada |acabado |seguidamente |aqui |puede |pueden |y continua|prosigue|dice en voz baja|dice:)/.test(t);}"
                + "function essential(t){return t.length<240&&/(se arrod|arrodill|inclina|genuflex|eleva|elevand|toma el pan|toma la hostia|toma el caliz|imposicion|signo de la paz|fraccion|consagr|epicles|extiende las manos sobre|muestra al pueblo)/.test(t);}"
                + "function people(t){return /^(r\\.|℟\\.|todos:|pueblo:|amen\\.?$|y con tu espiritu|gloria a ti, senor|te alabamos, senor|demos gracias a dios|es justo y necesario|tuyo es el reino|senor, no soy digno|cordero de dios|hemos recibido|anunciamos tu muerte|cada vez que comemos|salvador del mundo)/.test(t);}"
                + "function celebrant(t){return /^(v\\.|℣\\.|sacerdote:|celebrante:|diacono:|ministro:)/.test(t);}"
                + "function heading(t){return t.length<92&&/^(ritos? iniciales|ritus initiales|acto penitencial|actus paenitentialis|kyrie|gloria|oracion colecta|oratio collecta|liturgia de la palabra|liturgia verbi|profesion de fe|professio fidei|oracion universal|oratio universalis|liturgia eucaristica|liturgia eucharistica|preparacion de los dones|oracion sobre las ofrendas|oratio super oblata|prefacio|praefatio|plegaria eucaristica|prex eucharistica|rito de la comunion|ritus communionis|padre nuestro|pater noster|rito de la paz|ritus pacis|fraccion del pan|antifona de comunion|antiphona ad communionem|oracion despues de la comunion|oratio post communionem|rito de conclusion|ritus conclusionis)$/.test(t);}"
                + "var ps=document.querySelectorAll('p');"
                + "for(var i=0;i<ps.length;i++){var p=ps[i],t=n(p.textContent);if(!t)continue;"
                + "if(people(t)){p.classList.add('ministerium-people-response');continue;}"
                + "if(celebrant(t)){p.classList.add('ministerium-celebrant-speech');continue;}"
                + "if(heading(t)){p.classList.add('ministerium-missal-heading');continue;}"
                + "if(rubric(t)){p.classList.add('ministerium-source-rubric');"
                + "if(essential(t))p.classList.add('ministerium-essential-rubric');"
                + "else p.classList.add('ministerium-optional-rubric');}}"
                + "var notes=document.querySelectorAll('.source,.source-banner,.reference-source,.source-warning,.pending');"
                + "for(var j=0;j<notes.length;j++){var nt=n(notes[j].textContent);"
                + "if(notes[j].matches('.source,.source-banner,.reference-source')||/(paquete|epub|fallback|catalogo estructural|fuente textual|normalizad|implementacion)/.test(nt))"
                + "notes[j].classList.add('ministerium-technical-note');}"
                + "var host=document.body;var bar=document.createElement('div');bar.id='ministerium-rubric-controls';"
                + "var button=document.createElement('button');button.type='button';button.id='ministerium-rubric-toggle';"
                + "button.textContent='Mostrar todas las rúbricas';button.setAttribute('aria-pressed','false');"
                + "button.onclick=function(){var expanded=button.getAttribute('aria-pressed')==='true';"
                + "button.setAttribute('aria-pressed',expanded?'false':'true');"
                + "document.body.classList.toggle('ministerium-show-all-rubrics',!expanded);"
                + "button.textContent=expanded?'Mostrar todas las rúbricas':'Ocultar rúbricas opcionales';};"
                + "bar.appendChild(button);var first=host.firstElementChild;host.insertBefore(bar,first);"
                + "var s=document.createElement('style');s.id='ministerium-compact-missal-style';s.textContent="
                + JSONObject.quote(
                "#ministerium-rubric-controls{display:flex;justify-content:flex-end;margin:0 0 18px}" +
                "#ministerium-rubric-toggle{border:1px solid currentColor;border-radius:18px;background:transparent;padding:7px 12px;font:inherit;font-size:.82em;opacity:.82}" +
                ".ministerium-technical-note,.source,.source-banner,.reference-source{display:none!important}" +
                ".ministerium-missal-heading{display:block;font-weight:700;font-size:1.08em;margin:1.55em 0 .7em;padding-bottom:.32em;border-bottom:1px solid rgba(128,128,128,.28)}" +
                ".ministerium-source-rubric{font-size:.88em;font-style:italic;opacity:.78;margin:.7em 0;padding-left:12px;border-left:2px solid rgba(128,128,128,.35)}" +
                ".ministerium-optional-rubric{display:none}" +
                ".ministerium-show-all-rubrics .ministerium-optional-rubric{display:block}" +
                ".ministerium-essential-rubric{display:block;border-left-width:3px;padding-left:11px;opacity:.86}" +
                ".ministerium-celebrant-speech{margin:.88em 0;padding:8px 11px;border-left:3px solid rgba(110,29,42,.55);background:rgba(128,128,128,.055)}" +
                ".ministerium-people-response{font-weight:700;margin:.82em 0;padding:10px 13px;border-left:4px solid #C9A55C;border-radius:0 8px 8px 0;background:rgba(201,165,92,.10)}" +
                ".liturgia-papal p:not(.ministerium-source-rubric):not(.ministerium-people-response):not(.ministerium-celebrant-speech):not(.ministerium-missal-heading){margin:.78em 0;line-height:1.7}" +
                ".ministerium-section{margin-bottom:2.4em!important;padding-bottom:1.25em!important}" +
                ".missal-inline-section{margin:1.35em 0 1.8em!important}" +
                ".choicebar,.ministerium-alt-button{position:relative;z-index:1}" +
                "section{break-inside:avoid-page}"
                )
                + ";document.head.appendChild(s);})()";
        webView.evaluateJavascript(script, null);
    }
}
