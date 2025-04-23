package org.processmining.est2miner.models.coreobjects;

import org.processmining.est2miner.models.preprocessing.CountedPLog;
import org.processmining.framework.util.Pair;

import java.util.*;

public class ESTLog {
    private final CountedPLog partialOrderLog;
    int numberOfTraces;
    final ArrayList<ArrayList<Integer>> traceVariants; //transitions are encoded as integers according to the positioning in the (ingoing) transitions array
    final HashMap<ArrayList<Integer>, Integer> traceVariantCounts;
    private final String[] transitions;
    private final String[] outTransitions;
    final int[] outTransitionMapping;
    int inEndIndex;
    int outStartIndex;

    public ESTLog(CountedPLog countedPLog) {
        partialOrderLog = countedPLog;
        this.transitions = computeInTransitionOrder(); // also sets the necessary start and end indices
        this.outTransitions = computeOutTransitionOrder(); // also sets the necessary start and end indices
        this.outTransitionMapping = computeOutTransitionMapping(transitions, outTransitions);
        // Computes the list of trace variants and the hashmap of their frequencies
        Pair<ArrayList<ArrayList<Integer>>, HashMap<ArrayList<Integer>, Integer>> logObject = computeFinalLogObjects(countedPLog);
        this.traceVariants = logObject.getFirst();
        this.traceVariantCounts = logObject.getSecond();
    }


    //returns a set of variants corresponding to the given variant vector
    public ArrayList<ArrayList<Integer>> getReducedTraceVariants(boolean[] variantVector) {
        ArrayList<ArrayList<Integer>> reducedTraceVariants = new ArrayList<>();
        if (!(variantVector.length == traceVariants.size())) {
            System.out.println("Error reducing log to replayable variants! Variant vector does not match log size.");
        }
        for (int i = 0; i < variantVector.length; i++) {
            if (variantVector[i]) {
                reducedTraceVariants.add(this.traceVariants.get(i));
            }
        }
        return reducedTraceVariants;
    }

    //returns the sum of all traces encoded in the given variant vector
    public int countLiveTraces(boolean[] variantVector) {
        int sum = 0;
        for (int i = 0; i < variantVector.length; i++) {
            if (variantVector[i]) {
                sum = sum + traceVariantCounts.get(this.traceVariants.get(i));
            }
        }
        return sum;
    }


    //returns a transition array encoding which transitions are contained (live) in log encoded by the given variant vector
    public boolean[] getTransitionsLiveness(boolean[] variantVector) {
        //initialize all dead
        boolean[] transitionsLiveness = new boolean[transitions.length];
        //replay variants and set to true if occurring
        ArrayList<ArrayList<Integer>> reducedVariants = this.getReducedTraceVariants(variantVector);
        for (ArrayList<Integer> variant : reducedVariants) {
            for (int i = 0; i < transitionsLiveness.length; i++) {
                if (variant.contains(i)) {
                    transitionsLiveness[i] = true;
                }
            }
        }

        return transitionsLiveness;
    }


//________________________________log initialization methods___________________________________________________


    private String[] getTransitionsFromLog() {
        ArrayList<String> transitionsList = new ArrayList<>();
        for (ESTPartialOrder trace : partialOrderLog.getPLog()) {
            for (String activityLabel : trace.getActivities()) {
                if (!transitionsList.contains(activityLabel)) {
                    transitionsList.add(activityLabel);
                }
            }
        }
        String[] result = new String[transitionsList.size()];
        for (int i = 0; i < transitionsList.size(); i++) {
            result[i] = transitionsList.get(i);
        }
        return result;
    }

    //----------initialization: compute transition orderings------------------------
    public int getLogSize() {
        return traceVariantCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String[] sortLexicographically(String[] transitions) {
        Arrays.sort(transitions, String::compareTo);
        return transitions;
    }

    //adapt if needed (currently randomized), endActivity should be at position 0 (for easy skipping)
    private String[] computeInTransitionOrder() {
        //place ordering strategy here:
        String[] intransitions = sortLexicographically(getTransitionsFromLog()); //ordering: random shuffle
        moveEndActivityToPosZero(intransitions);
        return intransitions;
    }

    //adapt if needed (currently randomized), startActivity should be at position 0 (for easy skipping)
    private String[] computeOutTransitionOrder() {
        //place ordering strategy here:
        String[] outtransitions = sortLexicographically(transitions.clone());//ordering: random shuffle
        moveStartActivityToPosZero(outtransitions);
        return outtransitions;
    }

    //compute the mapping from outtransitions to in transitions
    private int[] computeOutTransitionMapping(String[] intransitions, String[] outtransitions) {
        int[] outTransitionMapping = new int[outtransitions.length];
        for (int i = 0; i < intransitions.length; i++) {
            for (int o = 0; o < outtransitions.length; o++) {
                if (Objects.equals(intransitions[i], outtransitions[o])) {
                    outTransitionMapping[o] = i;
                    break;
                }
            }
        }
        return outTransitionMapping;
    }

    //moves the end activity to position 0 for the intransitions
    private void moveEndActivityToPosZero(String[] intransitions) {
        this.inEndIndex = findEndIndex(intransitions);
        while (inEndIndex != 0) {
            String temp = intransitions[inEndIndex - 1];
            intransitions[inEndIndex - 1] = intransitions[inEndIndex];
            intransitions[inEndIndex] = temp;
            inEndIndex--;
        }
    }

    //moves the start activity to position 0 for the outtransitions
    private void moveStartActivityToPosZero(String[] outtransitions) {
        this.outStartIndex = findStartIndex(outtransitions);
        while (outStartIndex != 0) {
            String temp = outtransitions[outStartIndex - 1];
            outtransitions[outStartIndex - 1] = outtransitions[outStartIndex];
            outtransitions[outStartIndex] = temp;
            outStartIndex--;
        }
    }

    // find end index in the given transition array
    public int findEndIndex(String[] transitions) {
        int endIndex = 0;
        for (int i = 0; i < transitions.length; i++) {
            if ("ArtificialEnd".equals(transitions[i])) {
                endIndex = i;
                break;
            }
        }
        return endIndex;
    }

    //find start index in the given transition array
    public int findStartIndex(String[] transitions) {
        int startIndex = 0;
        for (int i = 0; i < transitions.length; i++) {
            if ("ArtificialStart".equals(transitions[i])) {
                startIndex = i;
                break;
            }
        }
        return startIndex;
    }

//---------- initialization: compute final log objects------------------------

    private Pair<ArrayList<ArrayList<Integer>>, HashMap<ArrayList<Integer>, Integer>> computeFinalLogObjects(CountedPLog inputLog) {
        ArrayList<ArrayList<Integer>> traceVariants = new ArrayList<>();
        HashMap<ArrayList<Integer>, Integer> frequencies = new HashMap<>();

        HashSet<Integer> variantPositions = inputLog.getVariantPositions();
        for (int i = 0; i < inputLog.getVariantLog().size(); i++) {
            if (variantPositions.isEmpty() || variantPositions.contains(i)) {
                ESTPartialOrder trace = inputLog.getVariantLog().get(i);
                ArrayList<Integer> convertedTrace = convertTrace(trace.getActivities());
                traceVariants.add(convertedTrace);
                frequencies.put(convertedTrace, inputLog.getPTraceCounts().get(trace));
            }
        }
        this.numberOfTraces = inputLog.getPTraceCounts().values().stream().mapToInt(Integer::intValue).sum();

        return new Pair<>(traceVariants, frequencies);
    }


    private ArrayList<Integer> convertTrace(ArrayList<String> trace) {
        ArrayList<Integer> result = new ArrayList<>();
        for (String activity : trace) {
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i].equals(activity)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }


    //_________________printing information_____________________________________

    public void printTransitionOrderings() {
        StringBuilder inorder = new StringBuilder("Inorder: ");
        StringBuilder outorder = new StringBuilder("Outorder: ");
        for (int i = 0; i < transitions.length; i++) {
            inorder.append(transitions[i]).append(";  ");
            outorder.append(outTransitions[i]).append(";  ");
        }
        System.out.println(inorder);
        System.out.println(outorder);
    }

    public void printBasicLogSummary() {
        System.out.println("Number of Traces: " + this.numberOfTraces + ", Unique Variants: " + this.traceVariants.size() + ", Number of Activities: " + transitions.length);
    }

    //___________________getter & setter_____________________________________
    public String[] getInTransitions() {
        return this.transitions;
    }

    public int[] getOutTransitionMapping() {
        return this.outTransitionMapping;
    }

    public ArrayList<ArrayList<Integer>> getTraceVariants() {
        return this.traceVariants;
    }


    public HashMap<ArrayList<Integer>, Integer> getTraceVariantCounts() {
        return this.traceVariantCounts;
    }

    public int getNumOfTraces() {
        return getLogSize();
    }

    public int getInEndIndex() {
        return this.inEndIndex;
    }
}