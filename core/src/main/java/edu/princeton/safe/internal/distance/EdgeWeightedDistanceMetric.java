package edu.princeton.safe.internal.distance;

import edu.princeton.safe.NetworkProvider;

public class EdgeWeightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u, int v) -> networkProvider.getWeight(u, v);
    }
    
}
