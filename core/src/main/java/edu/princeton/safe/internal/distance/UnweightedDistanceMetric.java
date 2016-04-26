package edu.princeton.safe.internal.distance;

import edu.princeton.safe.NetworkProvider;

public class UnweightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u, int v) -> 1;
    }
}
