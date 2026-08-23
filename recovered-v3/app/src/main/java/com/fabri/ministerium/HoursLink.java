package com.fabri.ministerium;

public final class HoursLink {
    public final HoursVolume volume;
    public final int tocIndex;
    public final String title;
    public final String subtitle;
    public final String liturgicalColor;
    public final String liturgicalRank;

    public HoursLink(HoursVolume volume, int tocIndex, String title, String subtitle) {
        this(volume, tocIndex, title, subtitle, "", "");
    }

    public HoursLink(HoursVolume volume, int tocIndex, String title, String subtitle,
                     String liturgicalColor) {
        this(volume, tocIndex, title, subtitle, liturgicalColor, "");
    }

    public HoursLink(HoursVolume volume, int tocIndex, String title, String subtitle,
                     String liturgicalColor, String liturgicalRank) {
        this.volume = volume;
        this.tocIndex = tocIndex;
        this.title = title;
        this.subtitle = subtitle;
        this.liturgicalColor = liturgicalColor;
        this.liturgicalRank = liturgicalRank;
    }

    public boolean isOptionalMemorial() {
        return "m".equals(liturgicalRank) || "m*".equals(liturgicalRank);
    }

    public boolean isMandatoryMemorial() {
        return "M".equals(liturgicalRank);
    }

    public boolean isFeastOrSolemnity() {
        return "F".equals(liturgicalRank) || "S".equals(liturgicalRank);
    }

    public boolean requiresProperOffice() {
        return isMandatoryMemorial() || isFeastOrSolemnity();
    }
}
