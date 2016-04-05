package edu.princeton.safe.internal;

import edu.princeton.safe.NodePair;

public class DefaultNodePair implements NodePair {
    int fromIndex;
    int toIndex;
    double distance;
    
    public DefaultNodePair(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public int getFromIndex() {
        return fromIndex;
    }

    @Override
    public int getToIndex() {
        return toIndex;
    }

    @Override
    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public double getDistance() {
        return distance;
    }
}