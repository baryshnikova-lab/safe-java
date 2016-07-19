package edu.princeton.safe.internal.cluster;

public abstract class BaseDendrogramNode implements DendrogramNode {

    DendrogramNode left;
    DendrogramNode right;
    int observationCount;

    BaseDendrogramNode(DendrogramNode left,
                       DendrogramNode right) {
        this.left = left;
        this.right = right;

        if (left != null) {
            observationCount += left.getObservationCount();
        }

        if (right != null) {
            observationCount += right.getObservationCount();
        }

        if (left == null && right == null) {
            observationCount = 1;
        }
    }

    @Override
    public DendrogramNode getLeft() {
        return left;
    }

    @Override
    public DendrogramNode getRight() {
        return right;
    }

    @Override
    public int getObservationCount() {
        return observationCount;
    }
}
