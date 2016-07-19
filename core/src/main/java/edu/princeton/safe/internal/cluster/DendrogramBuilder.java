package edu.princeton.safe.internal.cluster;

import edu.princeton.safe.internal.fastcluster.LinkageConsumer;

public class DendrogramBuilder implements LinkageConsumer {

    int totalObservations;
    DendrogramNode root;
    DendrogramNode[] nodes;
    int nextIndex;

    public DendrogramBuilder(int totalObservations) {
        this.totalObservations = totalObservations;
        nodes = new DendrogramNode[totalObservations - 1];
    }

    @Override
    public void accept(int node1,
                       int node2,
                       double dissimilarity) {

        DendrogramNode left = getObservation(node1);
        DendrogramNode right = getObservation(node2);
        root = new MergeNode(left, right, dissimilarity);
        nodes[nextIndex] = root;
        nextIndex++;
    }

    DendrogramNode getObservation(int index) {
        if (index < totalObservations) {
            return new ObservationNode(index);
        } else {
            return nodes[index - totalObservations];
        }
    }

    public DendrogramNode getRoot() {
        return root;
    }

}
