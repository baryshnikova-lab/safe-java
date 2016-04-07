package edu.princeton.safe.internal.distance;

import edu.princeton.safe.NetworkProvider;

public class EdgeWeightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    double getCost(NetworkProvider provider, int fromNode, int toNode) {
        return provider.getWeight(fromNode, toNode);
    }

}
