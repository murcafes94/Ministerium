package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;

/**
 * Single source-priority policy for liturgical content.
 *
 * Calendar: local Ecuador calendar/resolver is always authoritative.
 * Spanish Missal propers: local package -> verified Mercaba package -> Guadalajara cache.
 * Latin Missal propers: local Latin package -> explicitly labelled Spanish fallback.
 *
 * Repositories may expose additional material, but they must not silently override this policy.
 */
public final class LiturgicalSourceResolver {
    public static final String SOURCE_LOCAL_ECUADOR = "local-ecuador";
    public static final String SOURCE_LOCAL_LATIN = "local-latin";
    public static final String SOURCE_MERCABA_VERIFIED = "mercaba-verified-ecuador";
    public static final String SOURCE_GUADALAJARA = "arquidiocesis-gdl";

    public static final class TextResult {
        public final String text;
        public final String source;
        public final boolean fallback;

        TextResult(String text, String source, boolean fallback) {
            this.text = text == null ? "" : text;
            this.source = source == null ? "" : source;
            this.fallback = fallback;
        }

        public boolean isEmpty() {
            return text.trim().isEmpty();
        }
    }

    private LiturgicalSourceResolver() {}

    public static LiturgicalDay day(Context context, Calendar date) throws Exception {
        return LiturgicalResolver.resolve(context, date);
    }

    /**
     * Resolves only external/cached proper fallbacks. The caller should first try
     * the authoritative local Missal component for the requested celebration.
     */
    public static TextResult properFallback(Context context, Calendar date, LiturgicalDay day,
                                            String part, String language) {
        if ("es".equals(language) && day != null) {
            MercabaMissalRepository.Match match = MercabaMissalRepository.verifiedProper(
                    context, day.celebration, mercabaPart(part));
            if (match != null && !match.text.trim().isEmpty()) {
                return new TextResult(match.text, SOURCE_MERCABA_VERIFIED, true);
            }
        }

        DailyMassProperRepository.ProperDay daily = DailyMassProperRepository.cached(context, date);
        String text = dailyText(daily, part);
        if (!text.isEmpty()) {
            return new TextResult(text, SOURCE_GUADALAJARA, true);
        }
        return new TextResult("", "", false);
    }

    private static String mercabaPart(String part) {
        if (LiturgiaPapalMissalRepository.ENTRANCE.equals(part)) return "entrance";
        if (LiturgiaPapalMissalRepository.COLLECT.equals(part)) return "collect";
        if (LiturgiaPapalMissalRepository.OFFERINGS.equals(part)) return "offerings";
        if (LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON.equals(part)) return "communion_antiphon";
        if (LiturgiaPapalMissalRepository.POST_COMMUNION.equals(part)) return "post_communion";
        return part == null ? "" : part;
    }

    private static String dailyText(DailyMassProperRepository.ProperDay day, String part) {
        if (day == null) return "";
        if (LiturgiaPapalMissalRepository.ENTRANCE.equals(part)) return day.entrance;
        if (LiturgiaPapalMissalRepository.COLLECT.equals(part)) return day.collect;
        if (LiturgiaPapalMissalRepository.OFFERINGS.equals(part)) return day.offerings;
        if (LiturgiaPapalMissalRepository.COMMUNION_ANTIPHON.equals(part)) return day.communionAntiphon;
        if (LiturgiaPapalMissalRepository.POST_COMMUNION.equals(part)) return day.postCommunion;
        return "";
    }
}
