package edu.princeton.safe.distance;

import edu.princeton.safe.NetworkProvider;

public class UnweightedDistanceMetric extends ShortestPathDistanceMetric {

    public static final String ID = "unweighted";

    @Override
    protected EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> 1;
    }

    @Override
    public String getId() {
        return ID;
    }
}
