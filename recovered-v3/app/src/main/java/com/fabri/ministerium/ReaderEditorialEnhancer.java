package com.fabri.ministerium;

import android.content.Context;
import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Capa editorial común para los lectores HTML.
 *
 * Toma como referencia visual el Leccionario: títulos claramente separados,
 * referencias/rúbricas secundarias y respuestas reconocibles, sin alterar el
 * texto litúrgico ni los controles propios de cada documento.
 */
public final class ReaderEditorialEnhancer {
    private ReaderEditorialEnhancer() {}

    public static void apply(Context context, WebView webView) {
        if (webView == null) return;
        ReaderVisualPalette palette = ReaderVisualPalette.from(context);
        String css = ".ministerium-document-title{font-size:1.42em!important;line-height:1.25!important;"
                + "text-align:center!important;margin:.35em 0 1.2em!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-section-title,.ministerium-editorial-title{display:block!important;"
                + "font-size:1.12em!important;line-height:1.32!important;font-weight:700!important;text-align:left!important;"
                + "margin:1.65em 0 .72em!important;padding:0 0 .38em!important;border-bottom:1px solid "
                + palette.divider + "!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-subtitle{font-size:1.02em!important;font-weight:700!important;text-align:left!important;"
                + "margin:1.25em 0 .55em!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-section,.missal-inline-section,.eucharistic-prayers,.lp-prefaces{"
                + "margin:0 0 2.15em!important;padding:0 0 1.15em!important;border-bottom:1px solid "
                + palette.divider + "!important;}"
                + ".ministerium-section:last-child,.missal-inline-section:last-child{border-bottom:0!important;}"
                + ".liturgia-papal p{margin:.78em 0!important;line-height:1.68!important;"
                + "text-align:justify!important;text-align-last:left!important;-webkit-hyphens:auto!important;hyphens:auto!important;}"
                + ".ministerium-prose{line-height:1.68!important;text-align:justify!important;text-align-last:left!important;"
                + "-webkit-hyphens:auto!important;hyphens:auto!important;}"
                + ".ministerium-source-rubric{color:" + palette.muted + "!important;-webkit-text-fill-color:" + palette.muted
                + "!important;font-size:.89em!important;font-style:italic!important;line-height:1.56!important;"
                + "text-align:left!important;text-align-last:auto!important;-webkit-hyphens:none!important;hyphens:none!important;}"
                + ".ministerium-celebrant{line-height:1.64!important;text-align:left!important;text-align-last:auto!important;}"
                + ".ministerium-liturgical-response,.ministerium-people-response{font-weight:700!important;margin:.9em 0!important;"
                + "padding:9px 12px!important;border-left:4px solid " + palette.accent
                + "!important;border-radius:0 8px 8px 0!important;background:" + palette.panel
                + "!important;line-height:1.55!important;text-align:left!important;text-align-last:auto!important;}"
                + ".ministerium-option-label{font-size:.88em!important;font-weight:700!important;text-transform:uppercase!important;"
                + "letter-spacing:.035em!important;color:" + palette.accent + "!important;-webkit-text-fill-color:" + palette.accent
                + "!important;text-align:left!important;margin:1.05em 0 .35em!important;}"
                + ".ministerium-canticle{margin:1.45em 0 1.8em!important;padding:15px 16px!important;"
                + "border-left:4px solid " + palette.accent + "!important;border-radius:0 10px 10px 0!important;"
                + "background:" + palette.panel + "!important;}"
                + ".ministerium-canticle h3{margin:.05em 0 .85em!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-psalm,.psalm,.psalmus,.psalm-verse,.versus,.ministerium-antiphon{"
                + "text-align:left!important;text-align-last:auto!important;-webkit-hyphens:none!important;hyphens:none!important;}"
                + ".choicebar{margin-top:.7em!important;margin-bottom:1.15em!important;}"
                + ".proper-language-note{display:none!important;}"
                + "html[lang=\"la\"] .daily-proper[data-missal-source=\"arquidiocesis-gdl\"]{display:none!important;}";

        String script = "(function(){"
                + "var s=document.getElementById('ministerium-editorial-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-editorial-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + JSONObject.quote(css) + ";"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')"
                + ".replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "function shortText(e){return(e.textContent||'').replace(/\\s+/g,' ').trim();}"
                + "var hs=document.querySelectorAll('h1,h2,h3,h4');for(var i=0;i<hs.length;i++){"
                + "if(hs[i].tagName==='H1')hs[i].classList.add('ministerium-document-title');"
                + "else if(hs[i].tagName==='H2')hs[i].classList.add('ministerium-section-title');"
                + "else hs[i].classList.add('ministerium-subtitle');}"
                + "var title=/^(RITOS? INICIALES|RITUS INITIALES|ACTO PENITENCIAL|ACTUS PAENITENTIALIS|KYRIE|GLORIA|"
                + "ORACION COLECTA|ORATIO COLLECTA|LITURGIA DE LA PALABRA|LITURGIA VERBI|HOMILIA|"
                + "PROFESION DE FE|PROFESSIO FIDEI|ORACION UNIVERSAL|ORATIO UNIVERSALIS|LITURGIA EUCARISTICA|"
                + "LITURGIA EUCHARISTICA|PREPARACION DE LOS DONES|PRAEPARATIO DONORUM|ORACION SOBRE LAS OFRENDAS|"
                + "ORATIO SUPER OBLATA|PREFACIO(?: [IVXLCDM]+)?(?: PARA .*)?|PRAEFATIO(?: [IVXLCDM]+)?(?: .*)?|"
                + "PLEGARIA EUCARISTICA(?: [IVXLCDM]+)?|PREX EUCHARISTICA(?: [IVXLCDM]+)?|RITO DE LA COMUNION|"
                + "RITUS COMMUNIONIS|PADRE NUESTRO|PATER NOSTER|RITO DE LA PAZ|RITUS PACIS|FRACCION DEL PAN|"
                + "FRACTIO PANIS|ANTIFONA DE COMUNION|ANTIPHONA AD COMMUNIONEM|ORACION DESPUES DE LA COMUNION|"
                + "ORATIO POST COMMUNIONEM|RITO DE CONCLUSION|RITUS CONCLUSIONIS|HIMNO|HYMNUS|SALMODIA|"
                + "LECTURA BREVE|LECTIO BREVIS|RESPONSORIO BREVE|RESPONSORIUM BREVE|CANTICO EVANGELICO|"
                + "CANTICUM EVANGELICUM|PRECES|ORACION|ORATIO)$/;"
                + "var rubric=/^(EL SACERDOTE|EL DIACONO|EL CELEBRANTE|EL MINISTRO|EL OBISPO|LUEGO EL SACERDOTE|"
                + "DESPUES EL SACERDOTE|A CONTINUACION|ACABADA|TERMINADA|SI SE USA|SI SE EMPLEA|CUANDO|DONDE|"
                + "SACERDOS|DIACONUS|CELEBRANS|EPISCOPUS|DEINDE|TUNC|POSTEA|SI ADHIBETUR|UBI|HIS EXPLETIS)\\b/;"
                + "var people=/^(R\\.|℟\\.|PUEBLO:|TODOS:|ASAMBLEA:|OMNES:|POPULUS:|AMEN\\.?$)/;"
                + "var celebrant=/^(V\\.|℣\\.|SACERDOTE:|CELEBRANTE:|PRESIDENTE:|DIACONO:|MINISTRO:|SACERDOS:|CELEBRANS:|DIACONUS:)/;"
                + "var option=/^(O BIEN|OTRA FORMULA|VEL|ALIO MODO|AD LIBITUM)[:.]?$/;"
                + "var candidates=document.querySelectorAll('p,div');for(var j=0;j<candidates.length;j++){"
                + "var e=candidates[j];if(e.children.length>2||e.closest('.choicebar')||e.closest('button'))continue;"
                + "var raw=shortText(e),t=n(raw);if(!t)continue;"
                + "if(t.length<150&&title.test(t.replace(/\\s+\\d{1,3}\\.?$/,'')))e.classList.add('ministerium-editorial-title');"
                + "else if(t.length<480&&rubric.test(t))e.classList.add('ministerium-source-rubric');"
                + "else if(t.length<300&&people.test(t))e.classList.add('ministerium-liturgical-response');"
                + "else if(t.length<420&&celebrant.test(t))e.classList.add('ministerium-celebrant');"
                + "else if(t.length<80&&option.test(t))e.classList.add('ministerium-option-label');"
                + "else if(e.matches('.liturgia-papal p,.ritual-body,.reading-text'))e.classList.add('ministerium-prose');}"
                + "if(document.documentElement.lang==='la'){var notes=document.querySelectorAll('.proper-language-note');"
                + "for(var q=0;q<notes.length;q++)notes[q].remove();var spanish=document.querySelectorAll('.daily-proper[data-missal-source=arquidiocesis-gdl]');"
                + "for(var z=0;z<spanish.length;z++)spanish[z].remove();}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
