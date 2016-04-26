package edu.princeton.safe;

import java.util.List;

public interface DistanceMetric {

    <T extends Neighborhood> List<T> computeDistances(NetworkProvider networkProvider,
                                                      NeighborhoodFactory<T> neighborhoodFactory);

}
