package com.fabri.ministerium;

import java.util.Calendar;

/** Immutable shared snapshot for one civil/liturgical date. */
public final class LiturgicalDayPackage {
    public final Calendar date;
    public final LiturgicalDay day;
    public final boolean readingsAvailable;
    public final DailyMassProperRepository.ProperDay dailyProper;
    public final long preparedAt;

    LiturgicalDayPackage(Calendar date, LiturgicalDay day, boolean readingsAvailable,
                         DailyMassProperRepository.ProperDay dailyProper, long preparedAt) {
        this.date = (Calendar) date.clone();
        this.day = day;
        this.readingsAvailable = readingsAvailable;
        this.dailyProper = dailyProper;
        this.preparedAt = preparedAt;
    }

    public String cacheKey() {
        return date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH) + 1)
                + "-" + date.get(Calendar.DAY_OF_MONTH);
    }
}
