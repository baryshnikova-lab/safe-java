package edu.princeton.safe.model;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;

public interface SafeResult {
    double getMaximumDistanceThreshold();

    AnnotationProvider getAnnotationProvider();

    List<? extends Neighborhood> getNeighborhoods();

    NetworkProvider getNetworkProvider();
}
