package com.fabri.ministerium;

public final class CatalogEntry {
    public final String title;
    public final String sourceTitle;
    public final String section;
    public final int pdfPageIndex;
    public final int printedPage;

    public CatalogEntry(String title, String section, int pdfPageIndex, int printedPage) {
        this(title, title, section, pdfPageIndex, printedPage);
    }

    public CatalogEntry(String title, String sourceTitle, String section,
                        int pdfPageIndex, int printedPage) {
        this.title = title;
        this.sourceTitle = sourceTitle;
        this.section = section;
        this.pdfPageIndex = pdfPageIndex;
        this.printedPage = printedPage;
    }

    public String pageLabel() {
        String base = "Página PDF " + (pdfPageIndex + 1);
        return printedPage > 0 ? base + " · Página impresa " + printedPage : base;
    }
}
