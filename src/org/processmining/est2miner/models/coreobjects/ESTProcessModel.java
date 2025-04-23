package org.processmining.est2miner.models.coreobjects;

import java.util.ArrayList;
import java.util.Arrays;

public class ESTProcessModel {
    private ArrayList<ESTPlace> places;
    private final String[] transitions;
    private boolean[] transitionsLiveness;
    private boolean[] variantVector; //used to save which trace variants are fitting this process model
    private ArrayList<ESTPlace> potentialPlaces;
    private final ArrayList<ESTPlace> discardedPlaces;

    public ESTProcessModel(final ArrayList<ESTPlace> places, final String[] transitions, int numVariants) {
        this.places = places;
        this.transitions = transitions;

        this.variantVector = new boolean[numVariants];
        for (int i = 0; i < numVariants; i++) {
            variantVector[i] = true; // in the beginning all traces are replayable
        }
        this.potentialPlaces = new ArrayList<>();
        this.discardedPlaces = new ArrayList<>();
        //initially all transitions are live
        this.transitionsLiveness = new boolean[transitions.length];
        for (int i = 0; i < transitions.length; i++) {
            this.transitionsLiveness[i] = true;
        }
    }


    //add the place to the PM and update the PM variant vector accordingly (intersection of PM and Place replayable variants)
    public void addPlace(final ESTPlace p) {
        places.add(p);
        boolean[] placeVariantVector = p.getVariantVector();
        for (int i = 0; i < placeVariantVector.length; i++) {
            if (!placeVariantVector[i]) {
                this.variantVector[i] = false;
            }
        }
    }


    //recompute and set PM variant vector based on the PM places variant vectors
    private boolean[] recomputeVariantVector() {
        boolean[] newVariantVector = new boolean[this.getVariantVector().length];
        Arrays.fill(newVariantVector, true);
        //compute new vector from places
        for (ESTPlace place : this.getPlaces()) {
            for (int i = 0; i < newVariantVector.length; i++) {
                if (!place.getVariantVector()[i]) {
                    newVariantVector[i] = false; //set to false if any place cannot replay this
                }
            }
        }
        this.setVariantVector(newVariantVector);
        return newVariantVector;
    }


    // printing & debugging and stuff

    //updates PM status and then prints overview (comment out if not debugging)
    public void updateAndPrintStatus(ESTLog log) {
        this.updateStatus(log);
    }


    //updates PM Variant vector based on place variant vectors
    //updates the places activated activities set
    //updates PM active transitions
    //compares to old notifies about issues
    public void updateStatus(ESTLog log) {
        //variant vector
        boolean[] oldVariantVector = this.getVariantVector();
        boolean[] newVariantVector = this.recomputeVariantVector();
        for (int i = 0; i < oldVariantVector.length; i++) {
            if (!oldVariantVector[i] && newVariantVector[i]) {
                this.variantVector[i] = false;
            }
        }
        //dead transitions
        this.transitionsLiveness = this.recomputeTransitionsLiveness(log);
        //place connections
        this.setActiveKeys();
    }

    //recomputes the live transitions based on this PM variant vector
    private boolean[] recomputeTransitionsLiveness(ESTLog log) {
        return log.getTransitionsLiveness(this.getVariantVector());
    }


    public void printPlaceSummary() {
        StringBuilder result = new StringBuilder("Current places in model (" + this.getPlaces().size() + "): \n depth \t #fitVarsP \t #activeTr \t transitions: ");
        for (ESTPlace place : this.getPlaces()) {
            result.append("\n").append(place.getLevel()).append("\t").append(place.getLocalFitness()).append("\t \t").append(Integer.bitCount(place.getActiveKey())).append("\t \t").append(place.toTransitionsString(transitions));
        }
        System.out.println(result);
    }


    //for debugging. sets the PM places active keys based on the transitionLiveness
    private void setActiveKeys() {
        boolean[] transitionsLiveness = this.getTransitionsLiveness();
        for (ESTPlace place : places) {
            int placeActiveKey = 0;
            for (int i = 0; i < transitionsLiveness.length; i++) {
                if (((getMask(i, transitions) & place.getInputTrKey())) > 0 || ((getMask(i, transitions) & place.getOutputTrKey()) > 0)) {//if the activity is connected to the place
                    if (transitionsLiveness[i]) {//if the transition is live
                        placeActiveKey = (placeActiveKey | getMask(i, transitions));//add the activity to the ones activated by place
                    }
                }
            }
            place.setActiveKey(placeActiveKey);
        }
    }


    //returns number of stored live transitions
    private int countLiveTransitions() {
        int sum = 0;
        for (boolean liveness : this.transitionsLiveness) {
            if (liveness) {
                sum++;
            }
        }
        return sum;
    }

    //counts the 'true' entries in this variant vector
    public int countLiveVariants() {
        int result = 0;
        for (boolean b : variantVector) {
            if (b) {
                result++;
            }
        }
        return result;
    }

    //removes all dead transitions from the given key
    public int removeDeadTransitions(int transitionKey) {
        for (int i = 0; i < this.getTransitionsLiveness().length; i++) {
            if (!this.getTransitionsLiveness()[i]) {
                int mask = getMask(i, transitions);
                transitionKey = (transitionKey | mask) ^ mask;
            }
        }
        return transitionKey;
    }


    //merges places, that have the same set of ingoing and outgoing transitions, except for selfloops
    public ESTProcessModel mergeSelfLoopPlaces(ESTLog log) {
        this.updateStatus(log);
        ArrayList<ESTPlace> result = new ArrayList<>();
        ArrayList<ESTPlace> placesToMerge = this.getPlaces();
        ArrayList<ESTPlace> remainingPlaces = new ArrayList<>();
        while (!placesToMerge.isEmpty()) {
            ESTPlace place1 = placesToMerge.remove(0);
            while (!placesToMerge.isEmpty()) {
                ESTPlace place2 = placesToMerge.remove(0);
                int place1NonLoopInMask = this.removeDeadTransitions(place1.getNonLoopsInMask()); //mask indictaing all non-self-loop in transitions
                int place1NonLoopOutMask = this.removeDeadTransitions(place1.getNonLoopsOutMask());
                int place2NonLoopInMask = this.removeDeadTransitions(place2.getNonLoopsInMask()); //mask indictaing all non-self-loop in transitions
                int place2NonLoopOutMask = this.removeDeadTransitions(place2.getNonLoopsOutMask());
                if ((place1NonLoopInMask == place2NonLoopInMask) && (place1NonLoopOutMask == place2NonLoopOutMask)) {//if the non-looping transitions are exactly the same
                    place1 = place1.mergePlaces(place2);//merge if possible
                } else {
                    remainingPlaces.add(place2);//if not mergeable, keep for next iteration
                }
            }
            placesToMerge = new ArrayList<>(remainingPlaces);
            remainingPlaces.clear();
            result.add(place1); // add the (possibly merged) place1
        }
        this.setPlaces(result);
        return this;
    }


    //G&S
    public ArrayList<ESTPlace> getPlaces() {
        return places;
    }

    public String[] getTransitions() {
        return transitions;
    }

    public void setPlaces(final ArrayList<ESTPlace> places) {
        this.places = places;
    }

    public boolean[] getVariantVector() {
        return variantVector;
    }

    public int getNumLiveTraces(ESTLog log) {
        return log.countLiveTraces(this.variantVector);
    }

    private void setVariantVector(boolean[] variantVector) {
        this.variantVector = variantVector;
    }

    public ArrayList<ESTPlace> getPotentialPlaces() {
        return potentialPlaces;
    }

    public void setPotentialPlaces(ArrayList<ESTPlace> potentialPlaces) {
        this.potentialPlaces = potentialPlaces;
    }

    public ArrayList<ESTPlace> getDiscardedPlaces() {
        return discardedPlaces;
    }

    public boolean[] getTransitionsLiveness() {
        return transitionsLiveness;
    }

    public int getNumDeadTransitions() {
        return (this.getTransitions().length - this.countLiveTransitions());
    }

    protected int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }
}
