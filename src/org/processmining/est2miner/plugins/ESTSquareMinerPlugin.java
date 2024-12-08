package org.processmining.est2miner.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.est2miner.algorithms.candidatetraversal.BFSCandidateTraverser;
import org.processmining.est2miner.algorithms.discovery.DeltaDiscovery;
import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;
import org.processmining.est2miner.algorithms.implicitplaceremoval.AbstractImplicitPlacesRemover;
import org.processmining.est2miner.algorithms.implicitplaceremoval.OptimizationBasedImplicitPlaceRemover;
import org.processmining.est2miner.algorithms.placecombination.BFSDeltaCombinator;
import org.processmining.est2miner.algorithms.placeevaluation.PlaceEvaluator;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.est2miner.models.PlugInStatistics;
import org.processmining.est2miner.dialogs.UIDialog;
import org.processmining.est2miner.models.eSTPluginStatistics;
import org.processmining.est2miner.models.preprocessing.CountedPLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.wizard.ListWizard;
import org.processmining.framework.util.ui.wizard.ProMWizardDisplay;
import org.processmining.framework.util.ui.wizard.ProMWizardStep;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.partialorder.models.dependency.PDependency;
import org.processmining.partialorder.models.dependency.PDependencyDataAware;
import org.processmining.partialorder.ptrace.model.PLog;
import org.processmining.partialorder.ptrace.model.PTrace;
import org.processmining.partialorder.ptrace.model.imp.PLogImp;
import org.processmining.partialorder.ptrace.model.imp.PTraceImp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Plugin(name = "eST2-Miner",
        parameterLabels = {"PLog", "Counted PLog", "Parameters"},
        returnLabels = {"Petri net", "Initial marking", "Final marking", "Run statistics"},
        returnTypes = {Petrinet.class, Marking.class, Marking.class, eSTPluginStatistics.class},
        categories = {PluginCategory.Discovery},
        keywords = {"Discovery"},
        help = "eST-Miner implementation for partial Order Logs.")
public class ESTSquareMinerPlugin {

    @PluginVariant(requiredParameterLabels = {0})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(UIPluginContext context, PLog inputLog) {
        System.out.println("_____________ eST - Miner ___________________________________________________________________________________");
        Parameters parameters = getGeneralParameters(context);

        return runDiscovery(inputLog, null, parameters, context);
    }

    @PluginVariant(requiredParameterLabels = {1, 2})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(PluginContext context, CountedPLog inputLog, Parameters parameters) {
        System.out.println("_____________ eST - Miner ___________________________________________________________________________________");

        return runDiscovery(inputLog.getPLog(), inputLog.getpTraceCounts(), parameters, context);
    }

    @PluginVariant(requiredParameterLabels = {0, 2})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "christian.rennert@rwth-aachen.de")
    public Object[] discover(PluginContext context, PLog inputLog, Parameters parameters) {
        System.out.println("_____________ eST - Miner ___________________________________________________________________________________");

        return runDiscovery(inputLog, null, parameters, context);
    }


    public Object[] runDiscovery(PLog plog, HashMap<PTrace, Integer> pTraceCounts, Parameters parameters, PluginContext context) {
        eSTPluginStatistics statistics = new eSTPluginStatistics(parameters.getThresholdTau(), parameters.getThresholdDelta(), parameters.getMax_depth(), parameters.getDeltaAdaptionSteepness());

        long startTime;
        long endTime;

        startTime = System.nanoTime();

        //taking a copy of the input log to work with
        XLog inputLogCopy = (XLog) plog.getXLog().clone();

        //Pre-process the Log (create usable log object)
        System.out.println("Preprocessing the Log...");
//        MyLog log = new MyLog(inputLogCopy, parameters);
        CountedPLog countedPLog;
        if (pTraceCounts != null) {
            countedPLog = new CountedPLog(plog, pTraceCounts);
        } else {
            countedPLog = new CountedPLog(plog);
        }

        ESTLog log = new ESTLog(inputLogCopy, countedPLog, parameters);

        PLogImp inputPLogCopy = new PLogImp(inputLogCopy);
        for (int i = 0; i < plog.size(); i++) {
            inputPLogCopy.add(new PTraceImp(inputLogCopy.get(i), i));

            PTrace partialOrderTrace = plog.get(i);
            for (PDependency dependency : partialOrderTrace.getDependencies()) {
                inputPLogCopy.get(i).addDependency(new PDependencyDataAware(dependency.getSource() + 1, dependency.getTarget() + 1), dependency.getSource() + 1, dependency.getTarget() + 1);
            }
        }

        // Adding dependencies to the artificial start and end transition
        for (PTrace pTrace : inputPLogCopy) {
            if (pTrace.size() > 0) {
                int newStartIndex = 0;
                int newEndIndex = pTrace.getTrace().size() - 1;

                for (Integer startIndex : pTrace.getStartEventIndices()) {
                    pTrace.addDependency(new PDependencyDataAware(newStartIndex, startIndex), newStartIndex, startIndex);
                }

                for (Integer endIndex : pTrace.getEndEventIndices()) {
                    pTrace.addDependency(new PDependencyDataAware(endIndex, newEndIndex), endIndex, newEndIndex);
                }
            } else if (pTrace.getTrace().size() == 3) {
                pTrace.addDependency(new PDependencyDataAware(0, 1), 0, 1);
                pTrace.addDependency(new PDependencyDataAware(1, 2), 1, 2);
            } else {
                pTrace.addDependency(new PDependencyDataAware(0, 1), 0, 1);
            }
        }


        final String[] transitions = log.getInTransitions();
        final int[] outTransitionsMapping = log.getOutTransitionMapping();

        endTime = System.nanoTime();

        System.out.println("Log processing time: " + (endTime - startTime) / 1000000 + "ms");
        statistics.setLogProcessTime(startTime, endTime);
        log.printBasicLogSummary();
        log.printTransitionOrderings();

        final int P_all = (int) (Math.pow(Math.pow(2, (transitions.length - 1)), 2) - 2 * Math.pow(2, (transitions.length - 1)) + 1);


        final ArrayList<ArrayList<Integer>> traceVariants = log.getTraceVariants();
        final HashMap<ArrayList<Integer>, Integer> traceVariantCounts = log.getTraceVariantCounts();

        //setting the number of traceVariants within the class MyPlace
        ESTPlace.setNumVariants(traceVariants.size());

        //Initialize Process Model (unique transitions and empty list of places)
        ESTProcessModel pM = new ESTProcessModel(new ArrayList<>(), transitions, traceVariants.size());

        //-------------Initialize "working classes"-------------------------------------------------------------------------------------------------------
        AbstractImplicitPlacesRemover IPRemover;
        IPRemover = new OptimizationBasedImplicitPlaceRemover(transitions, log);

        //select traverser based on chosen traversal strategy
        BFSCandidateTraverser candidates = new BFSCandidateTraverser(transitions, outTransitionsMapping, parameters);

        CountedPLog countedPLogAdapted;
        if (pTraceCounts != null) {
            HashMap<PTrace, Integer> pTraceCountsAdapted = new HashMap<>();
            for (int i = 0; i < plog.size(); i++) {
                pTraceCountsAdapted.put(inputPLogCopy.get(i), pTraceCounts.get(plog.get(i)));
            }

            countedPLogAdapted = new CountedPLog(inputPLogCopy, pTraceCountsAdapted);
        } else {
            countedPLogAdapted = new CountedPLog(inputPLogCopy, countedPLog.getVariantPositions(), countedPLog.computeVariantPositionCounts());
        }

        PlaceEvaluator evaluator = new PlaceEvaluator(pM, countedPLogAdapted, parameters.getThresholdTau());

        int tauAbsolute = (int) Math.ceil(parameters.getThresholdTau() * log.getNumOfTraces());
        int deltaAbsolute = (int) Math.floor(parameters.getThresholdDelta() * log.getNumOfTraces());
        int[] traceCountsArray = new int[traceVariantCounts.size()];

        for (int i = 0; i < traceVariants.size(); i++) {
            ArrayList<Integer> currentVariant = traceVariants.get(i);
            traceCountsArray[i] = traceVariantCounts.get(currentVariant);
        }

        BFSDeltaCombinator combinator = new BFSDeltaCombinator(parameters.getDeltaAdaptionSteepness(), tauAbsolute, parameters.getThresholdDelta(), deltaAbsolute, traceCountsArray, parameters.getMax_depth(), log);

        DeltaDiscovery discovery = new DeltaDiscovery(pM, transitions, candidates, evaluator, IPRemover, parameters, log, combinator);

        //---------------------Evaluating Places (the important part)---------------------------------------------------------------------

        startTime = System.nanoTime();
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Start adding places...");
        //Evaluating and possibly adding the candidate places
        discovery.start();
        try {
            discovery.join(Long.MAX_VALUE);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        if (discovery.isAlive()) {
            discovery.interrupt();
        }

        System.out.println("End of Discovery____________________________________________________________________________________________ \n");

        endTime = System.nanoTime();
        System.out.println("Discovery took: " + (endTime - startTime) / 1000000 + "ms");
        statistics.setDiscoveryTime(startTime, endTime);

        //---------------------------Preliminary Results----------------------------------------------------
        ESTProcessModel discoveredPM = discovery.getpM();
        System.out.println("Remaining Potential Places after Discovery: " + discoveredPM.getPotentialPlaces().size());
        PlugInStatistics.getInstance().setRemainingPotentialPlaces(discoveredPM.getPotentialPlaces().size());
        System.out.println("Discarded Places after Discovery: " + discoveredPM.getDiscardedPlaces().size());
        if (discoveredPM.getDiscardedPlaces().size() != PlugInStatistics.getInstance().getNumDiscardedPlaces()) {
            System.out.println("ERROR counting discared places!");
        }

        final int P_replayed = PlugInStatistics.getInstance().getNumFitting() + PlugInStatistics.getInstance().getNumUnfitting();
        final int cutoffP = (P_all - P_replayed);
        final double cutoffPPercentageP_all = ((cutoffP * 100.0) / P_all);

        //print results for debugging
        System.out.println("------------------------------------------------------------------------------------------------------");
        System.out.println("P-all: " + P_all);
        System.out.println("Number of checked unfitting Places (Tree Traversal): " + PlugInStatistics.getInstance().getNumUnfitting());
        System.out.println("P-fit (Tree Traversal): " + PlugInStatistics.getInstance().getNumFitting());
        System.out.println("Cutoff Places: " + cutoffP + " | " + cutoffPPercentageP_all + " % of P-all");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        discoveredPM.updateAndPrintStatus(log);
        System.out.println("-----------------------------------------------------------------------------------------------------");


        //--------------------------Post-Processing---------------------------------------------------------------------------------
        System.out.println("Start post-processing...");
        startTime = System.nanoTime();
        if (parameters.isRemoveImps()) {
            discoveredPM.updateAndPrintStatus(log);

            //final implicit places removal
            final long IPRemoveStart = System.currentTimeMillis();
            System.out.println("\n Number of places before final removing implicit places: " + discoveredPM.getPlaces().size());
            discoveredPM = IPRemover.removeAllIPs(discoveredPM);

            PlugInStatistics.getInstance().incTimeImpTest(System.currentTimeMillis() - IPRemoveStart);
            IPRemover = new OptimizationBasedImplicitPlaceRemover(transitions, log);
            discoveredPM = IPRemover.removeAllIPs(discoveredPM);
            System.out.println("Number of places after final removing implicit places: " + discoveredPM.getPlaces().size() + "\n");

            discoveredPM.printPlaceSummary(log);
        } else {
            System.out.println("No IP Removal - Model has " + discoveredPM.getPlaces().size() + " places.");
        }

        // Merging Self Loop Places - important to do AFTER IP Removal
        System.out.println("\n Merging self-loop places...");
        int numPlacesBeforeMerging = discoveredPM.getPlaces().size();
        discoveredPM = discoveredPM.mergeSelfLoopPlaces(log);
        discoveredPM.printPlaceSummary(log);
        PlugInStatistics.getInstance().setNumMergedPlaces(numPlacesBeforeMerging - pM.getPlaces().size());
        discoveredPM.updateAndPrintStatus(log);


        discoveredPM.updateAndPrintStatus(log);
        System.out.println("P-final: " + discoveredPM.getPlaces().size());
        System.out.println("-------------------------------------------------------------------------------------");


        //-----------------create Petrinet from Process Model-------------------------------------------------------------------------

        //compute transitions and places that remain used
        boolean[] transitionsLiveness = discoveredPM.getTransitionsLiveness();
        int numLiveTransitions = 0;
        int numDeadTransitions = 0;
        int startIndex = log.findStartIndex(parameters, transitions);
        int endIndex = log.getInEndIndex();
        StringBuilder isRemainingTransition = new StringBuilder();
        StringBuilder isNotRemainingTransition = new StringBuilder();
        for (int i = 0; i < transitionsLiveness.length; i++) {
            if (transitionsLiveness[i]) {
                isRemainingTransition.append(transitions[i]).append(", ");
                numLiveTransitions++;
            } else {
                isNotRemainingTransition.append(transitions[i]).append(", ");
                numDeadTransitions++;
            }
        }
        System.out.println(numLiveTransitions + " live transitions: " + isRemainingTransition);
        System.out.println(numDeadTransitions + " dead transitions: " + isNotRemainingTransition);

        //debugging: identify places with insufficient connections
        discoveredPM = removeDeadPlaces(discoveredPM, transitionsLiveness);

        endTime = System.nanoTime();
        System.out.println("Postprocessing took: " + (endTime - startTime) / 1000000 + "ms");
        statistics.setPostprocessingTime(startTime, endTime);

        //initialize PN
        Petrinet net = PetrinetFactory.newPetrinet("Process Model");
        Marking initial_marking = new Marking();
        Marking final_marking = new Marking();
        //add start place and add it to initial markings
        Place startP = net.addPlace("Start");
        initial_marking.add(startP);
        //add end place and add it to final markings
        Place endP = net.addPlace("End");
        final_marking.add(endP);


        //add transitions and connecting arcs for start and end place (do not add unused transitions)
        for (int i = 0; i < discoveredPM.getTransitions().length; i++) {
            if (transitionsLiveness[i]) {
                Transition t = net.addTransition(discoveredPM.getTransitions()[i]);
                if (i == startIndex) {
                    net.addArc(startP, t);
                    t.setInvisible(true);
                } else if (i == endIndex) {
                    net.addArc(t, endP);
                    t.setInvisible(true);
                }
            }
        }


        //add places from process model (
        final Collection<Transition> petriTransitions = net.getTransitions();
        for (ESTPlace myP : discoveredPM.getPlaces()) {
            Place newP = net.addPlace("");
            for (Transition t : petriTransitions) {
                if (getTransitionNames(myP.getInputTrKey(), transitions).contains(t.getLabel())) {
                    net.addArc(t, newP);
                }
                if (getTransitionNames(myP.getOutputTrKey(), transitions).contains(t.getLabel())) {
                    net.addArc(newP, t);
                }
            }
        }
        context.getConnectionManager().addConnection(new InitialMarkingConnection(net, initial_marking));
        context.getConnectionManager().addConnection(new FinalMarkingConnection(net, final_marking));

        System.out.println("Number of Arcs: " + net.getEdges().size());
        System.out.println("Number of Places: " + net.getPlaces().size());
        System.out.println("____________________________________________________________________________________");

        //reformat and print results
        System.out.println("____________________________________________________________________________________");
        PlugInStatistics.getInstance().printStatisticsToConsol();
        statistics.setPluginStatisticsString("Replayable traces: " + combinator.countFittingTraces(pM.getVariantVector()) + "\n" +
                "Cutoff Places: " + cutoffP + " | " + cutoffPPercentageP_all + " % of P-all" + "\n" +
                numLiveTransitions + " live transitions: " + isRemainingTransition + "\n" +
                numDeadTransitions + " dead transitions: " + isNotRemainingTransition + "\n" +
                PlugInStatistics.getInstance().getConsoleString());
        System.out.println("____________________________________________________________________________________");

        //return results
        PlugInStatistics.getInstance().resetStatistics();

        return new Object[]{net, initial_marking, final_marking, statistics};
    }


    //_____________________________END OF MAIN - Methods and Helper Functions__________________________________________________________


    private ESTProcessModel removeDeadPlaces(ESTProcessModel pM, boolean[] transitionsLiveness) {
        ArrayList<ESTPlace> livePlaces = pM.getPlaces();
        ArrayList<ESTPlace> deadPlaces = new ArrayList<>();
        for (ESTPlace place : pM.getPlaces()) {
            boolean isNotProperlyConnected = hasToFewLiveConnections(place, transitionsLiveness);
            if (isNotProperlyConnected) {
                System.out.println("Remove place with too few live transitions: " + place.toTransitionsString(pM.getTransitions()));
                deadPlaces.add(place);
            }
        }
        livePlaces.removeAll(deadPlaces);
        pM.setPlaces(livePlaces);
        return pM;
    }

    //returns true if the place does not have at least one live connection for ingoing and outgoing
    private boolean hasToFewLiveConnections(ESTPlace place, boolean[] transitionsLiveness) {
        ESTPlace clonedPlace = place.clone().removeDeadTransitions(transitionsLiveness);
        return Integer.bitCount(clonedPlace.getInputTrKey()) < 1 || Integer.bitCount(clonedPlace.getOutputTrKey()) < 1;
    }


    //------------------------------------ User Input Handling -----------------------------------------------------


    //get input independent general parameters from user
    public Parameters getGeneralParameters(UIPluginContext context) {
        UIDialog myMinerWizardStep = new UIDialog();
        List<ProMWizardStep<Parameters>> wizStepList = new ArrayList<>();
        wizStepList.add(myMinerWizardStep);
        ListWizard<Parameters> listWizard = new ListWizard<>(wizStepList);
        return ProMWizardDisplay.show(context, listWizard, new Parameters());
    }


    //--------------------- General Helper Functions ------------------------------------------------------------

    //returns a collection containing all transitions names from the given transitions array
    private Collection<String> getTransitionNames(final int key, final String[] transitions) {
        Collection<String> result = new ArrayList<String>();
        if (key > (Math.pow(2, transitions.length))) {
            return null;
        }
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) { //test key for ones
                result.add(transitions[i]);
            }
        }
        return result;
    }

    //for a given position in the transition array return the corresponding bitmask
    private int getMask(final int pos, final String[] transitions) {
        return 1 << (transitions.length - 1 - pos);
    }


    @Override
    public String toString() {
        return "eST-Miner (faster, with maximal depth)";
    }
}