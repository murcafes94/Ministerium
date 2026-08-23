package com.fabri.ministerium;

public final class EpubTocEntry {
    public final String title;
    public final String filePath;
    public final String fragment;
    public final int depth;

    public EpubTocEntry(String title, String filePath, String fragment, int depth) {
        this.title = title;
        this.filePath = filePath;
        this.fragment = fragment;
        this.depth = depth;
    }
}
