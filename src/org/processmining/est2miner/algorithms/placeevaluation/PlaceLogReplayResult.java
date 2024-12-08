package org.processmining.est2miner.algorithms.placeevaluation;

public class PlaceLogReplayResult {
    int numbOfTraces;
    double fittingFraction;
    double underfedFraction;
    double overfedFraction;

    public PlaceLogReplayResult(int overfedCount, int underfedCount, int overAndUnderfedCount, int fittingCount) {
        numbOfTraces = overfedCount + underfedCount + overAndUnderfedCount + fittingCount;
        fittingFraction = (double) fittingCount / numbOfTraces;
        underfedFraction = (double) (underfedCount + overAndUnderfedCount) / numbOfTraces;
        overfedFraction = (double) (overfedCount + overAndUnderfedCount) / numbOfTraces;
    }

    public int getNumbOfTraces() {
        return numbOfTraces;
    }

    public double getFittingFraction() {
        return fittingFraction;
    }

    public double getUnderfedFraction() {
        return underfedFraction;
    }

    public double getOverfedFraction() {
        return overfedFraction;
    }
}
