package org.processmining.est2miner.algorithms.placeevaluation;

import org.deckfour.xes.model.XEvent;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.partialorder.ptrace.model.PTrace;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LpoESTFireValidator extends LpoESTFlowValidator {
    private final Place place;
    private final HashMap<XEvent, Integer> placeToLocalMarking;
    boolean valid;
    boolean flow;
    boolean branched;
    boolean overfed;
    boolean underfed;

    public LpoESTFireValidator(Petrinet petrinet, PTrace partialOrderTrace) throws Exception {
        super(petrinet, new Marking(), partialOrderTrace, false);
        this.place = petrinet.getPlaces().iterator().next();
        placeToLocalMarking = new HashMap<>();

        valid = true;
        flow = false;
        branched = false;
        overfed = false;
        underfed = false;
    }

    @Override
    public LpoESTValidationResult validate() {
        // Order the partial ordered trace into a valid total order of the partial order
        ArrayList<XEvent> totalOrder = buildTotalOrdering();

        fireForwards(totalOrder);

        if (underfed || valid) {
            return new LpoESTValidationResult(
                    createPlaceString(place),
                    LpoESTValidationResult.ValidationPhase.FORWARDS,
                    overfed,
                    underfed
            );
        }

        // Resetting the branched value to false such that we can check the value for the backwards firing
        valid = true;
        branched = false;
        fireBackwards(totalOrder);

        if (underfed || valid) {
            return new LpoESTValidationResult(
                    createPlaceString(place),
                    LpoESTValidationResult.ValidationPhase.BACKWARDS,
                    overfed,
                    underfed
            );
        }

        // Rest with flow
        valid = this.checkFlowForPlace(place, partialOrderTrace);
        underfed = !valid;

        return new LpoESTValidationResult(
                createPlaceString(place),
                LpoESTValidationResult.ValidationPhase.FLOW,
                overfed,
                underfed
        );
    }

    private ArrayList<XEvent> buildTotalOrdering() {
        ArrayList<XEvent> ordering = new ArrayList<>();
        partialOrderTrace.getStartEventIndices().forEach(integer -> ordering.add(partialOrderTrace.getEvent(integer)));
        HashSet<XEvent> contained = new HashSet<>(ordering);

        LinkedList<XEvent> examineLater = new LinkedList<>(partialOrderTrace.getTrace());
        while (!examineLater.isEmpty()) {
            XEvent e = examineLater.removeFirst();
            if (contained.contains(e)) {
                continue;
            }

            boolean add = true;
            for (Integer pre : partialOrderTrace.getPredecessorIndices(partialOrderTrace.getTrace().indexOf(e))) {
                if (!contained.contains(partialOrderTrace.getEvent(pre))) {
                    add = false;
                    break;
                }
            }
            if (add) {
                ordering.add(e);
                contained.add(e);
            } else {
                examineLater.add(e);
            }
        }

        return ordering;
    }

    private void fireForwards(ArrayList<XEvent> totalOrder) {
        // Initialize the marking of the tested place to be empty
        totalOrder.forEach(xEvent -> {
            placeToLocalMarking.put(xEvent, 0);
        });
        LinkedList<XEvent> queue = new LinkedList<>(totalOrder);
        fire(queue, true,
                petrinet::getInEdges,
                petrinet::getOutEdges,
                e -> partialOrderTrace.getSuccessorIndices(partialOrderTrace.getTrace().indexOf(e)).stream().map(partialOrderTrace::getEvent).collect(Collectors.toCollection(HashSet::new)));
    }

    private void fireBackwards(ArrayList<XEvent> totalOrder) {
        LinkedList<XEvent> queue = new LinkedList<>();

        for (int i = totalOrder.size() - 1; i >= 0; i--) {
            placeToLocalMarking.put(totalOrder.get(i), 0);
            queue.add(totalOrder.get(i));
        }

        fire(queue, false,
                petrinet::getOutEdges,
                petrinet::getInEdges,
                e -> partialOrderTrace.getPredecessorIndices(partialOrderTrace.getTrace().indexOf(e)).stream().map(partialOrderTrace::getEvent).collect(Collectors.toCollection(HashSet::new)));
    }

    private void fire(LinkedList<XEvent> firingOrder, Boolean forwards,
                      Function<PetrinetNode, Collection<PetrinetEdge<? extends PetrinetNode, ? extends
                              PetrinetNode>>> preArcs,
                      Function<PetrinetNode, Collection<PetrinetEdge<? extends PetrinetNode, ? extends
                              PetrinetNode>>> postArcs,
                      Function<XEvent, HashSet<XEvent>> nextEvents) {
        if (firingOrder.isEmpty()) {
            return;
        }

        XEvent endEvent = firingOrder.getLast();
        while (!firingOrder.isEmpty()) {
            XEvent e = firingOrder.removeFirst();

            // can fire?
            if (eventToTransition.containsKey(e)) {
                // fire
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preArcs.apply(eventToTransition.get(e))) {
                    int updatedNumberOfTokens = placeToLocalMarking.get(e) - getWeight(edge);
                    placeToLocalMarking.put(e, updatedNumberOfTokens);
                    if (updatedNumberOfTokens < 0) {
                        valid = false;
                    }
                }

                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postArcs.apply(eventToTransition.get(e))) {
                    placeToLocalMarking.put(e, placeToLocalMarking.get(e) + getWeight(edge));
                }
            }

            // push to first later and check for complex places
            if (!nextEvents.apply(e).isEmpty()) {
                if (nextEvents.apply(e).size() > 1 && placeToLocalMarking.get(e) > 0) {
                    branched = true;
                }
                XEvent firstLater = nextEvents.apply(e).iterator().next();
                placeToLocalMarking.put(firstLater, placeToLocalMarking.get(firstLater) + placeToLocalMarking.get(e));
            }
        }

        if (forwards) {
            overfed = placeToLocalMarking.get(endEvent) > 0;
            // TODO: Sobald ein marking weniger als 0 Token hat, sollte der Place underfed sein
            underfed = (placeToLocalMarking.get(endEvent) < 0) || (!valid && !branched);
        } else {
            underfed |= (!valid && !branched);
        }
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
