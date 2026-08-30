package com.fabri.ministerium;

public final class CommonOfficeChoice {
    public final String title;
    public final String filePath;
    public final String fragment;

    public CommonOfficeChoice(String title, String filePath, String fragment) {
        this.title = title;
        this.filePath = filePath;
        this.fragment = fragment == null ? "" : fragment;
    }
}
