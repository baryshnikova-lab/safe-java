package edu.princeton.safe.restriction;

import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public class SubtractiveRestrictionMethod implements RestrictionMethod {

    @Override
    public boolean shouldInclude(SafeResult result,
                                 Neighborhood neighborhood) {
        throw new RuntimeException("Unimplemented");
    }

}
