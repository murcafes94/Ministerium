package com.fabri.ministerium;

import android.content.Context;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HoursRepository {
    public static final HoursVolume DEVOTIONAL = new HoursVolume(
            "devotional",
            "Devocionario",
            "Oraciones y devociones en español y latín",
            "epubs/Devocionario-Opus-Dei.epub");
    public static final HoursVolume LATIN_2026 = new HoursVolume(
            "latin_2026",
            "Liturgia Horarum 2026",
            "Oficio completo en latín, organizado por meses",
            "epubs/Liturgia-horarum-2026-latin.epub");
    public static final HoursVolume ROMAN_MISSAL = new HoursVolume(
            "roman_missal",
            "Misal Diario Romano",
            "Ordinario latín/español, propios, lecturas y formularios",
            "epubs/Misal-Diario-Romano.epub");
    public static final HoursVolume VATICAN_II = new HoursVolume(
            "vatican_ii", "Concilio Vaticano II",
            "Constituciones, decretos y declaraciones", "epubs/Concilio-Vaticano-II.epub");
    public static final HoursVolume CATECHISM = new HoursVolume(
            "catechism", "Catecismo de la Iglesia Católica",
            "Partes, secciones, capítulos y numerales", "epubs/Catecismo-Iglesia-Catolica.epub");
    public static final HoursVolume CATECHISM_COMPENDIUM = new HoursVolume(
            "catechism_compendium", "Compendio del Catecismo",
            "Preguntas, respuestas y apéndice", "epubs/Compendio-Catecismo.epub");
    public static final HoursVolume SOCIAL_DOCTRINE = new HoursVolume(
            "social_doctrine", "Compendio de la Doctrina Social",
            "Principios y orientaciones de la doctrina social",
            "epubs/Compendio-Doctrina-Social.epub");
    public static final HoursVolume BIBLE = new HoursVolume(
            "bible_jerusalem", "Biblia de Jerusalén",
            "73 libros con notas y referencias", "epubs/Biblia-de-Jerusalen.epub");
    public static final HoursVolume SPANISH_DICTIONARY = new HoursVolume(
            "spanish_dictionary_rae_15", "Diccionario de la lengua española",
            "15.ª edición · Real Academia Española",
            "epubs/Diccionario-RAE-15-edicion.epub");
    public static final HoursVolume BIBLICAL_DICTIONARY = new HoursVolume(
            "biblical_dictionary_pdf", "Diccionario bíblico",
            "2.843 voces extraídas como texto del PDF",
            "epubs/Diccionario-Biblico-Texto.epub");
    public static final HoursVolume SAN_PABLO_BIBLICAL_DICTIONARY = new HoursVolume(
            "biblical_dictionary_san_pablo", "Diccionario bíblico abreviado",
            "881 voces · Equipo editorial San Pablo",
            "epubs/Diccionario-Biblico-Abreviado-San-Pablo.epub");
    public static final HoursVolume THEOLOGY_DICTIONARY = new HoursVolume(
            "theology_dictionary_eunsa", "Diccionario de Teología EUNSA",
            "94 artículos · edición 2006",
            "epubs/Diccionario-Teologia-Eunsa-2006.epub");
    public static final HoursVolume RATZINGER_WAY_OF_CROSS = new HoursVolume(
            "ratzinger_way_of_cross", "Viacrucis de Joseph Ratzinger",
            "Catorce estaciones, meditaciones, oraciones y bendición",
            "epubs/Via-Crucis-Joseph-Ratzinger.epub");

    private static final List<HoursVolume> REFERENCES = Collections.unmodifiableList(Arrays.asList(
            VATICAN_II, CATECHISM, CATECHISM_COMPENDIUM, SOCIAL_DOCTRINE));

    private static final List<HoursVolume> VOLUMES = Collections.unmodifiableList(Arrays.asList(
            new HoursVolume("advent", "Adviento", "Cuatro semanas y ferias privilegiadas",
                    "epubs/LH - 1. ADVIENTO.epub"),
            new HoursVolume("christmas", "Navidad", "Natividad, Epifanía y Bautismo del Señor",
                    "epubs/LH - 2. NAVIDAD.epub"),
            new HoursVolume("lent", "Cuaresma", "Desde el Miércoles de Ceniza hasta el Sábado Santo",
                    "epubs/LH - 3. CUARESMA.epub"),
            new HoursVolume("easter", "Pascua", "Octava, siete semanas, Ascensión y Pentecostés",
                    "epubs/LH - 4. PASCUA.epub"),
            new HoursVolume("ordinary", "Tiempo Ordinario", "Salterio de cuatro semanas y solemnidades",
                    "epubs/LH - 5. TIEMPO ORDINARIO.epub"),
            new HoursVolume("sanctoral", "Santoral", "Propio de los santos, comunes y Oficio de difuntos",
                    "epubs/LH - 6. SANTORAL.epub")
    ));

    private HoursRepository() {}

    public static List<HoursVolume> all() {
        return VOLUMES;
    }

    public static HoursVolume find(String id) {
        if (DEVOTIONAL.id.equals(id)) return DEVOTIONAL;
        if (LATIN_2026.id.equals(id)) return LATIN_2026;
        if (ROMAN_MISSAL.id.equals(id)) return ROMAN_MISSAL;
        if (BIBLE.id.equals(id)) return BIBLE;
        if (SPANISH_DICTIONARY.id.equals(id)) return SPANISH_DICTIONARY;
        if (BIBLICAL_DICTIONARY.id.equals(id)) return BIBLICAL_DICTIONARY;
        if (SAN_PABLO_BIBLICAL_DICTIONARY.id.equals(id)) return SAN_PABLO_BIBLICAL_DICTIONARY;
        if (THEOLOGY_DICTIONARY.id.equals(id)) return THEOLOGY_DICTIONARY;
        if (RATZINGER_WAY_OF_CROSS.id.equals(id)) return RATZINGER_WAY_OF_CROSS;
        for (HoursVolume volume : REFERENCES) {
            if (volume.id.equals(id)) return volume;
        }
        for (HoursVolume volume : VOLUMES) {
            if (volume.id.equals(id)) return volume;
        }
        return null;
    }

    public static boolean isDevotional(HoursVolume volume) {
        return volume != null && DEVOTIONAL.id.equals(volume.id);
    }

    public static boolean isLatin2026(HoursVolume volume) {
        return volume != null && LATIN_2026.id.equals(volume.id);
    }

    public static boolean isRomanMissal(HoursVolume volume) {
        return volume != null && ROMAN_MISSAL.id.equals(volume.id);
    }

    public static boolean isSpanishDictionary(HoursVolume volume) {
        return volume != null && SPANISH_DICTIONARY.id.equals(volume.id);
    }

    public static boolean isDictionary(HoursVolume volume) {
        return volume != null && (SPANISH_DICTIONARY.id.equals(volume.id)
                || BIBLICAL_DICTIONARY.id.equals(volume.id)
                || SAN_PABLO_BIBLICAL_DICTIONARY.id.equals(volume.id)
                || THEOLOGY_DICTIONARY.id.equals(volume.id));
    }

    public static boolean isBiblicalDictionary(HoursVolume volume) {
        return volume != null && (BIBLICAL_DICTIONARY.id.equals(volume.id)
                || SAN_PABLO_BIBLICAL_DICTIONARY.id.equals(volume.id));
    }

    public static boolean isTheologyDictionary(HoursVolume volume) {
        return volume != null && THEOLOGY_DICTIONARY.id.equals(volume.id);
    }

    public static boolean isRatzingerWayOfCross(HoursVolume volume) {
        return volume != null && RATZINGER_WAY_OF_CROSS.id.equals(volume.id);
    }

    public static List<HoursVolume> references() {
        return REFERENCES;
    }

    public static boolean isReference(HoursVolume volume) {
        return volume != null && REFERENCES.contains(volume);
    }

    public static List<SearchResult> search(Context context, String query, int maximum)
            throws Exception {
        String wanted = normalize(query);
        List<SearchResult> results = new ArrayList<>();
        List<HoursVolume> sources = new ArrayList<>(VOLUMES);
        sources.add(DEVOTIONAL);
        sources.add(RATZINGER_WAY_OF_CROSS);
        sources.add(ROMAN_MISSAL);
        sources.addAll(REFERENCES);

        for (HoursVolume volume : sources) {
            String section = volume.title;
            List<EpubTocEntry> entries = EpubUtils.tableOfContents(context, volume);
            for (int i = 0; i < entries.size(); i++) {
                EpubTocEntry entry = entries.get(i);
                if (entry.depth == 0) section = entry.title;
                if (isDevotional(volume) && shouldHideDevotionalEntry(entry.title)) continue;
                String haystack = entry.title + " " + section + " " + volume.title;
                if (normalize(haystack).contains(wanted)) {
                    String source = isReference(volume)
                            ? "Magisterio · " + volume.title + " · " + section
                            : isRatzingerWayOfCross(volume)
                            ? "Devocionario · Viacrucis de Joseph Ratzinger · " + section
                            : isDevotional(volume)
                            ? "Devocionario de Opus Dei · " + section
                            : isRomanMissal(volume)
                            ? "Misal Diario Romano · " + section
                            : "Liturgia de las Horas · " + volume.title + " · " + section;
                    results.add(new SearchResult(volume, i, entry.title, source));
                    if (results.size() >= maximum) return results;
                }
            }
        }
        return results;
    }

    public static boolean isBasicDuplicate(String title) {
        String value = normalize(title);
        return "padre nuestro".equals(value)
                || "avemaria".equals(value)
                || "gloria al padre".equals(value)
                || "salve".equals(value)
                || "credo".equals(value)
                || "credo apostolico".equals(value)
                || "angelus".equals(value)
                || "regina caeli".equals(value)
                || "santo rosario".equals(value)
                || "bendita sea tu pureza".equals(value);
    }

    public static boolean shouldHideDevotionalEntry(String title) {
        String value = normalize(title);
        return isBasicDuplicate(title)
                || "portada".equals(value)
                || value.startsWith("compartir");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace("æ", "ae")
                .replace("œ", "oe")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
