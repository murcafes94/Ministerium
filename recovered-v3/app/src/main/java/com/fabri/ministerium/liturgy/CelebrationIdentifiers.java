package com.fabri.ministerium.liturgy;

/**
 * Ministerium remains authoritative for its own IDs while keeping optional
 * interoperability keys for external Catholic calendar ecosystems.
 */
public final class CelebrationIdentifiers {
    private final String ministeriumId;
    private final String cledrId;
    private final String clbdrId;
    private final String litcalKey;
    private final String romcalKey;
    private final String eprexKey;

    public CelebrationIdentifiers(String ministeriumId, String cledrId, String clbdrId,
                                  String litcalKey, String romcalKey, String eprexKey) {
        if (ministeriumId == null || ministeriumId.trim().isEmpty()) {
            throw new IllegalArgumentException("ministeriumId is required");
        }
        this.ministeriumId = ministeriumId.trim();
        this.cledrId = normalize(cledrId);
        this.clbdrId = normalize(clbdrId);
        this.litcalKey = normalize(litcalKey);
        this.romcalKey = normalize(romcalKey);
        this.eprexKey = normalize(eprexKey);
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public String getMinisteriumId() { return ministeriumId; }
    public String getCledrId() { return cledrId; }
    public String getClbdrId() { return clbdrId; }
    public String getLitcalKey() { return litcalKey; }
    public String getRomcalKey() { return romcalKey; }
    public String getEprexKey() { return eprexKey; }
}
