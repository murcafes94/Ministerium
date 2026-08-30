package com.fabri.ministerium;

public final class LiturgicalDateHit {
    public final int year;
    public final int month;
    public final int day;
    public final String title;
    public final String snippet;

    public LiturgicalDateHit(int year, int month, int day, String title, String snippet) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.title = title;
        this.snippet = snippet;
    }
}
