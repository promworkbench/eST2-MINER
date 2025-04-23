package org.processmining.est2miner.algorithms.implicitplaceremoval;

import org.processmining.est2miner.models.coreobjects.ESTLog;
import org.processmining.est2miner.models.coreobjects.ESTPlace;
import org.processmining.est2miner.models.coreobjects.ESTProcessModel;

import java.util.ArrayList;

public abstract class AbstractImplicitPlacesRemover {
    protected final String[] transitions;
    protected final ESTLog log;

    public AbstractImplicitPlacesRemover(String[] transitions, ESTLog log) {
        this.transitions = transitions;
        this.log = log;
    }


//________________ main methods to remove implicit places____________________________	


    //without repair

    //trys to remove all IPs in the given PM
    public abstract ESTProcessModel removeAllIPs(ESTProcessModel inputPM);

    //trys to remove all IPs in the given PM that are related to a specific given place
    public abstract ArrayList<ESTPlace> implicitRelatedToPlace(final ESTPlace specificPlace,
                                                               final ArrayList<ESTPlace> placesToCheck);


    //Utility Methods_______________________________________________________________________________________________

    //return bitmask corresponding to position in the transition array
    protected int getMask(final int position, final String[] transitions) {
        return (1 << (transitions.length - 1 - position));
    }


}
