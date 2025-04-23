package org.processmining.est2miner.models.coreobjects;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.partialorder.models.dependency.PDependency;
import org.processmining.partialorder.models.dependency.PDependencyImp;
import org.processmining.partialorder.ptrace.model.PTrace;

import java.util.*;
import java.util.function.Consumer;

public class ESTPartialOrder extends DirectedSparseGraph<Integer, PDependency> implements Iterable<String> {
    private final ArrayList<String> activities;

    public ESTPartialOrder(ArrayList<String> activities) {
        this.activities = new ArrayList<>(activities);
    }

    public ESTPartialOrder(PTrace pTrace, XEventClassifier classifier) {
        LinkedList<String> activities = new LinkedList<>();

        activities.add("ArtificialStart");

        pTrace.getTrace().forEach(event -> activities.add(classifier.getClassIdentity(event)));

        activities.add("ArtificialEnd");

        this.activities = new ArrayList<>(activities);

        pTrace.getDependencies().forEach(dependency -> {
            int from = dependency.getSource() + 1;
            int to = dependency.getTarget() + 1;
            addEdge(new PDependencyImp(from, to), from, to);
        });

        HashSet<Integer> startActivityPos = new HashSet<>();

        for (Integer i : this.getVertices()) {
            if (this.getInEdges(i).isEmpty()) {
                startActivityPos.add(i);
            }
        }
        startActivityPos.remove(0);
        startActivityPos.remove(this.activities.size() - 1);

        startActivityPos.forEach(pos -> addEdge(new PDependencyImp(0, pos), 0, pos));

        HashSet<Integer> endActivityPos = new HashSet<>();

        for (Integer i : this.getVertices()) {
            if (this.getOutEdges(i).isEmpty()) {
                endActivityPos.add(i);
            }
        }
        endActivityPos.remove(this.activities.size() - 1);

        endActivityPos.forEach(pos -> addEdge(new PDependencyImp(pos, this.activities.size() - 1), pos, this.activities.size() - 1));
    }

    public Collection<Integer> getPredecessorIndices(int index) {
        Set<Integer> predecessors = new HashSet<>();
        if (this.getVertices().contains(index)) {
            for (PDependency in : this.getInEdges(index)) {
                predecessors.add(in.getSource());
            }
        }

        return predecessors;
    }

    public Collection<Integer> getSuccessorIndices(int index) {
        Set<Integer> successors = new HashSet<>();
        if (this.getVertices().contains(index)) {
            for (PDependency out : this.getOutEdges(index)) {
                successors.add(out.getTarget());
            }
        }

        return successors;
    }

    public Set<Integer> getStartEventIndices() {
        Set<Integer> result = new HashSet<>();

        for (Integer i : this.getVertices()) {
            if (this.getInEdges(i).isEmpty()) {
                result.add(i);
            }
        }

        return result;
    }

    public Set<Integer> getEndEventIndices() {
        Set<Integer> result = new HashSet<>();

        for (Integer i : this.getVertices()) {
            if (this.getOutEdges(i).isEmpty()) {
                result.add(i);
            }
        }

        return result;
    }

    public ArrayList<String> getActivities() {
        return activities;
    }

    public int size() {
        return activities.size();
    }

    public String get(int index) {
        return activities.get(index);
    }

    @Override
    public Iterator<String> iterator() {
        return activities.iterator();
    }

    @Override
    public void forEach(Consumer<? super String> action) {
        activities.forEach(action);
    }

    @Override
    public Spliterator<String> spliterator() {
        return activities.spliterator();
    }
}
