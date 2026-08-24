package com.fabri.ministerium.bible.semantic;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Maps Ministerium's current Spanish Bible labels to stable USFM-like book IDs. */
public final class BibleBookIdResolver {
    private static final Map<String, String> IDS = new HashMap<>();

    static {
        add("GEN", "Gn", "Génesis", "Genesis"); add("EXO", "Ex", "Éxodo", "Exodo");
        add("LEV", "Lv", "Levítico", "Levitico"); add("NUM", "Nm", "Números", "Numeros");
        add("DEU", "Dt", "Deuteronomio"); add("JOS", "Jos", "Josué", "Josue");
        add("JDG", "Jc", "Jueces"); add("RUT", "Rt", "Rut");
        add("1SA", "1 S", "1 Samuel", "I Samuel"); add("2SA", "2 S", "2 Samuel", "II Samuel");
        add("1KI", "1 R", "1 Reyes", "I Reyes"); add("2KI", "2 R", "2 Reyes", "II Reyes");
        add("1CH", "1 Cro", "1 Crónicas", "1 Cronicas"); add("2CH", "2 Cro", "2 Crónicas", "2 Cronicas");
        add("EZR", "Esd", "Esdras"); add("NEH", "Ne", "Nehemías", "Nehemias");
        add("TOB", "Tb", "Tobías", "Tobias"); add("JDT", "Jdt", "Judit");
        add("EST", "Est", "Ester"); add("1MA", "1 M", "1 Macabeos"); add("2MA", "2 M", "2 Macabeos");
        add("JOB", "Jb", "Job"); add("PSA", "Sal", "Salmos", "Salmo"); add("PRO", "Pr", "Proverbios");
        add("ECC", "Qo", "Eclesiastés", "Eclesiastes", "Qohélet", "Qohelet");
        add("SNG", "Ct", "Cantar de los Cantares", "Cantar"); add("WIS", "Sb", "Sabiduría", "Sabiduria");
        add("SIR", "Si", "Sirácida", "Siracida", "Eclesiástico", "Eclesiastico");
        add("ISA", "Is", "Isaías", "Isaias"); add("JER", "Jr", "Jeremías", "Jeremias");
        add("LAM", "Lm", "Lamentaciones"); add("BAR", "Ba", "Baruc"); add("EZK", "Ez", "Ezequiel");
        add("DAN", "Dn", "Daniel"); add("HOS", "Os", "Oseas"); add("JOL", "Jl", "Joel");
        add("AMO", "Am", "Amós", "Amos"); add("OBA", "Abd", "Abdías", "Abdias");
        add("JON", "Jon", "Jonás", "Jonas"); add("MIC", "Mi", "Miqueas"); add("NAM", "Na", "Nahúm", "Nahum");
        add("HAB", "Ha", "Habacuc"); add("ZEP", "So", "Sofonías", "Sofonias"); add("HAG", "Ag", "Ageo");
        add("ZEC", "Za", "Zacarías", "Zacarias"); add("MAL", "Ml", "Malaquías", "Malaquias");
        add("MAT", "Mt", "Mateo"); add("MRK", "Mc", "Marcos"); add("LUK", "Lc", "Lucas"); add("JHN", "Jn", "Juan");
        add("ACT", "Hch", "Hechos", "Hechos de los Apóstoles", "Hechos de los Apostoles"); add("ROM", "Rm", "Romanos");
        add("1CO", "1 Co", "1 Cor", "1 Corintios"); add("2CO", "2 Co", "2 Cor", "2 Corintios");
        add("GAL", "Ga", "Gálatas", "Galatas"); add("EPH", "Ef", "Efesios"); add("PHP", "Flp", "Filipenses");
        add("COL", "Col", "Colosenses"); add("1TH", "1 Ts", "1 Tesalonicenses"); add("2TH", "2 Ts", "2 Tesalonicenses");
        add("1TI", "1 Tm", "1 Timoteo"); add("2TI", "2 Tm", "2 Timoteo"); add("TIT", "Tt", "Tito");
        add("PHM", "Flm", "Filemón", "Filemon"); add("HEB", "Hb", "Hebreos"); add("JAS", "St", "Santiago");
        add("1PE", "1 P", "1 Pedro"); add("2PE", "2 P", "2 Pedro"); add("1JN", "1 Jn", "1 Juan");
        add("2JN", "2 Jn", "2 Juan"); add("3JN", "3 Jn", "3 Juan"); add("JUD", "Jds", "Judas");
        add("REV", "Ap", "Apocalipsis");
    }

    private BibleBookIdResolver() {}

    private static void add(String id, String... labels) {
        IDS.put(normalize(id), id);
        for (String label : labels) IDS.put(normalize(label), id);
    }

    public static String resolve(String abbreviation, String title) {
        String id = IDS.get(normalize(abbreviation));
        if (id == null) id = IDS.get(normalize(title));
        return id;
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "").trim();
    }
}
