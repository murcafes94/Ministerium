package com.fabri.ministerium;

import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

/** Presenta Completas como un flujo continuo, sin saltos a páginas auxiliares. */
public final class ComplineEnhancer {
    private ComplineEnhancer() {}

    public static void inject(WebView webView, boolean ordained, boolean easterSeason) {
        try {
            JSONArray penance = new JSONArray()
                    .put("Yo confieso ante Dios todopoderoso y ante ustedes, hermanos, que he pecado mucho de pensamiento, palabra, obra y omisión. Por mi culpa, por mi culpa, por mi gran culpa. Por eso ruego a santa María, siempre Virgen, a los ángeles, a los santos y a ustedes, hermanos, que intercedan por mí ante Dios, nuestro Señor.")
                    .put("V. Señor, ten misericordia de nosotros. R. Porque hemos pecado contra ti. V. Muéstranos, Señor, tu misericordia. R. Y danos tu salvación.")
                    .put("V. Tú que has sido enviado a sanar los corazones afligidos: Señor, ten piedad. R. Señor, ten piedad. V. Tú que has venido a llamar a los pecadores: Cristo, ten piedad. R. Cristo, ten piedad. V. Tú que estás sentado a la derecha del Padre para interceder por nosotros: Señor, ten piedad. R. Señor, ten piedad.");
            JSONArray hymns = new JSONArray()
                    .put(new JSONObject().put("title", "Cuando el sol, Señor, se apaga")
                            .put("text", "Cuando el sol, Señor, se apaga,\ny las sombras todo llenan,\nilumina nuestras almas\ncon la luz de tu presencia.\n\nQue en las horas de la noche\nnos defiendas con tu brazo;\ny a la sombra de tus alas\nveles Tú nuestro descanso.\n\nGloria a ti, oh Padre bueno,\ny a tu Hijo Jesucristo,\ny al Espíritu divino\npor los siglos de los siglos. Amén."))
                    .put(new JSONObject().put("title", "Cuando la luz del sol es ya poniente")
                            .put("text", "Cuando la luz del sol es ya poniente,\ngracias, Señor, es nuestra melodía;\nrecibe, como ofrenda, amablemente,\nnuestro dolor, trabajo y alegría.\n\nSi poco fue el amor en nuestro empeño\nde darle vida al día que fenece,\nconvierta en realidad lo que fue un sueño\ntu gran amor que todo lo engrandece.\n\nTu cruz, Señor, redime nuestra suerte\nde pecadora en justa, e ilumina\nla senda de la vida y de la muerte\ndel hombre que en la fe lucha y camina.\n\nJesús, Hijo del Padre, cuando avanza\nla noche oscura sobre nuestro día,\nconcédenos la paz y la esperanza\nde esperar cada noche tu gran día. Amén."));
            JSONArray marian = new JSONArray()
                    .put(new JSONObject().put("title", "Madre del Redentor")
                            .put("text", "Madre del Redentor, virgen fecunda, puerta del cielo siempre abierta, estrella del mar, ven a librar al pueblo que tropieza y se quiere levantar. Ante la admiración de cielo y tierra, engendraste a tu santo Creador, y permaneces siempre virgen. Recibe el saludo del ángel Gabriel, y ten piedad de nosotros, pecadores."))
                    .put(new JSONObject().put("title", "Salve, Reina de los cielos")
                            .put("text", "Salve, Reina de los cielos y Señora de los ángeles; salve raíz, salve puerta, que dio paso a nuestra luz. Alégrate, virgen gloriosa, entre todas la más bella; salve, agraciada doncella, ruega a Cristo por nosotros."))
                    .put(new JSONObject().put("title", "Dios te salve, Reina")
                            .put("text", "Dios te salve, Reina y Madre de misericordia, vida, dulzura y esperanza nuestra, Dios te salve. A ti llamamos los desterrados hijos de Eva; a ti suspiramos, gimiendo y llorando, en este valle de lágrimas. Ea, pues, Señora, abogada nuestra, vuelve a nosotros tus ojos misericordiosos, y después de este destierro muéstranos a Jesús, fruto bendito de tu vientre. ¡Oh clemente, oh piadosa, oh dulce Virgen María!"))
                    .put(new JSONObject().put("title", "Bajo tu amparo")
                            .put("text", "Bajo tu amparo nos acogemos, santa Madre de Dios; no desprecies las oraciones que te dirigimos en nuestras necesidades; antes bien, líbranos de todo peligro, oh Virgen gloriosa y bendita."));
            if (easterSeason) marian.put(new JSONObject().put("title", "Regina caeli")
                    .put("text", "Reina del cielo, alégrate, aleluya, porque el Señor, a quien mereciste llevar, aleluya, ha resucitado según su palabra, aleluya. Ruega al Señor por nosotros, aleluya."));

            String conclusion = ordained
                    ? "V. El Señor esté con ustedes.\nR. Y con tu espíritu.\nV. La bendición de Dios todopoderoso, Padre, Hijo y Espíritu Santo, descienda sobre ustedes.\nR. Amén."
                    : "V. El Señor todopoderoso nos conceda una noche tranquila y una santa muerte.\nR. Amén.";
            String script = "(function(p,h,m,conclusion){if(document.getElementById('ministerium-compline-start'))return;"
                    + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "function find(q){var a=document.querySelectorAll('p,h1,h2,h3,h4');for(var i=0;i<a.length;i++)if(n(a[i].textContent).indexOf(q)>=0)return a[i];return null;}"
                    + "function buttons(items,target,id){var box=document.createElement('section');box.className='ministerium-flow';"
                    + "var nav=document.createElement('div');nav.className='ministerium-options';var out=document.createElement('p');out.id=id;out.className='ministerium-selected';"
                    + "for(var i=0;i<items.length;i++){(function(index){var b=document.createElement('button');b.type='button';b.textContent=items[index].title||('Fórmula '+(index+1));"
                    + "b.onclick=function(){out.textContent=items[index].text;};nav.appendChild(b);})(i);}box.appendChild(nav);box.appendChild(out);target.parentNode.insertBefore(box,target.nextSibling);out.textContent=items[0].text;}"
                    + "var exam=find('EXAMEN DE CONCIENCIA');if(exam){var start=document.createElement('section');start.id='ministerium-compline-start';"
                    + "start.innerHTML='<h3>Invocación inicial</h3><p><b>V.</b> Dios mío, ven en mi auxilio.<br><b>R.</b> Señor, date prisa en socorrerme.</p><p>Gloria al Padre, y al Hijo, y al Espíritu Santo. Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p><h3>Acto penitencial</h3>';"
                    + "exam.parentNode.insertBefore(start,exam);var pi=[];for(var x=0;x<p.length;x++)pi.push({title:'Fórmula '+(x+1),text:p[x]});buttons(pi,start,'ministerium-penance');}"
                    + "var hymn=find('HIMNO');if(hymn){var old=hymn.nextElementSibling;if(old&&old.querySelector('a'))old.style.display='none';buttons(h,hymn,'ministerium-hymn');}"
                    + "var final=find('ANTIFONA FINAL');if(final){var links=final.nextElementSibling;if(links)links.style.display='none';buttons(m,final,'ministerium-marian');}"
                    + "var end=find('CONCLUSION');if(end&&end.nextElementSibling){end.nextElementSibling.textContent=conclusion;end.nextElementSibling.style.whiteSpace='pre-line';}"
                    + "var s=document.createElement('style');s.textContent='.ministerium-flow{margin:14px 0 22px;padding:14px;border-left:4px solid #6E1D2A;background:rgba(201,165,92,.13)}.ministerium-options{display:flex;flex-wrap:wrap;gap:8px}.ministerium-options button{border:1px solid #6E1D2A;border-radius:18px;padding:8px 12px;background:transparent;color:#6E1D2A}.ministerium-selected{white-space:pre-line;line-height:1.65;margin-bottom:0}';document.head.appendChild(s);})("
                    + penance.toString() + "," + hymns.toString() + "," + marian.toString()
                    + "," + JSONObject.quote(conclusion) + ")";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {}
    }
}
