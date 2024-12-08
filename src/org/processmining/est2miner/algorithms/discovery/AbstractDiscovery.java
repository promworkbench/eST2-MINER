package org.processmining.est2miner.algorithms.discovery;


import org.processmining.est2miner.algorithms.candidatetraversal.AbstractCandidateTraverser;
import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTPlaceStatus;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;
import org.processmining.est2miner.algorithms.implicitplaceremoval.AbstractImplicitPlacesRemover;
import org.processmining.est2miner.algorithms.placeevaluation.PlaceEvaluator;
import org.processmining.est2miner.parameters.Parameters;

public abstract class AbstractDiscovery extends Thread {
    protected ESTProcessModel pM;
    protected final String[] transitions;
    protected final AbstractCandidateTraverser candidates;
    protected final PlaceEvaluator evaluator;
    protected final AbstractImplicitPlacesRemover IPRemover;
    protected final ESTLog log;
    protected final boolean removeImpsConcurrently;

    public AbstractDiscovery(final ESTProcessModel pM, String[] transitions, final AbstractCandidateTraverser candidates, final PlaceEvaluator evaluator, AbstractImplicitPlacesRemover ipRemover, Parameters parameters, ESTLog log) {
        this.pM = pM;
        this.transitions = transitions;
        this.candidates = candidates;
        this.evaluator = evaluator;
        this.IPRemover = ipRemover;
        this.log = log;
        this.removeImpsConcurrently = parameters.isRemoveImps();
    }


    //runs this thread until termination or interuption
    @Override
    public void run() {
        try {
            this.addPlaces();
            System.out.println("Finished adding places without interuption.");

        } catch (InterruptedException e) {
            System.out.println("Place Discovery interrupted!");
            throw new RuntimeException();
        }
    }


    //adds places according to the subclass strategy
    protected void addPlaces() throws InterruptedException {
        int currentTreeDepth = 0;
        ESTPlace current = candidates.getNext(null, ESTPlaceStatus.FIT);
        this.pM.updateAndPrintStatus(log);
        String[] transitions = log.getInTransitions();
        while (current != null) {
            if (!this.isInterrupted()) {

                //update current tree depth and, possibly, perform corresponding actions (otherwise for debugging only)
                int updatedTreeDepth = getCurrentTreeDepth(current);
                if (currentTreeDepth != updatedTreeDepth) {// if tree depth changed
                    //System.out.println("\n New tree level: changed from "+currentTreeDepth+ " to " + updatedTreeDepth); //for debugging
                    performNextTreeLevelActions(currentTreeDepth, updatedTreeDepth, current); //e.g., update current tree depth in place combinator
                    currentTreeDepth = updatedTreeDepth;
                }

                //-------------evaluating local fitness of current----------------------------
                ESTPlaceStatus fitness = evaluator.testPlace(current.getIngoingTransitionNameSet(transitions), current.getOutgoingTransitionNameSet(transitions), current);
                if (fitness == ESTPlaceStatus.FIT) {//dealing with locally fit places
                    this.handleLocallyFittingPlace(current);
                } else {// dealing with locally unfit places (currently: just ignore them)
                }

                //prepare next candidate iteration
                current = candidates.getNext(current, fitness);
            }//end of (non-interupted) current place adding iteration
            else {
                //handling interuption (timelimit)
                System.out.println("Timelimit for adding places has been reached (or other interuption).");
                break;
            }
        }//end of candidate traversal loop (interupted or finished)
        System.out.println("________________________End of Standard Place Evaluation________________________________________________________________________ \n");
        this.pM.updateAndPrintStatus(log);


        System.out.println("\n ______________________Perform end of discovery actions: ________________________________________________________________________ \n");
        pM = endOfDiscoveryActions(pM); //for delta discovery, this evaluates additional ('virtual') levels without adding further potentialplaces
        this.pM.updateAndPrintStatus(log);

        if (this.removeImpsConcurrently) {//if enabled, remove implicit places from current model
            pM = IPRemover.removeAllIPs(pM);
            this.pM.updateAndPrintStatus(log);
        }

        System.out.println("_______________________________Returning to main... ________________________________________________________________________ \n");
    }


    abstract protected ESTProcessModel endOfDiscoveryActions(ESTProcessModel pM);


    abstract protected void handleLocallyFittingPlace(ESTPlace current);

    abstract protected void performNextTreeLevelActions(int currentTreeDepth, int updatedTreeDepth, ESTPlace current);


    protected int getCurrentTreeDepth(ESTPlace current) {
        return (Integer.bitCount(current.getInputTrKey()) + Integer.bitCount(current.getOutputTrKey()));
    }

    public ESTProcessModel getpM() {
        return pM;
    }


}
