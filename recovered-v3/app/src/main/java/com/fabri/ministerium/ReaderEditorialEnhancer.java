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
                + "font-size:1.12em!important;line-height:1.32!important;font-weight:700!important;"
                + "margin:1.65em 0 .72em!important;padding:0 0 .38em!important;border-bottom:1px solid "
                + palette.divider + "!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-subtitle{font-size:1.02em!important;font-weight:700!important;"
                + "margin:1.25em 0 .55em!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".ministerium-section,.missal-inline-section,.eucharistic-prayers,.lp-prefaces{"
                + "margin:0 0 2.15em!important;padding:0 0 1.15em!important;border-bottom:1px solid "
                + palette.divider + "!important;}"
                + ".ministerium-section:last-child,.missal-inline-section:last-child{border-bottom:0!important;}"
                + ".liturgia-papal p{margin:.76em 0!important;line-height:1.68!important;}"
                + ".ministerium-liturgical-response{font-weight:700!important;margin:.9em 0!important;"
                + "padding:9px 12px!important;border-left:4px solid " + palette.accent
                + "!important;border-radius:0 8px 8px 0!important;background:" + palette.surface
                + "!important;}"
                + ".ministerium-canticle{margin:1.45em 0 1.8em!important;padding:15px 16px!important;"
                + "border-left:4px solid " + palette.accent + "!important;border-radius:0 10px 10px 0!important;"
                + "background:" + palette.surface + "!important;}"
                + ".ministerium-canticle h3{margin:.05em 0 .85em!important;color:" + palette.accent
                + "!important;-webkit-text-fill-color:" + palette.accent + "!important;}"
                + ".proper-language-note{display:none!important;}"
                + "html[lang=\"la\"] .daily-proper[data-missal-source=\"arquidiocesis-gdl\"]{display:none!important;}";

        String benedictus = "<section class=\"ministerium-canticle\" data-ministerium-generated=\"gospel-canticle\">"
                + "<h3>Cántico evangélico · Benedictus</h3>"
                + "<p>Bendito sea el Señor, Dios de Israel, porque ha visitado y redimido a su pueblo,<br>"
                + "suscitándonos una fuerza de salvación en la casa de David, su siervo,<br>"
                + "según lo había predicho desde antiguo por boca de sus santos profetas.</p>"
                + "<p>Es la salvación que nos libra de nuestros enemigos y de la mano de todos los que nos odian;<br>"
                + "realizando la misericordia que tuvo con nuestros padres, recordando su santa alianza<br>"
                + "y el juramento que juró a nuestro padre Abrahán.</p>"
                + "<p>Para concedernos que, libres de temor, arrancados de la mano de los enemigos,<br>"
                + "le sirvamos con santidad y justicia, en su presencia, todos nuestros días.</p>"
                + "<p>Y a ti, niño, te llamarán profeta del Altísimo, porque irás delante del Señor a preparar sus caminos,<br>"
                + "anunciando a su pueblo la salvación, el perdón de sus pecados.</p>"
                + "<p>Por la entrañable misericordia de nuestro Dios, nos visitará el sol que nace de lo alto,<br>"
                + "para iluminar a los que viven en tiniebla y en sombra de muerte,<br>"
                + "para guiar nuestros pasos por el camino de la paz.</p>"
                + "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p></section>";
        String magnificat = "<section class=\"ministerium-canticle\" data-ministerium-generated=\"gospel-canticle\">"
                + "<h3>Cántico evangélico · Magníficat</h3>"
                + "<p>Proclama mi alma la grandeza del Señor, se alegra mi espíritu en Dios, mi salvador;<br>"
                + "porque ha mirado la humillación de su esclava.</p>"
                + "<p>Desde ahora me felicitarán todas las generaciones, porque el Poderoso ha hecho obras grandes por mí: su nombre es santo,<br>"
                + "y su misericordia llega a sus fieles de generación en generación.</p>"
                + "<p>Él hace proezas con su brazo: dispersa a los soberbios de corazón,<br>"
                + "derriba del trono a los poderosos y enaltece a los humildes,<br>"
                + "a los hambrientos los colma de bienes y a los ricos los despide vacíos.</p>"
                + "<p>Auxilia a Israel, su siervo, acordándose de la misericordia<br>"
                + "—como lo había prometido a nuestros padres— en favor de Abrahán y su descendencia por siempre.</p>"
                + "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p></section>";

        String script = "(function(){"
                + "var s=document.getElementById('ministerium-editorial-style');"
                + "if(!s){s=document.createElement('style');s.id='ministerium-editorial-style';document.head.appendChild(s);}"
                + "s.innerHTML=" + JSONObject.quote(css) + ";"
                + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')"
                + ".replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "var hs=document.querySelectorAll('h1,h2,h3,h4');for(var i=0;i<hs.length;i++){"
                + "if(hs[i].tagName==='H1')hs[i].classList.add('ministerium-document-title');"
                + "else if(hs[i].tagName==='H2')hs[i].classList.add('ministerium-section-title');"
                + "else hs[i].classList.add('ministerium-subtitle');}"
                + "var title=/^(RITOS? INICIALES|RITUS INITIALES|ACTO PENITENCIAL|ACTUS PAENITENTIALIS|KYRIE|GLORIA|"
                + "ORACION COLECTA|ORATIO COLLECTA|LITURGIA DE LA PALABRA|LITURGIA VERBI|HOMILIA|HOMILIA|"
                + "PROFESION DE FE|PROFESSIO FIDEI|ORACION UNIVERSAL|ORATIO UNIVERSALIS|LITURGIA EUCARISTICA|"
                + "LITURGIA EUCHARISTICA|PREPARACION DE LOS DONES|PRAEPARATIO DONORUM|ORACION SOBRE LAS OFRENDAS|"
                + "ORATIO SUPER OBLATA|PREFACIO|PRAEFATIO|PLEGARIA EUCARISTICA|PREX EUCHARISTICA|RITO DE LA COMUNION|"
                + "RITUS COMMUNIONIS|PADRE NUESTRO|PATER NOSTER|RITO DE LA PAZ|RITUS PACIS|FRACCION DEL PAN|"
                + "FRACTIO PANIS|ANTIFONA DE COMUNION|ANTIPHONA AD COMMUNIONEM|ORACION DESPUES DE LA COMUNION|"
                + "ORATIO POST COMMUNIONEM|RITO DE CONCLUSION|RITUS CONCLUSIONIS|HIMNO|HYMNUS|SALMODIA|"
                + "LECTURA BREVE|LECTIO BREVIS|RESPONSORIO BREVE|RESPONSORIUM BREVE|CANTICO EVANGELICO|"
                + "CANTICUM EVANGELICUM|PRECES|ORACION|ORATIO)$/;"
                + "var candidates=document.querySelectorAll('p,div');for(var j=0;j<candidates.length;j++){"
                + "var e=candidates[j];if(e.children.length>2||e.closest('.choicebar')||e.closest('button'))continue;"
                + "var t=n(e.textContent);if(t.length>1&&t.length<92&&title.test(t))e.classList.add('ministerium-editorial-title');"
                + "if((/^R\\.|^℟\\.|^AMEN\\.?$/.test(t))&&t.length<260)e.classList.add('ministerium-liturgical-response');}"
                + "var meta=document.querySelector('meta[name=ministerium-source]');var source=meta?(meta.content||''):'';"
                + "if(source.indexOf('clean-hours')===0&&source.indexOf('latin')<0&&!document.querySelector('.ministerium-canticle')){"
                + "var links=document.querySelectorAll('a'),hasLink=false;for(var a=0;a<links.length;a++){var lt=n(links[a].textContent);"
                + "if(lt==='BENEDICTUS'||lt==='MAGNIFICAT'){hasLink=true;break;}}"
                + "var whole=n(document.body.textContent);if(!hasLink&&whole.indexOf('BENDITO SEA EL SENOR, DIOS DE ISRAEL')<0"
                + "&&whole.indexOf('PROCLAMA MI ALMA LA GRANDEZA DEL SENOR')<0){"
                + "var nodes=document.querySelectorAll('p,div,h1,h2,h3,h4'),preces=null;for(var k=0;k<nodes.length;k++){"
                + "if(n(nodes[k].textContent)==='PRECES'){preces=nodes[k];break;}}"
                + "if(preces){var before=n(document.body.innerText.substring(0,Math.max(0,document.body.innerText.indexOf(preces.innerText))));"
                + "var lauds=before.lastIndexOf('LAUDES')>=before.lastIndexOf('VISPERAS');"
                + "var anchor=preces.closest('p,div,h1,h2,h3,h4')||preces;anchor.insertAdjacentHTML('beforebegin',"
                + "lauds?" + JSONObject.quote(benedictus) + ":" + JSONObject.quote(magnificat) + ");}}}"
                + "if(document.documentElement.lang==='la'){var notes=document.querySelectorAll('.proper-language-note');"
                + "for(var q=0;q<notes.length;q++)notes[q].remove();var spanish=document.querySelectorAll('.daily-proper[data-missal-source=arquidiocesis-gdl]');"
                + "for(var z=0;z<spanish.length;z++)spanish[z].remove();}"
                + "})()";
        webView.evaluateJavascript(script, null);
    }
}
