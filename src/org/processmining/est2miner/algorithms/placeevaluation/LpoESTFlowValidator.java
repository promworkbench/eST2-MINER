package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LpoESTFlowValidator extends LpoValidator {
    private static final Logger LOGGER = Logger.getLogger(LpoESTFlowValidator.class.getName());

    public LpoESTFlowValidator(Petrinet petrinet, Marking initialMarking, ESTPartialOrder partialOrderTrace) {
        super(petrinet, initialMarking, partialOrderTrace);
    }

    @Override
    public LpoESTValidationResult validate() {
        Place place = petrinet.getPlaces().iterator().next();

        return new LpoESTValidationResult(createPlaceString(place), checkFlowForPlace(place), LpoESTValidationResult.ValidationPhase.FLOW);
    }

    protected boolean checkFlowForPlace(Place place) {
        int n = partialOrderTrace.size() * 2 + 2;
        int source = 0;
        int sink = n - 1;

        MaxFlowPreflowN3 network = new MaxFlowPreflowN3(n);

        for (int i = 0; i < partialOrderTrace.size(); i++) {
            network.setUnbounded(eventStart(i), eventEnd(i));

            String activity = partialOrderTrace.get(i);
            if (!eventToTransition.containsKey(activity)) {
                if (initialMarkingCount.containsKey(place)) {
                    network.setCapValue(source, eventEnd(i), initialMarkingCount.get(place));
                }
            } else {
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : petrinet.getOutEdges(eventToTransition.get(activity))) {
                    Place postPlace = (Place) outEdge.getTarget();
                    if (postPlace == place) {
                        network.setCapValue(source, eventEnd(i), getWeight(outEdge));
                    }
                }
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : petrinet.getInEdges(eventToTransition.get(activity))) {
                    Place prePlace = (Place) inEdge.getSource();
                    if (prePlace == place) {
                        network.setCapValue(eventStart(i), sink, getWeight(inEdge));
                    }
                }
            }
            for (Integer successorIndex : partialOrderTrace.getSuccessorIndices(i)) {
                network.setUnbounded(eventEnd(i), eventStart(successorIndex));
            }
        }

        int need = 0;
        for (int i = 0; i < n; i++) {
            need += network.getCapValue(i, sink);
        }
        int f = network.maxFlow(source, sink);
        LOGGER.log(Level.FINE, "flow " + place + " " + f);
        LOGGER.log(Level.FINE, "flow " + place + " " + need);

        return need == f;
    }

    private int eventStart(int eventIndex) {
        return eventIndex * 2 + 1;
    }

    private int eventEnd(int eventIndex) {
        return eventIndex * 2 + 2;
    }

    private String createPlaceString(Place p) {
        HashSet<String> ingoingTransitions = new HashSet<>();
        HashSet<String> outgoingTransitions = new HashSet<>();

        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : p.getGraph().getInEdges(p)) {
            ingoingTransitions.add(inEdge.getSource().getLabel());
        }

        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : p.getGraph().getOutEdges(p)) {
            outgoingTransitions.add(outEdge.getTarget().getLabel());
        }

        return "(" + ingoingTransitions + " | " + outgoingTransitions + ")";
    }
}
