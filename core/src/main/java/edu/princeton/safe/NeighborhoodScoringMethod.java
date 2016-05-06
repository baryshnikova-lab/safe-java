package edu.princeton.safe;

import edu.princeton.safe.model.Neighborhood;

public interface NeighborhoodScoringMethod {
    double[] computeRandomizedScores(Neighborhood current,
                                     int attributeIndex);
}
