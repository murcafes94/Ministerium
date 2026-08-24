package com.fabri.ministerium;

import java.util.Calendar;

public final class RomanCalendarMathTest {
    public static void main(String[] args) {
        assertWeek(2026, Calendar.AUGUST, 24, 21, "24 Aug 2026 must be Ordinary Time week XXI");
        assertWeek(2026, Calendar.NOVEMBER, 22, 34, "Christ the King 2026 is week XXXIV");

        // 2023: transferred Epiphany = Sunday 8 Jan; Baptism = Monday 9 Jan.
        assertWeek(2023, Calendar.JANUARY, 9, 0, "Baptism Monday is not Ordinary Time");
        assertWeek(2023, Calendar.JANUARY, 10, 1, "Tuesday after Baptism belongs to week I");
        assertWeek(2023, Calendar.JANUARY, 15, 2, "First Ordinary Sunday is Sunday II");

        // 2024: transferred Epiphany = Sunday 7 Jan; Baptism = Monday 8 Jan.
        assertWeek(2024, Calendar.JANUARY, 8, 0, "Baptism Monday is not Ordinary Time");
        assertWeek(2024, Calendar.JANUARY, 9, 1, "Day after Baptism belongs to week I");

        Calendar baptism2023 = RomanCalendarMath.baptismOfTheLord(2023);
        assertDate(baptism2023, 2023, Calendar.JANUARY, 9, "2023 Baptism date");
        Calendar baptism2024 = RomanCalendarMath.baptismOfTheLord(2024);
        assertDate(baptism2024, 2024, Calendar.JANUARY, 8, "2024 Baptism date");

        System.out.println("RomanCalendarMath regression tests OK");
    }

    private static void assertWeek(int year, int month, int day, int expected, String label) {
        int actual = RomanCalendarMath.ordinaryWeekNumber(RomanCalendarMath.day(year, month, day));
        if (actual != expected) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertDate(Calendar actual, int year, int month, int day, String label) {
        if (actual.get(Calendar.YEAR) != year || actual.get(Calendar.MONTH) != month
                || actual.get(Calendar.DAY_OF_MONTH) != day) {
            throw new AssertionError(label + ": got " + actual.getTime());
        }
    }
}
