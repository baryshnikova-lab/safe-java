package edu.princeton.safe.internal;

import java.util.List;

import edu.princeton.safe.model.FunctionalGroup;
import edu.princeton.safe.model.SafeResult;

public class DefaultSafeResult implements SafeResult {

    double maximumDistanceThreshold;
    List<DefaultNeighborhood> neighborhoods;
    List<FunctionalGroup> groups;

    public DefaultSafeResult() {
    }

    @Override
    public double getMaximumDistanceThreshold() {
        return maximumDistanceThreshold;
    }
}
