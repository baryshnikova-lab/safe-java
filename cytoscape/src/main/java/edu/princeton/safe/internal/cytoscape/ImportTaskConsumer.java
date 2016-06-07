package edu.princeton.safe.internal.cytoscape;

import com.carrotsearch.hppc.LongIntMap;

import edu.princeton.safe.model.EnrichmentLandscape;

public interface ImportTaskConsumer {
    void consume(LongIntMap nodeMappings);

    void consume(EnrichmentLandscape landscape);
}
