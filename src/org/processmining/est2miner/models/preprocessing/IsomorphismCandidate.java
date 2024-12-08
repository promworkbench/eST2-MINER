package org.processmining.est2miner.models.preprocessing;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.processmining.partialorder.ptrace.model.PTrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class IsomorphismCandidate {
    private PTrace pTraceOfTarget;
    private Integer target;
    private PTrace pTraceOfCandidates;
    private Collection<Integer> candidates;
    private static final XEventNameClassifier classifier = new XEventNameClassifier();

    public IsomorphismCandidate(PTrace pTraceOfTarget, PTrace pTraceOfCandidates, Integer target,
                                Collection<Integer> candidates) {
        this.pTraceOfTarget = pTraceOfTarget;
        this.target = target;
        this.pTraceOfCandidates = pTraceOfCandidates;
        this.candidates = candidates;
    }

    public XEvent getTargetEvent() {
        return pTraceOfTarget.getEvent(target);
    }

    public Integer getTarget() {
        return target;
    }

    public Collection<Integer> getCandidates() {
        return candidates;
    }

    public Collection<Integer> getIngoingIndicesOfCandidate(int candidate){
        return pTraceOfCandidates.getPredecessorIndices(candidate);
    }

    public Collection<Integer> getOutgoingIndicesOfCandidate(int candidate){
        return pTraceOfCandidates.getSuccessorIndices(candidate);
    }

    public Collection<Integer> getIngoingIndicesOfTarget() {
        return pTraceOfTarget.getPredecessorIndices(target);
    }

    public Collection<Integer> getOutgoingIndicesOfTarget() {
        return pTraceOfTarget.getSuccessorIndices(target);
    }

    public ArrayList<String> getIngoingEventNamesOfTarget() {
        ArrayList<String> result = new ArrayList<>();

        for (Integer predecessorIndex : pTraceOfTarget.getPredecessorIndices(target)) {
            result.add(classifier.getClassIdentity(pTraceOfTarget.getEvent(predecessorIndex)));
        }

        return result;
    }

    public void updateCandidatesUsingFilter(HashMap<Integer, Integer> mappingToFit){
        candidates = candidates.stream().filter(id -> !mappingToFit.containsKey(id)).collect(Collectors.toList());
    }

    public String getTargetPTraceEventName(Integer id) {
        return classifier.getClassIdentity(pTraceOfTarget.getEvent(id));
    }

    public String getCandidatePTraceEventName(Integer id) {
        return classifier.getClassIdentity(pTraceOfCandidates.getEvent(id));
    }
}
