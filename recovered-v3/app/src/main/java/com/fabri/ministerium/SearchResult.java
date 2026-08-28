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
    public final String title;
    public final String snippet;
    public final String directFilePath;
    public final String findText;
    public final String sourceLabel;
    private final boolean magisterium;

    public SearchResult(DocumentInfo document, int pageIndex, String title, String snippet) {
        this.document = document;
        this.prayer = null;
        this.ritualDocument = null;
        this.ritualEntry = null;
        this.hoursVolume = null;
        this.hoursIndex = -1;
        this.dateYear = -1;
        this.dateMonth = -1;
        this.dateDay = -1;
        this.ritualIndex = -1;
        this.pageIndex = pageIndex;
        this.title = title;
        this.snippet = snippet;
        this.directFilePath = "";
        this.findText = "";
        this.sourceLabel = "";
        this.magisterium = false;
    }

    public SearchResult(PrayerEntry prayer, String snippet) {
        this.document = null;
        this.prayer = prayer;
        this.ritualDocument = null;
        this.ritualEntry = null;
        this.hoursVolume = null;
        this.hoursIndex = -1;
        this.dateYear = -1;
        this.dateMonth = -1;
        this.dateDay = -1;
        this.ritualIndex = -1;
        this.pageIndex = -1;
        this.title = prayer.title;
        this.snippet = snippet;
        this.directFilePath = "";
        this.findText = "";
        this.sourceLabel = "";
        this.magisterium = false;
    }

    public boolean isPrayer() {
        return prayer != null;
    }

    public SearchResult(RitualDocument ritualDocument, RitualEntry ritualEntry,
                        int ritualIndex, String snippet) {
        this.document = null;
        this.prayer = null;
        this.ritualDocument = ritualDocument;
        this.ritualEntry = ritualEntry;
        this.hoursVolume = null;
        this.hoursIndex = -1;
        this.dateYear = -1;
        this.dateMonth = -1;
        this.dateDay = -1;
        this.ritualIndex = ritualIndex;
        this.pageIndex = -1;
        this.title = ritualEntry.title;
        this.snippet = snippet;
        this.directFilePath = "";
        this.findText = "";
        this.sourceLabel = "";
        this.magisterium = false;
    }

    public boolean isRitual() {
        return ritualDocument != null;
    }

    public SearchResult(HoursVolume volume, int hoursIndex, String title, String snippet) {
        this.document = null;
        this.prayer = null;
        this.ritualDocument = null;
        this.ritualEntry = null;
        this.hoursVolume = volume;
        this.hoursIndex = hoursIndex;
        this.dateYear = -1;
        this.dateMonth = -1;
        this.dateDay = -1;
        this.ritualIndex = -1;
        this.pageIndex = -1;
        this.title = title;
        this.snippet = snippet;
        this.directFilePath = "";
        this.findText = "";
        this.sourceLabel = "";
        this.magisterium = false;
    }

    public SearchResult(HoursVolume volume, String directFilePath, String title,
                        String sourceLabel, String findText, String snippet) {
        this.document = null;
        this.prayer = null;
        this.ritualDocument = null;
        this.ritualEntry = null;
        this.hoursVolume = volume;
        this.hoursIndex = -1;
        this.dateYear = -1;
        this.dateMonth = -1;
        this.dateDay = -1;
        this.ritualIndex = -1;
        this.pageIndex = -1;
        this.title = title;
        this.snippet = snippet;
        this.directFilePath = directFilePath == null ? "" : directFilePath;
        this.findText = findText == null ? "" : findText;
        this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
        this.magisterium = true;
    }

    public SearchResult(LiturgicalDateHit hit) {
        this.document = null;
        this.prayer = null;
        this.ritualDocument = null;
        this.ritualEntry = null;
        this.hoursVolume = null;
        this.hoursIndex = -1;
        this.dateYear = hit.year;
        this.dateMonth = hit.month;
        this.dateDay = hit.day;
        this.ritualIndex = -1;
        this.pageIndex = -1;
        this.title = hit.title;
        this.snippet = hit.snippet;
        this.directFilePath = "";
        this.findText = "";
        this.sourceLabel = "";
        this.magisterium = false;
    }

    public boolean isHours() {
        return hoursVolume != null;
    }

    public boolean isMagisterium() {
        return magisterium;
    }

    public boolean isLiturgicalDate() {
        return dateYear >= 0;
    }
}
