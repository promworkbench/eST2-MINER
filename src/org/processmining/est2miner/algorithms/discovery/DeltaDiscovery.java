package org.processmining.est2miner.algorithms.discovery;

import org.processmining.est2miner.algorithms.candidatetraversal.AbstractCandidateTraverser;
import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;
import org.processmining.est2miner.algorithms.implicitplaceremoval.AbstractImplicitPlacesRemover;
import org.processmining.est2miner.algorithms.placecombination.BFSDeltaCombinator;
import org.processmining.est2miner.algorithms.placeevaluation.PlaceEvaluator;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.est2miner.models.PlugInStatistics;

import java.util.ArrayList;
import java.util.HashMap;

public class DeltaDiscovery extends AbstractDiscovery {
    // discovery using the threshold delta and the potential traces queue to ensure minimal global fitness

    private final BFSDeltaCombinator combinator; //only combinator is BFS, this discovery is tailored towards BFS


    public DeltaDiscovery(final ESTProcessModel pM, String[] transitions, final AbstractCandidateTraverser candidates,
                          final PlaceEvaluator evaluator, final AbstractImplicitPlacesRemover ipRemover,
                          final Parameters parameters, ESTLog log, final BFSDeltaCombinator combinator) {
        super(pM, transitions, candidates, evaluator, ipRemover, parameters, log);
        this.combinator = combinator;
    }


    protected void performNextTreeLevelActions(int currentTreeDepth, int updatedTreeDepth, ESTPlace currentPlace) {
        System.out.println("\n New tree level: changed from " + currentTreeDepth + " to " + updatedTreeDepth); //for debugging
        combinator.setCurrentDepth(updatedTreeDepth); //update combinator
        //trigger retesting of potential places queue & trigger IP removal, if concurrent IP removal is set
        System.out.println("\n ___________DeltaDiscovery: Trigger next tree level actions!______________");
        pM.updateAndPrintStatus(log);
        System.out.println("Revisit potential places queue...");
        pM = combinator.revisitQueueOfPlaces(pM); //update statistics in combinator (accept, discard, delay)
        pM.updateAndPrintStatus(log);
        if (this.removeImpsConcurrently) {//if enabled, remove implicit places from current model
//			System.out.println("Removing all currently implicit places...");
            pM = IPRemover.removeAllIPs(pM);
            System.out.println("Places after intermediate IP removal: " + pM.getPlaces().size());
            pM.updateAndPrintStatus(log);
        }
        endOfLevelUpdateStatistics(currentTreeDepth);
        System.out.println("_____________end of next level actions______________ \n");
    }


    private void endOfLevelUpdateStatistics(int currentTreeDepth) {
        HashMap<String, Integer> currentLevelStatistics = new HashMap<String, Integer>();
        currentLevelStatistics.put("numPlaces", pM.getPlaces().size());
        currentLevelStatistics.put("numDeadTransitions", pM.getNumDeadTransitions());
        currentLevelStatistics.put("numLiveTraces", pM.getNumLiveTraces(log));
        currentLevelStatistics.put("numLiveVariants", pM.countLiveVariants());
        currentLevelStatistics.put("numDiscardedPlaces", pM.getDiscardedPlaces().size());
        currentLevelStatistics.put("numPotentialPlaces", pM.getPotentialPlaces().size());
        currentLevelStatistics.put("numIPs", PlugInStatistics.getInstance().getNumImpPlace());
        currentLevelStatistics.put("numDelayedPlaces", PlugInStatistics.getInstance().getNumDelayedPlaces());
        PlugInStatistics.getInstance().updateLevelStatistics(currentTreeDepth, currentLevelStatistics);

    }


    //triggered only, if fitness == MyPlaceStatus.FIT
    //use delta to determine global fitness
    protected void handleLocallyFittingPlace(ESTPlace current) {
        pM.updateStatus(log);
        Object[] globalFitnessStatus = combinator.combinePlace(pM.getVariantVector(), current);

        // adding this place to this PM will never be possible, discard
        if ((int) globalFitnessStatus[0] == -1) {// adding this place to this PM will never be possible, discard
            pM.getDiscardedPlaces().add(current); //for debugging and statistics
            PlugInStatistics.getInstance().incDiscardedPlaces(1);
        }
        //place may be addable later, add to potential places. sort&shorten potential places
        if ((int) globalFitnessStatus[0] == 0) {//place might be combinable with PM later
            ArrayList<ESTPlace> potentialPlaces = pM.getPotentialPlaces();
            potentialPlaces.add(current);
            PlugInStatistics.getInstance().incDelayedPlaces(1);
            pM.setPotentialPlaces(potentialPlaces);
        }
        //place can be added to PM now. If enabled, check for implicitness.
        if ((int) globalFitnessStatus[0] == 1) {
            PlugInStatistics.getInstance().incAcceptedPlaces(1);
            //recheck for implicitness with respect to newly added place (if concurrent implicitness check is turned on)
            if (removeImpsConcurrently) {
                ArrayList<ESTPlace> pMPlaces = new ArrayList<ESTPlace>(pM.getPlaces()); // does not contain current
                pM.addPlace(current);
                pM.updateStatus(log); //ensure reduced variants
                ArrayList<ESTPlace> implicitPlaces;

                implicitPlaces = IPRemover.implicitRelatedToPlace(current, pMPlaces);

                pMPlaces.add(current);//will be removed if implicit
                pMPlaces.removeAll(implicitPlaces);
                pM.setPlaces(pMPlaces);
                pM.updateStatus(log);
            } else {
                pM.addPlace(current);
                pM.updateStatus(log);
            }
        }
    }


    //empty potential places queue as much as possible
    protected ESTProcessModel endOfDiscoveryActions(ESTProcessModel pM) {
        pM.updateAndPrintStatus(log);
        //try virtual levels
        System.out.println("Revisit potential places using virtual tree levels...");
        int maxVirtualDepth = combinator.getMaxDepth();
        while (combinator.getCurrentDepth() <= maxVirtualDepth) {
            System.out.println("Current virtual depth: " + combinator.getCurrentDepth());
            pM = combinator.revisitQueueOfPlaces(pM);//update statistics in combinator (accept, discard, delay)
            pM.updateAndPrintStatus(log);
            if (removeImpsConcurrently) {
                pM = IPRemover.removeAllIPs(pM);
            }
            endOfLevelUpdateStatistics(combinator.getCurrentDepth());
            combinator.setCurrentDepth(combinator.getCurrentDepth() + 1);
        }
        pM.updateAndPrintStatus(log);
        if (removeImpsConcurrently) {
            pM = IPRemover.removeAllIPs(pM);
        }
        pM.updateAndPrintStatus(log);
        return pM;
    }


}
	
	
