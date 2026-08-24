package com.fabri.ministerium.bible.semantic;

import java.util.List;

/** Renders a semantic chapter into minimal HTML compatible with Ministerium reader tooling. */
public final class SemanticBibleRenderer {
    private SemanticBibleRenderer() {}

    public static String chapter(BibleEdition edition, BibleBook book, int chapter,
                                 List<BibleVerse> verses) {
        StringBuilder html = new StringBuilder(8192);
        html.append("<!doctype html><html lang=\"")
                .append(escapeAttribute(edition.getLanguage() == null ? "es" : edition.getLanguage()))
                .append("\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("</head><body data-ministerium-bible=\"semantic-v1\">")
                .append("<main id=\"ministerium-chapter\" data-edition-id=\"")
                .append(escapeAttribute(edition.getEditionId()))
                .append("\" data-book-id=\"").append(escapeAttribute(book.getBookId()))
                .append("\" data-chapter=\"").append(chapter).append("\">")
                .append("<h2 class=\"ministerium-chapter-title\">")
                .append(escape(book.getName())).append(" ").append(chapter).append("</h2>");

        boolean paragraphOpen = false;
        for (BibleVerse verse : verses) {
            if (verse.isHeading()) {
                if (paragraphOpen) { html.append("</p>"); paragraphOpen = false; }
                html.append("<h3 class=\"bible-heading\">").append(escape(verse.getText())).append("</h3>");
                continue;
            }
            if (!paragraphOpen || verse.isParagraphStart()) {
                if (paragraphOpen) html.append("</p>");
                html.append("<p class=\"bible-paragraph\">");
                paragraphOpen = true;
            } else {
                html.append(" ");
            }
            html.append("<span class=\"bible-verse\" data-verse-id=\"")
                    .append(escapeAttribute(verse.stableId())).append("\">")
                    .append("<sup id=\"").append(legacyVerseAnchor(chapter, verse.getVerseLabel()))
                    .append("\" data-verse-label=\"").append(escapeAttribute(verse.getVerseLabel()))
                    .append("\">").append(escape(verse.getVerseLabel())).append("</sup> ")
                    .append(escape(verse.getText())).append("</span>");
        }
        if (paragraphOpen) html.append("</p>");
        html.append("</main></body></html>");
        return html.toString();
    }

    private static String legacyVerseAnchor(int chapter, String verseLabel) {
        String safe = verseLabel == null ? "" : verseLabel.replaceAll("[^A-Za-z0-9_-]", "");
        return "v" + chapter + safe;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeAttribute(String value) {
        return escape(value);
    }
}
