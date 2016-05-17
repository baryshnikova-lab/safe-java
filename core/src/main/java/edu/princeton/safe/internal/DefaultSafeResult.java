package edu.princeton.safe.internal;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.model.DomainDetails;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public class DefaultSafeResult implements SafeResult {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    double maximumDistanceThreshold;
    List<DefaultNeighborhood> neighborhoods;
    DomainDetails domains;

    boolean[][] isTop;

    public DefaultSafeResult(AnnotationProvider annotationProvider,
                             int totalTypes) {
        this.annotationProvider = annotationProvider;
        int totalAttributes = annotationProvider.getAttributeCount();
        isTop = new boolean[totalTypes][totalAttributes];
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
    public DomainDetails getDomainDetails() {
        return domains;
    }

    @Override
    public boolean isTop(int attributeIndex,
                         int typeIndex) {
        return isTop[typeIndex][attributeIndex];
    }

    @Override
    public void setTop(int attributeIndex,
                       int typeIndex,
                       boolean value) {
        isTop[typeIndex][attributeIndex] = value;
    }

}
