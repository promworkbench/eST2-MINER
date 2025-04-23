package org.processmining.est2miner.algorithms.placeevaluation;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LpoESTFireValidator extends LpoESTFlowValidator {
    private final Place place;
    private final HashMap<Integer, Integer> placeToLocalMarking;
    boolean valid;
    boolean flow;
    boolean branched;
    boolean overfed;
    boolean underfed;

    public LpoESTFireValidator(Petrinet petrinet, ESTPartialOrder partialOrderTrace) {
        super(petrinet, new Marking(), partialOrderTrace);
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
        ArrayList<Integer> totalOrder = buildTotalOrdering();

        fireForwards(totalOrder);

        if (underfed || valid) {
            return new LpoESTValidationResult(createPlaceString(place), LpoESTValidationResult.ValidationPhase.FORWARDS, overfed, underfed);
        }

        // Resetting the branched value to false such that we can check the value for the backwards firing
        valid = true;
        branched = false;
        fireBackwards(totalOrder);

        if (underfed || valid) {
            return new LpoESTValidationResult(createPlaceString(place), LpoESTValidationResult.ValidationPhase.BACKWARDS, overfed, underfed);
        }

        // Rest with flow
        valid = this.checkFlowForPlace(place);
        underfed = !valid;

        return new LpoESTValidationResult(createPlaceString(place), LpoESTValidationResult.ValidationPhase.FLOW, overfed, underfed);
    }

    private ArrayList<Integer> buildTotalOrdering() {
        ArrayList<Integer> ordering = new ArrayList<>();
        ordering.add(0);
        HashSet<Integer> contained = new HashSet<>(ordering);

        LinkedList<Integer> examineLater = IntStream.range(0, partialOrderTrace.getActivities().size()).boxed().collect(Collectors.toCollection(LinkedList::new));
        while (!examineLater.isEmpty()) {
            Integer e = examineLater.removeFirst();
            if (contained.contains(e)) {
                continue;
            }

            boolean add = true;
            for (Integer pre : partialOrderTrace.getPredecessorIndices(e)) {
                if (!contained.contains(pre)) {
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

    private void fireForwards(ArrayList<Integer> totalOrder) {
        // Initialize the marking of the tested place to be empty
        totalOrder.forEach(activity_pos -> placeToLocalMarking.put(activity_pos, 0));
        LinkedList<Integer> queue = new LinkedList<>(totalOrder);
        fire(queue, true, petrinet::getInEdges, petrinet::getOutEdges, pos -> partialOrderTrace.getSuccessorIndices(pos));
    }

    private void fireBackwards(ArrayList<Integer> totalOrder) {
        LinkedList<Integer> queue = new LinkedList<>();

        for (int i = totalOrder.size() - 1; i >= 0; i--) {
            placeToLocalMarking.put(totalOrder.get(i), 0);
            queue.add(totalOrder.get(i));
        }

        fire(queue, false, petrinet::getOutEdges, petrinet::getInEdges, pos -> partialOrderTrace.getPredecessorIndices(pos));
    }

    private void fire(LinkedList<Integer> firingOrder, Boolean forwards, Function<PetrinetNode, Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> preArcs, Function<PetrinetNode, Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> postArcs, Function<Integer, Collection<Integer>> nextEvents) {
        if (firingOrder.isEmpty()) {
            return;
        }

        Integer lastActivityPos = firingOrder.getLast();
        while (!firingOrder.isEmpty()) {
            Integer activityPos = firingOrder.removeFirst();
            String activity = partialOrderTrace.get(activityPos);

            // can fire?
            if (eventToTransition.containsKey(activity)) {
                // fire
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preArcs.apply(eventToTransition.get(activity))) {
                    int updatedNumberOfTokens = placeToLocalMarking.get(activityPos) - getWeight(edge);
                    placeToLocalMarking.put(activityPos, updatedNumberOfTokens);
                    if (updatedNumberOfTokens < 0) {
                        valid = false;
                    }
                }

                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postArcs.apply(eventToTransition.get(activity))) {
                    placeToLocalMarking.put(activityPos, placeToLocalMarking.get(activityPos) + getWeight(edge));
                }
            }

            // push to first later and check for complex places
            if (!nextEvents.apply(activityPos).isEmpty()) {
                if (nextEvents.apply(activityPos).size() > 1 && placeToLocalMarking.get(activityPos) > 0) {
                    branched = true;
                }
                Integer firstLater = nextEvents.apply(activityPos).iterator().next();
                placeToLocalMarking.put(firstLater, placeToLocalMarking.get(firstLater) + placeToLocalMarking.get(activityPos));
            }
        }

        if (forwards) {
            overfed = placeToLocalMarking.get(lastActivityPos) > 0;
            underfed = (placeToLocalMarking.get(lastActivityPos) < 0) || (!valid && !branched);
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
