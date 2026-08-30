package com.fabri.ministerium;

/** Contexto común del lector y de sus anotaciones. */
public final class ReaderContext {
    public final String source;
    /** Clave histórica usada para restaurar datos existentes. */
    public final String sourceKey;
    /** Identificador canónico estable para relaciones y exportación. */
    public final String contentId;
    public final String title;
    public final String reference;
    public final String category;
    public final boolean omitRubricsInTts;
    public final boolean allowTts;

    public ReaderContext(String source, String sourceKey, String title,
                         String reference, String category,
                         boolean omitRubricsInTts) {
        this(source, sourceKey, "", title, reference, category,
                omitRubricsInTts, true);
    }

    public ReaderContext(String source, String sourceKey, String title,
                         String reference, String category,
                         boolean omitRubricsInTts, boolean allowTts) {
        this(source, sourceKey, "", title, reference, category,
                omitRubricsInTts, allowTts);
    }

    public ReaderContext(String source, String sourceKey, String contentId,
                         String title, String reference, String category,
                         boolean omitRubricsInTts, boolean allowTts) {
        this.source = value(source);
        this.sourceKey = value(sourceKey);
        this.title = value(title);
        this.reference = value(reference);
        String explicit = value(contentId);
        this.contentId = explicit.isEmpty()
                ? ContentReference.infer(this.sourceKey, this.reference) : explicit;
        this.category = value(category);
        this.omitRubricsInTts = omitRubricsInTts;
        this.allowTts = allowTts;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
