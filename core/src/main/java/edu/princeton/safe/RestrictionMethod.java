package edu.princeton.safe;

import edu.princeton.safe.model.SafeResult;

@FunctionalInterface
public interface RestrictionMethod {

    void applyRestriction(SafeResult result);
}
