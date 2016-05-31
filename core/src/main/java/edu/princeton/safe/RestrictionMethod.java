package edu.princeton.safe;

import edu.princeton.safe.model.EnrichmentLandscape;

@FunctionalInterface
public interface RestrictionMethod {

    void applyRestriction(EnrichmentLandscape result);
}
