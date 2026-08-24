package com.fabri.ministerium.liturgy.mass;

/** Optional chant/notation linked to a semantic Mass section. */
public final class ChantResource {
    private final String chantId;
    private final MassSectionType sectionType;
    private final String language;
    private final String notationFormat;
    private final String source;
    private final String license;
    private final String edition;
    private final String variant;
    private final String assetPath;

    public ChantResource(String chantId, MassSectionType sectionType, String language,
                         String notationFormat, String source, String license,
                         String edition, String variant, String assetPath) {
        this.chantId = chantId;
        this.sectionType = sectionType;
        this.language = language;
        this.notationFormat = notationFormat;
        this.source = source;
        this.license = license;
        this.edition = edition;
        this.variant = variant;
        this.assetPath = assetPath;
    }

    public String getChantId() { return chantId; }
    public MassSectionType getSectionType() { return sectionType; }
    public String getLanguage() { return language; }
    public String getNotationFormat() { return notationFormat; }
    public String getSource() { return source; }
    public String getLicense() { return license; }
    public String getEdition() { return edition; }
    public String getVariant() { return variant; }
    public String getAssetPath() { return assetPath; }
}
