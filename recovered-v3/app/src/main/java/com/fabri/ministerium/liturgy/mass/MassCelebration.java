package com.fabri.ministerium.liturgy.mass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete Mass composition addressed by a stable celebration ID. */
public final class MassCelebration {
    private final String celebrationId;
    private final String title;
    private final String rank;
    private final String liturgicalColor;
    private final String sourceEdition;
    private final List<MassSection> sections;

    public MassCelebration(String celebrationId, String title, String rank,
                           String liturgicalColor, String sourceEdition,
                           List<MassSection> sections) {
        if (celebrationId == null || celebrationId.trim().isEmpty()) {
            throw new IllegalArgumentException("celebrationId is required");
        }
        this.celebrationId = celebrationId.trim();
        this.title = title == null ? "" : title;
        this.rank = rank == null ? "" : rank;
        this.liturgicalColor = liturgicalColor == null ? "" : liturgicalColor;
        this.sourceEdition = sourceEdition == null ? "" : sourceEdition;
        this.sections = sections == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sections));
    }

    public String getCelebrationId() { return celebrationId; }
    public String getTitle() { return title; }
    public String getRank() { return rank; }
    public String getLiturgicalColor() { return liturgicalColor; }
    public String getSourceEdition() { return sourceEdition; }
    public List<MassSection> getSections() { return sections; }

    public MassSection first(MassSectionType type) {
        for (MassSection section : sections) {
            if (section.getType() == type) return section;
        }
        return null;
    }
}
