package edu.princeton.safe;

import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public interface RestrictionMethod {
    boolean shouldInclude(SafeResult result,
                          Neighborhood neighborhood);
}
