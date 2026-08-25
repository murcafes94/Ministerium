package com.fabri.ministerium;

public final class ReaderContext {
    public final String source;
    public final String sourceKey;
    public final String title;
    public final String reference;
    public final String category;
    public final boolean omitRubricsInTts;
    public final boolean allowTts;

    public ReaderContext(String source, String sourceKey, String title,
                         String reference, String category,
                         boolean omitRubricsInTts) {
        this(source, sourceKey, title, reference, category, omitRubricsInTts, true);
    }

    public ReaderContext(String source, String sourceKey, String title,
                         String reference, String category,
                         boolean omitRubricsInTts, boolean allowTts) {
        this.source = value(source);
        this.sourceKey = value(sourceKey);
        this.title = value(title);
        this.reference = value(reference);
        this.category = value(category);
        this.omitRubricsInTts = omitRubricsInTts;
        this.allowTts = allowTts;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
