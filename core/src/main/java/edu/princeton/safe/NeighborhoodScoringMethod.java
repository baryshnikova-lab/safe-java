package edu.princeton.safe;

public interface NeighborhoodScoringMethod {
    double[] computeRandomizedScores(Neighborhood current,
                                     int attributeIndex);
}
