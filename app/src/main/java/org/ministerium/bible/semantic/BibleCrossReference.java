package org.ministerium.bible.semantic;

public final class BibleCrossReference {
    private final String referenceId;
    private final String sourceVerseId;
    private final String targetBookId;
    private final int targetChapter;
    private final String targetVerseStart;
    private final String targetVerseEnd;

    public BibleCrossReference(String referenceId, String sourceVerseId, String targetBookId,
                               int targetChapter, String targetVerseStart, String targetVerseEnd) {
        this.referenceId = referenceId;
        this.sourceVerseId = sourceVerseId;
        this.targetBookId = targetBookId;
        this.targetChapter = targetChapter;
        this.targetVerseStart = targetVerseStart;
        this.targetVerseEnd = targetVerseEnd;
    }

    public String getReferenceId() { return referenceId; }
    public String getSourceVerseId() { return sourceVerseId; }
    public String getTargetBookId() { return targetBookId; }
    public int getTargetChapter() { return targetChapter; }
    public String getTargetVerseStart() { return targetVerseStart; }
    public String getTargetVerseEnd() { return targetVerseEnd; }
}
