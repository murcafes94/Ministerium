package com.fabri.ministerium;

import android.content.Context;

/** Applies only manually verified Mercabá propers to gaps left by the primary Missal sources. */
public final class MercabaMissalFallback {
    private MercabaMissalFallback() {}

    public static String apply(Context context, String html, String section,
                               String language, String celebration) {
        if (html == null || html.isEmpty() || !"es".equals(language)) return html;
        // Never override an existing proper. Mercabá only fills an explicit pending gap.
        if (!html.contains("class=\"pending\"")) return html;
        String key = section == null ? "day" : section;
        if ("day".equals(key)) {
            String out = html;
            out = replacePendingInSemantic(context, out, celebration,
                    "mass:entrance", "entrance");
            out = replacePendingInSemantic(context, out, celebration,
                    "mass:collect", "collect");
            out = replacePendingInSemantic(context, out, celebration,
                    "mass:offerings", "offerings");
            out = replacePendingInSemantic(context, out, celebration,
                    "mass:communion", "communion_antiphon");
            out = replacePendingInSemantic(context, out, celebration,
                    "mass:post-communion", "post_communion");
            return out;
        }
        String part = partForSection(key);
        if (part == null) return html;
        MercabaMissalRepository.Match match = MercabaMissalRepository.verifiedProper(
                context, celebration, part);
        return match == null ? html : replaceFirstPending(html, MercabaMissalRepository.render(match));
    }

    private static String replacePendingInSemantic(Context context, String html,
                                                   String celebration, String semanticId,
                                                   String part) {
        MercabaMissalRepository.Match match = MercabaMissalRepository.verifiedProper(
                context, celebration, part);
        if (match == null) return html;
        String marker = "data-semantic-id=\"" + semanticId + "\"";
        int start = html.indexOf(marker);
        if (start < 0) return html;
        int nextSection = html.indexOf("data-semantic-id=\"", start + marker.length());
        int end = nextSection < 0 ? html.length() : nextSection;
        int pending = html.indexOf("<div class=\"pending\">", start);
        if (pending < 0 || pending >= end) return html;
        int close = html.indexOf("</div>", pending);
        if (close < 0 || close >= end) return html;
        close += "</div>".length();
        return html.substring(0, pending) + MercabaMissalRepository.render(match)
                + html.substring(close);
    }

    private static String replaceFirstPending(String html, String replacement) {
        int start = html.indexOf("<div class=\"pending\">");
        if (start < 0) return html;
        int end = html.indexOf("</div>", start);
        if (end < 0) return html;
        end += "</div>".length();
        return html.substring(0, start) + replacement + html.substring(end);
    }

    private static String partForSection(String section) {
        if ("collect".equals(section)) return "collect";
        if ("offerings".equals(section)) return "offerings";
        if ("communion_antiphon".equals(section)) return "communion_antiphon";
        if ("post_communion".equals(section)) return "post_communion";
        return null;
    }
}
