package org.processmining.est2miner.models.preprocessing;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.partialorder.ptrace.model.PLog;
import org.processmining.partialorder.ptrace.model.PTrace;
import org.processmining.partialorder.ptrace.model.imp.PLogImp;

import java.util.*;


public class CountedPLog {
    private PLog log;
    private HashMap<PTrace, Integer> pTraceCounts;
    private PLog variantLog;
    private HashSet<Integer> variantPositions;

    public CountedPLog(PLog log) {
        this.log = log;
        pTraceCounts = computeCounts(log);
        variantLog = computeVariantLog();
    }

    public CountedPLog(PLog log, HashMap<PTrace, Integer> pTraceCounts) {
        this.log = log;
        this.pTraceCounts = pTraceCounts;
        variantLog = computeVariantLog();
    }

    public CountedPLog(PLog log, HashSet<Integer> variantPositions, HashMap<Integer, Integer> variantPositionCounts) {
        this.log = log;
        this.variantPositions = variantPositions;
        variantLog = computeVariantLog(variantPositionCounts);
    }

    public HashMap<PTrace, Integer> getpTraceCounts() {
        return pTraceCounts;
    }

    public PLog getPLog() {
        return log;
    }

    public int computeSize() {
        return pTraceCounts.values().stream().mapToInt(value -> value).sum();
    }

    public void putCountForPTrace(PTrace trace, int count) {
        pTraceCounts.put(trace, count);
    }

    public PLog getVariantLog() {
        return variantLog;
    }

    private PLog computeVariantLog() {
        XLogImpl variantXLog = new XLogImpl(log.getXLog().getAttributes());
        PLogImp result = new PLogImp(variantXLog);
        variantPositions = new HashSet<>();

        for (int i = 0; i < log.size(); i++) {
            PTrace pTrace = log.get(i);
            if (pTraceCounts.containsKey(pTrace)) {
                variantXLog.add(pTrace.getTrace());
                result.add(pTrace);
                variantPositions.add(i);
            }
        }

        return result;
    }

    public HashMap<Integer, Integer> computeVariantPositionCounts() {
        HashMap<Integer, Integer> positionCounts = new HashMap<>();

        for (Integer variantPosition : variantPositions) {
            positionCounts.put(variantPosition, pTraceCounts.get(log.get(variantPosition)));
        }

        return positionCounts;
    }

    private PLog computeVariantLog(HashMap<Integer, Integer> variantPositionCounts) {
        XLogImpl variantXLog = new XLogImpl(log.getXLog().getAttributes());
        PLogImp result = new PLogImp(variantXLog);
        ArrayList<Integer> variantPositions = new ArrayList<>(variantPositionCounts.keySet());
        variantPositions.sort(Integer::compareTo);
        pTraceCounts = new HashMap<>();

        for (Integer variantPosition : variantPositions) {
            PTrace pTrace = log.get(variantPosition);
            variantXLog.add(pTrace.getTrace());
            result.add(pTrace);
            pTraceCounts.put(pTrace, variantPositionCounts.get(variantPosition));
        }

        return result;
    }

    public HashSet<Integer> getVariantPositions() {
        return variantPositions;
    }

    private HashMap<PTrace, Integer> computeCounts(PLog log) {
        HashMap<String, HashSet<PTrace>> variantStringToPTrace = computeVariantStringToPTrace(log);
        HashMap<PTrace, Integer> preliminaryCounts = makePreliminaryCounts(variantStringToPTrace);
        HashMap<Integer, HashMap<PTrace, Integer>> countsForLength = computeCountsInLengthBins(preliminaryCounts);
        HashMap<Integer, HashMap<PTrace, Integer>> countsForLengthPartial = combineCountsOfHomomorphismsWithBins(countsForLength);
        HashMap<PTrace, Integer> counts = combineResult(countsForLengthPartial);

        return counts;
    }

    private HashMap<PTrace, Integer> combineResult(HashMap<Integer, HashMap<PTrace, Integer>> countsForLengthPartial) {
        HashMap<PTrace, Integer> result = new HashMap<>();

        for (HashMap<PTrace, Integer> value : countsForLengthPartial.values()) {
            result.putAll(value);
        }

        return result;
    }

    private HashMap<Integer, HashMap<PTrace, Integer>> combineCountsOfHomomorphismsWithBins(HashMap<Integer, HashMap<PTrace, Integer>> countsForLength) {
        HashMap<Integer, HashMap<PTrace, Integer>> result = new HashMap<>();

        for (Map.Entry<Integer, HashMap<PTrace, Integer>> integerHashMapEntry : countsForLength.entrySet()) {
            result.put(integerHashMapEntry.getKey(), combineCountsOfHomomorphisms(integerHashMapEntry.getValue()));
        }

        return result;
    }

    private HashMap<Integer, HashMap<PTrace, Integer>> computeCountsInLengthBins(HashMap<PTrace, Integer> preliminaryCounts) {
        HashMap<Integer, HashMap<PTrace, Integer>> result = new HashMap<>();

        for (Map.Entry<PTrace, Integer> pTraceIntegerEntry : preliminaryCounts.entrySet()) {
            int length = pTraceIntegerEntry.getKey().size();

            if (!result.containsKey(length)) {
                result.put(length, new HashMap<>());
            }

            result.get(length).put(pTraceIntegerEntry.getKey(), pTraceIntegerEntry.getValue());
        }

        return result;
    }

    private HashMap<PTrace, Integer> combineCountsOfHomomorphisms(HashMap<PTrace, Integer> preliminaryCounts) {
        HashMap<PTrace, Integer> result = new HashMap<>();
        LinkedList<PTrace> stack = new LinkedList<>(preliminaryCounts.keySet());
        outerLoop:
        while (!stack.isEmpty()) {
            PTrace top = stack.pop();
            for (PTrace trace : result.keySet()) {
                if (arePartialOrdersIsomorphic(top, trace)) {
                    result.put(trace, result.get(trace) + preliminaryCounts.get(trace));
                    continue outerLoop;
                }
            }

            result.put(top, preliminaryCounts.get(top));
        }

        return result;
    }

    private HashMap<String, HashSet<PTrace>> computeVariantStringToPTrace(PLog pLog) {
        HashMap<String, HashSet<PTrace>> result = new HashMap<>();
        for (PTrace pTrace : pLog) {
            String traceString = createTraceString(pTrace.getTrace());
            if (!result.containsKey(traceString)) {
                result.put(traceString, new HashSet<>());
            }

            result.get(traceString).add(pTrace);
        }

        return result;
    }

    private HashMap<PTrace, Integer> makePreliminaryCounts(HashMap<String, HashSet<PTrace>> stringToVariantInstance) {
        HashMap<PTrace, Integer> result = new HashMap<>();

        for (HashSet<PTrace> value : stringToVariantInstance.values()) {
            result.put(value.iterator().next(), value.size());
        }

        return result;
    }

    private String createTraceString(XTrace trace) {
        StringBuilder traceString = new StringBuilder();
        traceString.append("<");

        for (XEvent event : trace) {
            XExtendedEvent wrapper = new XExtendedEvent(event);

            traceString.append(wrapper.getName()).append("+").append(wrapper.getStandardTransition());
            traceString.append(", ");
        }

        traceString.append(">");

        return traceString.toString();
    }

    public boolean arePartialOrdersIsomorphic(PTrace partialOrderA, PTrace partialOrderB) {
        if (partialOrderA.size() != partialOrderB.size()) {
            return false;
        }

        Collection<Integer> startEventsA = partialOrderA.getStartEventIndices();
        Collection<Integer> startEventsB = partialOrderB.getStartEventIndices();

        if (startEventsA.size() != startEventsB.size()) {
            return false;
        }

        Collection<Integer> endEventsA = partialOrderA.getEndEventIndices();
        Collection<Integer> endEventsB = partialOrderB.getEndEventIndices();

        if (endEventsA.size() != endEventsB.size()) {
            return false;
        }

        LinkedList<IsomorphismCandidate> unsolved = new LinkedList<>();
        for (Integer startIndex : startEventsA) {
            unsolved.add(new IsomorphismCandidate(partialOrderA, partialOrderB, startIndex, startEventsB));
        }

        HashMap<Integer, Integer> mappingAB = new HashMap<>();
        HashMap<Integer, Integer> mappingBA = new HashMap<>();
        HashSet<IsomorphismCandidate> pushedToBack = new HashSet<>();

        while (!unsolved.isEmpty()) {
            IsomorphismCandidate problem = unsolved.removeFirst();
            if (mappingAB.containsKey(problem.getTarget())) {
                continue;
            }

            if (problem.getIngoingIndicesOfTarget().stream().anyMatch(index -> !mappingAB.containsKey(index))) {
                // pre-set was not yet determined, we have to wait
                if (pushedToBack.contains(problem)) {
                    return false;
                }
                pushedToBack.add(problem);
                unsolved.add(problem);
                continue;
            }
            problem.updateCandidatesUsingFilter(mappingBA);

            int match = problem.getCandidates().stream().filter(candidate -> {
                boolean sameLabel = problem.getCandidatePTraceEventName(candidate).equals(problem.getTargetPTraceEventName(problem.getTarget()));
                if (!sameLabel) {
                    return false;
                }
                if (problem.getIngoingIndicesOfCandidate(candidate).size() != problem.getIngoingIndicesOfTarget().size()) {
                    return false;
                }
                if (problem.getOutgoingIndicesOfCandidate(candidate).size() != problem.getOutgoingIndicesOfTarget().size()) {
                    return false;
                }
                HashMap<String, Integer> previousLabels = new HashMap<>();
                for (Integer id : problem.getIngoingIndicesOfCandidate(candidate)) {
                    String eventName = problem.getCandidatePTraceEventName(id);
                    if (!previousLabels.containsKey(eventName)) {
                        previousLabels.put(eventName, 1);
                    } else {
                        previousLabels.put(eventName, previousLabels.get(eventName) + 1);
                    }
                }

                for (String eventName : problem.getIngoingEventNamesOfTarget()) {
                    if (!previousLabels.containsKey(eventName)) {
                        return false;
                    }
                    previousLabels.put(eventName, previousLabels.get(eventName) - 1);
                    if (previousLabels.get(eventName) == 0) {
                        previousLabels.remove(eventName);
                    }
                }

                return true;
            }).findAny().orElse(-1);

            if (match == -1) {
                return false;
            }

            pushedToBack = new HashSet<>();

            mappingAB.put(problem.getTarget(), match);
            mappingBA.put(match, problem.getTarget());

            for (Integer next : problem.getOutgoingIndicesOfTarget()) {
                unsolved.add(new IsomorphismCandidate(partialOrderA, partialOrderB, next, problem.getOutgoingIndicesOfCandidate(match)));
            }

        }

        return true;
    }
}
