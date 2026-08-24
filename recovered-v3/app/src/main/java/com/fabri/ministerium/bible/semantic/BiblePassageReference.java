package com.fabri.ministerium.bible.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One printed citation may contain several verse ranges; preserve them as semantic segments. */
public final class BiblePassageReference {
    private final String originalCitation;
    private final List<BibleReference> segments;

    public BiblePassageReference(String originalCitation, List<BibleReference> segments) {
        this.originalCitation = originalCitation == null ? "" : originalCitation.trim();
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("At least one Bible reference segment is required");
        }
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public String getOriginalCitation() { return originalCitation; }
    public List<BibleReference> getSegments() { return segments; }

    public String stableKey() {
        StringBuilder value = new StringBuilder();
        for (BibleReference segment : segments) {
            if (value.length() > 0) value.append(";");
            value.append(segment.stableKey());
        }
        return value.toString();
    }
}
