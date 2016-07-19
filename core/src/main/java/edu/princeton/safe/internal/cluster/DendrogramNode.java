package edu.princeton.safe.internal.cluster;

public interface DendrogramNode {
    DendrogramNode getLeft();

    DendrogramNode getRight();

    int getObservationCount();
}
