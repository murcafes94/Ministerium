package org.ministerium.liturgy.semantic;

/** Optional reference to chant notation/audio kept separate from liturgical text. */
public final class ChantRef {
    private final String chantId;
    private final String notationFormat;
    private final String language;
    private final String melodyId;
    private final String source;
    private final String licenseId;
    private final String contentHash;

    public ChantRef(String chantId, String notationFormat, String language, String melodyId,
                    String source, String licenseId, String contentHash) {
        this.chantId = chantId;
        this.notationFormat = notationFormat;
        this.language = language;
        this.melodyId = melodyId;
        this.source = source;
        this.licenseId = licenseId;
        this.contentHash = contentHash;
    }

    public String getChantId() { return chantId; }
    public String getNotationFormat() { return notationFormat; }
    public String getLanguage() { return language; }
    public String getMelodyId() { return melodyId; }
    public String getSource() { return source; }
    public String getLicenseId() { return licenseId; }
    public String getContentHash() { return contentHash; }
}
