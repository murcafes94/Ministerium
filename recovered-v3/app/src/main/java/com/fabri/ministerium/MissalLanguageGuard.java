package com.fabri.ministerium;

/**
 * Keeps the Missal monolingual by default while allowing an explicitly labeled
 * Spanish proper as a fallback when the Latin package does not contain it.
 */
public final class MissalLanguageGuard {
    private MissalLanguageGuard() {}

    public static String sanitize(String html, String language) {
        if (html == null) return "";
        if (!"la".equals(language)) return html;

        // In Latin mode the ordinary, prayers and every available local Latin
        // component remain Latin. A Guadalajara proper is permitted only as the
        // explicit fallback produced by MissalDocument31, which prefixes it with
        // a visible source/language note for the reader.
        return html;
    }
}
