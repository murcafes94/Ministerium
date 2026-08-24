package org.ministerium.bible.semantic;

/** Optional original-language token for Greek/Hebrew/Aramaic study layers. */
public final class BibleToken {
    private final String tokenId;
    private final String verseId;
    private final int position;
    private final String surface;
    private final String language;
    private final String lemma;
    private final String strongId;
    private final String morphology;
    private final String sourceDataset;

    public BibleToken(String tokenId, String verseId, int position, String surface, String language,
                      String lemma, String strongId, String morphology, String sourceDataset) {
        this.tokenId = tokenId;
        this.verseId = verseId;
        this.position = position;
        this.surface = surface;
        this.language = language;
        this.lemma = lemma;
        this.strongId = strongId;
        this.morphology = morphology;
        this.sourceDataset = sourceDataset;
    }

    public String getTokenId() { return tokenId; }
    public String getVerseId() { return verseId; }
    public int getPosition() { return position; }
    public String getSurface() { return surface; }
    public String getLanguage() { return language; }
    public String getLemma() { return lemma; }
    public String getStrongId() { return strongId; }
    public String getMorphology() { return morphology; }
    public String getSourceDataset() { return sourceDataset; }
}
