package edu.princeton.safe.internal.distance;

import java.util.List;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;

public abstract class ShortestPathDistanceMetric implements DistanceMetric {

    abstract double getCost(NetworkProvider provider,
                            int fromNode,
                            int toNode);

    @Override
    public List<NodePair> computeDistances(NetworkProvider networkProvider) {
        // TODO Auto-generated method stub
        return null;
    }
}
