package org.processmining.est2miner.algorithms.placeevaluation;

public class LpoESTValidationResult {
    private String placeName;
    private boolean valid;
    private ValidationPhase phase;
    private boolean overfed;
    private boolean underfed;

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


    public ValidationPhase getPhase() {
        return phase;
    }

    public boolean isOverfed() {
        return overfed;
    }

    public boolean isUnderfed() {
        return underfed;
    }

    @Override
    public String toString() {
        return "LpoValidationResult" + "{" + (placeName != null ? placeName + ": " : "") + "valid=" + valid + ", " + "phase=" + phase + ", " + "overfed=" + overfed + ", " + "underfed=" + underfed + '}';
    }

    enum ValidationPhase {
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
