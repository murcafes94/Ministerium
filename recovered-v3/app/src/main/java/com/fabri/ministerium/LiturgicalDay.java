package com.fabri.ministerium;

import java.util.Collections;
import java.util.List;

public final class LiturgicalDay {
    public final int year;
    public final int month;
    public final int day;
    public final String dateLabel;
    public final String celebration;
    public final String sourceNote;
    public final String psalterWeek;
    public final String liturgicalColor;
    public final HoursLink temporalOffice;
    public final List<HoursLink> saintOffices;

    public LiturgicalDay(int year, int month, int day, String dateLabel,
                         String celebration, String sourceNote,
                         String psalterWeek, String liturgicalColor,
                         HoursLink temporalOffice, List<HoursLink> saintOffices) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.dateLabel = dateLabel;
        this.celebration = celebration;
        this.sourceNote = sourceNote;
        this.psalterWeek = psalterWeek;
        this.liturgicalColor = liturgicalColor;
        this.temporalOffice = temporalOffice;
        this.saintOffices = Collections.unmodifiableList(saintOffices);
    }
}
