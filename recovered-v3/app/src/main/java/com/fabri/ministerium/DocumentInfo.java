package com.fabri.ministerium;

public final class DocumentInfo {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String textAsset;
    public final String pdfAsset;
    public final int pageCount;

    public DocumentInfo(String id, String title, String subtitle,
                        String textAsset, String pdfAsset, int pageCount) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.textAsset = textAsset;
        this.pdfAsset = pdfAsset;
        this.pageCount = pageCount;
    }
}
