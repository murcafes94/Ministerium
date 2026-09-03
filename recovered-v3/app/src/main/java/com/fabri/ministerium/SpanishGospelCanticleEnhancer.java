package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Garantiza el cántico evangélico completo en la Liturgia de las Horas española.
 * El EPUB limpio puede conservar Benedictus/Magníficat como enlace; aquí se
 * sustituye en su posición litúrgica y, como respaldo, se inserta antes de Preces.
 */
public final class SpanishGospelCanticleEnhancer {
    private SpanishGospelCanticleEnhancer() {}

    public static void inject(WebView webView) {
        if (webView == null) return;
        String benedictus = "<section class=\"ministerium-canticle\" data-ministerium-generated=\"gospel-canticle\">"
                + "<h3>Benedictus</h3>"
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
                + "<h3>Magníficat</h3>"
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
        String script = "(function(){function n(v){return(v||'').normalize('NFD')"
                + ".replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                + "var meta=document.querySelector('meta[name=ministerium-source]');"
                + "var source=meta?(meta.content||''):'';if(source.indexOf('clean-hours-')!==0)return;"
                + "if(document.querySelector('[data-ministerium-generated=gospel-canticle]'))return;"
                + "var links=document.querySelectorAll('a');for(var i=0;i<links.length;i++){var t=n(links[i].textContent);"
                + "if(t!=='BENEDICTUS'&&t!=='MAGNIFICAT')continue;var host=links[i].closest('p,div')||links[i];"
                + "host.insertAdjacentHTML('beforebegin',t==='BENEDICTUS'?" + JSONObject.quote(benedictus)
                + ":" + JSONObject.quote(magnificat) + ");host.remove();return;}"
                + "var all=n(document.body.textContent);if(all.indexOf('CANTICO EVANGELICO')<0)return;"
                + "if(all.indexOf('BENDITO SEA EL SENOR, DIOS DE ISRAEL')>=0||"
                + "all.indexOf('PROCLAMA MI ALMA LA GRANDEZA DEL SENOR')>=0)return;"
                + "var nodes=document.querySelectorAll('p,div,h1,h2,h3,h4'),preces=null;"
                + "for(var j=0;j<nodes.length;j++){if(n(nodes[j].textContent)==='PRECES'){preces=nodes[j];break;}}"
                + "if(!preces)return;var text=n(document.body.textContent);var isLauds=text.indexOf('LAUDES')>=0"
                + "&&text.indexOf('VISPERAS')<0;var anchor=preces.closest('p,div,h1,h2,h3,h4')||preces;"
                + "anchor.insertAdjacentHTML('beforebegin',isLauds?" + JSONObject.quote(benedictus)
                + ":" + JSONObject.quote(magnificat) + ");})()";
        webView.evaluateJavascript(script, null);
    }
}
