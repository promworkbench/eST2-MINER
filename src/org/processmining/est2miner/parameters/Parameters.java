package org.processmining.est2miner.parameters;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.processmining.basicutils.parameters.impl.PluginParametersImpl;

public class Parameters extends PluginParametersImpl {
    private double threshold_tau;  // in[0,1], determmines fraction of traces in original log
    private double threshold_delta; // in [0,1], determmines fraction of traces in original log
    private boolean removeImps;
    private int max_depth;

    public Parameters(int max_depth, double threshold_tau_result, double threshold_delta_result, boolean remIP) {
        this.threshold_tau = threshold_tau_result;
        this.threshold_delta = threshold_delta_result;
        this.removeImps = remIP;
        this.max_depth = max_depth;
    }

    public Parameters() {
        this.threshold_tau = 0.75; //fraction of fitting traces for a place to be fitting
        this.threshold_delta = 1;
        this.removeImps = true;
        this.max_depth = 4;
    }

    public Parameters(double threshold_tau) {
        this.threshold_tau = threshold_tau; //fraction of fitting traces for a place to be fitting
        this.threshold_delta = 1;
        this.removeImps = true;
        this.max_depth = 5;
    }

    public double getThresholdTau() {
        return threshold_tau;
    }

    public double getThresholdDelta() {
        return threshold_delta;
    }

    public int getDeltaAdaptionSteepness() {
        return 5;
    }

    public XEventClassifier getClassifier() {
        return XLogInfoImpl.NAME_CLASSIFIER;
    }

    public int getMax_depth() {
        return max_depth;
    }

    public boolean isRemoveImps() {
        return removeImps;
    }

    @Override
    public String toString() {
        return "tau-" + threshold_tau +
                "-delta-" + threshold_delta +
                "-rImp-" + removeImps +
                "-dep-" + max_depth;
    }
}