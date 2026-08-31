package com.fabri.ministerium;

import java.util.Calendar;

/** Pure calendar arithmetic used by the Roman temporal cycle. */
public final class RomanCalendarMath {
    private RomanCalendarMath() {}

    public static int ordinaryWeekNumber(Calendar selected) {
        Calendar date = normalized(selected);
        int year = date.get(Calendar.YEAR);
        Calendar easter = easterSunday(year);
        Calendar ashWednesday = addDays(easter, -46);
        Calendar pentecost = addDays(easter, 49);
        Calendar advent = adventStart(year);
        Calendar baptism = baptismOfTheLord(year);

        // The weekday span immediately after the Baptism is week I. The first
        // Sunday of Ordinary Time is therefore Sunday II.
        if (date.after(baptism) && date.before(ashWednesday)) {
            Calendar firstOrdinaryDay = addDays(baptism, 1);
            int week = daysBetween(firstOrdinaryDay, date) / 7 + 1;
            if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) week++;
            return clampOrdinaryWeek(week);
        }

        // After Pentecost the temporal resumes at the week that leads to
        // Sunday XXXIV (Christ the King), immediately before Advent.
        if (date.after(pentecost) && date.before(advent)) {
            Calendar christKing = addDays(advent, -7);
            int remaining = daysBetween(date, christKing);
            int week = 34 - ((Math.max(0, remaining) + 6) / 7);
            return clampOrdinaryWeek(week);
        }
        return 0;
    }

    public static Calendar baptismOfTheLord(int year) {
        Calendar epiphany = epiphanySunday(year);
        int day = epiphany.get(Calendar.DAY_OF_MONTH);
        // In places where Epiphany is transferred to Sunday, when that Sunday
        // is 7 or 8 January the Baptism is observed on the following Monday.
        if (day == 7 || day == 8) return addDays(epiphany, 1);
        return addDays(epiphany, 7);
    }

    public static Calendar epiphanySunday(int year) {
        Calendar date = day(year, Calendar.JANUARY, 2);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) date.add(Calendar.DATE, 1);
        return date;
    }

    public static Calendar adventStart(int year) {
        Calendar date = day(year, Calendar.DECEMBER, 3);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) date.add(Calendar.DATE, -1);
        return date;
    }

    public static Calendar easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return day(year, month - 1, day);
    }

    public static Calendar day(int year, int month, int day) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month, day, 12, 0, 0);
        return date;
    }

    private static Calendar normalized(Calendar source) {
        return day(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar addDays(Calendar source, int amount) {
        Calendar result = (Calendar) source.clone();
        result.add(Calendar.DATE, amount);
        return result;
    }

    private static int daysBetween(Calendar start, Calendar end) {
        long difference = normalized(end).getTimeInMillis() - normalized(start).getTimeInMillis();
        return (int) Math.round(difference / 86400000d);
    }

    private static int clampOrdinaryWeek(int week) {
        return Math.max(1, Math.min(34, week));
    }
}
