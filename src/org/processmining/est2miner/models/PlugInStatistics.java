package org.processmining.est2miner.models;

import java.util.HashMap;

//statistics should always be collceted within the method generating them, not around the call
public class PlugInStatistics {

    private int numFittingPlaces = 0; //places  evaluated to be  fitting
    private int numUnfittingPlaces = 0; //places evaluated to be not fitting (not necessarily malfed)
    private int numImplicitPlaces = 0; //number of places identified to be implicit (or dead, see delta)
    private int numMergedSelfLoopPlaces = 0;
    private int numCutPaths = 0; //number of cut paths (i.e., children not visited) due to malfedness (not tree depth) TODO is this used at all????

    private long timeCandidateTraversal = 0; //time needed to ompute the next candidates
    private long timeCandidateEvaluation = 0; //time needed for place evaluation (mainly replay)
    private long timeImplicitnessTest = 0; //time needed for implicitness testing

    // statistics related to replay based IP removal
    private int comparisons = 0;


    // statistics related to delta discovery
    int numAcceptedPlaces = 0; //places added to PM (even if immedeatly removed as implicit)
    int numDelayedPlaces = 0; //number of times a place is added to the potential places queue (counts the same place multiple times)
    int numDiscardedPlaces = 0; //places that have been added to the discarded queue
    int numSkippedPotentialPlaces = 0; //number of places removed from potential places queue when shortening
    int numRemainingPotentialPlaces = 0; //number of places remaining potential after end of discovery
    HashMap<Integer, HashMap<String, Integer>> levelStatistics = new HashMap<>(); // before a new tree level is investigated, take a snapshot of current relevant statistics


    //final model quality results
    private double precision = 0;
    private double alignmentBasedFitness = 0;
    private double binaryFitness = 0;
    private double variantFitness = 0;

    //singleton-pattern
    private PlugInStatistics() {
    }

    private static PlugInStatistics instance = null;

    public static PlugInStatistics getInstance() {
        if (instance == null) {
            instance = new PlugInStatistics();
        }
        return instance;
    }

    public PlugInStatistics resetStatistics() {
        instance = new PlugInStatistics();
        return instance;
    }


    public String getConsoleString() {
        String result = "\n \n _______________PlugInStatistics_____________";
        result = result + "\n Places evaluated - fitting: " + this.numFittingPlaces;
        result = result + "\n Places evaluated - unfitting: " + this.numUnfittingPlaces;
        result = result + "\n Places evaluated - implicit: " + (this.numImplicitPlaces);
        result = result + "\n Merged in self-loop places: " + (this.numMergedSelfLoopPlaces);
        result = result + "\n Time candidate traversal: " + this.timeCandidateTraversal;
        result = result + "\n Time candidate evaluation: " + this.timeCandidateEvaluation;
        result = result + "\n Time implicitness test: " + (this.timeImplicitnessTest);
        result = result + "\n Delta discovery - places accepted: " + this.numAcceptedPlaces;
        result = result + "\n Delta discovery - places discarded: " + this.numDiscardedPlaces;
        result = result + "\n Delta discovery - places delayed: " + this.numDelayedPlaces;
        result = result + "\n Delta discovery - potential places skipped: " + this.numSkippedPotentialPlaces;
        result = result + "\n Delta discovery - potential places remaining: " + this.numRemainingPotentialPlaces;
        result = result + "\n Precision (ETC): " + this.precision;
        result = result + "\n Fitness (alignment-based): " + this.alignmentBasedFitness;
        result = result + "\n Fraction of replayable traces: " + this.binaryFitness;
        result = result + "\n Fraction of replayable variants: " + this.variantFitness;
//		result = result+"\n Plausibility Check: final places = "+(this.numAcceptedPlaces-this.numImplicitPlaces-this.numRedundantPlaces-this.numMergedSelfLoopPlaces);
//		result = result+"\n Plausibility Check: 0 = "+(this.numFittingPlaces-this.numAcceptedPlaces-this.numSkippedPotentialPlaces-this.numDiscardedPlaces-this.numRemainingPotentialPlaces);
        result = result + "\n";
        return result;
    }

    //printing all results
    public void printStatisticsToConsol() {
        System.out.println(getConsoleString());
    }


    //used in delta discovery
    public void updateLevelStatistics(int currentTreeDepth, HashMap<String, Integer> currentLevelStatistics) {
        this.levelStatistics.put(currentTreeDepth, currentLevelStatistics);
    }


    //Increment num methods

    public void incAcceptedPlaces(int num) {
        numAcceptedPlaces = numAcceptedPlaces + num;
    }

    public void incImplicitPlaces(int num) {
        numImplicitPlaces = numImplicitPlaces + num;
    }

    public void incDelayedPlaces(int num) {
        numDelayedPlaces = numDelayedPlaces + num;
    }

    public void incDiscardedPlaces(int num) {
        numDiscardedPlaces = numDiscardedPlaces + num;
    }

    public void incSkippedPlaces(int num) {
        numSkippedPotentialPlaces = numSkippedPotentialPlaces + num;
    }

    public void incComparisons(int num) {
        this.comparisons = this.comparisons + num;
    }

    public void incNumUnfitting() {
        numUnfittingPlaces++;
    }

    public void incNumFitting() {
        numFittingPlaces++;
    }

    public void incNumCutPaths(int num) {
        numCutPaths = numCutPaths + num;
    }


    //Increment time methods

    public void incTimeCandFind(final long time) {
        timeCandidateTraversal = timeCandidateTraversal + time;
    }

    public void incTimeImpTest(final long time) {
        timeImplicitnessTest = timeImplicitnessTest + time;
    }

    public void incTimeEval(final long time) {
        this.timeCandidateEvaluation = timeCandidateEvaluation + time;
    }


    //Getter

    public int getNumFitting() {
        return numFittingPlaces;
    }

    public int getNumDiscardedPlaces() {
        return numDiscardedPlaces;
    }

    public int getNumUnfitting() {
        return numUnfittingPlaces;
    }

    public long getTimeEval() {
        return timeCandidateEvaluation;
    }

    public long getTimeCandFind() {
        return timeCandidateTraversal;
    }

    public int getNumCutPaths() {
        return numCutPaths;
    }

    public int getNumMergedSelfloopPlaces() {
        return numMergedSelfLoopPlaces;
    }

    public int getNumImpPlace() {
        return numImplicitPlaces;
    }

    public long getTimeImpTest() {
        return timeImplicitnessTest;
    }

    public int getComparisons() {
        return comparisons;
    }

    public double getPrecision() {
        return precision;
    }


    public int getNumDelayedPlaces() {
        return numDelayedPlaces;
    }

    public double getBinaryFitness() {
        return binaryFitness;
    }


    public double getAlignmentBasedFitness() {
        return alignmentBasedFitness;
    }

    public HashMap<Integer, HashMap<String, Integer>> getLevelStatistics() {
        return levelStatistics;
    }

    public double getVariantFitness() {
        return this.variantFitness;
    }


    //setter


    public void setNumMergedPlaces(int numMergedPlaces) {
        this.numMergedSelfLoopPlaces = numMergedPlaces;
    }


    public void setPrecision(double precision) {
        this.precision = precision;
    }


    public void setAlignmentBasedFitness(double alignmentBasedFitness) {
        this.alignmentBasedFitness = alignmentBasedFitness;
    }


    public void setBinaryFitness(double binaryFitness) {
        this.binaryFitness = binaryFitness;
    }

    public void setVariantFitness(double frac) {
        this.variantFitness = frac;

    }

    public void setRemainingPotentialPlaces(int num) {
        this.numRemainingPotentialPlaces = num;

    }


}
