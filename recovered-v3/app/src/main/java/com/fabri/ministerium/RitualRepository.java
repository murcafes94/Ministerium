package com.fabri.ministerium;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Ritual catalog sourced from Liturgia Papal build packages. */
public final class RitualRepository {
    private static final String SOURCE = "Liturgia Papal · libros litúrgicos";
    private static final String BASE = "rituals/liturgiapapal/";

    private RitualRepository() {}

    public static List<RitualDocument> documents() {
        return Arrays.asList(
                baptismOneChild(),
                baptismDanger(),
                baptismAlreadyBaptized(),
                sickPastoral(),
                funeralPraenotanda(),
                funeralPreces(),
                funeralTypical(),
                funeralSimplified(),
                funeralAshes(),
                blessingFamily(),
                blessingHouse(),
                blessingSick(),
                blessingTravel(),
                blessingTransport(),
                blessingLiturgicalObjects(),
                blessingWater(),
                blessingRosaries(),
                blessingAnimals()
        );
    }

    public static RitualDocument find(String id) {
        if (id == null) return null;
        for (RitualDocument document : documents()) {
            if (id.equals(document.id)) return document;
        }
        return null;
    }

    public static String readSection(Context context, RitualDocument document,
                                     RitualEntry entry) throws Exception {
        String source = readAsset(context, document.assetPath);
        int start = findOccurrence(source, entry.sourceTitle, entry.sourceOccurrence, 0);
        if (start < 0) return source.trim();
        int end = source.length();
        int position = document.entries.indexOf(entry);
        if (position >= 0 && position + 1 < document.entries.size()) {
            RitualEntry next = document.entries.get(position + 1);
            int candidate = findOccurrence(source, next.sourceTitle,
                    next.sourceOccurrence == 0 ? 0 : next.sourceOccurrence, start + 1);
            if (candidate > start) end = candidate;
        }
        return source.substring(start, end).trim();
    }

    public static String readAll(Context context, RitualDocument document) throws Exception {
        return readAsset(context, document.assetPath).trim();
    }

    private static RitualDocument baptismOneChild() {
        return one("baptism", "Bautismo de un solo niño",
                "Rito completo ordinario del Bautismo de un niño",
                "baptism_one_child.txt", "BAUTISMO DE UN SOLO NIÑO", "Bautismo");
    }

    private static RitualDocument baptismDanger() {
        return one("baptism-danger", "Bautismo en peligro de muerte",
                "Ritual abreviado para un niño en peligro de muerte",
                "baptism_danger.txt", "BAUTISMO DE UN NIÑO EN PELIGRO DE MUERTE", "Bautismo");
    }

    private static RitualDocument baptismAlreadyBaptized() {
        return one("baptism-reception", "Recepción de un niño ya bautizado",
                "Rito para recibir en la Iglesia a un niño ya bautizado",
                "baptism_received.txt", "PARA RECIBIR EN LA IGLESIA A UN NIÑO YA BAUTIZADO", "Bautismo");
    }

    private static RitualDocument sickPastoral() {
        List<RitualEntry> entries = Arrays.asList(
                new RitualEntry("Praenotanda", "PRAENOTANDA", "Enfermos", 1),
                new RitualEntry("Visita y comunión de los enfermos", "CAPÍTULO I.", "Enfermos", 1),
                new RitualEntry("Unción del enfermo", "CAPÍTULO II.", "Enfermos", 1),
                new RitualEntry("El Viático", "CAPÍTULO III.", "Enfermos", 1),
                new RitualEntry("Sacramentos en peligro inmediato de muerte", "CAPÍTULO IV.", "Enfermos", 1),
                new RitualEntry("Confirmación en peligro de muerte", "CAPÍTULO V.", "Enfermos", 1),
                new RitualEntry("Recomendación del alma", "CAPÍTULO VI.", "Enfermos", 1)
        );
        return new RitualDocument("sick", "Unción y pastoral de enfermos",
                "Visita, comunión, Unción, Viático y recomendación del alma",
                SOURCE + " · Ritual de la Unción y de la pastoral de enfermos",
                BASE + "unction.txt", entries);
    }

    private static RitualDocument funeralPraenotanda() {
        return one("funeral-praenotanda", "Exequias · Praenotanda",
                "Observaciones generales previas del Ritual de exequias",
                "funeral_praenotanda.txt", "OBSERVACIONES GENERALES PREVIAS", "Exequias");
    }

    private static RitualDocument funeralPreces() {
        return one("funeral-preces", "Preces antes de las exequias",
                "Oraciones para los momentos previos a las exequias",
                "funeral_preces.txt", "PRECES PARA ANTES DE LAS EXEQUIAS", "Exequias");
    }

    private static RitualDocument funeralTypical() {
        return one("funeral-typical", "Exequias · forma típica",
                "Celebración con tres estaciones",
                "funeral_typical.txt", "EXEQUIAS", "Exequias");
    }

    private static RitualDocument funeralSimplified() {
        return one("funeral-simplified", "Exequias · rito simplificado",
                "Celebración con una sola estación en la iglesia",
                "funeral_simplified.txt", "EXEQUIAS", "Exequias");
    }

    private static RitualDocument funeralAshes() {
        return one("funeral-ashes", "Exequias ante la urna de las cenizas",
                "Celebración ante la urna de las cenizas",
                "funeral_ashes.txt", "CELEBRACIÓN DE LAS EXEQUIAS ANTE LA URNA", "Exequias");
    }

    private static RitualDocument blessingFamily() {
        return one("blessing-family", "Bendición de una familia", "Bendicional",
                "blessing_family.txt", "BENDICIÓN DE UNA FAMILIA", "Bendiciones");
    }

    private static RitualDocument blessingHouse() {
        return one("blessing-house", "Bendición de una nueva casa", "Bendicional",
                "blessing_house.txt", "BENDICIÓN DE UNA NUEVA CASA", "Bendiciones");
    }

    private static RitualDocument blessingSick() {
        return one("blessing-sick", "Bendición de los enfermos", "Bendicional",
                "blessing_sick.txt", "BENDICIÓN DE LOS ENFERMOS", "Bendiciones");
    }

    private static RitualDocument blessingTravel() {
        return one("blessing-travel", "Bendición antes de un viaje", "Bendicional",
                "blessing_travel.txt", "BENDICIÓN DE LOS QUE VAN A EMPRENDER UN VIAJE", "Bendiciones");
    }

    private static RitualDocument blessingTransport() {
        return one("blessing-transport", "Bendición relativa a desplazamientos humanos",
                "Vehículos y otros medios de desplazamiento · Bendicional",
                "blessing_transport.txt", "DESPLAZAMIENTOS HUMANOS", "Bendiciones");
    }

    private static RitualDocument blessingLiturgicalObjects() {
        return one("blessing-liturgical-objects", "Bendición de objetos litúrgicos", "Bendicional",
                "blessing_liturgical_objects.txt", "BENDICIÓN DE OBJETOS QUE SE USAN EN LAS", "Bendiciones");
    }

    private static RitualDocument blessingWater() {
        return one("blessing-water", "Bendición del agua fuera de la Misa", "Bendicional",
                "blessing_water.txt", "RITO DE LA BENDICIÓN", "Bendiciones");
    }

    private static RitualDocument blessingRosaries() {
        return one("blessing-rosaries", "Bendición de los rosarios", "Bendicional",
                "blessing_rosaries.txt", "BENDICIÓN DE LOS ROSARIOS", "Bendiciones");
    }

    private static RitualDocument blessingAnimals() {
        return one("blessing-animals", "Bendición de los animales", "Bendicional",
                "blessing_animals.txt", "BENDICIÓN DE LOS ANIMALES", "Bendiciones");
    }

    private static RitualDocument one(String id, String title, String subtitle,
                                      String file, String marker, String category) {
        return new RitualDocument(id, title, subtitle, SOURCE,
                BASE + file,
                Collections.singletonList(new RitualEntry(title, marker, category)));
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static int findOccurrence(String source, String needle, int occurrence, int from) {
        if (source == null || needle == null || needle.isEmpty()) return -1;
        String haystack = normalize(source);
        String target = normalize(needle);
        int normalizedFrom = Math.max(0, Math.min(from, source.length()));
        int search = 0;
        int found = -1;
        int count = 0;
        while ((found = haystack.indexOf(target, search)) >= 0) {
            if (found >= normalizedFrom) {
                if (count == occurrence) return mapNormalizedIndex(source, target, found);
                count++;
            }
            search = found + Math.max(1, target.length());
        }
        return -1;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Normalization only removes combining marks, so a direct scan can map the
     * normalized index back to the original UTF-16 offset closely enough for section slicing.
     */
    private static int mapNormalizedIndex(String original, String normalizedNeedle, int normalizedIndex) {
        if (normalizedIndex <= 0) return 0;
        int seen = 0;
        for (int i = 0; i < original.length(); i++) {
            String one = normalize(String.valueOf(original.charAt(i)));
            seen += one.length();
            if (seen > normalizedIndex) return i;
        }
        return original.length();
    }
}
