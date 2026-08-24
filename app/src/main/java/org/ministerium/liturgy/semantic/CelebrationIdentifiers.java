package org.ministerium.liturgy.semantic;

/** Stable Ministerium celebration identity with optional interoperability keys. */
public final class CelebrationIdentifiers {
    private final String ministeriumId;
    private final String cledrId;
    private final String litcalKey;
    private final String romcalKey;
    private final String eprexKey;

    public CelebrationIdentifiers(String ministeriumId, String cledrId, String litcalKey,
                                  String romcalKey, String eprexKey) {
        if (ministeriumId == null || ministeriumId.trim().isEmpty()) {
            throw new IllegalArgumentException("ministeriumId is required");
        }
        this.ministeriumId = ministeriumId.trim();
        this.cledrId = clean(cledrId);
        this.litcalKey = clean(litcalKey);
        this.romcalKey = clean(romcalKey);
        this.eprexKey = clean(eprexKey);
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public String getMinisteriumId() { return ministeriumId; }
    public String getCledrId() { return cledrId; }
    public String getLitcalKey() { return litcalKey; }
    public String getRomcalKey() { return romcalKey; }
    public String getEprexKey() { return eprexKey; }
}
