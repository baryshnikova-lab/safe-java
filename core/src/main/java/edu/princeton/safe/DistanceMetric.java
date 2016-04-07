package edu.princeton.safe;

import java.util.List;

public interface DistanceMetric {

    List<NodePair> computeDistances(NetworkProvider networkProvider);

}
