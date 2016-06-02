package edu.princeton.safe.distance;

import edu.princeton.safe.NetworkProvider;

public class EdgeWeightedDistanceMetric extends ShortestPathDistanceMetric {

    public static final String ID = "edge";

    @Override
    protected EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> networkProvider.getWeight(u, v);
    }

    @Override
    public String getId() {
        return ID;
    }

}
