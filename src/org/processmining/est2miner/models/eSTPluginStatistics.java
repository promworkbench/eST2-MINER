package org.processmining.est2miner.models;

public class eSTPluginStatistics {
    private final double thresholdTau;
    private final double thresholdDelta;
    private final int maxDepth;
    private final int deltaAdaptationSteepness;
    private double logProcessTime;
    private double discoveryTime;
    private double postprocessingTime;
    private String pluginStatisticsString;

    public eSTPluginStatistics(double thresholdTau, double thresholdDelta, int maxDepth, int deltaAdaptionSteepness) {
        this.thresholdTau = thresholdTau;
        this.thresholdDelta = thresholdDelta;
        this.maxDepth = maxDepth;
        this.deltaAdaptationSteepness = deltaAdaptionSteepness;
    }

    public void setLogProcessTime(long startTime, long endTime) {
        this.logProcessTime = (endTime - startTime) / 1000000.0;
    }

    public void setDiscoveryTime(long startTime, long endTime) {
        this.discoveryTime = (endTime - startTime) / 1000000.0;
    }

    public void setPostprocessingTime(long startTime, long endTime) {
        this.postprocessingTime = (endTime - startTime) / 1000000.0;
    }

    public void setPluginStatisticsString(String pluginStatisticsString) {
        this.pluginStatisticsString = pluginStatisticsString;
    }

    @Override
    public String toString() {
        return pluginStatisticsString + "\n\n" + "eSTPluginStatistics{" + "\n\tthresholdTau = " + thresholdTau + ",\n\tthresholdDelta = " + thresholdDelta + ",\n\tmaxDepth = " + maxDepth + ",\n\tdeltaAdaptationSteepness = " + deltaAdaptationSteepness + ",\n\tlogProcessTime = " + logProcessTime + " [ms]" + ",\n\tdiscoveryTime = " + discoveryTime + " [ms]" + ",\n\tpostprocessingTime = " + postprocessingTime + " [ms]" + "\n}";
    }
}
