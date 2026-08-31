package org.ministerium.liturgy.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ordered plan for a single continuous celebration of Lauds/Vespers with Mass.
 *
 * It intentionally models the celebration as one sequence rather than a menu
 * that opens several independent reader activities.
 */
public final class CombinedCelebrationPlan {
    public enum Source {
        HOURS, MASS, TRANSITION
    }

    public static final class Step {
        private final String stepId;
        private final int order;
        private final Source source;
        private final String semanticUnitId;
        private final String label;
        private final boolean omitted;
        private final String ruleReference;

        public Step(String stepId, int order, Source source, String semanticUnitId,
                    String label, boolean omitted, String ruleReference) {
            if (stepId == null || stepId.trim().isEmpty()) {
                throw new IllegalArgumentException("stepId is required");
            }
            this.stepId = stepId.trim();
            this.order = order;
            this.source = source == null ? Source.TRANSITION : source;
            this.semanticUnitId = semanticUnitId;
            this.label = label;
            this.omitted = omitted;
            this.ruleReference = ruleReference;
        }

        public String getStepId() { return stepId; }
        public int getOrder() { return order; }
        public Source getSource() { return source; }
        public String getSemanticUnitId() { return semanticUnitId; }
        public String getLabel() { return label; }
        public boolean isOmitted() { return omitted; }
        public String getRuleReference() { return ruleReference; }
    }

    private final String planId;
    private final String celebrationId;
    private final String hourKey;
    private final String languageMode;
    private final List<Step> steps;

    public CombinedCelebrationPlan(String planId, String celebrationId, String hourKey,
                                   String languageMode, List<Step> steps) {
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("planId is required");
        }
        this.planId = planId.trim();
        this.celebrationId = celebrationId;
        this.hourKey = hourKey;
        this.languageMode = languageMode;
        ArrayList<Step> ordered = new ArrayList<>();
        if (steps != null) ordered.addAll(steps);
        Collections.sort(ordered, Comparator.comparingInt(Step::getOrder));
        this.steps = Collections.unmodifiableList(ordered);
    }

    public String getPlanId() { return planId; }
    public String getCelebrationId() { return celebrationId; }
    public String getHourKey() { return hourKey; }
    public String getLanguageMode() { return languageMode; }
    public List<Step> getSteps() { return steps; }
}
