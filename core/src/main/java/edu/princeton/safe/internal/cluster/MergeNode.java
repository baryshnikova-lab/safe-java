package edu.princeton.safe.internal.cluster;

public class MergeNode extends BaseDendrogramNode {

    double dissimilarity;

    public MergeNode(DendrogramNode left,
                     DendrogramNode right,
                     double dissimilarity) {

        super(left, right);
        this.dissimilarity = dissimilarity;
    }

    public double getDissimilarity() {
        return dissimilarity;
    }
}
