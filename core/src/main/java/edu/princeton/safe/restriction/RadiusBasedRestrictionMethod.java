package edu.princeton.safe.restriction;

import edu.princeton.safe.internal.Util;
import edu.princeton.safe.model.EnrichmentLandscape;

public class RadiusBasedRestrictionMethod extends DistanceBasedRestrictionMethod {

    public static final String ID = "radius";

    double distancePercentile;

    public RadiusBasedRestrictionMethod(int minimumLandscapeSize,
                                        double distancePercentile) {
        super(minimumLandscapeSize);
        this.distancePercentile = distancePercentile;
    }

    @Override
    protected boolean isIncluded(EnrichmentLandscape result,
                                 double[] distances) {

        double radius = Util.percentile(distances, distancePercentile);
        return radius <= 2 * result.getMaximumDistanceThreshold();

    }

    @Override
    public String getId() {
        return ID;
    }
}
