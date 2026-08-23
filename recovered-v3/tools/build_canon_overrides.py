#!/usr/bin/env python3
"""Build the current CIC overrides from the official reform documents.

The application base is the official Vatican archive.  The 2001 study PDF is
used here only to assemble unchanged paragraphs around a promulgated
replacement and, separately, to extract historical commentary.  Every changed
canon receives the promulgated Latin text and, when Vatican.va supplies it,
its Spanish publication.  A checked human reference translation is marked as
such whenever the replacement was published only in Latin or Italian.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from build_canon_text import extract


def clean(value: str) -> str:
    value = re.sub(r"\s+", " ", value.replace("\f", " ")).strip()
    return value.replace("§1", "§ 1").replace("§2", "§ 2").replace("§3", "§ 3").replace("§4", "§ 4").replace("§5", "§ 5")


def sections(value: str) -> dict[int, str]:
    value = clean(value)
    matches = list(re.finditer(r"§\s*(\d+)\.\s*", value))
    if not matches:
        return {0: value}
    result: dict[int, str] = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(value)
        result[int(match.group(1))] = f"§ {match.group(1)}. " + value[match.end():end].strip()
    return result


def merge(base: str, replacements: dict[int, str]) -> str:
    current = sections(base)
    current.update({number: clean(text) for number, text in replacements.items()})
    return " ".join(current[number] for number in sorted(current))


def parse_book_vi(path: Path) -> dict[int, str]:
    result: dict[int, list[str]] = {}
    current = 0
    for raw in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.replace("\f", "").strip()
        marker = re.match(r"Can\.\s*(\d+)\s*[-–]\s*(.*)", line)
        if marker:
            current = int(marker.group(1))
            if 1311 <= current <= 1399:
                result[current] = [marker.group(2)]
            else:
                current = 0
            continue
        if not current or not line or re.fullmatch(r"\d+", line):
            continue
        if (re.match(r"^(LIBRO|PARTE|T[íi]tulo|Cap[íi]tulo)\b", line)
                or (len(raw) - len(raw.lstrip()) > 24 and re.match(r"^(DE|Del|De la|De las|De los)\b", line))):
            continue
        result[current].append(line)
    return {canon: clean(" ".join(lines)) for canon, lines in result.items()}


def parse_mitis(path: Path) -> dict[int, str]:
    result: dict[int, list[str]] = {}
    current = 0
    started = False
    for raw in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.strip()
        marker = re.match(r"Can\.\s*(16(?:7\d|8\d|90|91))\.?\s*(.*)", line)
        if marker:
            canon = int(marker.group(1))
            if 1671 <= canon <= 1691:
                started = True
                current = canon
                result[current] = [marker.group(2)]
                continue
        if not started:
            continue
        if line.startswith("La disposición del can.") or line.startswith("Dispositio can."):
            break
        if line.startswith("Art.") or line.startswith("* * *"):
            continue
        if current and line:
            result[current].append(line)
    return {canon: clean(" ".join(lines)) for canon, lines in result.items()}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("base_pdf", type=Path)
    parser.add_argument("sources", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    base_es, base_la = extract(args.base_pdf)
    es = {canon: values[0] for canon, values in base_es.items()}
    la = {canon: values[0] for canon, values in base_la.items()}
    records: dict[str, dict[str, object]] = {}

    sources = {
        "omnium": "https://www.vatican.va/content/benedict-xvi/es/apost_letters/documents/hf_ben-xvi_apl_20091026_codex-iuris-canonici.html",
        "mitis": "https://www.vatican.va/content/francesco/es/motu_proprio/documents/papa-francesco-motu-proprio_20150815_mitis-iudex-dominus-iesus.html",
        "concordia": "https://www.vatican.va/content/francesco/es/motu_proprio/documents/papa-francesco-motu-proprio_20160531_de-concordia-inter-codices.html",
        "magnum": "https://press.vatican.va/content/salastampa/es/bollettino/pubblico/2017/09/09/car.html",
        "communis": "https://www.vatican.va/content/francesco/es/motu_proprio/documents/papa-francesco-motu-proprio-20190319_communis-vita.html",
        "authenticum": "https://www.vatican.va/content/francesco/la/motu_proprio/documents/papa-francesco-motu-proprio-20201101_authenticum-charismatis.html",
        "spiritus": "https://www.vatican.va/content/francesco/es/motu_proprio/documents/papa-francesco-motu-proprio-20210110_spiritus-domini.html",
        "book6": "https://www.vatican.va/archive/cod-iuris-canonici/esp/documents/cic_libro6_sp.pdf",
        "competentias": "https://www.vatican.va/content/francesco/es/motu_proprio/documents/20220211-motu-proprio-assegnare-alcune-competenze.html",
        "recognitum": "https://www.vatican.va/content/francesco/la/motu_proprio/documents/20220426-motu-proprio-recognitum-librum-vi.html",
        "expedit": "https://www.vatican.va/content/francesco/it/motu_proprio/documents/20230402-motu-proprio-expedit-ut-iura.html",
        "prelatures": "https://www.vatican.va/content/francesco/it/motu_proprio/documents/20230808-motu-proprio-prelature-personali.html",
    }

    def add(canon: int, es_text: str, la_text: str, reform: str, date: str,
            source: str, spanish_status: str = "vatican") -> None:
        records[str(canon)] = {
            "es": [clean(es_text)], "la": [clean(la_text)], "reform": reform,
            "date": date, "source": sources[source], "spanish_status": spanish_status,
        }

    # Omnium in mentem (2009)
    add(1008,
        "Mediante el sacramento del Orden, por institución divina, algunos de entre los fieles quedan constituidos ministros sagrados, al ser marcados con un carácter indeleble, y así son consagrados y destinados a servir, según el grado de cada uno, con nuevo y peculiar título, al pueblo de Dios.",
        "Sacramento ordinis ex divina institutione inter christifideles quidam, charactere indelebili quo signantur, constituuntur sacri ministri, qui nempe consecrantur et deputantur ut, pro suo quisque gradu, novo et peculiari titulo Dei populo inserviant.",
        "Omnium in mentem", "8 de abril de 2010", "omnium")
    add(1009, merge(es[1009], {3: "§ 3. Aquellos que han sido constituidos en el orden del episcopado o del presbiterado reciben la misión y la facultad de actuar en la persona de Cristo Cabeza; los diáconos, en cambio, son habilitados para servir al pueblo de Dios en la diaconía de la liturgia, de la palabra y de la caridad."}),
        merge(la[1009], {3: "§ 3. Qui constituti sunt in ordine episcopatus aut presbyteratus missionem et facultatem agendi in persona Christi Capitis accipiunt, diaconi vero vim populo Dei serviendi in diaconia liturgiae, verbi et caritatis."}),
        "Omnium in mentem", "8 de abril de 2010", "omnium")
    add(1086, merge(es[1086], {1: "§ 1. Es inválido el matrimonio entre dos personas, una de las cuales fue bautizada en la Iglesia católica o recibida en su seno, y otra no bautizada."}),
        merge(la[1086], {1: "§ 1. Matrimonium inter duas personas, quarum altera sit baptizata in Ecclesia catholica vel in eandem recepta, et altera non baptizata, invalidum est."}),
        "Omnium in mentem", "8 de abril de 2010", "omnium")
    add(1117, "La forma arriba establecida se ha de observar si al menos uno de los contrayentes fue bautizado en la Iglesia católica o recibido en ella, sin perjuicio de lo establecido en el c. 1127 § 2.",
        "Statuta superius forma servanda est, si saltem alterutra pars matrimonium contrahentium in Ecclesia catholica baptizata vel in eandem recepta sit, salvis praescriptis can. 1127, § 2.",
        "Omnium in mentem", "8 de abril de 2010", "omnium")
    add(1124, "Está prohibido, sin licencia expresa de la autoridad competente, el matrimonio entre dos personas bautizadas, una de las cuales haya sido bautizada en la Iglesia católica o recibida en ella después del bautismo, y otra adscrita a una Iglesia o comunidad eclesial que no se halle en comunión plena con la Iglesia católica.",
        "Matrimonium inter duas personas baptizatas, quarum altera sit in Ecclesia catholica baptizata vel in eandem post baptismum recepta, altera vero Ecclesiae vel communitati ecclesiali plenam communionem cum Ecclesia catholica non habenti adscripta, sine expressa auctoritatis competentis licentia prohibitum est.",
        "Omnium in mentem", "8 de abril de 2010", "omnium")

    # De concordia inter Codices (2016)
    add(111,
        "§ 1. Con la recepción del bautismo queda adscrito a la Iglesia latina el hijo de los progenitores que pertenecen a ella o, si uno de los dos no pertenece a ella, cuando ambos progenitores de común acuerdo hayan elegido que la prole fuera bautizada en la Iglesia latina; si falta el común acuerdo queda adscrito a la Iglesia sui iuris a la que pertenece el padre. § 2. Si solamente uno de los progenitores es católico, queda adscrito a la Iglesia a la que pertenece este progenitor católico. § 3. Cualquier bautizando que haya cumplido catorce años de edad puede libremente elegir ser bautizado en la Iglesia latina o en otra Iglesia sui iuris; en este caso, pertenece a la Iglesia que haya elegido.",
        "§ 1. Ecclesiae latinae per receptum baptismum adscribitur filius parentum, qui ad eam pertineant vel, si alteruter ad eam non pertineat, ambo concordi voluntate optaverint ut proles in Ecclesia latina baptizaretur; quodsi concors voluntas desit, Ecclesiae sui iuris ad quam pater pertinet adscribitur. § 2. Si vero unus tantum ex parentibus sit catholicus, Ecclesiae ad quam hic parens catholicus pertinet adscribitur. § 3. Quilibet baptizandus qui quartum decimum aetatis annum expleverit, libere potest eligere ut in Ecclesia latina vel in alia Ecclesia sui iuris baptizetur; quo in casu, ipse ad eam Ecclesiam pertinet quam elegerit.",
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(112,
        "§ 1. Después de recibido el bautismo se adscriben a otra Iglesia sui iuris: 1.º quien obtenga una licencia de la Sede Apostólica; 2.º el cónyuge que, al contraer matrimonio o durante el mismo, declare que pasa a la Iglesia sui iuris del otro cónyuge; pero una vez disuelto el matrimonio puede volver libremente a la Iglesia latina; 3.º los hijos de aquellos de los que se trata en los números 1 y 2 antes de cumplir catorce años de edad y, de igual manera, en el matrimonio mixto, los hijos de la parte católica que haya pasado legítimamente a otra Iglesia sui iuris; no obstante, alcanzada esa edad, ellos mismos pueden volver a la Iglesia latina. § 2. La costumbre, por prolongada que sea, de recibir los sacramentos según el rito de otra Iglesia sui iuris no comporta la adscripción a dicha Iglesia. § 3. Todo paso a otra Iglesia sui iuris tiene valor desde el momento de la declaración hecha en presencia del Ordinario del lugar de dicha Iglesia o del párroco propio o del sacerdote delegado por uno de ellos y de dos testigos, a no ser que un rescrito de la Sede Apostólica disponga otra cosa; y se anotará en el libro de bautismos.",
        "§ 1. Post receptum baptismum, alii Ecclesiae sui iuris adscribuntur: 1.º qui licentiam ab Apostolica Sede obtinuerit; 2.º coniux qui, in matrimonio ineundo vel eo durante, ad Ecclesiam sui iuris alterius coniugis se transire declaraverit; matrimonio autem soluto, libere potest ad latinam Ecclesiam redire; 3.º filii eorum, de quibus in nn. 1 et 2, ante decimum quartum aetatis annum completum itemque, in matrimonio mixto, filii partis catholicae quae ad aliam Ecclesiam sui iuris legitime transierit; adepta vero hac aetate, iidem possunt ad latinam Ecclesiam redire. § 2. Mos, quamvis diuturnus, sacramenta secundum ritum alius Ecclesiae sui iuris recipiendi, non secumfert adscriptionem eidem Ecclesiae. § 3. Omnis transitus ad aliam Ecclesiam sui iuris vim habet a momento declarationis factae coram eiusdem Ecclesiae Ordinario loci vel parocho proprio aut sacerdote ab alterutro delegato et duobus testibus, nisi rescriptum Sedis Apostolicae aliud ferat; et in libro baptizatorum adnotetur.",
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(535, merge(es[535], {2: "§ 2. En el libro de bautizados se anotará también la adscripción a una Iglesia sui iuris o el paso a otra Iglesia, así como la confirmación y todo lo que se refiere al estado canónico de los fieles por razón del matrimonio, quedando a salvo lo que prescribe el c. 1133, por razón de la adopción, de la recepción del orden sagrado y de la profesión perpetua emitida en un instituto religioso; y esas anotaciones han de hacerse constar siempre en la partida de bautismo."}),
        merge(la[535], {2: "§ 2. In libro baptizatorum adnotentur quoque adscriptio Ecclesiae sui iuris vel ad aliam transitus, necnon confirmatio, item quae pertinent ad statum canonicum christifidelium, ratione matrimonii, salvo quidem praescripto can. 1133, ratione adoptionis, ratione suscepti ordinis sacri, necnon professionis perpetuae in instituto religioso emissae; eaeque adnotationes in documento accepti baptismi semper referantur."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(868,
        "§ 1. Para bautizar lícitamente a un niño, se requiere: 1.º que den su consentimiento los padres o al menos uno de los dos, o quienes legítimamente hacen sus veces; 2.º que haya esperanza fundada de que el niño va a ser educado en la religión católica, sin perjuicio del § 3; si falta por completo esa esperanza, debe diferirse el bautismo, según las disposiciones del derecho particular, haciendo saber la razón a sus padres. § 2. El niño de padres católicos, e incluso de no católicos, en peligro de muerte, puede lícitamente ser bautizado, aun contra la voluntad de sus padres. § 3. El niño de cristianos no católicos puede ser lícitamente bautizado si los padres o al menos uno de ellos o la persona que legítimamente ocupa su lugar lo piden y si es imposible para ellos, física o moralmente, acceder a su propio ministro.",
        "§ 1. Ut infans licite baptizetur, oportet: 1.º parentes, saltem eorum unus aut qui legitime eorundem locum tenet, consentiant; 2.º spes habeatur fundata eum in religione catholica educatum iri, firma § 3; quae si prorsus deficiat, baptismus secundum praescripta iuris particularis differatur, monitis de ratione parentibus. § 2. Infans parentum catholicorum, immo et non catholicorum in periculo mortis licite baptizatur, etiam invitis parentibus. § 3. Infans christianorum non catholicorum licite baptizatur, si parentes aut unus saltem eorum aut is, qui legitime eorundem locum tenet, id petunt et si eis corporaliter aut moraliter impossibile sit accedere ad ministrum proprium.",
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1108, merge(es[1108], {3: "§ 3. Solo el sacerdote asiste válidamente al matrimonio entre dos partes orientales o entre una parte latina y una parte oriental católica o no católica."}),
        merge(la[1108], {3: "§ 3. Solus sacerdos valide assistit matrimonio inter partes orientales vel inter partem latinam et partem orientalem sive catholicam sive non catholicam."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1109, "El Ordinario del lugar y el párroco, a no ser que por sentencia o por decreto estuvieran excomulgados, o en entredicho, o suspendidos del oficio, o declarados tales, en virtud del oficio asisten válidamente en su territorio a los matrimonios no sólo de los súbditos, sino también de los que no son súbditos, con tal de que al menos una de las partes esté adscrita a la Iglesia latina.",
        "Loci Ordinarius et parochus, nisi per sententiam vel per decretum fuerint excommunicati vel interdicti vel suspensi ab officio aut tales declarati, vi officii, intra fines sui territorii, valide matrimoniis assistunt non tantum subditorum, sed etiam, dummodo alterutra saltem pars sit adscripta Ecclesiae latinae, non subditorum.",
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1111, merge(es[1111], {1: "§ 1. El Ordinario del lugar y el párroco, mientras desempeñan válidamente su oficio, pueden delegar a sacerdotes y a diáconos la facultad, incluso general, de asistir a los matrimonios dentro de los límites de su territorio, quedando firme sin embargo lo que prescribe el c. 1108 § 3."}),
        merge(la[1111], {1: "§ 1. Loci Ordinarius et parochus, quamdiu valide officio funguntur, possunt facultatem intra fines sui territorii matrimoniis assistendi, etiam generalem, sacerdotibus et diaconis delegare, firmo tamen eo quod praescribit can. 1108 § 3."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1112, merge(es[1112], {1: "§ 1. Donde no haya sacerdotes ni diáconos, el Obispo diocesano, previo voto favorable de la Conferencia Episcopal y obtenida licencia de la Santa Sede, puede delegar a laicos para que asistan a los matrimonios, quedando firme lo establecido en el c. 1108 § 3."}),
        merge(la[1112], {1: "§ 1. Ubi desunt sacerdotes et diaconi, potest Episcopus dioecesanus, praevio voto favorabili Episcoporum conferentiae et obtenta licentia Sanctae Sedis, delegare laicos, qui matrimoniis assistant, firmo praescripto can. 1108 § 3."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1116, merge(es[1116], {3: "§ 3. En las mismas circunstancias enumeradas en el § 1 nn. 1 y 2, el Ordinario del lugar puede otorgar a cualquier sacerdote católico la facultad de bendecir el matrimonio de fieles de las Iglesias orientales que no tienen plena comunión con la Iglesia católica, si espontáneamente lo piden, y con tal de que nada se oponga a la válida y lícita celebración del matrimonio. El mismo sacerdote, si prudentemente lo puede hacer, lo comunicará a la autoridad competente interesada de la Iglesia no católica."}),
        merge(la[1116], {3: "§ 3. In iisdem rerum adiunctis, de quibus in § 1, nn. 1 et 2, Ordinarius loci cuilibet sacerdoti catholico facultatem conferre potest matrimonium benedicendi christifidelium Ecclesiarum orientalium quae plenam cum Ecclesia catholica communionem non habent si sponte id petant, et dummodo nihil validae vel licitae celebrationi matrimonii obstet. Idem sacerdos, semper necessaria cum prudentia, auctoritatem competentem Ecclesiae non catholicae, cuius interest, de re certiorem faciat."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")
    add(1127, merge(es[1127], {1: "§ 1. En cuanto a la forma que debe emplearse en el matrimonio mixto, se han de observar las prescripciones del c. 1108; pero si contrae matrimonio una parte católica con otra no católica de rito oriental, la forma canónica se requiere únicamente para la licitud; para la validez se requiere la intervención de un sacerdote, observadas las demás prescripciones del derecho."}),
        merge(la[1127], {1: "§ 1. Ad formam quod attinet in matrimonio mixto adhibendam, serventur praescripta can. 1108; si tamen pars catholica matrimonium contrahit cum parte non catholica ritus orientalis, forma canonica celebrationis servanda est ad liceitatem tantum; ad validitatem autem requiritur interventus sacerdotis, servatis aliis de iure servandis."}),
        "De concordia inter Codices", "15 de septiembre de 2016", "concordia")

    # Later individual reforms.
    add(838,
        "§ 1. Regular la sagrada liturgia depende únicamente de la autoridad de la Iglesia: esto compete a la Sede Apostólica y, según el derecho, al Obispo diocesano. § 2. Es competencia de la Sede Apostólica ordenar la sagrada liturgia de la Iglesia universal, publicar los libros litúrgicos, revisar las adaptaciones aprobadas según la norma del derecho por la Conferencia Episcopal, así como vigilar para que en todos los lugares se respeten fielmente las normas litúrgicas. § 3. Corresponde a las Conferencias Episcopales preparar fielmente las versiones de los libros litúrgicos en las lenguas vernáculas, adaptadas convenientemente dentro de los límites definidos, aprobarlas y publicar los libros litúrgicos, para las regiones de su pertinencia, después de la confirmación de la Sede Apostólica. § 4. Al Obispo diocesano en la Iglesia a él confiada corresponde, dentro de los límites de su competencia, dar normas en materia litúrgica, a las cuales todos están obligados.",
        "§ 1. Sacrae liturgiae moderatio ab Ecclesiae auctoritate unice pendet; quae quidem est penes Apostolicam Sedem et, ad normam iuris, penes Episcopum dioecesanum. § 2. Apostolicae Sedis est sacram liturgiam Ecclesiae universae ordinare, libros liturgicos edere, aptationes, ad normam iuris a Conferentia Episcoporum approbatas, recognoscere, necnon advigilare ut ordinationes liturgicae ubique fideliter observentur. § 3. Ad Episcoporum Conferentias spectat versiones librorum liturgicorum in linguas vernaculas fideliter et convenienter intra limites definitos accommodatas parare et approbare atque libros liturgicos, pro regionibus ad quas pertinent, post confirmationem Apostolicae Sedis edere. § 4. Ad Episcopum dioecesanum in Ecclesia sibi commissa pertinet, intra limites suae competentiae, normas de re liturgica dare, quibus omnes tenentur.",
        "Magnum Principium", "1 de octubre de 2017", "magnum", "reference")
    add(694,
        "§ 1. Se ha de considerar expulsado ipso facto de un instituto el miembro que: 1.º haya abandonado notoriamente la fe católica; 2.º haya contraído matrimonio o lo haya atentado, aunque sea sólo de manera civil; 3.º se haya ausentado ilegítimamente de la casa religiosa, según el c. 665 § 2, por doce meses ininterrumpidos, teniendo en cuenta que el religioso está ilocalizable. § 2. En estos casos, una vez recogidas las pruebas, el Superior mayor con su consejo debe emitir sin ninguna demora una declaración del hecho, para que la expulsión conste jurídicamente. § 3. En el caso previsto por el § 1 n. 3, dicha declaración, para que conste jurídicamente, debe ser confirmada por la Santa Sede; para los institutos de derecho diocesano la confirmación corresponde al Obispo de la sede principal.",
        "§ 1. Ipso facto dimissus ab instituto habendus est sodalis qui: 1.º a fide catholica notorie defecerit; 2.º matrimonium contraxerit vel, etiam civiliter tantum, attentaverit; 3.º a domo religiosa illegitime absens fuerit, secundum can. 665 § 2, duodecim continuos menses, prae oculis habita eiusdem sodalis irreperibilitate. § 2. His in casibus Superior maior cum suo consilio, nulla mora interposita, collectis probationibus, declarationem facti emittat, ut iuridice constet de dimissione. § 3. In casu de quo in § 1 n. 3, talis declaratio, ut iuridice constet, a Sancta Sede confirmari debet; quod ad instituta iuris dioecesani attinet, confirmatio ad principis sedis Episcopum spectat.",
        "Communis vita", "10 de abril de 2019", "communis")
    add(729,
        "La expulsión de un miembro del instituto se realiza de acuerdo con lo establecido en los cc. 694 § 1, 1.º y 2.º y 695; las constituciones determinarán además otras causas de expulsión, con tal de que sean proporcionalmente graves, externas, imputables y jurídicamente comprobadas, procediendo de acuerdo con lo establecido en los cc. 697-700. A la expulsión se aplica lo prescrito en el c. 701.",
        "Sodalis ab instituto dimittitur ad normam cann. 694 § 1, 1.º et 2.º atque 695; constitutiones praeterea determinent alias causas dimissionis, dummodo sint proportionate graves, externae, imputabiles et iuridice comprobatae, atque modus procedendi servetur in cann. 697-700 statutus. Dimisso applicatur praescriptum can. 701.",
        "Communis vita", "10 de abril de 2019", "communis")
    add(579,
        "Los Obispos diocesanos, cada uno en su territorio, pueden erigir válidamente institutos de vida consagrada mediante decreto formal, previa licencia escrita de la Sede Apostólica.",
        "Episcopi dioecesani, in suo quisque territorio, instituta vitae consecratae formali decreto valide erigere possunt, praevia licentia Sedis Apostolicae scripto data.",
        "Authenticum charismatis", "10 de noviembre de 2020", "authenticum", "reference")
    add(230, merge(es[230], {1: "§ 1. Los laicos que tengan la edad y condiciones determinadas por decreto de la Conferencia Episcopal pueden ser llamados para el ministerio estable de lector y acólito, mediante el rito litúrgico prescrito; sin embargo, la colación de esos ministerios no les da derecho a ser sustentados o remunerados por la Iglesia."}),
        merge(la[230], {1: "§ 1. Laici, qui aetate dotibusque pollent Episcoporum conferentiae decreto statutis, per ritum liturgicum praescriptum ad ministeria lectoris et acolythi stabiliter assumi possunt; quae tamen ministeriorum collatio eisdem ius non confert ad sustentationem remunerationemve ab Ecclesia praestandam."}),
        "Spiritus Domini", "11 de enero de 2021", "spiritus")

    # Pascite gregem Dei: official 2021 Book VI, Spanish and Latin PDFs.
    book6_es = parse_book_vi(args.sources / "book6-es.txt")
    book6_la = parse_book_vi(args.sources / "book6-la.txt")
    if set(book6_es) != set(range(1311, 1400)) or set(book6_la) != set(range(1311, 1400)):
        raise RuntimeError("Book VI parser did not find all 89 canons in both languages")
    for canon in range(1311, 1400):
        add(canon, book6_es[canon], book6_la[canon], "Pascite gregem Dei",
            "8 de diciembre de 2021", "book6")

    # Competentias quasdam decernere (2022).
    add(237,
        "§ 1. En cada diócesis, cuando sea posible y conveniente, ha de haber un seminario mayor; en caso contrario, los alumnos, a fin de que se preparen para los ministerios sagrados, se encomendarán a otro seminario, o se erigirá un seminario interdiocesano. § 2. No se debe erigir un seminario interdiocesano sin que la Conferencia Episcopal, cuando se trate de un seminario para todo su territorio, o, en caso contrario, los Obispos interesados hayan obtenido antes la confirmación de la Sede Apostólica, tanto de la erección del mismo seminario como de sus estatutos.",
        "§ 1. In singulis dioecesibus sit seminarium maius, ubi id fieri possit atque expediat; secus concredantur alumni, qui ad sacra ministeria sese praeparent, alieno seminario aut erigatur seminarium interdioecesanum. § 2. Seminarium interdioecesanum ne erigatur nisi prius confirmatio Apostolicae Sedis, tum ipsius seminarii erectionis tum eiusdem statutorum, obtenta fuerit, et quidem ab Episcoporum conferentia, si agatur de seminario pro universo eius territorio, secus ab Episcopis quorum interest.",
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(242, merge(es[242], {1: "§ 1. En cada nación ha de haber un Plan de formación sacerdotal, que establecerá la Conferencia Episcopal, teniendo en cuenta las normas dadas por la autoridad suprema de la Iglesia, y que ha de ser confirmado por la Santa Sede; y debe adaptarse a las nuevas circunstancias, igualmente con la confirmación de la Santa Sede; en este Plan se establecerán los principios y normas generales, acomodados a las necesidades pastorales de cada región o provincia."}),
        merge(la[242], {1: "§ 1. In singulis nationibus habeatur institutionis sacerdotalis Ratio, ab Episcoporum conferentia, attentis quidem normis a suprema Ecclesiae auctoritate latis, statuenda et a Sancta Sede confirmanda, novis quoque adiunctis, confirmante item Sancta Sede, accommodanda, qua institutionis in seminario tradendae definiantur summa principia atque normae generales necessitatibus pastoralibus uniuscuiusque regionis vel provinciae aptatae."}),
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(265,
        "Es necesario que todo clérigo esté incardinado en una Iglesia particular o en una prelatura personal, o en un instituto de vida consagrada o en una sociedad que goce de esta facultad, o también en una asociación pública clerical que haya obtenido de la Sede Apostólica tal facultad, de modo que de ninguna manera se admitan los clérigos acéfalos o vagos.",
        "Quemlibet clericum oportet esse incardinatum aut alicui Ecclesiae particulari vel praelaturae personali, aut alicui instituto vitae consecratae vel societati hac facultate praeditis, aut etiam alicui consociationi publicae clericali quae eandem facultatem ab Apostolica Sede obtinuerit, ita ut clerici acephali seu vagi minime admittantur.",
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(604, merge(es[604], {3: "§ 3. La admisión y erección de tales asociaciones a nivel diocesano es competencia del Obispo diocesano, en el ámbito de su territorio; a nivel nacional es competencia de la Conferencia Episcopal, en el ámbito del propio territorio."}),
        merge(la[604], {3: "§ 3. Has consociationes recognoscere atque erigere est, pro consociationibus dioecesanis, Episcopi dioecesani, intra fines sui territorii, et, pro consociationibus nationalibus, Conferentiae Episcoporum, intra fines sui territorii."}),
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(686, merge(es[686], {1: "§ 1. El Superior general, con el consentimiento de su consejo, puede conceder por causa grave el indulto de exclaustración a un profeso de votos perpetuos, pero no por más de un quinquenio, y habiendo obtenido previamente, si se trata de un clérigo, el consentimiento del Ordinario del lugar en el que debe residir. Prorrogar ese indulto o concederlo por más de un quinquenio se reserva solamente a la Santa Sede o, cuando se trata de un instituto de derecho diocesano, al Obispo diocesano."}),
        merge(la[686], {1: "§ 1. Supremus Moderator, de consensu sui consilii, sodali a votis perpetuis professo, gravi de causa concedere potest indultum exclaustrationis, non tamen ultra quinquennium, praevio consensu Ordinarii loci in quo commorari debet, si agitur de clerico. Indultum prorogare vel illud ultra quinquennium concedere solummodo Sanctae Sedi vel, si de institutis iuris dioecesani agitur, Episcopo dioecesano reservatur."}),
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(688,
        "§ 1. Quien quisiera salir de un instituto después de haber transcurrido el tiempo de profesión, puede abandonarlo. § 2. Quien, durante la profesión temporal, pide, con causa grave, abandonar el instituto, puede conseguir del Superior general, con el consentimiento de su consejo, el indulto para marcharse; para un monasterio sui iuris, de los que trata el c. 615, ese indulto, para ser válido, ha de ser confirmado por el Obispo de la casa a la que el miembro está asignado.",
        "§ 1. Qui expleto professionis tempore ab instituto egredi voluerit, illud derelinquere potest. § 2. Qui perdurante professione temporaria, gravi de causa, petit ut institutum derelinquat, indultum discedendi consequi potest a supremo Moderatore de consensu eius consilii; quoad monasterium sui iuris, de quo in can. 615, indultum, ut valeat, confirmari debet ab Episcopo domus assignationis.",
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(699, merge(es[699], {2: "§ 2. En los monasterios autónomos de los que trata el c. 615, corresponde decidir sobre la expulsión al Superior mayor, con el consentimiento de su consejo."}),
        merge(la[699], {2: "§ 2. In monasteriis sui iuris, de quibus in can. 615, dimissionem sodalis professi decernere pertinet ad Superiorem Maiorem, de consensu eius Consilii."}),
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(775, merge(es[775], {2: "§ 2. Compete a la Conferencia Episcopal, si se considera útil, procurar la edición de catecismos para su territorio, previa confirmación de la Sede Apostólica."}),
        merge(la[775], {2: "§ 2. Episcoporum conferentiae est, si utile videatur, curare ut catechismi pro suo territorio, praevia Sedis Apostolicae confirmatione, edantur."}),
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(1308,
        "§ 1. La reducción de las cargas de Misas, que sólo se hará por causa justa y necesaria, se reserva al Obispo diocesano o al Superior general de un instituto de vida consagrada o de una sociedad de vida apostólica clericales. § 2. Compete al Obispo diocesano la facultad de reducir el número de Misas que han de celebrarse en virtud de legados válidos por sí mismos, cuando han disminuido las rentas y mientras persista esta causa, habida cuenta del estipendio legítimamente vigente en la diócesis, siempre que no haya alguien que esté obligado y a quien se le pueda exigir con eficacia que aumente la limosna. § 3. Compete al mismo Obispo la facultad de reducir las cargas o legados de Misas que pesan sobre instituciones eclesiásticas, si las rentas hubieran llegado a ser insuficientes para alcanzar convenientemente el fin propio de dicha institución. § 4. Goza de las mismas facultades expresadas en los §§ 2 y 3 el Superior general de un instituto de vida consagrada o de una sociedad de vida apostólica clericales.",
        "§ 1. Reductio onerum Missarum, ex iusta tantum et necessaria causa facienda, reservatur Episcopo dioecesano et supremo Moderatori instituti vitae consecratae vel societatis vitae apostolicae clericalium. § 2. Episcopo dioecesano competit facultas reducendi ob deminutionem redituum, quamdiu causa perduret, ad rationem eleemosynae in dioecesi legitime vigentis, Missas legatorum, quae sint per se stantia, dummodo nemo sit qui obligatione teneatur et utiliter cogi possit ad eleemosynae augmentum faciendum. § 3. Eidem competit facultas reducendi onera seu legata Missarum gravantia institutum ecclesiasticum, si reditus insufficientes evaserint ad finem proprium eiusdem instituti congruenter consequendum. § 4. Iisdem facultatibus, de quibus in §§ 2 et 3, gaudet supremus Moderator instituti vitae consecratae vel societatis vitae apostolicae clericalium.",
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")
    add(1310,
        "§ 1. El Ordinario podrá reducir, moderar o conmutar la voluntad de los fieles sobre causas pías, sólo por causa justa y necesaria, después de oír a los interesados y a su propio consejo de asuntos económicos, y respetando de la mejor manera posible la voluntad del fundador. § 2. En los demás casos, hay que recurrir a la Sede Apostólica.",
        "§ 1. Fidelium voluntatum pro piis causis reductio, moderatio et commutatio possunt ab Ordinario fieri ex iusta tantum et necessaria causa, auditis iis, quorum interest, et proprio consilio a rebus oeconomicis atque servata, meliore quo fieri potest modo, fundatoris voluntate. § 2. In ceteris casibus recurrendum est ad Sedem Apostolicam.",
        "Competentias quasdam decernere", "15 de febrero de 2022", "competentias")

    # Recognitum Librum VI and Expedit ut iura: Latin is promulgated; Spanish is a
    # checked human reference translation because the Vatican page has no Spanish replacement.
    add(695,
        "§ 1. Debe ser expulsado el miembro por los delitos de los que se trata en los cc. 1395, 1397 y 1398, a no ser que, en los delitos de los que se trata en los cc. 1395 §§ 2-3 y 1398 § 1, el Superior mayor juzgue que la expulsión no es absolutamente necesaria y que la enmienda del miembro, el restablecimiento de la justicia y la reparación del escándalo pueden proveerse suficientemente de otro modo. § 2. En esos casos, el Superior mayor, después de recoger las pruebas sobre los hechos y su imputabilidad, presentará al miembro la acusación y las pruebas, dándole la posibilidad de defenderse. Se enviarán al Superior general todas las actas, firmadas por el Superior mayor y por el notario, así como también las respuestas escritas del miembro y firmadas por él mismo.",
        "§ 1. Sodalis dimitti debet ob delicta de quibus in cann. 1395, 1397 et 1398, nisi in delictis, de quibus in cann. 1395 §§ 2-3 et 1398 § 1, Superior maior censeat dimissionem non esse omnino necessariam et emendationi sodalis atque restitutioni iustitiae et reparationi scandali satis alio modo consuli posse. § 2. Hisce in casibus, Superior maior, collectis probationibus circa facta et imputabilitatem, sodali dimittendo accusationem atque probationes significet, data eidem facultate sese defendendi. Acta omnia a Superiore maiore et a notario subscripta, una cum responsionibus sodalis scripto redactis et ab ipso sodale subscriptis, supremo Moderatori transmittantur.",
        "Recognitum Librum VI", "26 de abril de 2022", "recognitum", "reference")
    add(700,
        "El decreto de expulsión contra un profeso tiene vigor desde el momento en que se le notifica al interesado. Sin embargo, para que sea válido el decreto, debe indicar el derecho de que goza el expulsado de recurrir, sin la petición de que habla el c. 1734 § 1, dentro de los treinta días siguientes de haber recibido la notificación, a la autoridad competente. El recurso tiene efecto suspensivo.",
        "Decretum dimissionis in sodalem professum latum vim habet simul ac ei, cuius interest, notificatur. Decretum vero, ut valeat, indicare debet ius, quo dimissus gaudet, recurrendi, absque petitione de qua in can. 1734, § 1, intra triginta dies a recepta notificatione ad auctoritatem competentem. Recursus effectum habet suspensivum.",
        "Expedit ut iura", "7 de mayo de 2023", "expedit", "reference")
    add(295,
        "§ 1. La prelatura personal, que se asimila a las asociaciones públicas clericales de derecho pontificio con facultad de incardinar clérigos, se rige por los estatutos aprobados o emanados por la Sede Apostólica y su gobierno se confía a un Prelado como Moderador, dotado de las facultades de Ordinario, a quien corresponde la potestad de erigir un seminario nacional o internacional así como incardinar a los alumnos y promoverlos a las órdenes a título de servicio a la prelatura. § 2. En cuanto Moderador dotado de las facultades de Ordinario, el Prelado debe cuidar de la formación espiritual de los ordenados con el mencionado título así como de su conveniente sustento.",
        "§ 1. Praelatura personalis, quae consociationibus publicis clericalibus iuris pontificii cum facultate incardinandi clericos assimilatur, regitur statutis ab Apostolica Sede probatis vel emanatis eique praeficitur Praelatus veluti Moderator, facultatibus Ordinarii praeditus, cui ius est nationale vel internationale seminarium erigere necnon alumnos incardinare, eosque titulo servitii praelaturae ad ordines promovere. § 2. Utpote Moderator facultatibus Ordinarii praeditus, Praelatus prospicere debet sive spirituali institutioni illorum, quos titulo praedicto promoverit, sive eorundem decorae sustentationi.",
        "Modificación de los cánones 295-296", "8 de agosto de 2023", "prelatures", "reference")
    add(296,
        "Observadas las prescripciones del c. 107, mediante acuerdos establecidos con la prelatura, los laicos pueden dedicarse a las obras apostólicas de la prelatura personal; pero han de determinarse adecuadamente en los estatutos el modo de esta cooperación orgánica y los principales deberes y derechos anejos a ella.",
        "Servatis can. 107 praescriptis, conventionibus cum praelatura initis, laici operibus apostolicis praelaturae personalis sese dedicare possunt; modus vero huius organicae cooperationis atque praecipua officia et iura cum illa coniuncta in statutis apte determinentur.",
        "Modificación de los cánones 295-296", "8 de agosto de 2023", "prelatures", "reference")

    # Mitis Iudex: complete new cann. 1671-1691 from the Vatican Spanish/Latin pages.
    mitis_es = parse_mitis(args.sources / "mitis-es.txt")
    mitis_la = parse_mitis(args.sources / "mitis-la.txt")
    if set(mitis_es) != set(range(1671, 1692)) or set(mitis_la) != set(range(1671, 1692)):
        raise RuntimeError("Mitis Iudex parser did not find all 21 canons in both languages")
    for canon in range(1671, 1692):
        add(canon, mitis_es[canon], mitis_la[canon], "Mitis Iudex Dominus Iesus",
            "8 de diciembre de 2015", "mitis")

    output = {
        "schema": 1,
        "verified_through": "2026-08-21",
        "base": "Archivo oficial bilingüe del Vaticano; comentario histórico de 2001",
        "canons": dict(sorted(records.items(), key=lambda item: int(item[0]))),
        "notes": {
            "588": {
                "text": "El Rescripto de 18 de mayo de 2022 establece una excepción concreta relacionada con el c. 588 § 2 sin sustituir el texto del canon.",
                "source": "https://press.vatican.va/content/salastampa/it/bollettino/pubblico/2022/05/18/0378/00789.html"
            },
            "699": {
                "text": "El Rescripto publicado el 28 de mayo de 2026 concede al Dicasterio competente una facultad particular respecto del c. 699 § 2; no sustituye el texto del canon.",
                "source": "https://press.vatican.va/content/salastampa/it/bollettino/pubblico/2026/05/28/0450/00884.html"
            }
        }
    }
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(records)} amended canons, all paired in Spanish and Latin")


if __name__ == "__main__":
    main()
