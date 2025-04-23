package org.processmining.est2miner.models;

import java.util.HashMap;

//statistics should always be collected within the method generating them, not around the call
public class PlugInStatistics {

    private int numFittingPlaces = 0; //places  evaluated to be  fitting
    private int numUnfittingPlaces = 0; //places evaluated to be not fitting (not necessarily malfed)
    private int numMergedSelfLoopPlaces = 0;
    private int numCutPaths = 0; //number of cut paths (i.e., children not visited) due to malfedness (not tree depth) TODO is this used at all????

    private long timeCandidateTraversal = 0; //time needed to compute the next candidates
    private long timeCandidateEvaluation = 0; //time needed for place evaluation (mainly replay)
    private long timeImplicitnessTest = 0; //time needed for implicitness testing

    // statistics related to delta discovery
    int numAcceptedPlaces = 0; //places added to PM (even if immediately removed as implicit)
    int numDelayedPlaces = 0; //number of times a place is added to the potential places queue (counts the same place multiple times)
    int numDiscardedPlaces = 0; //places that have been added to the discarded queue
    int numSkippedPotentialPlaces = 0; //number of places removed from potential places queue when shortening
    int numRemainingPotentialPlaces = 0; //number of places remaining potential after end of discovery
    HashMap<Integer, HashMap<String, Integer>> levelStatistics = new HashMap<>(); // before a new tree level is investigated, take a snapshot of current relevant statistics


    //final model quality results
    private double precision = 0;

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

    public void resetStatistics() {
        instance = new PlugInStatistics();
    }


    public String getConsoleString() {
        String result = "\n \n _______________PlugInStatistics_____________";
        result = result + "\n Places evaluated - fitting: " + this.numFittingPlaces;
        result = result + "\n Places evaluated - unfitting: " + this.numUnfittingPlaces;
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
        result = result + "\n";
        return result;
    }

    //printing all results
    public void printStatisticsToConsole() {
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

    public void incDelayedPlaces(int num) {
        numDelayedPlaces = numDelayedPlaces + num;
    }

    public void incDiscardedPlaces(int num) {
        numDiscardedPlaces = numDiscardedPlaces + num;
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

    public void incTimeCandidateFinding(final long time) {
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

    public double getPrecision() {
        return precision;
    }


    public int getNumDelayedPlaces() {
        return numDelayedPlaces;
    }

    //setter

    public void setNumMergedPlaces(int numMergedPlaces) {
        this.numMergedSelfLoopPlaces = numMergedPlaces;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public void setRemainingPotentialPlaces(int num) {
        this.numRemainingPotentialPlaces = num;

    }
}
