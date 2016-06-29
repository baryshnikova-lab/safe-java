package edu.princeton.safe.model;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.CompositeMapBuilder;
import edu.princeton.safe.NetworkProvider;

public interface EnrichmentLandscape {

    static final int TYPE_HIGHEST = 0;
    static final int TYPE_LOWEST = 1;

    double getMaximumDistanceThreshold();

    AnnotationProvider getAnnotationProvider();

    List<? extends Neighborhood> getNeighborhoods();

    NetworkProvider getNetworkProvider();

    CompositeMapBuilder getCompositeMapBuilder();

}
