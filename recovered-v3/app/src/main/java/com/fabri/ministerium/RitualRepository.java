package com.fabri.ministerium;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RitualRepository {
    public static final String COMMON_BLESSINGS_ID = "common_blessings";
    public static final String BAPTISM_ID = "baptism_children";

    private static final Map<String, String> TEXT_CACHE = new ConcurrentHashMap<>();
    private static final String[] BAPTISM_RUBRICS = {
            "Entonces el celebrante se dirige a los padres con estas palabras u otras semejantes:",
            "Y, en silencio, signa al niño en la frente. Después invita a los padres, y si parece oportuno a los padrinos, para que hagan lo mismo.",
            "Estando todos sentados, se lee una o algunas de las siguientes perícopas, según la oportunidad.",
            "Entre las lecturas pueden cantarse los salmos responsoriales con sus respuestas, tal como se proponen en los números 194-197.",
            "Después de la lectura el celebrante hace una breve homilía, para ilustrar a los oyentes sobre lo que han oído, haciéndoles penetrar más profundamente en el misterio del Bautismo e invitándoles a abrazar con entusiasmo la misión que les concierne especialmente como padres y padrinos. Después de la homilía o de la letanía, o durante la misma letanía, es muy conveniente que el celebrante invite a la asamblea a orar en silencio, y que los fieles oren al Señor en su interior. Después, si se puede, se entona un canto apropiado.",
            "Después el celebrante invita a los presentes a invocar a los santos.",
            "Pueden añadirse los nombres de otros santos, sobre todo de los que sean patronos del niño, de la iglesia o del lugar. Se termina así:",
            "Se hace la unción con el óleo de los catecúmenos en el pecho.",
            "Bendición e invocación a Dios sobre el agua",
            "El celebrante toca el agua con la mano derecha y prosigue:",
            "Renuncias y profesión de fe",
            "Seguidamente el celebrante pide esta triple profesión de fe a los padres y padrinos:",
            "E inmediatamente el celebrante bautiza al niño diciendo:",
            "El celebrante dice:",
            "Después el celebrante muestra el cirio pascual y dice:",
            "Seguidamente el celebrante dice:",
            "Recitación de la oración dominical",
            "Celebrante:"
    };
    private static final List<RitualDocument> DOCUMENTS = Collections.unmodifiableList(
            Arrays.asList(commonBlessings(), baptism(), enfermos(), atencionBreve()));
    private static final List<RitualDocument> PASTORAL_DOCUMENTS = Collections.unmodifiableList(
            Arrays.asList(baptism(), enfermos(), atencionBreve()));

    private RitualRepository() {}

    public static List<RitualDocument> all() {
        return DOCUMENTS;
    }

    public static List<RitualDocument> pastoral() {
        return PASTORAL_DOCUMENTS;
    }

    public static RitualDocument find(String id) {
        for (RitualDocument document : DOCUMENTS) {
            if (document.id.equals(id)) return document;
        }
        return null;
    }

    public static String readSection(Context context, RitualDocument document, int position)
            throws IOException {
        if (position < 0 || position >= document.entries.size()) return "";
        String complete = readDocument(context, document);
        RitualEntry entry = document.entries.get(position);
        int start = findOccurrence(complete, entry.sourceTitle, entry.sourceOccurrence, 0);
        if (start < 0) return "No se pudo localizar esta sección dentro de la fuente.";

        int end = complete.length();
        if (position + 1 < document.entries.size()) {
            RitualEntry next = document.entries.get(position + 1);
            int candidate = findOccurrence(complete, next.sourceTitle, 0,
                    start + entry.sourceTitle.length());
            if (candidate > start) end = candidate;
        }
        return clean(complete.substring(start, end));
    }

    public static CharSequence readSectionStyled(Context context, RitualDocument document,
                                                  int position) throws IOException {
        String text = readSection(context, document, position);
        if (!BAPTISM_ID.equals(document.id)) return text;
        SpannableString styled = new SpannableString(text);
        int rubricColor = Color.parseColor("#C62828");
        for (String rubric : BAPTISM_RUBRICS) {
            int from = 0;
            while (from < text.length()) {
                int start = text.indexOf(rubric, from);
                if (start < 0) break;
                styled.setSpan(new ForegroundColorSpan(rubricColor), start,
                        start + rubric.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                from = start + rubric.length();
            }
        }
        return styled;
    }

    public static List<SearchResult> search(Context context, String query, int maximum)
            throws IOException {
        String wanted = normalize(query);
        List<SearchResult> results = new ArrayList<>();
        for (RitualDocument document : DOCUMENTS) {
            for (int i = 0; i < document.entries.size(); i++) {
                RitualEntry entry = document.entries.get(i);
                String content = readSection(context, document, i);
                if (normalize(entry.title + " " + entry.category + " " + content)
                        .contains(wanted)) {
                    results.add(new SearchResult(document, entry, i,
                            entry.category + " · " + document.sourceName));
                    if (results.size() >= maximum) return results;
                }
            }
        }
        return results;
    }

    private static String readDocument(Context context, RitualDocument document)
            throws IOException {
        String cached = TEXT_CACHE.get(document.id);
        if (cached != null) return cached;
        try (InputStream input = context.getAssets().open(document.assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            String text = output.toString(StandardCharsets.UTF_8.name());
            TEXT_CACHE.put(document.id, text);
            return text;
        }
    }

    private static int findOccurrence(String text, String marker, int occurrence, int from) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerMarker = marker.toLowerCase(Locale.ROOT);
        int position = Math.max(0, from);
        for (int found = 0; found <= occurrence; found++) {
            position = lowerText.indexOf(lowerMarker, position);
            if (position < 0) return -1;
            if (found < occurrence) position += lowerMarker.length();
        }
        return position;
    }

    private static String clean(String value) {
        return value.replace('\u00a0', ' ')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private static RitualDocument commonBlessings() {
        List<RitualEntry> entries = Arrays.asList(
                new RitualEntry("Bendición de una familia",
                        "BENDICIÓN DE UNA FAMILIA", "Familia"),
                new RitualEntry("Bendición anual de las familias en sus casas",
                        "BENDICIÓN ANUAL DE LAS FAMILIAS EN SUS PROPIAS CASAS", "Familia y vivienda"),
                new RitualEntry("Bendición de quienes emprenden un viaje",
                        "BENDICIÓN DE LOS QUE VAN A EMPRENDER UN VIAJE", "Viaje"),
                new RitualEntry("Bendición de los enfermos",
                        "BENDICIÓN DE LOS ENFERMOS", "Enfermos"),
                new RitualEntry("Bendición de medios de transporte y desplazamientos",
                        "BENDICIÓN DE TODO LO RELACIONADO CON LOS DESPLAZAMIENTOS HUMANOS",
                        "Vehículos, vías y embarcaciones")
        );
        return new RitualDocument(COMMON_BLESSINGS_ID, "Bendicional",
                "Bendiciones comunes para la familia, enfermos y movilidad",
                "Selección pastoral del Bendicional",
                "rituals/bendicional_comun.txt", entries);
    }

    private static RitualDocument baptism() {
        List<RitualEntry> entries = Collections.singletonList(
                new RitualEntry("Bautismo de un solo niño",
                        "BAUTISMO DE UN SOLO NIÑO", "Ritual completo"));
        return new RitualDocument(BAPTISM_ID, "Ritual Del Bautismo de Niños",
                "Rito de acogida, Palabra, sacramento y conclusión",
                "Ritual Del Bautismo de Niños",
                "rituals/bautismo_ninos.txt", entries);
    }

    private static RitualDocument enfermos() {
        List<RitualEntry> entries = Arrays.asList(
                new RitualEntry("Nociones generales", "NOCIONES GENERALES", "Orientaciones", 1),
                new RitualEntry("Visita y comunión de los enfermos",
                        "Capítulo I\n\nVISITA Y COMUNIÓN DE LOS ENFERMOS", "Capítulo I"),
                new RitualEntry("Unción de los enfermos",
                        "Capítulo II\n\nUNCION DE LOS ENFERMOS", "Capítulo II"),
                new RitualEntry("El Viático", "Capítulo III\n\nEL VIATICO", "Capítulo III"),
                new RitualEntry("Sacramentos en peligro próximo de muerte",
                        "Capítulo IV\n\nRITO PARA ADMINISTRAR LOS SACRAMENTOS A UN ENFERMO QUE ESTA EN PELIGRO PROXIMO DE MUERTE", "Capítulo IV"),
                new RitualEntry("Confirmación en peligro de muerte",
                        "Capítulo V\n\nCONFIRMACION EN PELIGRO DE MUERTE", "Capítulo V"),
                new RitualEntry("Asistencia a los moribundos",
                        "ASISTENCIA A LOS MORIBUNDOS", "Capítulo VI", 1),
                new RitualEntry("Textos varios del Ritual de Enfermos",
                        "Capítulo VII\n\nTEXTOS VARIOS DEL RITUAL DE ENFERMOS", "Capítulo VII"),
                new RitualEntry("Orden de la Misa",
                        "APENDICE I\n\nORDEN DE LA MISA", "Apéndice I"),
                new RitualEntry("Confirmación sin Misa",
                        "APENDICE II\n\nORDEN PARA ADMINISTRAR LA CONFIRMACION SIN MISA", "Apéndice II")
        );
        return new RitualDocument("sick_ritual", "Ritual de Enfermos",
                "Unción, Viático y cuidado pastoral", "Edición pastoral argentina",
                "rituals/ritual_enfermos.txt", entries);
    }

    private static RitualDocument atencionBreve() {
        List<RitualEntry> entries = Arrays.asList(
                new RitualEntry("Sagrada Comunión al enfermo", "SAGRADA COMUNIÓN", "Atención de enfermos"),
                new RitualEntry("Penitencia, Unción y Viático",
                        "RITO CONJUNTO DE LA PENITENCIA, UNCIÓN Y VIÁTICO", "Rito conjunto"),
                new RitualEntry("Plegaria por un difunto", "PLEGARIA POR UN DIFUNTO", "Difuntos")
        );
        return new RitualDocument("sick_deceased_quick", "Enfermos y difuntos",
                "Formularios pastorales de consulta rápida", "Edición pastoral argentina",
                "rituals/enfermos_difuntos.txt", entries);
    }
}
