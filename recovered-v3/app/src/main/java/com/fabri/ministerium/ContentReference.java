package com.fabri.ministerium;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;

/**
 * Identificadores canónicos de contenido para enlaces, anotaciones y exportación.
 *
 * La clave legacy de cada lector se conserva por compatibilidad; estos IDs son
 * estables frente a cambios de posición interna, índices de listas o navegación.
 */
public final class ContentReference {
    private ContentReference() {}

    public static String bible(String book, int chapter) {
        return "bible:" + token(book) + ":" + chapter;
    }

    public static String bibleVerse(String book, int chapter, String verse) {
        String base = bible(book, chapter);
        String value = token(verse);
        return value.isEmpty() ? base : base + ":" + value;
    }

    public static String canon(int number) {
        return "canon:" + number;
    }

    public static String magisterium(String document, String section) {
        String base = "magisterium:" + token(document);
        String part = token(section);
        return part.isEmpty() ? base : base + ":" + part;
    }

    public static String hours(Calendar date, String hour) {
        return "liturgy:" + isoDate(date) + ":" + token(hour);
    }

    public static String mass(Calendar date, String part) {
        return "mass:" + isoDate(date) + ":" + token(part);
    }

    public static String missal(Calendar date, String section, String language) {
        return "missal:" + isoDate(date) + ":" + token(section) + ":" + token(language);
    }

    public static String combined(Calendar date, String hour, String language) {
        return "celebration:" + isoDate(date) + ":mass+" + token(hour)
                + ":" + token(language);
    }

    public static String ritual(String documentId, String unitId) {
        String base = "ritual:" + token(documentId);
        String part = token(unitId);
        return part.isEmpty() ? base : base + ":" + part;
    }

    public static String generic(String module, String id) {
        return token(module) + ":" + token(id);
    }

    /**
     * Migra de forma no destructiva claves antiguas conocidas. Si no reconoce
     * una clave, devuelve la original para no inventar una referencia falsa.
     */
    public static String infer(String sourceKey, String reference) {
        String key = sourceKey == null ? "" : sourceKey.trim();
        String ref = reference == null ? "" : reference.trim();
        try {
            if (key.startsWith("bible:")) {
                int split = ref.lastIndexOf(' ');
                if (split > 0 && split + 1 < ref.length()) {
                    String chapterText = ref.substring(split + 1).replaceAll("[^0-9].*$", "");
                    if (!chapterText.isEmpty()) {
                        return bible(ref.substring(0, split), Integer.parseInt(chapterText));
                    }
                }
            }
            if (key.startsWith("mass:")) {
                String[] p = key.split(":");
                if (p.length >= 4) {
                    int year = Integer.parseInt(p[1]);
                    int month = Integer.parseInt(p[2]);
                    int day = Integer.parseInt(p[3]);
                    // La clave histórica de Lecturas usaba Calendar.MONTH (0–11).
                    if (month >= 0 && month <= 11) month++;
                    return String.format(Locale.US, "mass:%04d-%02d-%02d:readings",
                            year, month, day);
                }
            }
            if (key.startsWith("combined-mass:")) {
                String[] p = key.split(":");
                if (p.length >= 4) {
                    return "celebration:" + p[1] + ":mass+" + token(p[2])
                            + ":" + token(p[3]);
                }
            }
            if (key.startsWith("missal31:")) {
                String[] p = key.split(":");
                if (p.length >= 7) {
                    return String.format(Locale.US, "missal:%04d-%02d-%02d:%s:%s",
                            Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), token(p[4]), token(p[5]));
                }
            }
            if (key.startsWith("hours:compline:")) {
                return "liturgy:" + key.substring("hours:compline:".length()) + ":compline";
            }
        } catch (Exception ignored) {}
        return key;
    }

    public static String isoDate(Calendar date) {
        if (date == null) return "0000-00-00";
        return String.format(Locale.US, "%04d-%02d-%02d",
                date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
    }

    public static String token(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace("æ", "ae")
                .replace("œ", "oe")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
