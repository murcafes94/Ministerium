package com.fabri.ministerium;

import java.util.List;

public final class RitualDocument {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String sourceName;
    public final String assetPath;
    public final List<RitualEntry> entries;

    public RitualDocument(String id, String title, String subtitle, String sourceName,
                          String assetPath, List<RitualEntry> entries) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.sourceName = sourceName;
        this.assetPath = assetPath;
        this.entries = entries;
    }
}
