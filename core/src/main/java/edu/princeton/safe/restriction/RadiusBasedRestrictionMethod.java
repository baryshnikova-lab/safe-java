package edu.princeton.safe.restriction;

import edu.princeton.safe.internal.Util;
import edu.princeton.safe.model.SafeResult;

public class RadiusBasedRestrictionMethod extends DistanceBasedRestrictionMethod {

    double distancePercentile;

    public RadiusBasedRestrictionMethod(double distancePercentile) {
        this.distancePercentile = distancePercentile;
    }

    @Override
    protected boolean isIncluded(SafeResult result,
                                 double[] distances) {

        double radius = Util.percentile(distances, distancePercentile);
        return radius <= 2 * result.getMaximumDistanceThreshold();

    }
}
