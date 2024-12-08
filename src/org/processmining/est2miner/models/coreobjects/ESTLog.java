package org.processmining.est2miner.models.coreobjects;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.est2miner.models.preprocessing.CountedPLog;
import org.processmining.log.utils.XLogBuilder;
import org.processmining.partialorder.ptrace.model.PLog;
import org.processmining.partialorder.ptrace.model.PTrace;

import java.util.*;

public class ESTLog {
    final XLog xLog; //input log
    final XEventClassifier classifier;
    int numberOfTraces;
    final ArrayList<ArrayList<Integer>> traceVariants; //transitions are encoded as integers according to the positioning in the (ingoing) transitions array
    final HashMap<ArrayList<Integer>, Integer> traceVariantCounts;
    private final String[] transitions;
    private final String[] outTransitions;
    final int[] outTransitionMapping;
    XEvent endActivity;
    XEvent startActivity;
    int inEndIndex;
    int outStartIndex;
    HashMap<String, Integer> transitionOccurences;
    HashMap<String, Integer> transitionAverageIndices;


    public ESTLog(XLog inputLog, PLog plog, Parameters parameters) {
        this.xLog = inputLog;
        this.classifier = parameters.getClassifier();
        this.addUniqueStartEnd();
//		this.transitions = getTransitionsFromLog(parameters);
//		this.transitionOccurences=computeTransitionsOccurences(parameters.getClassifier()); // TODO compute only when needed
//		this.transitionAverageIndices=computeAverageTransitionIndices(parameters.getClassifier()); // TODO compute only when needed
        this.transitions = computeInTransitionOrder(parameters); // also sets the necessary start and end indices
        this.outTransitions = computeOutTransitionOrder(parameters); // also sets the necessary start and end indices
        this.outTransitionMapping = computeOutTransitionMapping(transitions, outTransitions);
        Object[] logObject = computeFinalLogObjects(new CountedPLog(plog));    //computes the list of trace variants and the hasmap of their frequencies
        // transitions are converted to integers according to their position in the (ingoing) transitions array
        //stores the results direcly for the log object
        this.traceVariants = (ArrayList<ArrayList<Integer>>) logObject[0];
        this.traceVariantCounts = (HashMap<ArrayList<Integer>, Integer>) logObject[1];
    }

    public ESTLog(XLog inputLog, CountedPLog countedPLog, Parameters parameters) {
        this.xLog = inputLog;
        this.classifier = parameters.getClassifier();
        this.addUniqueStartEnd();
        this.transitions = computeInTransitionOrder(parameters); // also sets the necessary start and end indices
        this.outTransitions = computeOutTransitionOrder(parameters); // also sets the necessary start and end indices
        this.outTransitionMapping = computeOutTransitionMapping(transitions, outTransitions);
        Object[] logObject = computeFinalLogObjects(countedPLog);    //computes the list of trace variants and the hasmap of their frequencies
        this.traceVariants = (ArrayList<ArrayList<Integer>>) logObject[0];
        this.traceVariantCounts = (HashMap<ArrayList<Integer>, Integer>) logObject[1];
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


//    //returns a XLog containig all remaining traces (simlpyfied)
//    public XLog createXLog(boolean[] variantVector) {
//        ArrayList<ArrayList<Integer>> variants = getReducedTraceVariants(variantVector);
//        XLogBuilder builder = new XLogBuilder();
//        builder.startLog("ReducedLog");
//        int traceCounter = 0;
//        for (ArrayList<Integer> traceVariant : variants) {
//            int variantFrequency = traceVariantCounts.get(traceVariant);
//            for (int i = 0; i < variantFrequency; i++) {
//                traceCounter++;
//                builder.addTrace("Trace " + traceCounter);
//                for (Integer event : traceVariant) {
//                    builder.addEvent(transitions[event]);
//                }
//            }
//        }
//        return builder.build();
//    }

    //used for computing best of worst alignments, i.e., shortest path through model
    public XLog createEmptyTraceLog() {
        XLogBuilder builder = new XLogBuilder();
        builder.startLog("emptyTraceLog");
        builder.addTrace("emptyTrace");
        return builder.build();
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
        for (int i = 0; i < transitionsLiveness.length; i++) {
            transitionsLiveness[i] = false;
        }
        //replay variants and set to true if occuring
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


    private String[] getTransitionsFromLog(Parameters parameters) {
        XEventClassifier classifier = parameters.getClassifier();
        ArrayList<String> transitionsList = new ArrayList<String>();
        for (XTrace trace : xLog) {
            for (XEvent event : trace) {
                String activityLabel = classifier.getClassIdentity(event);
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


    private void addUniqueStartEnd() {
        XFactory factory = XFactoryRegistry.instance().currentDefault();
        XAttributeMap startAttributes = factory.createAttributeMap();
        XAttributeMap endAttributes = factory.createAttributeMap();
        String startTransitionName = "ArtificialStart";
        String endTransitionName = "ArtificialEnd";
        startAttributes.put(XConceptExtension.KEY_NAME,
                factory.createAttributeLiteral(XConceptExtension.KEY_NAME, startTransitionName, XConceptExtension.instance()));
        endAttributes.put(XConceptExtension.KEY_NAME,
                factory.createAttributeLiteral(XConceptExtension.KEY_NAME, endTransitionName, XConceptExtension.instance()));
        final XEvent startActivity = factory.createEvent(startAttributes);
        final XEvent endActivity = factory.createEvent(endAttributes);
        for (XTrace trace : xLog) {
            trace.add(0, startActivity);
            trace.add(endActivity);
        }
        this.endActivity = endActivity;
        this.startActivity = startActivity;
    }


//----------initialization: compute basic log properties------------------------


    private HashMap<String, Integer> computeTransitionsOccurences(XEventClassifier classifier) {
        HashMap<String, Integer> transitionWeights = new HashMap<String, Integer>();
        for (XTrace trace : xLog) {
            for (XEvent event : trace) {
                String key = classifier.getClassIdentity(event);
                if (transitionWeights.containsKey(key)) {
                    int weight = transitionWeights.get(key);
                    transitionWeights.put(key, weight + 1);
                } else {
                    transitionWeights.put(key, 1);
                }
            }
        }
        return transitionWeights;
    }

    //MUST compute number of occurences in advance
    //returns the naive average indices of the activities. prone to errors when repeated activities exist
    private HashMap<String, Integer> computeAverageTransitionIndices(XEventClassifier classifier) {
        HashMap<String, Integer> average_indices = new HashMap<String, Integer>();
        //compute the sum of all indices
        for (XTrace trace : xLog) {
            for (int i = 0; i < trace.size(); i++) {
                XEvent event = trace.get(i);
                String key = classifier.getClassIdentity(event);
                if (average_indices.containsKey(key)) {
                    int index = average_indices.get(key);
                    average_indices.put(key, index + i);
                } else {
                    average_indices.put(key, i);
                }
            }
        }
        //use the activity weights (number of occurences) to compute the average indices
        for (String key : average_indices.keySet()) {
            int indexSum = average_indices.get(key);
            indexSum = indexSum / transitionOccurences.get(key);
            average_indices.put(key, indexSum);
        }
        return average_indices;
    }


    //compute directly-follow-scores: score(x,y)=#(x<y)/|x|*|y|
    public double[][] computeDFScores() {
        double[][] scores = new double[transitions.length][transitions.length];
        //TODO
        System.out.println("DFScores computation not implemented yet!!!");
        return scores;
    }


//----------initialization: compute transition orderings------------------------

    public int getLogSize() {
        return traceVariantCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    //randomly shuffles the transitions array
    private String[] shuffleTransitions(String[] transitions) {
        int index;
        String temp;
        Random random = new Random();
        for (int i = transitions.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = transitions[index];
            transitions[index] = transitions[i];
            transitions[i] = temp;
        }
        return transitions;
    }

    //returns the given array in reversed order
    private String[] reverseTransitions(String[] transitions) {
        String[] reversed_transitions = new String[transitions.length];
        for (int i = 0; i < transitions.length; i++) {
            reversed_transitions[transitions.length - 1 - i] = transitions[i];
        }
        return reversed_transitions;
    }

    private String[] sortLexicographically(String[] transitions) {
        Arrays.sort(transitions, (s1, s2) -> s1.compareTo(s2));
        return transitions;
    }

    //adapt if needed (currently randomized), endActivity should be at position 0 (for easy skipping)
    private String[] computeInTransitionOrder(Parameters parameters) {
        //place ordering strategy here:
        String[] intransitions = sortLexicographically(getTransitionsFromLog(parameters)); //ordering: random shuffle
        intransitions = moveEndActivityToPosZero(parameters, intransitions);
        return intransitions;
    }

    //adapt if needed (currently randomized), startActivity should be at position 0 (for easy skipping)
    private String[] computeOutTransitionOrder(Parameters parameters) {
        //place ordering strategy here:
        String[] outtransitions = sortLexicographically(transitions.clone());//ordering: random shuffle
        outtransitions = moveStartActivityToPosZero(parameters, outtransitions);
        return outtransitions;
    }

    //compute the mapping from outtransitions to in transitions
    private int[] computeOutTransitionMapping(String[] intransitions, String[] outtransitions) {
        int[] outTransitionMapping = new int[outtransitions.length];
        for (int i = 0; i < intransitions.length; i++) {
            for (int o = 0; o < outtransitions.length; o++) {
                if (intransitions[i] == outtransitions[o]) {
                    outTransitionMapping[o] = i;
                    break;
                }
            }
        }
        return outTransitionMapping;
    }

    //moves the end activity to position 0 for the intransitions
    private String[] moveEndActivityToPosZero(Parameters parameters, String[] intransitions) {
        this.inEndIndex = findEndIndex(parameters, intransitions);
        while (inEndIndex != 0) {
            String temp = intransitions[inEndIndex - 1];
            intransitions[inEndIndex - 1] = intransitions[inEndIndex];
            intransitions[inEndIndex] = temp;
            inEndIndex--;
        }
        return intransitions;
    }

    //moves the start activity to position 0 for the outtransitions
    private String[] moveStartActivityToPosZero(Parameters parameters, String[] outtransitions) {
        this.outStartIndex = findStartIndex(parameters, outtransitions);
        while (outStartIndex != 0) {
            String temp = outtransitions[outStartIndex - 1];
            outtransitions[outStartIndex - 1] = outtransitions[outStartIndex];
            outtransitions[outStartIndex] = temp;
            outStartIndex--;
        }
        return outtransitions;
    }

    // find end index in the given transition array
    public int findEndIndex(Parameters parameters, String[] transitions) {
        int endIndex = 0;
        for (int i = 0; i < transitions.length; i++) {
            if (parameters.getClassifier().getClassIdentity(endActivity).equals(transitions[i])) {
                endIndex = i;
                break;
            }
        }
        return endIndex;
    }

    //find start index in the given transition array
    public int findStartIndex(Parameters parameters, String[] transitions) {
        int startIndex = 0;
        for (int i = 0; i < transitions.length; i++) {
            if (parameters.getClassifier().getClassIdentity(startActivity).equals(transitions[i])) {
                startIndex = i;
                break;
            }
        }
        return startIndex;
    }

//---------- initialization: compute final log objects------------------------

    private Object[] computeFinalLogObjects(CountedPLog inputLog) {
//        ArrayList<ArrayList<Integer>> convertedLog = new ArrayList<>();
        HashMap<ArrayList<Integer>, Integer> frequencies = new HashMap<>();
        ArrayList<ArrayList<Integer>> traceVariants = new ArrayList<>();

        HashSet<Integer> variantPositions = inputLog.getVariantPositions();
        for (int i = 0; i < xLog.size(); i++) {
            if(variantPositions.contains(i)) {
                XTrace trace = xLog.get(i);
                ArrayList<Integer> convertedTrace = convertXtrace(trace);
                traceVariants.add(convertedTrace);
            }
        }
        this.numberOfTraces = xLog.size();

        for (int i = 0; i < traceVariants.size(); i++) {
            ArrayList<Integer> trace = traceVariants.get(i);
            PTrace pTrace = inputLog.getVariantLog().get(i);

            if (!frequencies.containsKey(trace)) {
                frequencies.put(trace, 0);
            }

            if (inputLog.getpTraceCounts().containsKey(pTrace)) {
                frequencies.put(trace, frequencies.get(trace) + inputLog.getpTraceCounts().get(pTrace));
            }
        }

        return new Object[]{traceVariants, frequencies};
    }


    private ArrayList<Integer> convertXtrace(XTrace xTrace) {
        ArrayList<Integer> result = new ArrayList<>();
        for (XEvent event : xTrace) {
            String transitionName = classifier.getClassIdentity(event);
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i].equals(transitionName)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }


    //_________________printing information_____________________________________

    public void printTransitionOrderings() {
        String inorder = "Inorder: ";
        String outorder = "Outorder: ";
        for (int i = 0; i < transitions.length; i++) {
            inorder = inorder + transitions[i] + ";  ";
            outorder = outorder + outTransitions[i] + ";  ";
        }
        System.out.println(inorder);
        System.out.println(outorder);
    }

    public void printBasicLogSummary() {
        System.out.println("Number of Traces: " + this.numberOfTraces + ", Unique Variants: " + this.traceVariants.size() + ", Number of Activities: " + transitions.length);
    }


//    private void printExtensiveLogSummary() {
//        String result = "Log overview: ";
//        for (int i = 0; i < this.traceVariants.size(); i++) {
//            result = result + "\n" + this.traceVariants.get(i).toString() + "\t" + this.getTraceVariantCounts().get(this.traceVariants.get(i));
//        }
//        System.out.println(result);
//    }


    //___________________getter & setter_____________________________________
    public String[] getInTransitions() {
        return this.transitions;
    }

    public String[] getOutTransitions() {
        return outTransitions;
    }

    public int[] getOutTransitionMapping() {
        return this.outTransitionMapping;
    }

    public XLog getxLog() {
        return xLog;
    }


    public ArrayList<ArrayList<Integer>> getTraceVariants() {
        return this.traceVariants;
    }


    public HashMap<ArrayList<Integer>, Integer> getTraceVariantCounts() {
        return this.traceVariantCounts;
    }

    public int getNumOfTraces() {
//        return this.numberOfTraces;
        return getLogSize();
    }


    public int getInEndIndex() {
        return this.inEndIndex;
    }


}
