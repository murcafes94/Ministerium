package org.ministerium.liturgy.semantic;

import org.ministerium.bible.semantic.BibleReference;

/** One ordered, renderable semantic unit of a Mass form. */
public final class MissalUnit {
    public enum Type {
        ENTRANCE_ANTIPHON,
        SIGN_OF_CROSS,
        GREETING,
        PENITENTIAL_ACT,
        KYRIE,
        GLORIA,
        COLLECT,
        FIRST_READING,
        RESPONSORIAL_PSALM,
        SECOND_READING,
        GOSPEL_ACCLAMATION,
        GOSPEL,
        CREED,
        UNIVERSAL_PRAYER,
        OFFERTORY,
        PRAYER_OVER_OFFERINGS,
        PREFACE,
        SANCTUS,
        EUCHARISTIC_PRAYER,
        MEMORIAL_ACCLAMATION,
        DOXOLOGY,
        LORDS_PRAYER,
        SIGN_OF_PEACE,
        AGNUS_DEI,
        COMMUNION_ANTIPHON,
        PRAYER_AFTER_COMMUNION,
        BLESSING,
        DISMISSAL,
        RUBRIC,
        OTHER
    }

    public enum Role {
        PRIEST, DEACON, READER, CANTOR, ASSEMBLY, ALL, RUBRIC
    }

    private final String unitId;
    private final Type type;
    private final int order;
    private final Role role;
    private final String language;
    private final String text;
    private final String rubric;
    private final BibleReference bibleReference;
    private final ChantRef chantRef;
    private final String responseTo;
    private final boolean optional;
    private final String conditions;

    public MissalUnit(String unitId, Type type, int order, Role role, String language, String text,
                      String rubric, BibleReference bibleReference, ChantRef chantRef,
                      String responseTo, boolean optional, String conditions) {
        if (unitId == null || unitId.trim().isEmpty()) {
            throw new IllegalArgumentException("unitId is required");
        }
        this.unitId = unitId.trim();
        this.type = type == null ? Type.OTHER : type;
        this.order = order;
        this.role = role == null ? Role.ALL : role;
        this.language = language;
        this.text = text;
        this.rubric = rubric;
        this.bibleReference = bibleReference;
        this.chantRef = chantRef;
        this.responseTo = responseTo;
        this.optional = optional;
        this.conditions = conditions;
    }

    public String getUnitId() { return unitId; }
    public Type getType() { return type; }
    public int getOrder() { return order; }
    public Role getRole() { return role; }
    public String getLanguage() { return language; }
    public String getText() { return text; }
    public String getRubric() { return rubric; }
    public BibleReference getBibleReference() { return bibleReference; }
    public ChantRef getChantRef() { return chantRef; }
    public String getResponseTo() { return responseTo; }
    public boolean isOptional() { return optional; }
    public String getConditions() { return conditions; }
}
