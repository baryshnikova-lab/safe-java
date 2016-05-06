package edu.princeton.safe.restriction;

import java.util.stream.IntStream;

import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public class RadiusBasedRestrictionMethod implements RestrictionMethod {

    double distancePercentile;

    public RadiusBasedRestrictionMethod(double distancePercentile) {
        this.distancePercentile = distancePercentile;
    }

    @Override
    public boolean shouldInclude(SafeResult result,
                                 Neighborhood neighborhood) {
        double[] distances = IntStream.range(0, neighborhood.getMemberCount())
                                      .mapToDouble(i -> neighborhood.getMemberDistance(i))
                                      .filter(d -> d > 0)
                                      .toArray();
        double radius = Util.percentile(distances, distancePercentile);
        return radius <= 2 * result.getMaximumDistanceThreshold();
    }

}
