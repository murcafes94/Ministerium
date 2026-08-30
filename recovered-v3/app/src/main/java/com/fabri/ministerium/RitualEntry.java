package com.fabri.ministerium;

public final class RitualEntry {
    public final String title;
    public final String sourceTitle;
    public final String category;
    public final int sourceOccurrence;

    public RitualEntry(String title, String sourceTitle, String category) {
        this(title, sourceTitle, category, 0);
    }

    public RitualEntry(String title, String sourceTitle, String category,
                       int sourceOccurrence) {
        this.title = title;
        this.sourceTitle = sourceTitle;
        this.category = category;
        this.sourceOccurrence = sourceOccurrence;
    }
}
