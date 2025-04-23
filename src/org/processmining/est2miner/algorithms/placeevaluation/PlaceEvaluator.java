package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.*;
import org.processmining.est2miner.models.PlugInStatistics;
import org.processmining.est2miner.models.preprocessing.CountedPLog;

import java.util.HashMap;
import java.util.HashSet;


public class PlaceEvaluator {
    private final ESTPartialOrderLog variantLog;
    private final double threshold;
    private final HashMap<ESTPartialOrder, Integer> traceCounts;

    public PlaceEvaluator(final CountedPLog countedPLog, final double threshold) {
        this.threshold = threshold;
        variantLog = countedPLog.getVariantLog();
        traceCounts = countedPLog.getPTraceCounts();
    }

    public ESTPlaceStatus testPlace(HashSet<String> ingoingTransitions, HashSet<String> outgoingTransitions, ESTPlace place) {
        long startTime = System.currentTimeMillis();

        PlaceLogReplayResult placeLogReplayResult;
        try {
            placeLogReplayResult = eST2PlaceEvaluator.evaluatePlaceWithLog(variantLog, traceCounts, ingoingTransitions, outgoingTransitions, place);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // evaluate results of replay of the log
        ESTPlaceStatus result = ESTPlaceStatus.UNFIT;
        if (placeLogReplayResult.getNumbOfTraces() > 0) {//if place is never activated, it is useless and thus unfitting
            if ((placeLogReplayResult.getFittingFraction() >= threshold)) {//enough evidence that place is fit
                result = ESTPlaceStatus.FIT;
            } else {
                boolean UF = placeLogReplayResult.getUnderfedFraction() > (1.0 - threshold);
                boolean OF = placeLogReplayResult.getOverfedFraction() > (1.0 - threshold);

                if (UF && OF) {
                    result = ESTPlaceStatus.MALFED;
                } else if (UF) {
                    result = ESTPlaceStatus.UNDERFED;
                } else if (OF) {
                    result = ESTPlaceStatus.OVERFED;
                }
            }
        }

        PlugInStatistics.getInstance().incTimeEval(System.currentTimeMillis() - startTime);
        if (result == ESTPlaceStatus.FIT) {
            PlugInStatistics.getInstance().incNumFitting();
        } else {//malfed, overfed, underfed or unfit
            PlugInStatistics.getInstance().incNumUnfitting();
        }

        return result;
    }
}
