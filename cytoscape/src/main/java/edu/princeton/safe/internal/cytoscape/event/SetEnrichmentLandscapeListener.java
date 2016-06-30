package edu.princeton.safe.internal.cytoscape.event;

import edu.princeton.safe.model.EnrichmentLandscape;

@FunctionalInterface
public interface SetEnrichmentLandscapeListener {
    void set(EnrichmentLandscape landscape);
}
