package com.fabri.ministerium;

import android.content.Context;

/**
 * Paleta semántica compartida por los lectores WebView.
 *
 * Evita que Biblia, Magisterio, Liturgia y documentos vuelvan a definir por
 * separado los mismos tonos. Los recursos XML continúan siendo la referencia
 * para vistas Android; esta clase ofrece los equivalentes CSS necesarios en
 * documentos HTML inyectados en WebView.
 */
public final class ReaderVisualPalette {
    public final String background;
    public final String ink;
    public final String muted;
    public final String accent;
    public final String divider;
    public final String panel;
    public final String highlight;
    public final String highlightInk;

    private ReaderVisualPalette(String background, String ink, String muted,
                                String accent, String divider, String panel,
                                String highlight, String highlightInk) {
        this.background = background;
        this.ink = ink;
        this.muted = muted;
        this.accent = accent;
        this.divider = divider;
        this.panel = panel;
        this.highlight = highlight;
        this.highlightInk = highlightInk;
    }

    public static ReaderVisualPalette from(Context context) {
        String mode = ThemeUtils.getMode(context);
        if (ThemeUtils.DARK.equals(mode) || ThemeUtils.isDark(context)) {
            return new ReaderVisualPalette(
                    "#26211E", "#F3EDE4", "#C8BDB0", "#D9B96F",
                    "#594D43", "#332C28", "#6B5728", "#FFF7DF");
        }
        if (ThemeUtils.SEPIA.equals(mode)) {
            return new ReaderVisualPalette(
                    "#F0E2C7", "#30261E", "#75604D", "#6E1D2A",
                    "#CDBB9C", "#E7D3B1", "#E2C66F", "#30261E");
        }
        return new ReaderVisualPalette(
                "#FFFDF7", "#2A2521", "#6F665E", "#772233",
                "#D9CDBE", "#F5EDDF", "#F6E58D", "#231F1B");
    }

    public String bodyPaletteCss() {
        return "background:" + background + "!important;color:" + ink + "!important;";
    }
}
