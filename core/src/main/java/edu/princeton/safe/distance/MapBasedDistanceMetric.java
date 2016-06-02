package edu.princeton.safe.distance;

import edu.princeton.safe.NetworkProvider;

public class MapBasedDistanceMetric extends ShortestPathDistanceMetric {

    public static final String ID = "map";

    @Override
    protected EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider) {
        return (int u,
                int v) -> networkProvider.getDistance(u, v);
    }

    @Override
    public String getId() {
        return ID;
    }

}
