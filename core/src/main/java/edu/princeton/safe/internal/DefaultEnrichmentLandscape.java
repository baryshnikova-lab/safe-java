package edu.princeton.safe.internal;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.CompositeMapBuilder;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public class DefaultEnrichmentLandscape implements EnrichmentLandscape {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    double maximumDistanceThreshold;
    List<DefaultNeighborhood> neighborhoods;

    public DefaultEnrichmentLandscape(AnnotationProvider annotationProvider,
                                      int totalTypes) {
        this.annotationProvider = annotationProvider;
    }

    @Override
    public double getMaximumDistanceThreshold() {
        return maximumDistanceThreshold;
    }

    @Override
    public NetworkProvider getNetworkProvider() {
        return networkProvider;
    }

    @Override
    public AnnotationProvider getAnnotationProvider() {
        return annotationProvider;
    }

    @Override
    public List<? extends Neighborhood> getNeighborhoods() {
        return neighborhoods;
    }

    @Override
    public CompositeMapBuilder getCompositeMapBuilder() {
        return new DefaultCompositeMapBuilder(this);
    }
    
}
