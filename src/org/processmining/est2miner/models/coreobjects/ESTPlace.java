package org.processmining.est2miner.models.coreobjects;


import org.processmining.framework.util.Pair;

import java.util.HashSet;

public class ESTPlace {
    private static int numVariants;

    private final int inputTrKey;
    private final int outputTrKey;

    private int activeKey;

    private boolean[] variantVector; //used to save which trace variants are fitting this place


    public ESTPlace(final int inputTrKey, final int outputTrKey) {
        this.inputTrKey = inputTrKey;
        this.outputTrKey = outputTrKey;
        this.activeKey = 0;
        this.variantVector = new boolean[numVariants];
    }

    public ESTPlace() {
        inputTrKey = 0;
        outputTrKey = 0;
        this.variantVector = new boolean[numVariants];
    }

    public void editVariantVector(int pos, boolean fitnessStatus) {
        variantVector[pos] = fitnessStatus;
    }

    public boolean[] getVariantVector() {
        return variantVector;
    }

    public void setVariantVector(boolean[] variantVector) {
        this.variantVector = variantVector;
    }

    public String toString() {
        return "(" + getInputTrKey() + "|" + getOutputTrKey() + ")";
    }

    @Override
    public boolean equals(Object place) {
        if (place == null || !place.getClass().equals(this.getClass())) {
            return false;
        }
        return this.getInputTrKey() == ((ESTPlace) place).getInputTrKey() && this.getOutputTrKey() == ((ESTPlace) place).getOutputTrKey();
    }

    public HashSet<String> getIngoingTransitionNameSet(final String[] transitions) {
        return getTransitionNameSet(inputTrKey, transitions);
    }

    public HashSet<String> getOutgoingTransitionNameSet(final String[] transitions) {
        return getTransitionNameSet(outputTrKey, transitions);
    }

    private HashSet<String> getTransitionNameSet(int key, final String[] transitions) {
        HashSet<String> result = new HashSet<>();

        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) {// test whether this transition is contained in given key
                result.add(transitions[i]);
            }
        }

        return result;
    }

    //returns a string version of the place with named transitions
    public String toTransitionsString(final String[] transitions) {
        String result = "(";
        result = result + getKeysTransitionNames(this.getInputTrKey(), transitions) + "|" + getKeysTransitionNames(this.getOutputTrKey(), transitions) + ")";
        return result;
    }

    public String toString(final String[] transitions) {
        String result = "(";
        result = result + getKeysTransitionNames(this.getInputTrKey(), transitions) + "|" + getKeysTransitionNames(this.getOutputTrKey(), transitions) + ")";
        return result;
    }

    private String getKeysTransitionNames(int key, String[] transitions) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) {// test whether this transition is contained in given key
                result.append(transitions[i]).append(",");
            }
        }
        return result.toString();
    }

    //G&S
    public int getInputTrKey() {
        return inputTrKey;
    }

    public int getOutputTrKey() {
        return outputTrKey;
    }

    //return bitmask corresponding to position in the transition array
    private int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }

    //return bitmask corresponding to position in the transition array
    private int getMask(int position, int lengthOfArray) {
        return (1 << (lengthOfArray - 1 - position));
    }

    public static void setNumVariants(int numVariants) {
        ESTPlace.numVariants = numVariants;
    }

    public int getLevel() {
        return Integer.bitCount(this.getInputTrKey()) + Integer.bitCount(this.getOutputTrKey());
    }

    //returns number of locally replayable variants
    public int getLocalFitness() {
        int result = 0;
        for (int i = 0; i < this.getVariantVector().length; i++) {
            if (this.getVariantVector()[i]) {
                result++;
            }
        }
        return result;
    }

    //returns a mask with 1 where input and output are equal
    public int getLoopsMask() {
        return (this.getInputTrKey() & this.getOutputTrKey());
    }

    //returns a mask with 1 where non-self-loop ingoing transitions are
    public int getNonLoopsInMask() {
        return (this.getInputTrKey() ^ this.getLoopsMask());
    }

    //returns a mask with 1 where non-self-loop outgoing transitions are
    public int getNonLoopsOutMask() {
        return (this.getOutputTrKey() ^ this.getLoopsMask());
    }

    //merges two places by creating the union of ingoing and outgoing transitions
    public ESTPlace mergePlaces(ESTPlace place) {
        int newInkey = (this.getInputTrKey() | place.getInputTrKey());
        int newOutKey = (this.getOutputTrKey() | place.getOutputTrKey());
        boolean[] newVariantVector = this.getVariantVector();
        for (int i = 0; i < this.getVariantVector().length; i++) {
            if (place.getVariantVector()[i]) {
                newVariantVector[i] = true;
            }
        }
        ESTPlace result = new ESTPlace(newInkey, newOutKey);
        result.setVariantVector(newVariantVector);
        return result;
    }

    public int getActiveKey() {
        return activeKey;
    }

    public void setActiveKey(int activeKey) {
        this.activeKey = activeKey;
    }

    //removes the transitions with liveness = false
    public Pair<Integer, Integer> inOutKeyWithoutDeadTransitions(boolean[] transitionsLiveness) {
        int resultInputTrKey = inputTrKey;
        int resultOutputTrKey = outputTrKey;

        for (int i = 0; i < transitionsLiveness.length; i++) {
            if (!transitionsLiveness[i]) {
                int mask = getMask(i, transitionsLiveness.length);
                resultInputTrKey = (resultInputTrKey | mask) ^ mask;
                resultOutputTrKey = (resultOutputTrKey | mask) ^ mask;
            }
        }
        return new Pair<>(resultInputTrKey, resultOutputTrKey);
    }
}
