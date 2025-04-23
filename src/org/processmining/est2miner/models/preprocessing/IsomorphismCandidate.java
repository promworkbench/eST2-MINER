package org.processmining.est2miner.models.preprocessing;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class IsomorphismCandidate {
    private final ESTPartialOrder pTraceOfTarget;
    private final Integer target;
    private final ESTPartialOrder pTraceOfCandidates;
    private Collection<Integer> candidates;

    public IsomorphismCandidate(ESTPartialOrder pTraceOfTarget, ESTPartialOrder pTraceOfCandidates, Integer target, Collection<Integer> candidates) {
        this.pTraceOfTarget = pTraceOfTarget;
        this.target = target;
        this.pTraceOfCandidates = pTraceOfCandidates;
        this.candidates = candidates;
    }

    public Integer getTarget() {
        return target;
    }

    public Collection<Integer> getCandidates() {
        return candidates;
    }

    public Collection<Integer> getIngoingIndicesOfCandidate(int candidate) {
        return pTraceOfCandidates.getPredecessorIndices(candidate);
    }

    public Collection<Integer> getOutgoingIndicesOfCandidate(int candidate) {
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
            result.add(pTraceOfTarget.get(predecessorIndex));
        }

        return result;
    }

    public void updateCandidatesUsingFilter(HashMap<Integer, Integer> mappingToFit) {
        candidates = candidates.stream().filter(id -> !mappingToFit.containsKey(id)).collect(Collectors.toList());
    }

    public String getTargetPTraceEventName(Integer id) {
        return pTraceOfTarget.get(id);
    }

    public String getCandidatePTraceEventName(Integer id) {
        return pTraceOfCandidates.get(id);
    }
}
