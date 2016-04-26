package edu.princeton.safe.internal.distance;

import edu.princeton.safe.NetworkProvider;

public class MapBasedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> networkProvider.getDistance(u, v);
    }
}
