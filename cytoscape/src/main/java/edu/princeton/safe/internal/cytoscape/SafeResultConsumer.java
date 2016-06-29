package edu.princeton.safe.internal.cytoscape;

import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface SafeResultConsumer {
    void acceptEnrichmentLandscape(EnrichmentLandscape landscape);

    void acceptCompositeMap(CompositeMap map);
}
