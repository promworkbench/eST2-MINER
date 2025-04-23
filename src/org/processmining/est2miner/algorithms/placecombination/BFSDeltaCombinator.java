package org.processmining.est2miner.algorithms.placecombination;

import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;
import org.processmining.est2miner.models.PlugInStatistics;

import java.util.ArrayList;

public class BFSDeltaCombinator {
    private final double delta;
    private final int tauAbsolute;
    private final int[] traceCounts;
    private int currentDepth;
    private final int maxDepth;
    private final int adaptiveDeltaSteepness;
    private final ESTLog log;

    public BFSDeltaCombinator(int adaptiveDeltaSteepness, int tauAbsolute, double delta, int[] traceCounts, int maxDepth, ESTLog log) {
        this.tauAbsolute = tauAbsolute;
        this.delta = delta;
        this.traceCounts = traceCounts;
        this.currentDepth = 2;
        this.maxDepth = maxDepth;
        this.log = log;
        this.adaptiveDeltaSteepness = adaptiveDeltaSteepness;
    }

    //tries adding the given place to the given PM, both expressed by their variant vectors.
    //uses (a possibly modified) delta and tau for decision
    // Returns -1 if generally impossible, 0 if currently impossible, 1 if possible (with resulting vector)

    //Note: statistics are changed, where the places are actually discarded/added/etc (discovery)
    public Object[] combinePlace(boolean[] pMVariantVector, ESTPlace place) {
        boolean[] placeVariantVector = place.getVariantVector();
        int previousFittingTraces = countFittingTraces(pMVariantVector);
        boolean[] variantVectorIfCombined = computeVariantVectorIfCombined(pMVariantVector, placeVariantVector);
        int remainingFittingTraces = countFittingTraces(variantVectorIfCombined);
        if (remainingFittingTraces < tauAbsolute) {// adding the place reduces the global fitness below the threshold tau, discard
            return new Object[]{-1, variantVectorIfCombined};
        }
        int diff = (previousFittingTraces - remainingFittingTraces); //when adding place, PM will have diff less fitting traces
        //set the chosen adaptive delta strategy to compute the delta needed to decide on acceptance or delay of place
        double adaptiveDelta = getAdaptedDeltaSigmoid(place);

        if (diff > (adaptiveDelta * log.getLogSize())) {//adding the place now would remove more than (adaptive) delta fitting traces, delay place
            return new Object[]{0, variantVectorIfCombined};
        }
        //place can be added now
        return new Object[]{1, variantVectorIfCombined};             //accept place
    }


    //computes the variantVector resulting from adding the new VV to the current VV (and - gate)
    private boolean[] computeVariantVectorIfCombined(boolean[] currentVV, boolean[] addedVV) {
        boolean[] resultVV = currentVV.clone();
        for (int i = 0; i < addedVV.length; i++) {
            if (!addedVV[i]) {
                resultVV[i] = false;
            }
        }
        return resultVV;
    }


    //use traceCounts to get the actual number of traces for each variant
    public int countFittingTraces(boolean[] traceVector) {
        int count = 0;
        for (int i = 0; i < traceVector.length; i++) {
            if (traceVector[i]) {
                count = count + this.traceCounts[i];
            }
        }
        return count;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public void setCurrentDepth(int newDepth) {
        this.currentDepth = newDepth;
    }

    //revisits the PRE-SORTED queue of potential places ONCE and trys adding them to model
    public ESTProcessModel revisitQueueOfPlaces(ESTProcessModel pM) {
        ArrayList<ESTPlace> currentPotentialPlaces = pM.getPotentialPlaces();
        boolean[] currentPMVariantVector = pM.getVariantVector();
        pM.setPotentialPlaces(new ArrayList<>());
        int replayableBeforeAdding = countFittingTraces(currentPMVariantVector);
        while (!currentPotentialPlaces.isEmpty()) {
            ESTPlace place = currentPotentialPlaces.remove(0);
            Object[] combinationResults = combinePlace(currentPMVariantVector, place);
            if ((int) combinationResults[0] == -1) {// discard place
                pM.getDiscardedPlaces().add(place);
                PlugInStatistics.getInstance().incDiscardedPlaces(1);
            } else if ((int) combinationResults[0] == 0) { //delay place
                pM.getPotentialPlaces().add(place);
                PlugInStatistics.getInstance().incDelayedPlaces(1);
            } else if ((int) combinationResults[0] == 1) { //add place
                pM.addPlace(place);
                PlugInStatistics.getInstance().incAcceptedPlaces(1);
            }
        }
        pM.updateStatus(log);
        System.out.println("Replayable traces: " + replayableBeforeAdding + " --> " + countFittingTraces(pM.getVariantVector()));
        return pM;
    }

    //--------------- adaptive delta----------------------

    // these SIGMOID functions can be used to modify delta dynamically
    // parameterized based on place depth
    // adapted delta between 0 (no deviation allowed) and delta (max deviation allowed)
    // prefer low depths (steepness and right shift based on depth)
    private double getAdaptedDeltaSigmoid(ESTPlace place) {
        double placeDepth = getPlaceDepth(place);
        double steepness = adaptiveDeltaSteepness / placeDepth; //increase for steeper function (simple places get more steepness)
        // every place starts at 0
        //sigmoid modification factor computation
        double f = (-1) * steepness * (this.currentDepth - placeDepth);
        double modificationFactor = (2.0 / (1.0 + Math.exp(f)) - 1.0); //2/(...)-1 results ins maximum 1, minimum 0, with proper behaviour in between
        //describe what happens for potential debugging
        return delta * modificationFactor;
    }

    //Utility Methods_______________________________________________________________________________________________

    //sum of place transitions
    private int getPlaceDepth(ESTPlace place) {
        return (Integer.bitCount(place.getInputTrKey()) + Integer.bitCount(place.getOutputTrKey()));
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
