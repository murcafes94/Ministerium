package com.fabri.ministerium;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Documento inmutable compuesto por bloques reutilizables. */
public final class StructuredDocument {
    public final String id;
    public final String title;
    public final String source;
    public final String version;
    public final List<ContentBlock> blocks;

    public StructuredDocument(String id, String title, String source, String version,
                              List<ContentBlock> blocks) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.source = source == null ? "" : source;
        this.version = version == null ? "" : version;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
    }
}
