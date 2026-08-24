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
                    .put("Yo confieso ante Dios todopoderoso y ante ustedes, hermanos, que he pecado mucho de pensamiento, palabra, obra y omisión.\nPor mi culpa, por mi culpa, por mi gran culpa.\nPor eso ruego a santa María, siempre Virgen, a los ángeles, a los santos y a ustedes, hermanos, que intercedan por mí ante Dios, nuestro Señor.")
                    .put("℣. Señor, ten misericordia de nosotros.\n℟. Porque hemos pecado contra ti.\n\n℣. Muéstranos, Señor, tu misericordia.\n℟. Y danos tu salvación.")
                    .put("℣. Tú que has sido enviado a sanar los corazones afligidos: Señor, ten piedad.\n℟. Señor, ten piedad.\n\n℣. Tú que has venido a llamar a los pecadores: Cristo, ten piedad.\n℟. Cristo, ten piedad.\n\n℣. Tú que estás sentado a la derecha del Padre para interceder por nosotros: Señor, ten piedad.\n℟. Señor, ten piedad.");
            JSONArray hymns = new JSONArray()
                    .put(new JSONObject().put("title", "Cuando el sol, Señor, se apaga")
                            .put("text", "Cuando el sol, Señor, se apaga,\ny las sombras todo llenan,\nilumina nuestras almas\ncon la luz de tu presencia.\n\nQue en las horas de la noche\nnos defiendas con tu brazo;\ny a la sombra de tus alas\nveles Tú nuestro descanso.\n\nGloria a ti, oh Padre bueno,\ny a tu Hijo Jesucristo,\ny al Espíritu divino\npor los siglos de los siglos. Amén."))
                    .put(new JSONObject().put("title", "Cuando la luz del sol es ya poniente")
                            .put("text", "Cuando la luz del sol es ya poniente,\ngracias, Señor, es nuestra melodía;\nrecibe, como ofrenda, amablemente,\nnuestro dolor, trabajo y alegría.\n\nSi poco fue el amor en nuestro empeño\nde darle vida al día que fenece,\nconvierta en realidad lo que fue un sueño\ntu gran amor que todo lo engrandece.\n\nTu cruz, Señor, redime nuestra suerte\nde pecadora en justa, e ilumina\nla senda de la vida y de la muerte\ndel hombre que en la fe lucha y camina.\n\nJesús, Hijo del Padre, cuando avanza\nla noche oscura sobre nuestro día,\nconcédenos la paz y la esperanza\nde esperar cada noche tu gran día. Amén."))
                    .put(new JSONObject().put("title", "Cristo, Señor de la noche")
                            .put("text", "Cristo, Señor de la noche,\nque disipas las tinieblas:\nmientras los cuerpos reposan,\nsé tú nuestro centinela.\n\nDespués de tanta fatiga,\ndespués de tanta dureza,\nacógenos en tus brazos\ny danos noche serena.\n\nSi nuestros ojos se duermen,\nque el alma esté siempre en vela;\nen paz cierra nuestros párpados\npara que cesen las penas.\n\nY que al despuntar el alba,\notra vez con fuerzas nuevas,\nte demos gracias, oh Cristo,\npor la vida que comienza. Amén."))
                    .put(new JSONObject().put("title", "Se inclina ya mi frente")
                            .put("text", "Se inclina ya mi frente,\nsellado está el trabajo;\nSeñor, tu pecho sea\nla gracia del descanso.\n\nMis ojos se retiran,\nla voz deja su canto,\npero el amor enciende\nsu lámpara velando.\n\nLucero que te fuiste,\ncon gran amor amado,\nen tu gloria dormimos\ny en sueños te adoramos. Amén."))
                    .put(new JSONObject().put("title", "Cuando acabamos el día")
                            .put("text", "Cuando acabamos el día\nte suplicamos, Señor,\nnos hagas de centinela\ny otorgues tu protección.\n\nQue te sintamos: contigo\nsueñe nuestro corazón\npara cantar tus loores\nde nuevo al salir el sol.\n\nDanos vida saludable,\nalienta nuestro calor,\ntu claridad ilumine\nla oscuridad que llegó.\n\nDánoslo, Padre piadoso,\npor Jesucristo, el Señor,\nque reina con el Espíritu\nSanto vivificador. Amén."));
            JSONArray marian = new JSONArray()
                    .put(new JSONObject().put("title", "Madre del Redentor")
                            .put("text", "Madre del Redentor, virgen fecunda,\npuerta del cielo siempre abierta,\nestrella del mar,\nven a librar al pueblo que tropieza\ny se quiere levantar.\n\nAnte la admiración de cielo y tierra,\nengendraste a tu santo Creador,\ny permaneces siempre virgen.\n\nRecibe el saludo del ángel Gabriel,\ny ten piedad de nosotros, pecadores."))
                    .put(new JSONObject().put("title", "Salve, Reina de los cielos")
                            .put("text", "Salve, Reina de los cielos\ny Señora de los ángeles;\nsalve raíz, salve puerta,\nque dio paso a nuestra luz.\n\nAlégrate, virgen gloriosa,\nentre todas la más bella;\nsalve, agraciada doncella,\nruega a Cristo por nosotros."))
                    .put(new JSONObject().put("title", "Dios te salve, Reina")
                            .put("text", "Dios te salve, Reina y Madre de misericordia,\nvida, dulzura y esperanza nuestra, Dios te salve.\nA ti llamamos los desterrados hijos de Eva;\na ti suspiramos, gimiendo y llorando,\nen este valle de lágrimas.\n\nEa, pues, Señora, abogada nuestra,\nvuelve a nosotros tus ojos misericordiosos,\ny después de este destierro muéstranos a Jesús,\nfruto bendito de tu vientre.\n¡Oh clemente, oh piadosa, oh dulce Virgen María!"))
                    .put(new JSONObject().put("title", "Bajo tu amparo")
                            .put("text", "Bajo tu amparo nos acogemos,\nsanta Madre de Dios;\nno desprecies las oraciones\nque te dirigimos en nuestras necesidades;\nantes bien, líbranos de todo peligro,\noh Virgen gloriosa y bendita."));
            if (easterSeason) marian.put(new JSONObject().put("title", "Regina caeli")
                    .put("text", "Reina del cielo, alégrate, aleluya,\nporque el Señor, a quien mereciste llevar, aleluya,\nha resucitado según su palabra, aleluya.\nRuega al Señor por nosotros, aleluya."));

            // En Completas la conclusión es propia de la Hora; no se sustituye por la
            // bendición ministerial usada en Laudes o Vísperas. El parámetro ordained se
            // conserva en la firma para no romper los llamadores existentes.
            String conclusion = "℣. El Señor todopoderoso nos conceda una noche tranquila y una santa muerte.\n℟. Amén.";
            String penitentialConclusion = "Dios todopoderoso tenga misericordia de nosotros, perdone nuestros pecados y nos lleve a la vida eterna.\n℟. Amén.";

            String script = "(function(p,h,m,conclusion,penitentialConclusion){if(document.getElementById('ministerium-compline-start'))return;"
                    + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "function findLabel(q){var a=document.querySelectorAll('p,h1,h2,h3,h4');for(var i=0;i<a.length;i++){var t=n(a[i].textContent);if(t===q||t.indexOf(q+' ')===0)return a[i];}return null;}"
                    + "function buttons(items,target,id){var box=document.createElement('section');box.className='ministerium-flow';"
                    + "var nav=document.createElement('div');nav.className='ministerium-options';var out=document.createElement('p');out.id=id;out.className='ministerium-selected';"
                    + "function select(index){out.textContent=items[index].text;var bs=nav.querySelectorAll('button');for(var j=0;j<bs.length;j++){var active=j===index;bs[j].classList.toggle('ministerium-option-active',active);bs[j].setAttribute('aria-pressed',active?'true':'false');}}"
                    + "for(var i=0;i<items.length;i++){(function(index){var b=document.createElement('button');b.type='button';b.textContent=items[index].title||('Opción '+(index+1));b.setAttribute('aria-pressed','false');b.onclick=function(){select(index);};nav.appendChild(b);})(i);}"
                    + "box.appendChild(nav);box.appendChild(out);target.parentNode.insertBefore(box,target.nextSibling);select(0);return box;}"
                    + "var exam=findLabel('EXAMEN DE CONCIENCIA');var hymn=findLabel('HIMNO');"
                    + "if(exam){var start=document.createElement('section');start.id='ministerium-compline-start';start.className='ministerium-compline-start';"
                    + "start.innerHTML='<h3>Invocación inicial</h3><p class=\"ministerium-dialogue\"><b>℣.</b> Dios mío, ven en mi auxilio.<br><b>℟.</b> Señor, date prisa en socorrerme.</p><p>Gloria al Padre, y al Hijo, y al Espíritu Santo. Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p>';"
                    + "exam.parentNode.insertBefore(start,exam);}"
                    + "if(hymn){var penanceTitle=document.createElement('h3');penanceTitle.id='ministerium-penance-title';penanceTitle.textContent='Acto penitencial';hymn.parentNode.insertBefore(penanceTitle,hymn);"
                    + "var formulaTitles=['1.ª Fórmula','2.ª Fórmula','3.ª Fórmula'];var pi=[];for(var x=0;x<p.length;x++)pi.push({title:formulaTitles[x]||('Fórmula '+(x+1)),text:p[x]});var penanceBox=buttons(pi,penanceTitle,'ministerium-penance');"
                    + "var rubric=document.createElement('p');rubric.className='ministerium-rubric';rubric.textContent='Si preside la celebración un ministro, él solo dice la conclusión siguiente; en caso contrario, la dicen todos:';"
                    + "var pc=document.createElement('p');pc.className='ministerium-penitential-conclusion';pc.textContent=penitentialConclusion;pc.style.whiteSpace='pre-line';"
                    + "penanceBox.parentNode.insertBefore(rubric,penanceBox.nextSibling);rubric.parentNode.insertBefore(pc,rubric.nextSibling);"
                    + "var old=hymn.nextElementSibling;if(old&&old.querySelector&&old.querySelector('a'))old.style.display='none';buttons(h,hymn,'ministerium-hymn');}"
                    + "var final=findLabel('ANTIFONA FINAL');if(final){var links=final.nextElementSibling;if(links)links.style.display='none';buttons(m,final,'ministerium-marian');}"
                    + "var end=findLabel('CONCLUSION');if(end&&end.nextElementSibling){end.nextElementSibling.textContent=conclusion;end.nextElementSibling.style.whiteSpace='pre-line';}"
                    + "var s=document.createElement('style');s.textContent='"
                    + ".ministerium-compline-start{margin:0 0 18px;}"
                    + ".ministerium-flow{margin:12px 0 16px;padding:14px;border-left:4px solid #6E1D2A;background:rgba(201,165,92,.13);border-radius:0 8px 8px 0;}"
                    + ".ministerium-options{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px;}"
                    + ".ministerium-options button{border:1px solid #6E1D2A;border-radius:18px;padding:8px 12px;background:transparent;color:#6E1D2A;line-height:1.2;}"
                    + ".ministerium-options button.ministerium-option-active{background:#6E1D2A;color:#FFF!important;-webkit-text-fill-color:#FFF!important;}"
                    + ".ministerium-selected{white-space:pre-line;line-height:1.65;margin:0;}"
                    + ".ministerium-rubric{margin:12px 0 8px;font-size:.92em;font-style:italic;color:#6E1D2A!important;-webkit-text-fill-color:#6E1D2A!important;}"
                    + ".ministerium-penitential-conclusion{white-space:pre-line;line-height:1.65;margin:0 0 24px;}"
                    + ".ministerium-dialogue{line-height:1.75;}';document.head.appendChild(s);})("
                    + penance.toString() + "," + hymns.toString() + "," + marian.toString()
                    + "," + JSONObject.quote(conclusion) + "," + JSONObject.quote(penitentialConclusion) + ")";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {}
    }
}
