package edu.princeton.safe.distance;

import edu.princeton.safe.NetworkProvider;

public class UnweightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    protected EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> 1;
    }
}
