package org.processmining.est2miner.algorithms.candidatetraversal;

import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTPlaceStatus;
import org.processmining.est2miner.parameters.Parameters;
import org.processmining.est2miner.models.PlugInStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;


/*
 * This class public method getNext() returns the next candidate place based on the given candidate place.
 */

//ASSUME THAT POS 0 OF TRANSITIONS IS END, POS 0 OF OUTMAPPING MAPS TO START
public abstract class AbstractCandidateTraverser {
    final protected String[] transitions; //array of transition names corresponding to inOrder
    protected int[] outTrMapping; //array of indices (of position in transitions array) linking to transition names corresponding to outorder
    protected int largestOutIndex;
    protected int largestInIndex;
    protected LinkedList<ESTPlace> roots;
    protected ESTPlace currentRoot;
    protected boolean limitDepth;
    protected int depthLimit;
//	protected String rootsPlacesNames; //for debugging


    //returns the next place based on the last place, additional information, and the chosen strategy (subclass).
    //returns null when all candidates have been considered/skipped
    public abstract ESTPlace getNext(ESTPlace lastP, ESTPlaceStatus fitness);

    //abstract class cannot be instantiated, constructor used by subclasses
    public AbstractCandidateTraverser(final String[] transitions, final int[] outTrMapping, Parameters parameters) {
        this.transitions = transitions;
        this.outTrMapping = outTrMapping;
        roots = computeBaseRoots();
        currentRoot = roots.removeFirst();
        this.largestOutIndex = getMappedTransitionIndex(outTrMapping.length - 1); //largest out index can be mapped anywhere in the transitions array
        this.largestInIndex = transitions.length - 1;
        this.limitDepth = true;
        this.depthLimit = parameters.getMax_depth();
    }


    //adds all base roots, that are places with only one in and one out transition (first level places)
    //assume in-index 0 = end, out-index 0 = start (transitions[0]=end, transitions[getTransitionIndex(0)]=start)
    protected LinkedList<ESTPlace> computeBaseRoots() {
        long startTime = System.currentTimeMillis();
        LinkedList<ESTPlace> baseRoots = new LinkedList<ESTPlace>();
        for (int in = 1; in < transitions.length; in++) {
            for (int out = 1; out < transitions.length; out++) {
//				if(!(transitions[in].equals(transitions[getTransitionIndex(0)]) && transitions[getTransitionIndex(out)].equals(transitions[0]))) {//exclude (start|end)
                baseRoots.add(new ESTPlace(getMask(in), getMask(getMappedTransitionIndex(out))));
//				}
            }
        }
        PlugInStatistics.getInstance().incTimeCandFind(System.currentTimeMillis() - startTime);
        return baseRoots;
    }


    //for debugging - returns the string describing the place set
    protected String placeSetToString(ArrayList<ESTPlace> placeSet) {
        String result = "";
        for (ESTPlace place : placeSet) {
            result = result + place.toTransitionsString(transitions);
        }
        return result;
    }


    //returns the index of the ingoing transition array corresponding to the given index of the out transition order
    protected int getMappedTransitionIndex(int outIndex) {
        return outTrMapping[outIndex];
    }


    //returns the index in the outgoing transition mapping array corresponding to the 'largest' (highest ordering, rightmost) transition in the given outkey
    protected int getLargestOutTrIndex(int outKey) {
        for (int i = outTrMapping.length - 1; i >= 0; i--) { //reverse iterate the out mapping and compare until transition is found
            if ((getMask(getMappedTransitionIndex(i)) & outKey) > 0) {
                return i;
            }
        }
        return 0;
    }

    //returns the index in the outgoing transition mapping array corresponding to the 'lowest' (lowest ordering, leftmost) transition in the given outkey
    protected int getLowestOutTrIndex(int outKey) {
        for (int i = 0; i < outTrMapping.length; i++) { // iterate the out mapping and compare until transition is found
            if ((getMask(getMappedTransitionIndex(i)) & outKey) > 0) {
                return i;
            }
        }
        return 0;
    }

    //returns the index in the (ingoing) transition array corresponding to the 'largest' (highest ordering, rightmost) transition in the given inkey
    protected int getLargestInTrIndex(int inKey) {
        for (int i = transitions.length - 1; i >= 0; i--) { //reverse iterate the transitions and compare until transition is found
            if ((getMask(i) & inKey) > 0) {
                return i;
            }
        }
        return 0;
    }


    //test whether this place is eligible for overfed pruning
    protected boolean hasSingleMaximalOutTransition(ESTPlace place) {
        return place.getOutputTrKey() == getMask(largestOutIndex);
    }

    // return current tree depth, i.e., the overall number of transitions
    protected int getCurrentDepth(ESTPlace place) {
        return Integer.bitCount(place.getInputTrKey()) + Integer.bitCount(place.getOutputTrKey());
    }

    //removes the current root from roots, and sets first element of roots as new current root
    protected ESTPlace getNextRoot() {
        if (!roots.isEmpty()) {
            currentRoot = roots.remove(0);
            return currentRoot;
        }
        return null;
    }

    //for a given position in the (ingoing) transition array return the corresponding bitmask
    public int getMask(final int pos) {
        return 1 << (transitions.length - 1 - pos);
    }

    //returns a collection containing all transitions names for the given key from the given transitions array
    public Collection<String> getTransitionNames(final int key, final String[] transitions) {
        Collection<String> result = new ArrayList<String>();
        if (key > (Math.pow(2, transitions.length))) {
            return null;
        }
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i)) > 0) { //test key for ones
                result.add(transitions[i]);
            }
        }
        return result;
    }


}