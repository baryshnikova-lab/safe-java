package edu.princeton.safe.distance;

import edu.princeton.safe.NetworkProvider;

public class EdgeWeightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    protected EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> networkProvider.getWeight(u, v);
    }

}
