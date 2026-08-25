package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Turns Liturgia Papal source text into a practical celebration view without
 * changing the underlying text. Optional editorial rubrics are hidden by
 * default and can be restored with one button.
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
                + "function people(t){return /^(r\\.|℟\\.|amen\\.?$|y con tu espiritu|gloria a ti, senor|te alabamos, senor|demos gracias a dios|es justo y necesario|tuyo es el reino|senor, no soy digno|cordero de dios|hemos recibido|anunciamos tu muerte|cada vez que comemos|salvador del mundo)/.test(t);}"
                + "var ps=document.querySelectorAll('p');"
                + "for(var i=0;i<ps.length;i++){var p=ps[i],t=n(p.textContent);if(!t)continue;"
                + "if(people(t)){p.classList.add('ministerium-people-response');continue;}"
                + "if(rubric(t)){p.classList.add('ministerium-source-rubric');"
                + "if(essential(t))p.classList.add('ministerium-essential-rubric');"
                + "else p.classList.add('ministerium-optional-rubric');}}"
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
                "#ministerium-rubric-controls{display:flex;justify-content:flex-end;margin:0 0 14px}" +
                "#ministerium-rubric-toggle{border:1px solid currentColor;border-radius:18px;background:transparent;padding:7px 12px;font:inherit;font-size:.82em;opacity:.82}" +
                ".ministerium-source-rubric{font-size:.88em;font-style:italic;opacity:.78;margin:.55em 0}" +
                ".ministerium-optional-rubric{display:none}" +
                ".ministerium-show-all-rubrics .ministerium-optional-rubric{display:block}" +
                ".ministerium-essential-rubric{display:block;border-left:3px solid currentColor;padding-left:10px;opacity:.82}" +
                ".ministerium-people-response{font-weight:700;margin:.7em 0;padding:9px 12px;border-left:4px solid #C9A55C;border-radius:0 8px 8px 0;background:rgba(201,165,92,.10)}" +
                ".liturgia-papal p:not(.ministerium-source-rubric):not(.ministerium-people-response){margin:.72em 0}" +
                "section{break-inside:avoid-page}"
                )
                + ";document.head.appendChild(s);})()";
        webView.evaluateJavascript(script, null);
    }
}
