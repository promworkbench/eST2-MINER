package org.processmining.est2miner.algorithms.placeevaluation;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.partialorder.models.dependency.PDependency;
import org.processmining.partialorder.models.dependency.PDependencyDataAware;
import org.processmining.partialorder.ptrace.model.PTrace;
import org.processmining.partialorder.ptrace.model.imp.PTraceImp;

import java.util.HashMap;
import java.util.Objects;

public abstract class LpoValidator {
    protected final Petrinet petrinet;
    protected final HashMap<Place, Integer> initialMarkingCount;
    protected PTrace partialOrderTrace;
    protected final HashMap<XEvent, Transition> eventToTransition;

    protected LpoValidator(Petrinet petrinet, Marking initialMarking, PTrace partialOrderTrace, boolean addArtificialStartEnd) throws Exception {
        this.petrinet = petrinet;
        initialMarkingCount = new HashMap<>();
        initialMarking.forEach(p -> initialMarkingCount.put(p, initialMarkingCount.containsKey(p) ? initialMarkingCount.get(p) + 1 : 1));
        petrinet.getPlaces().stream().filter(p -> !initialMarkingCount.containsKey(p)).forEach(p -> initialMarkingCount.put(p, 0));

        this.partialOrderTrace = new PTraceImp((XTrace) partialOrderTrace.getTrace().clone(), partialOrderTrace.getTraceIndex());
        for (PDependency dependency : partialOrderTrace.getDependencies()) {
            this.partialOrderTrace.addDependency(new PDependencyDataAware(dependency.getSource(), dependency.getTarget()), dependency.getSource(), dependency.getTarget());
        }

        eventToTransition = new HashMap<>();
        modifyPartialOrder(addArtificialStartEnd);
    }

    protected void modifyPartialOrder(boolean addArtificialStartEnd) throws Exception {
        for (XEvent e : partialOrderTrace) {
            for (Transition t : petrinet.getTransitions()) {
                if (Objects.equals(XExtendedEvent.wrap(e).getName(), t.getLabel())) {
                    if (eventToTransition.containsKey(e)) {
                        throw new Exception("The algorithm does not support label-splitted nets");
                    }
                    eventToTransition.put(e, t);
                }
            }
//            if (!eventToTransition.containsKey(e)) {
//                throw new Exception("The net does not contain a transition with the label " + XExtendedEvent.wrap(e).getName());
//            }
        }

        if (addArtificialStartEnd) {
            setArtificialStartAndEnd();
        }
    }

    private void setArtificialStartAndEnd() {
        XTrace trace = partialOrderTrace.getTrace();
        PTrace prevPartialOrderTrace = partialOrderTrace;

        XFactory factory = new XFactoryNaiveImpl();
        XEvent artificialStart = factory.createEvent();
        XExtendedEvent.wrap(artificialStart).setName("initial marking");
        XEvent artificialEnd = factory.createEvent();
        XExtendedEvent.wrap(artificialEnd).setName("final marking");

        trace.add(artificialStart);
        trace.add(artificialEnd);

        partialOrderTrace = new PTraceImp(trace, prevPartialOrderTrace.getTraceIndex());
        for (PDependency dependency : prevPartialOrderTrace.getDependencies()) {
            partialOrderTrace.addDependency(new PDependencyDataAware(dependency.getSource(), dependency.getTarget()), dependency.getSource(), dependency.getTarget());
        }

        int traceSize = partialOrderTrace.getTrace().size();
        for (Integer startEventIndex : partialOrderTrace.getStartEventIndices()) {
            partialOrderTrace.addDependency(new PDependencyDataAware(traceSize - 2, startEventIndex), traceSize - 2, startEventIndex);
        }

        for (Integer endEventIndex : partialOrderTrace.getEndEventIndices()) {
            partialOrderTrace.addDependency(new PDependencyDataAware(endEventIndex, traceSize - 1), endEventIndex, traceSize - 1);
        }
    }


    public abstract LpoESTValidationResult validate();

    protected int getWeight(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge) {
        return edge.getAttributeMap().containsKey("ProM_Vis_attr_label") ? Integer.parseInt(edge.getAttributeMap().get("ProM_Vis_attr_label").toString()) : 1;
    }

}
