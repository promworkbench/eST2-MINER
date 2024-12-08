package org.processmining.est2miner.models.coreobjects;


import java.util.HashSet;

public class ESTPlace {
    private static int numVariants;

    private int inputTrKey;
    private int outputTrKey;

    private int currentTokens;
    private int underfed;
    private boolean activated;
    private int activeKey;

    private boolean[] variantVector; //used to save which trace variants are fitting thhis place


    public ESTPlace(final int inputTrKey, final int outputTrKey) {
        this.inputTrKey = inputTrKey;
        this.outputTrKey = outputTrKey;
        this.activeKey = 0;
        currentTokens = 0;
        underfed = 0;
        activated = false;
        this.variantVector = new boolean[numVariants];
    }

    public ESTPlace() {
        inputTrKey = 0;
        outputTrKey = 0;
        currentTokens = 0;
        underfed = 0;
        activated = false;
        this.variantVector = new boolean[numVariants];
    }


    public ESTPlace clone() {
        int newUf = new Integer(getUnderfed());
        int newTokens = new Integer(getCurrentTokens());
        int newInKey = new Integer(getInputTrKey());
        int newOutKey = new Integer(getOutputTrKey());
        int newActiveKey = new Integer(getActiveKey());
        ESTPlace newP = new ESTPlace(newInKey, newOutKey);
        newP.setCurrentTokens(newTokens);
        newP.setUnderfed(newUf);
        newP.setActiveKey(newActiveKey);
        if (getActivated()) {
            newP.setActivated(true);
        }
        boolean[] newVariantVector = variantVector.clone();
        this.setVariantVector(newVariantVector);
        return newP;
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

    public String toBinaryString() {
        return "(" + Long.toBinaryString(getInputTrKey()) + "|" + Long.toBinaryString(getOutputTrKey()) + ")";
    }


    public void addToken() {
        currentTokens++;
    }

    public void consumeToken() {
        if (currentTokens == 0) {
            underfed++;
        } else {
            currentTokens--;
        }
    }

    public void activate() {
        activated = true;
    }

    public void fire(final int mask) {
        if ((getOutputTrKey() & mask) > 0) {//if place contains transition indicated by mask as output
            consumeToken();
            activate();
        }
        if ((getInputTrKey() & mask) > 0) {//if place contains transition indicated by mask as input
            addToken();
            activate();
        }
    }

    //only adds tokens, no activation
    public void producefire(int mask) {
        if ((getInputTrKey() & mask) > 0) {//if place contains transition indicated by mask as input
            currentTokens++;
        }
    }

    //only decreases tokens, no check of possible, no activation
    public void consumefire(int mask) {
        if ((getOutputTrKey() & mask) > 0) {//if place contains transition indicated by mask as output
            currentTokens--;
        }
    }

    //returns true if places are equal
    public boolean isEqual(ESTPlace place) {
        if (this.getInputTrKey() == place.getInputTrKey() && this.getOutputTrKey() == place.getOutputTrKey()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object place) {
        if (place == null) {
            return false;
        }
        if (this.getInputTrKey() == ((ESTPlace) place).getInputTrKey() && this.getOutputTrKey() == ((ESTPlace) place).getOutputTrKey()) {
            return true;
        }
        return false;
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
        result = result + getKeysTransitionNames(this.getInputTrKey(), transitions) + "|"
                + getKeysTransitionNames(this.getOutputTrKey(), transitions) + ")";
        return result;
    }

    //TODO can this be used to override the toSTring method somehow
    public String toString(final String[] transitions) {
        String result = "(";
        result = result + getKeysTransitionNames(this.getInputTrKey(), transitions) + "|"
                + getKeysTransitionNames(this.getOutputTrKey(), transitions) + ")";
        return result;
    }

    private String getKeysTransitionNames(int key, String[] transitions) {
        String result = "";
        for (int i = 0; i < transitions.length; i++) {
            if ((key & getMask(i, transitions)) > 0) {// test whether this transition is contained in given key
                result = result + transitions[i] + ",";
            }
        }
        return result;
    }

    //tests, whether given place is subplace of this
    public boolean isSubPlace(ESTPlace subP) {
        int subPinKey = subP.getInputTrKey();
        int subPoutKey = subP.getOutputTrKey();
        if ((subPinKey & this.getInputTrKey()) == subPinKey && (subPoutKey & this.getOutputTrKey()) == subPoutKey) {
            return true;
        }
        return false;
    }

    //G&S
    public int getInputTrKey() {
        return inputTrKey;
    }

    public int getOutputTrKey() {
        return outputTrKey;
    }

    public int getCurrentTokens() {
        return currentTokens;
    }

    public int getUnderfed() {
        return underfed;
    }

    public void setCurrentTokens(final int currentTokens) {
        this.currentTokens = currentTokens;
    }

    public void setUnderfed(final int underfed) {
        this.underfed = underfed;
    }

    public boolean getActivated() {
        return activated;
    }

    public void setActivated(final boolean activated) {
        this.activated = activated;
    }

    //return bitmask corresponding to position in the transition array
    private int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }

    //return bitmask corresponding to position in the transition array
    private int getMask(int position, int lengthOfArray) {
        return (1 << (lengthOfArray - 1 - position));
    }


    public static int getNumVariants() {
        return numVariants;
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

    //returns a mask with 1 where non-selfloop ingoing transitions are
    public int getNonLoopsInMask() {
        return (this.getInputTrKey() ^ this.getLoopsMask());
    }

    //returns a mask with 1 where non-selfloop outgoing transitions are
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

    public void setActiveKey(int activKey) {
        this.activeKey = activKey;
    }

    public int getSumOfTransitions() {
        int sum = 0;
        sum = sum + Integer.bitCount(this.getInputTrKey());
        sum = sum + Integer.bitCount(this.getOutputTrKey());
        return sum;
    }

    private void removeTransition(int i, int transitionsLength) {
        int mask = getMask(i, transitionsLength);
        this.inputTrKey = (inputTrKey | mask) ^ mask;
        this.outputTrKey = (outputTrKey | mask) ^ mask;
    }

    //removes the transitions with livebness = false
    public ESTPlace removeDeadTransitions(boolean[] transitionsLiveness) {
        for (int i = 0; i < transitionsLiveness.length; i++) {
            if (!transitionsLiveness[i]) {
                this.removeTransition(i, transitionsLiveness.length);
            }
        }
        return this;
    }

    public boolean isEqualWithoutDead(ESTPlace place, boolean[] transitionsLiveness) {
        ESTPlace p1 = this.clone();
        ESTPlace p2 = this.clone();
        p1.removeDeadTransitions(transitionsLiveness);
        p2.removeDeadTransitions(transitionsLiveness);
        return p1.isEqual(p2);
    }


}
