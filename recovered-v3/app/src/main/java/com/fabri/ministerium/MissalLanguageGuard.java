package com.fabri.ministerium;

import java.util.regex.Pattern;

/** Ensures a monolingual Missal never exposes liturgical text from the other language. */
public final class MissalLanguageGuard {
    private static final Pattern SPANISH_DAILY_PROPER = Pattern.compile(
            "(?is)<div class=\\\"proper-language-note\\\">.*?</div>\\s*"
                    + "<div class=\\\"daily-proper\\\"[^>]*data-missal-source=\\\"arquidiocesis-gdl\\\"[^>]*>.*?</div>");
    private static final Pattern ORPHAN_SPANISH_DAILY_PROPER = Pattern.compile(
            "(?is)<div class=\\\"daily-proper\\\"[^>]*data-missal-source=\\\"arquidiocesis-gdl\\\"[^>]*>.*?</div>");

    private MissalLanguageGuard() {}

    public static String sanitize(String html, String language) {
        if (html == null || !"la".equals(language)) return html == null ? "" : html;
        String replacement = "<p class=\"rubric ministerium-language-guard\">"
                + "Proprium huius celebrationis in fonte latino locali nondum adest.</p>";
        String clean = SPANISH_DAILY_PROPER.matcher(html).replaceAll(replacement);
        return ORPHAN_SPANISH_DAILY_PROPER.matcher(clean).replaceAll(replacement);
    }
}
