package com.fabri.ministerium.bible.semantic;

/** Metadata for one independently versioned Bible content package. */
public final class BibleEdition {
    private final String editionId;
    private final String name;
    private final String abbreviation;
    private final String language;
    private final String canon;
    private final String sourceFormat;
    private final String version;
    private final String copyrightNotice;
    private final String licenseId;
    private final String contentHash;
    private final String minAppVersion;

    public BibleEdition(String editionId, String name, String abbreviation, String language, String canon,
                        String sourceFormat, String version, String copyrightNotice, String licenseId,
                        String contentHash, String minAppVersion) {
        this.editionId = require(editionId, "editionId");
        this.name = require(name, "name");
        this.abbreviation = abbreviation;
        this.language = language;
        this.canon = canon;
        this.sourceFormat = sourceFormat;
        this.version = version;
        this.copyrightNotice = copyrightNotice;
        this.licenseId = licenseId;
        this.contentHash = contentHash;
        this.minAppVersion = minAppVersion;
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    public String getEditionId() { return editionId; }
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public String getLanguage() { return language; }
    public String getCanon() { return canon; }
    public String getSourceFormat() { return sourceFormat; }
    public String getVersion() { return version; }
    public String getCopyrightNotice() { return copyrightNotice; }
    public String getLicenseId() { return licenseId; }
    public String getContentHash() { return contentHash; }
    public String getMinAppVersion() { return minAppVersion; }
}
