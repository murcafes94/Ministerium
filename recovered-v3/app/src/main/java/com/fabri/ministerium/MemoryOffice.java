package com.fabri.ministerium;

public final class MemoryOffice {
    public final String html;
    public final String baseUrl;

    public MemoryOffice(String html, String baseUrl) {
        this.baseUrl = baseUrl;
        this.html = PsalmodyInlineResolver.resolve(html, baseUrl);
    }
}
