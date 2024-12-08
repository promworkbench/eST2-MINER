package org.processmining.est2miner.algorithms.placecombination;

import org.processmining.est2miner.models.coreobjects.ESTPlace;

import java.util.Comparator;

//places with few transitions are ordered first
public class PlaceComparatorNumOfTransitions implements Comparator<ESTPlace> {
	
	//sorts places based on number of transition
	public int compare(ESTPlace p1, ESTPlace p2) {
		int p1depth = Integer.bitCount(p1.getInputTrKey())+Integer.bitCount(p1.getOutputTrKey());
		int p2depth = Integer.bitCount(p2.getInputTrKey())+Integer.bitCount(p2.getOutputTrKey());
		if(p1depth<p2depth) {
			return -1;
		}
		if(p1depth>p2depth) {
			return 1;
		}
		return 0;
	}



}
