package com.fabri.ministerium.liturgy.mass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One ordered liturgical unit of a Mass celebration. */
public final class MassSection {
    private final String sectionId;
    private final MassSectionType type;
    private final String label;
    private final String body;
    private final String language;
    private final String source;
    private final boolean optional;
    private final List<ChantResource> chants;

    public MassSection(String sectionId, MassSectionType type, String label, String body,
                       String language, String source, boolean optional,
                       List<ChantResource> chants) {
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sectionId is required");
        }
        if (type == null) throw new IllegalArgumentException("type is required");
        this.sectionId = sectionId.trim();
        this.type = type;
        this.label = label == null ? "" : label;
        this.body = body == null ? "" : body;
        this.language = language == null ? "" : language;
        this.source = source == null ? "" : source;
        this.optional = optional;
        this.chants = chants == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(chants));
    }

    public String getSectionId() { return sectionId; }
    public MassSectionType getType() { return type; }
    public String getLabel() { return label; }
    public String getBody() { return body; }
    public String getLanguage() { return language; }
    public String getSource() { return source; }
    public boolean isOptional() { return optional; }
    public List<ChantResource> getChants() { return chants; }
}
