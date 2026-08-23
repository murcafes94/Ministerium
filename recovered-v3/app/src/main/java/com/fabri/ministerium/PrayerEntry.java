package com.fabri.ministerium;

public final class PrayerEntry {
    public final String id;
    public final String title;
    public final String category;
    public final String description;
    public final String assetPath;

    public PrayerEntry(String id, String title, String category,
                       String description, String assetPath) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.assetPath = assetPath;
    }
}
