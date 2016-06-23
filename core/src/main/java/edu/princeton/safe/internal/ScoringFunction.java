package edu.princeton.safe.internal;

import edu.princeton.safe.model.Neighborhood;

@FunctionalInterface
public interface ScoringFunction {
    double get(Neighborhood neighborhood,
               int attributeIndex);
}