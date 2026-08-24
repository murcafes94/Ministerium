package org.ministerium.bible.semantic;

public final class BibleFootnote {
    public enum Type { TEXTUAL, EDITORIAL, CROSS_REFERENCE, STUDY }

    private final String footnoteId;
    private final String verseId;
    private final String marker;
    private final String text;
    private final Type type;

    public BibleFootnote(String footnoteId, String verseId, String marker, String text, Type type) {
        this.footnoteId = footnoteId;
        this.verseId = verseId;
        this.marker = marker;
        this.text = text;
        this.type = type == null ? Type.EDITORIAL : type;
    }

    public String getFootnoteId() { return footnoteId; }
    public String getVerseId() { return verseId; }
    public String getMarker() { return marker; }
    public String getText() { return text; }
    public Type getType() { return type; }
}
