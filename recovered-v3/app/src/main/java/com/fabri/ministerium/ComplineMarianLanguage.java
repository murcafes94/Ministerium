package com.fabri.ministerium;

import android.content.Context;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

/** Adds an ES/LAT switch to the final Marian antiphons of Compline. */
public final class ComplineMarianLanguage {
    private ComplineMarianLanguage() {}

    public static void inject(Context context, WebView webView, boolean easterSeason) {
        if (context == null || webView == null) return;
        try {
            JSONObject data = ComplineContentRepository.load(context);
            JSONArray items = ComplineContentRepository.marianAntiphons(data, easterSeason);
            JSONArray latin = new JSONArray();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                latin.put(item == null ? "" : latinFor(item.optString("id", "")));
            }
            String script = "(function(){"
                    + "var box=document.querySelector('[data-choice-group=\"marian\"]');if(!box)return;"
                    + "if(box.getAttribute('data-bilingual-ready')==='1')return;"
                    + "box.setAttribute('data-bilingual-ready','1');"
                    + "var latin=" + latin.toString() + ";"
                    + "var items=box.querySelectorAll('.choice-content');"
                    + "for(var i=0;i<items.length;i++){var item=items[i];var la=latin[i]||'';if(!la)continue;"
                    + "var es=document.createElement('div');es.className='ministerium-marian-es';"
                    + "while(item.firstChild)es.appendChild(item.firstChild);"
                    + "var lat=document.createElement('div');lat.className='ministerium-marian-la';lat.hidden=true;"
                    + "lat.textContent=la;lat.style.whiteSpace='pre-line';"
                    + "var nav=document.createElement('div');nav.className='ministerium-marian-language';"
                    + "var bes=document.createElement('button');bes.type='button';bes.textContent='ES';bes.className='active';"
                    + "var bla=document.createElement('button');bla.type='button';bla.textContent='LAT';"
                    + "(function(esNode,laNode,esButton,laButton){"
                    + "esButton.onclick=function(){esNode.hidden=false;laNode.hidden=true;esButton.classList.add('active');laButton.classList.remove('active');};"
                    + "laButton.onclick=function(){esNode.hidden=true;laNode.hidden=false;laButton.classList.add('active');esButton.classList.remove('active');};"
                    + "})(es,lat,bes,bla);nav.appendChild(bes);nav.appendChild(bla);item.appendChild(nav);item.appendChild(es);item.appendChild(lat);"
                    + "}"
                    + "var s=document.createElement('style');s.textContent='"
                    + ".ministerium-marian-language{display:flex;gap:8px;margin:0 0 12px}.ministerium-marian-language button{border:1px solid currentColor;border-radius:16px;background:transparent;color:inherit;padding:5px 11px;font:inherit}.ministerium-marian-language button.active{font-weight:700;text-decoration:underline}.ministerium-marian-la{white-space:pre-line}';document.head.appendChild(s);"
                    + "})()";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {}
    }

    private static String latinFor(String id) {
        if ("marian.alma-redemptoris".equals(id)) {
            return "Alma Redemptoris Mater, quae pervia caeli\n"
                    + "porta manes, et stella maris, succurre cadenti,\n"
                    + "surgere qui curat, populo: tu quae genuisti,\n"
                    + "natura mirante, tuum sanctum Genitorem,\n"
                    + "Virgo prius ac posterius, Gabrielis ab ore\n"
                    + "sumens illud Ave, peccatorum miserere.";
        }
        if ("marian.ave-regina".equals(id)) {
            return "Ave, Regina caelorum,\nave, Domina angelorum,\nsalve, radix, salve, porta,\n"
                    + "ex qua mundo lux est orta.\n\nGaude, Virgo gloriosa,\nsuper omnes speciosa;\n"
                    + "vale, o valde decora,\net pro nobis Christum exora.";
        }
        if ("marian.salve-regina".equals(id)) {
            return "Salve, Regina, mater misericordiae,\nvita, dulcedo, et spes nostra, salve.\n"
                    + "Ad te clamamus exsules filii Hevae,\nad te suspiramus, gementes et flentes\n"
                    + "in hac lacrimarum valle.\n\nEia ergo, advocata nostra,\n"
                    + "illos tuos misericordes oculos ad nos converte;\net Iesum, benedictum fructum ventris tui,\n"
                    + "nobis post hoc exsilium ostende.\nO clemens, O pia, O dulcis Virgo Maria!";
        }
        if ("marian.sub-tuum".equals(id)) {
            return "Sub tuum praesidium confugimus,\nsancta Dei Genetrix;\n"
                    + "nostras deprecationes ne despicias in necessitatibus;\n"
                    + "sed a periculis cunctis libera nos semper,\nVirgo gloriosa et benedicta.";
        }
        if ("marian.regina-caeli".equals(id)) {
            return "Regina caeli, laetare, alleluia.\nQuia quem meruisti portare, alleluia.\n"
                    + "Resurrexit, sicut dixit, alleluia.\nOra pro nobis Deum, alleluia.";
        }
        return "";
    }
}
