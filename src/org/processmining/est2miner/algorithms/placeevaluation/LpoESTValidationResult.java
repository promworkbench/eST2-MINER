package org.processmining.est2miner.algorithms.placeevaluation;

public class LpoESTValidationResult {
    private final String placeName;
    private final ValidationPhase phase;
    private final boolean overfed;
    private final boolean underfed;

    public LpoESTValidationResult(String placeName, boolean valid, ValidationPhase phase) {
        this.placeName = placeName;
        this.phase = phase;
        overfed = false;
        underfed = valid;
    }

    public LpoESTValidationResult(String placeName, ValidationPhase phase, boolean overfed, boolean underfed) {
        this.placeName = placeName;
        this.phase = phase;
        this.overfed = overfed;
        this.underfed = underfed;
    }

    public boolean isOverfed() {
        return overfed;
    }

    public boolean isUnderfed() {
        return underfed;
    }

    @Override
    public String toString() {
        return "LpoValidationResult" + "{" + (placeName != null ? placeName + ": " : "") + "phase=" + phase + ", " + "overfed=" + overfed + ", " + "underfed=" + underfed + '}';
    }

    public enum ValidationPhase {
        FLOW, FORWARDS, BACKWARDS;

        public String toString() {
            switch (this) {
                case FLOW:
                    return "flow";
                case FORWARDS:
                    return "forwards";
                case BACKWARDS:
                    return "backwards";
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }
}
