package com.fabri.ministerium;

public final class SearchResult {
    public final DocumentInfo document;
    public final PrayerEntry prayer;
    public final RitualDocument ritualDocument;
    public final RitualEntry ritualEntry;
    public final HoursVolume hoursVolume;
    public final int hoursIndex;
    public final int dateYear;
    public final int dateMonth;
    public final int dateDay;
    public final int ritualIndex;
    public final int pageIndex;
    public final int canonNumber;
    public final String title;
    public final String snippet;
    public final String directFilePath;
    public final String findText;
    public final String sourceLabel;
    private final boolean magisterium;

    public SearchResult(DocumentInfo document, int pageIndex, String title, String snippet) {
        this(document, null, null, null, null, -1, -1, -1, -1, -1, pageIndex, -1,
                title, snippet, "", "", "", false);
    }

    public SearchResult(PrayerEntry prayer, String snippet) {
        this(null, prayer, null, null, null, -1, -1, -1, -1, -1, -1, -1,
                prayer.title, snippet, "", "", "", false);
    }

    public SearchResult(RitualDocument ritualDocument, RitualEntry ritualEntry,
                        int ritualIndex, String snippet) {
        this(null, null, ritualDocument, ritualEntry, null, -1, -1, -1, -1,
                ritualIndex, -1, -1, ritualEntry.title, snippet, "", "", "", false);
    }

    public SearchResult(HoursVolume volume, int hoursIndex, String title, String snippet) {
        this(null, null, null, null, volume, hoursIndex, -1, -1, -1, -1, -1, -1,
                title, snippet, "", "", "", false);
    }

    public SearchResult(HoursVolume volume, String directFilePath, String title,
                        String sourceLabel, String findText, String snippet) {
        this(null, null, null, null, volume, -1, -1, -1, -1, -1, -1, -1,
                title, snippet, directFilePath, findText, sourceLabel, true);
    }

    public SearchResult(LiturgicalDateHit hit) {
        this(null, null, null, null, null, -1, hit.year, hit.month, hit.day,
                -1, -1, -1, hit.title, hit.snippet, "", "", "", false);
    }

    private SearchResult(DocumentInfo document, PrayerEntry prayer,
                         RitualDocument ritualDocument, RitualEntry ritualEntry,
                         HoursVolume hoursVolume, int hoursIndex,
                         int dateYear, int dateMonth, int dateDay, int ritualIndex,
                         int pageIndex, int canonNumber, String title, String snippet,
                         String directFilePath, String findText, String sourceLabel,
                         boolean magisterium) {
        this.document = document;
        this.prayer = prayer;
        this.ritualDocument = ritualDocument;
        this.ritualEntry = ritualEntry;
        this.hoursVolume = hoursVolume;
        this.hoursIndex = hoursIndex;
        this.dateYear = dateYear;
        this.dateMonth = dateMonth;
        this.dateDay = dateDay;
        this.ritualIndex = ritualIndex;
        this.pageIndex = pageIndex;
        this.canonNumber = canonNumber;
        this.title = title == null ? "" : title;
        this.snippet = snippet == null ? "" : snippet;
        this.directFilePath = directFilePath == null ? "" : directFilePath;
        this.findText = findText == null ? "" : findText;
        this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
        this.magisterium = magisterium;
    }

    public static SearchResult canon(int canon, String snippet) {
        return new SearchResult(null, null, null, null, null, -1, -1, -1, -1,
                -1, -1, canon, "Canon " + canon, snippet, "", "",
                "Código de Derecho Canónico", false);
    }

    public boolean isPrayer() { return prayer != null; }
    public boolean isRitual() { return ritualDocument != null; }
    public boolean isHours() { return hoursVolume != null; }
    public boolean isMagisterium() { return magisterium; }
    public boolean isLiturgicalDate() { return dateYear >= 0; }
    public boolean isCanon() { return canonNumber > 0; }
}
