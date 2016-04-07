package edu.princeton.safe.internal.distance;

import java.util.ArrayList;
import java.util.List;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;
import edu.princeton.safe.internal.DefaultNodePair;

public class MapBasedDistanceMetric implements DistanceMetric {

    @Override
    public List<NodePair> computeDistances(NetworkProvider networkProvider) {
        int totalNodes = networkProvider.getNodeCount();
    
        // Compute pair-wise distances and filter out NaNs.
        List<NodePair> distances = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            for (int j = 0; j < totalNodes; j++) {
                double distance = networkProvider.getDistance(i, j);
                if (!Double.isNaN(distance)) {
                    NodePair details = new DefaultNodePair(i, j);
                    details.setDistance(distance);
                    distances.add(details);
                }
            }
        }
        return distances;
    }

}
