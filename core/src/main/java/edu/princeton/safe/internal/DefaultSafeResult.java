package edu.princeton.safe.internal;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.model.FunctionalGroup;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public class DefaultSafeResult implements SafeResult {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    double maximumDistanceThreshold;
    List<DefaultNeighborhood> neighborhoods;
    List<FunctionalGroup> groups;

    public DefaultSafeResult() {
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
}
