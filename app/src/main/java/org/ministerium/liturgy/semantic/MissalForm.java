package org.ministerium.liturgy.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Versioned form resolved from one stable celebration ID. */
public final class MissalForm {
    public enum FormType {
        PROPER, COMMON, RITUAL, VOTIVE, VARIOUS_NEEDS, FUNERAL
    }

    private final String formId;
    private final CelebrationIdentifiers celebration;
    private final String editionId;
    private final String language;
    private final FormType formType;
    private final String variant;
    private final List<MissalUnit> units;
    private final String source;
    private final String version;
    private final String contentHash;

    public MissalForm(String formId, CelebrationIdentifiers celebration, String editionId,
                      String language, FormType formType, String variant, List<MissalUnit> units,
                      String source, String version, String contentHash) {
        if (formId == null || formId.trim().isEmpty()) {
            throw new IllegalArgumentException("formId is required");
        }
        if (celebration == null) {
            throw new IllegalArgumentException("celebration is required");
        }
        this.formId = formId.trim();
        this.celebration = celebration;
        this.editionId = editionId;
        this.language = language;
        this.formType = formType == null ? FormType.PROPER : formType;
        this.variant = variant;
        ArrayList<MissalUnit> ordered = new ArrayList<>();
        if (units != null) ordered.addAll(units);
        Collections.sort(ordered, Comparator.comparingInt(MissalUnit::getOrder));
        this.units = Collections.unmodifiableList(ordered);
        this.source = source;
        this.version = version;
        this.contentHash = contentHash;
    }

    public String getFormId() { return formId; }
    public CelebrationIdentifiers getCelebration() { return celebration; }
    public String getEditionId() { return editionId; }
    public String getLanguage() { return language; }
    public FormType getFormType() { return formType; }
    public String getVariant() { return variant; }
    public List<MissalUnit> getUnits() { return units; }
    public String getSource() { return source; }
    public String getVersion() { return version; }
    public String getContentHash() { return contentHash; }
}
