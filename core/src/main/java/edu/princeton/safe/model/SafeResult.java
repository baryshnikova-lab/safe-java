package edu.princeton.safe.model;

import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;

public interface SafeResult {

    static final int TYPE_HIGHEST = 0;
    static final int TYPE_LOWEST = 1;

    double getMaximumDistanceThreshold();

    AnnotationProvider getAnnotationProvider();

    List<? extends Neighborhood> getNeighborhoods();

    NetworkProvider getNetworkProvider();

    DomainDetails getDomainDetails();

    boolean isTop(int attributeIndex,
                  int typeIndex);

    void setTop(int attributeIndex,
                int typeIndex,
                boolean isTop);

}
