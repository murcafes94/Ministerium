package com.fabri.ministerium;

public final class LiturgicalEvent {
    public final String dateKey;
    public final String summary;
    public final String rank;
    public final String psalterWeek;
    public final String color;

    public LiturgicalEvent(String dateKey, String summary, String rank,
                           String psalterWeek, String color) {
        this.dateKey = dateKey;
        this.summary = summary;
        this.rank = rank;
        this.psalterWeek = psalterWeek;
        this.color = color;
    }

    public boolean isOptionalMemorial() {
        return "m".equals(rank) || "m*".equals(rank);
    }

    public boolean isMandatoryMemorial() {
        return "M".equals(rank);
    }

    public boolean isFeast() {
        return "F".equals(rank);
    }

    public boolean isSolemnity() {
        return "S".equals(rank);
    }

    public boolean requiresProperOffice() {
        return isMandatoryMemorial() || isFeast() || isSolemnity();
    }

    public String rankLabel() {
        if (isOptionalMemorial()) return "Memoria libre";
        if (isMandatoryMemorial()) return "Memoria obligatoria";
        if (isFeast()) return "Fiesta";
        if (isSolemnity()) return "Solemnidad";
        return "Celebración litúrgica";
    }
}
