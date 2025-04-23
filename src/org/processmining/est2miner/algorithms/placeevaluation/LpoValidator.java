package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.HashMap;

public abstract class LpoValidator {
    protected final Petrinet petrinet;
    protected final HashMap<Place, Integer> initialMarkingCount;
    protected ESTPartialOrder partialOrderTrace;
    protected final HashMap<String, Transition> eventToTransition;

    protected LpoValidator(Petrinet petrinet, Marking initialMarking, ESTPartialOrder partialOrderTrace) {
        this.petrinet = petrinet;
        initialMarkingCount = new HashMap<>();
        initialMarking.forEach(p -> initialMarkingCount.put(p, initialMarkingCount.containsKey(p) ? initialMarkingCount.get(p) + 1 : 1));
        petrinet.getPlaces().stream().filter(p -> !initialMarkingCount.containsKey(p)).forEach(p -> initialMarkingCount.put(p, 0));

        this.partialOrderTrace = partialOrderTrace;

        eventToTransition = new HashMap<>();
        modifyPartialOrder();
    }

    protected void modifyPartialOrder() {
        for (String activity : partialOrderTrace.getActivities()) {
            for (Transition t : petrinet.getTransitions()) {
                if (activity.equals(t.getLabel())) {
                    eventToTransition.put(activity, t);
                }
            }
        }
    }

    public abstract LpoESTValidationResult validate();

    protected int getWeight(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge) {
        return edge.getAttributeMap().containsKey("ProM_Vis_attr_label") ? Integer.parseInt(edge.getAttributeMap().get("ProM_Vis_attr_label").toString()) : 1;
    }

}
