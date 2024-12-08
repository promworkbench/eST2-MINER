package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTPlaceStatus;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;
import org.processmining.est2miner.models.PlugInStatistics;
import org.processmining.est2miner.models.preprocessing.CountedPLog;
import org.processmining.partialorder.ptrace.model.PLog;
import org.processmining.partialorder.ptrace.model.PTrace;

import java.util.HashMap;
import java.util.HashSet;


public class PlaceEvaluator {
//    private final PLog log;
    private final PLog variantLog;
    private final double threshold;
    private final HashMap<PTrace, Integer> traceCounts;
    private final ESTProcessModel pM;
//    private final HashMap<PTrace, Integer> traceCounts;

    public PlaceEvaluator(final ESTProcessModel pM, final CountedPLog countedPLog, final double threshold) {
        this.pM = pM;
        this.threshold = threshold;
        variantLog = countedPLog.getVariantLog();
        traceCounts = countedPLog.getpTraceCounts();
//        log = pLog;
//        this.traceCounts = traceCounts; // TODO: specifying the frequencies of the trac variants
    }

    public ESTPlaceStatus testPlace(HashSet<String> ingoingTransitions, HashSet<String> outgoingTransitions, ESTPlace place) {
        long startTime = System.currentTimeMillis();

        PlaceLogReplayResult placeLogReplayResult;
        try {
            placeLogReplayResult = eST2PlaceEvaluator.evaluatePlaceWithLog(variantLog, traceCounts, ingoingTransitions, outgoingTransitions, place);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //evaluate results of replay of the log
        ESTPlaceStatus result = ESTPlaceStatus.UNFIT;
        if (placeLogReplayResult.getNumbOfTraces() > 0) {//if place is never activated, it is useless and thus unfitting
            if ((placeLogReplayResult.getFittingFraction() >= threshold)) {//enough evidence that place is fit
                result = ESTPlaceStatus.FIT;

//                System.out.println("(" + ingoingTransitions + "|" + outgoingTransitions + ")");

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

    public void adaptLogToVariantLog(){

    }
}
