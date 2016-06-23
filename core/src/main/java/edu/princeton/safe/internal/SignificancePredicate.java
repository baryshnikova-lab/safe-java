package edu.princeton.safe.internal;

import edu.princeton.safe.model.Neighborhood;

@FunctionalInterface
public interface SignificancePredicate {
    boolean test(Neighborhood neighborhood,
                 int attributeIndex);
}