package com.fabri.ministerium;

import android.content.Context;

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

public final class PrayerRepository {
    private static final List<PrayerEntry> BASIC = Collections.unmodifiableList(Arrays.asList(
            new PrayerEntry("padre_nuestro", "Padre nuestro", "Oración del Señor",
                    "La oración que Jesús enseñó a sus discípulos.", "prayers/padre_nuestro.txt"),
            new PrayerEntry("ave_maria", "Ave María", "Oración mariana",
                    "Saludo evangélico y súplica a la Madre de Dios.", "prayers/ave_maria.txt"),
            new PrayerEntry("gloria", "Gloria al Padre", "Doxología",
                    "Alabanza al Padre, al Hijo y al Espíritu Santo.", "prayers/gloria.txt"),
            new PrayerEntry("credo_niceno", "Credo Niceno-Constantinopolitano", "Profesión de fe",
                    "La profesión de fe utilizada ordinariamente en la celebración eucarística.",
                    "prayers/credo_niceno.txt"),
            new PrayerEntry("credo_apostolico", "Símbolo de los Apóstoles", "Profesión de fe",
                    "La fórmula bautismal tradicional del Credo.", "prayers/credo_apostolico.txt"),
            new PrayerEntry("salve", "Salve", "Oración mariana",
                    "Súplica tradicional a la Virgen María.", "prayers/salve.txt"),
            new PrayerEntry("bendita_pureza", "Bendita sea tu pureza", "Oración mariana",
                    "Consagración breve y tradicional a la Virgen María.",
                    "prayers/bendita_pureza.txt"),
            new PrayerEntry("angelus", "Ángelus", "Oración mariana",
                    "Memoria cotidiana del misterio de la Encarnación.", "prayers/angelus.txt"),
            new PrayerEntry("regina_caeli", "Regina Cæli", "Oración mariana",
                    "Antífona pascual que sustituye al Ángelus durante el tiempo de Pascua.",
                    "prayers/regina_caeli.txt"),
            new PrayerEntry("santo_rosario", "Santo Rosario", "Devoción mariana",
                    "Guía de rezo y misterios para cada día de la semana.",
                    "prayers/santo_rosario.txt")
    ));
    private static final List<PrayerEntry> ADDITIONAL = Collections.unmodifiableList(Arrays.asList(
            new PrayerEntry("ofrecimiento_manana", "Ofrecimiento de la mañana",
                    "Oración cotidiana", "Ofrecer a Dios el nuevo día.",
                    "devotions/ofrecimiento_manana.txt"),
            new PrayerEntry("oracion_jesus", "Oración de Jesús",
                    "Oración contemplativa", "Invocación breve del nombre de Jesús.",
                    "devotions/oracion_jesus.txt"),
            new PrayerEntry("divinas_alabanzas", "Divinas alabanzas",
                    "Reparación y alabanza", "Alabanzas tradicionales al Nombre de Dios.",
                    "devotions/divinas_alabanzas.txt"),
            new PrayerEntry("coronilla_misericordia", "Coronilla de la Divina Misericordia",
                    "Devoción", "Guía breve para rezarla con un rosario común.",
                    "devotions/coronilla_misericordia.txt")
    ));

    private PrayerRepository() {}

    public static List<PrayerEntry> all() {
        List<PrayerEntry> result = new ArrayList<>(BASIC);
        result.addAll(ADDITIONAL);
        return Collections.unmodifiableList(result);
    }

    public static List<PrayerEntry> basic() {
        return BASIC;
    }

    public static List<PrayerEntry> additional() {
        return ADDITIONAL;
    }

    public static PrayerEntry find(String id) {
        for (PrayerEntry entry : all()) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    public static String read(Context context, PrayerEntry entry) throws IOException {
        try (InputStream input = context.getAssets().open(entry.assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name()).trim();
        }
    }

    public static List<PrayerEntry> search(Context context, String query) throws IOException {
        String wanted = normalize(query);
        List<PrayerEntry> results = new ArrayList<>();
        for (PrayerEntry entry : all()) {
            String haystack = entry.title + " " + entry.category + " "
                    + entry.description + " " + read(context, entry);
            if (normalize(haystack).contains(wanted)) results.add(entry);
        }
        return results;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
