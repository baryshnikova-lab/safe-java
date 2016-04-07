package edu.princeton.safe.internal.distance;

import edu.princeton.safe.NetworkProvider;

public class UnweightedDistanceMetric extends ShortestPathDistanceMetric {

    @Override
    double getCost(NetworkProvider provider,
                   int fromNode,
                   int toNode) {
        return 1;
    }

}
