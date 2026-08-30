package com.fabri.ministerium;

import java.util.Locale;

/**
 * Identidad interna de una celebración y del formulario que debe resolver.
 *
 * Sigue el principio celebrationId → missalFormId → unidades semánticas. Los
 * identificadores externos CLEDR/CLBDR se dejan vacíos mientras no exista una
 * correspondencia verificada; Ministerium nunca los inventa.
 */
public final class LiturgicalIdentity {
    public final String celebrationId;
    public final String missalFormId;
    public final String cledrId;
    public final String clbdrId;

    private LiturgicalIdentity(String celebrationId, String missalFormId,
                               String cledrId, String clbdrId) {
        this.celebrationId = celebrationId;
        this.missalFormId = missalFormId;
        this.cledrId = cledrId;
        this.clbdrId = clbdrId;
    }

    public static LiturgicalIdentity internal(int year, int month, int day,
                                              String celebration, HoursLink temporalOffice) {
        String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
        String celebrationToken = ContentReference.token(celebration);
        if (celebrationToken.isEmpty()) celebrationToken = "feria";
        String formToken = celebrationToken;
        if (temporalOffice != null) {
            String volume = temporalOffice.volume == null ? "" : temporalOffice.volume.id;
            String title = ContentReference.token(temporalOffice.title);
            String temporal = ContentReference.token(volume);
            if (!temporal.isEmpty() || !title.isEmpty()) {
                formToken = (temporal.isEmpty() ? "temporal" : temporal)
                        + (title.isEmpty() ? "" : ":" + title);
            }
        }
        return new LiturgicalIdentity(
                "celebration:" + date + ":" + celebrationToken,
                "missal-form:" + formToken,
                "",
                "");
    }
}
