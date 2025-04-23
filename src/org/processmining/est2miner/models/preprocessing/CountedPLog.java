package org.processmining.est2miner.models.preprocessing;

import org.processmining.est2miner.models.coreobjects.ESTPartialOrder;
import org.processmining.est2miner.models.coreobjects.ESTPartialOrderLog;
import org.processmining.framework.util.Pair;

import java.util.*;


public class CountedPLog {
    private final ESTPartialOrderLog log;
    private final HashMap<ESTPartialOrder, Integer> pTraceCounts;
    private final ESTPartialOrderLog variantLog;
    private HashSet<Integer> variantPositions;

    public CountedPLog(ESTPartialOrderLog log) {
        this.log = log;
        pTraceCounts = computeCounts(log);
        variantLog = computeVariantLog();
    }

    public CountedPLog(ESTPartialOrderLog log, HashMap<ESTPartialOrder, Integer> pTraceCounts) {
        this.log = log;
        this.pTraceCounts = pTraceCounts;
        variantLog = computeVariantLog();
    }

    public HashMap<ESTPartialOrder, Integer> getPTraceCounts() {
        return pTraceCounts;
    }

    public ESTPartialOrderLog getPLog() {
        return log;
    }

    public ESTPartialOrderLog getVariantLog() {
        return variantLog;
    }

    private ESTPartialOrderLog computeVariantLog() {
        ESTPartialOrderLog result = new ESTPartialOrderLog();
        variantPositions = new HashSet<>();

        for (ESTPartialOrder pTrace : log) {
            if (pTraceCounts.containsKey(pTrace)) {
                result.add(pTrace);
            }
        }

        return result;
    }

    public HashSet<Integer> getVariantPositions() {
        return variantPositions;
    }

    private HashMap<ESTPartialOrder, Integer> computeCounts(ESTPartialOrderLog log) {
        HashMap<String, HashSet<ESTPartialOrder>> variantStringToPTrace = computeVariantStringToPTrace(log);
        HashMap<ESTPartialOrder, Integer> preliminaryCounts = makePreliminaryCounts(variantStringToPTrace);
        HashMap<Integer, ArrayList<Pair<ESTPartialOrder, Integer>>> countsForLength = computeCountsInLengthBins(preliminaryCounts);
        HashMap<Integer, HashMap<ESTPartialOrder, Integer>> countsForLengthPartial = combineCountsOfHomomorphismsWithBins(countsForLength);

        return combineResult(countsForLengthPartial);
    }

    private HashMap<ESTPartialOrder, Integer> combineResult(HashMap<Integer, HashMap<ESTPartialOrder, Integer>> countsForLengthPartial) {
        HashMap<ESTPartialOrder, Integer> result = new HashMap<>();

        for (HashMap<ESTPartialOrder, Integer> value : countsForLengthPartial.values()) {
            result.putAll(value);
        }

        return result;
    }

    private HashMap<Integer, HashMap<ESTPartialOrder, Integer>> combineCountsOfHomomorphismsWithBins(HashMap<Integer, ArrayList<Pair<ESTPartialOrder, Integer>>> countsForLength) {
        HashMap<Integer, HashMap<ESTPartialOrder, Integer>> result = new HashMap<>();

        for (Map.Entry<Integer, ArrayList<Pair<ESTPartialOrder, Integer>>> integerHashMapEntry : countsForLength.entrySet()) {
            result.put(integerHashMapEntry.getKey(), combineCountsOfHomomorphisms(integerHashMapEntry.getValue()));
        }

        return result;
    }

    private HashMap<Integer, ArrayList<Pair<ESTPartialOrder, Integer>>> computeCountsInLengthBins(HashMap<ESTPartialOrder, Integer> preliminaryCounts) {
        HashMap<Integer, ArrayList<Pair<ESTPartialOrder, Integer>>> result = new HashMap<>();

        for (Map.Entry<ESTPartialOrder, Integer> pTraceIntegerEntry : preliminaryCounts.entrySet()) {
            int length = pTraceIntegerEntry.getKey().size();

            if (!result.containsKey(length)) {
                result.put(length, new ArrayList<>());
            }

            result.get(length).add(new Pair<>(pTraceIntegerEntry.getKey(), pTraceIntegerEntry.getValue()));
        }

        return result;
    }

    private HashMap<ESTPartialOrder, Integer> combineCountsOfHomomorphisms(ArrayList<Pair<ESTPartialOrder, Integer>> preliminaryCounts) {
        HashMap<ESTPartialOrder, Integer> result = new HashMap<>();
        LinkedList<Pair<ESTPartialOrder, Integer>> stack = new LinkedList<>(preliminaryCounts);
        outerLoop:
        while (!stack.isEmpty()) {
            Pair<ESTPartialOrder, Integer> top = stack.pop();
            for (ESTPartialOrder trace : result.keySet()) {
                if (arePartialOrdersIsomorphic(top.getFirst(), trace)) {
                    result.put(trace, result.get(trace) + top.getSecond());
                    continue outerLoop;
                }
            }

            result.put(top.getFirst(), top.getSecond());
        }

        return result;
    }

    private HashMap<String, HashSet<ESTPartialOrder>> computeVariantStringToPTrace(ESTPartialOrderLog pLog) {
        HashMap<String, HashSet<ESTPartialOrder>> result = new HashMap<>();
        for (ESTPartialOrder pTrace : pLog) {
            String traceString = createTraceString(pTrace);
            if (!result.containsKey(traceString)) {
                result.put(traceString, new HashSet<>());
            }

            result.get(traceString).add(pTrace);
        }

        return result;
    }

    private HashMap<ESTPartialOrder, Integer> makePreliminaryCounts(HashMap<String, HashSet<ESTPartialOrder>> stringToVariantInstance) {
        HashMap<ESTPartialOrder, Integer> result = new HashMap<>();

        for (HashSet<ESTPartialOrder> value : stringToVariantInstance.values()) {
            result.put(value.iterator().next(), value.size());
        }

        return result;
    }

    private String createTraceString(ESTPartialOrder trace) {
        StringBuilder traceString = new StringBuilder();
        traceString.append("<");

        for (String event : trace) {
            traceString.append(event);
            traceString.append(", ");
        }

        traceString.append(">");

        return traceString.toString();
    }

    public boolean arePartialOrdersIsomorphic(ESTPartialOrder partialOrderA, ESTPartialOrder partialOrderB) {
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
