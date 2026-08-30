package com.fabri.ministerium;

public final class HourEntry {
    public final String key;
    public final String title;
    public final String subtitle;
    public final HoursVolume volume;
    public final String filePath;
    public final String fragment;
    public final String scrollText;
    public final boolean showIntentions;

    public HourEntry(String key, String title, String subtitle, HoursVolume volume,
                     String filePath, String fragment, String scrollText,
                     boolean showIntentions) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.volume = volume;
        this.filePath = filePath;
        this.fragment = fragment == null ? "" : fragment;
        this.scrollText = scrollText == null ? "" : scrollText;
        this.showIntentions = showIntentions;
    }
}
