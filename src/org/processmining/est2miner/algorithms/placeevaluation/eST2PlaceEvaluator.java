package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;
import org.processmining.est2miner.models.coreobjects.ESTPartialOrderLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class eST2PlaceEvaluator {
    public enum FitnessStatus {
        OVERFED, UNDERFED, OVER_AND_UNDERFED, Fitting;

        @Override
        public String toString() {
            switch (this) {
                case OVERFED:
                    return "Overfed";
                case UNDERFED:
                    return "Underfed";
                case OVER_AND_UNDERFED:
                    return "Over- and Underfed";
                case Fitting:
                    return "Fitting";
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    public static FitnessStatus getStatus(boolean isOverfed, boolean isUnderfed) {
        if (isOverfed && isUnderfed) {
            return FitnessStatus.OVER_AND_UNDERFED;
        } else if (isOverfed) {
            return FitnessStatus.OVERFED;
        } else if (isUnderfed) {
            return FitnessStatus.UNDERFED;
        } else {
            return FitnessStatus.Fitting;
        }
    }

    public static PlaceLogReplayResult evaluatePlaceWithLog(ESTPartialOrderLog pLog, HashMap<ESTPartialOrder, Integer> traceCounts, HashSet<String> ingoingTransitions, HashSet<String> outgoingTransitions, ESTPlace place) {
        Petrinet placePetriNet = createPlacePetriNet(ingoingTransitions, outgoingTransitions);

        int overfedCount = 0;
        int underfedCount = 0;
        int overAndUnderfedCount = 0;
        int fittingCount = 0;

        for (int i = 0; i < pLog.size(); i++) {
            ESTPartialOrder trace = pLog.get(i);
            FitnessStatus fitnessStatus = evaluatePlace(trace, placePetriNet);

            switch (fitnessStatus) {
                case OVERFED:
                    overfedCount = overfedCount + traceCounts.get(trace);
                    break;
                case UNDERFED:
                    underfedCount = underfedCount + traceCounts.get(trace);
                    break;
                case OVER_AND_UNDERFED:
                    overAndUnderfedCount = overAndUnderfedCount + traceCounts.get(trace);
                    break;
                case Fitting:
                    place.editVariantVector(i, true);
                    fittingCount = fittingCount + traceCounts.get(trace);
                    break;
            }
        }

        return new PlaceLogReplayResult(overfedCount, underfedCount, overAndUnderfedCount, fittingCount);
    }

    public static FitnessStatus evaluatePlace(ESTPartialOrder pTrace, Petrinet placePetriNet) {
        LpoESTFireValidator validator = new LpoESTFireValidator(placePetriNet, pTrace);

        LpoESTValidationResult validate = validator.validate();

        return getStatus(validate.isOverfed(), validate.isUnderfed());
    }

    public static Petrinet createPlacePetriNet(Set<String> inTransitions, Set<String> outTransitions) {
        Petrinet result = new PetrinetImpl("");

        Place evalPlace = result.addPlace("Test Place");

        HashMap<String, Transition> labelToTransition = new HashMap<>();

        for (String label : inTransitions) {
            Transition t_in = result.addTransition(label);
            labelToTransition.put(label, t_in);

            result.addArc(t_in, evalPlace);
        }

        for (String label : outTransitions) {
            Transition t_out;
            if (labelToTransition.containsKey(label)) {
                t_out = labelToTransition.get(label);
            } else {
                t_out = result.addTransition(label);
            }
            result.addArc(evalPlace, t_out);
        }

        return result;
    }
}
