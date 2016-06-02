package edu.princeton.safe;

import edu.princeton.safe.model.EnrichmentLandscape;

public interface RestrictionMethod extends Identifiable {
    void applyRestriction(EnrichmentLandscape result);
}
