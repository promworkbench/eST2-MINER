package org.processmining.est2miner.models.coreobjects;

import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.partialorder.ptrace.model.PLog;

import java.util.ArrayList;

public class ESTPartialOrderLog extends ArrayList<ESTPartialOrder> {
    public ESTPartialOrderLog() {
        super();
    }

    public ESTPartialOrderLog(PLog log, XEventClassifier classifier) {
        super();
        log.forEach(pTrace -> add(new ESTPartialOrder(pTrace, classifier)));
    }
}
