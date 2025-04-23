package org.processmining.est2miner.algorithms.candidatetraversal;

import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTPlaceStatus;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.est2miner.models.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;


/*
 * This class public method getNext() returns the next candidate place based on
 * the given candidate place.
 * It aims for a BFS search of the candidate tree by maintaining a queue of subtree roots: the first root is evaluated, and its children (next level)
 *  are either cut-off (ignored) or appended at the end of the queue. This results in the current level being evaluated first, before the next level is reached.
 */


//ASSUME THAT POS 0 OF TRANSITIONS IS END, POS 0 OF OUTMAPPING MAPS TO START
public class BFSCandidateTraverser extends AbstractCandidateTraverser {

    public BFSCandidateTraverser(final String[] transitions, final int[] outTrMapping, Parameters parameters) {
        super(transitions, outTrMapping, parameters);
    }


    //returns the next place based on the given last place and additional information
    //uses a tree structure ordered according to transitions array (ingoing) and mapping (outgoing)
    //Makes use of the class variables roots and current root to keep track of tree traversal
    //Cuts off uninteresting branches based on fitness, other criteria planned
    //For a given place, add valid (not cut-off) children to roots queue, then return the next place (roots queue ordering ensures level by level traversal)
    //Return null when queue is empty
    public ESTPlace getNext(ESTPlace lastP, ESTPlaceStatus fitness) {
        long startTime = System.currentTimeMillis();
        //case first place
        if (lastP == null) {
            PlugInStatistics.getInstance().incTimeCandidateFinding(System.currentTimeMillis() - startTime);
            return currentRoot;
        }
        addValidChildrenToQueue(lastP, fitness);
        ESTPlace nextP = getNextRoot();
        PlugInStatistics.getInstance().incTimeCandidateFinding(System.currentTimeMillis() - startTime);
        return nextP;
    }


    private void addValidChildrenToQueue(ESTPlace place, ESTPlaceStatus fitness) {
        ArrayList<ESTPlace> newChildren = new ArrayList<>();
        //do not add children if depth limit is reached
        if (!this.limitDepth || getCurrentDepth(place) != this.depthLimit) {
            if (fitness != ESTPlaceStatus.UNDERFED && fitness != ESTPlaceStatus.MALFED) {
                newChildren.addAll(getValidOutChildren(place));
            } else {//for statistics only
                PlugInStatistics.getInstance().incNumCutPaths(getValidOutChildren(place).size());
            }
            if ((fitness != ESTPlaceStatus.MALFED && fitness != ESTPlaceStatus.OVERFED) || !hasSingleMaximalOutTransition(place)) { //asymmetric pruning of overfed places
                newChildren.addAll(getValidInChildren(place));
            } else {//for statistics only
                PlugInStatistics.getInstance().incNumCutPaths(getValidInChildren(place).size());
            }
        }

        this.roots.addAll(newChildren);
    }


    //assume fitness and tree level have been tested, other criteria still need to be checked
    private Collection<? extends ESTPlace> getValidInChildren(ESTPlace place) {
        ArrayList<ESTPlace> inChildren = new ArrayList<>();
        if (Integer.bitCount(place.getOutputTrKey()) > 1) {
            return inChildren; //this place has no in transition children (more than one out transition)
        }
        int nextOutKey = place.getOutputTrKey();
        int lastInKey = place.getInputTrKey();
        int largestInIndex = getLargestInTrIndex(lastInKey);
        for (int i = largestInIndex + 1; i < transitions.length; i++) {
            int nextInKey = (lastInKey | getMask(i));
            inChildren.add(new ESTPlace(nextInKey, nextOutKey));
        }
        return inChildren;
    }

    //assume fitness and tree level have been tested, other criteria still need to be checked
    private Collection<? extends ESTPlace> getValidOutChildren(ESTPlace place) {
        ArrayList<ESTPlace> outChildren = new ArrayList<>();
        int nextInKey = place.getInputTrKey();
        int lastOutKey = place.getOutputTrKey();
        int largestOutMappingIndex = getLargestOutTrIndex(lastOutKey);
        for (int i = largestOutMappingIndex + 1; i < outTrMapping.length; i++) {
            int nextOutKey = (lastOutKey | getMask(getMappedTransitionIndex(i)));
            outChildren.add(new ESTPlace(nextInKey, nextOutKey));
        }
        return outChildren;
    }

}