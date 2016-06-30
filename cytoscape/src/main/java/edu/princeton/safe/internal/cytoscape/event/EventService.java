package edu.princeton.safe.internal.cytoscape.event;

import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface EventService {
    void notifyListeners(EnrichmentLandscape landscape);

    void addEnrichmentLandscapeListener(SetEnrichmentLandscapeListener listener);

    void notifyListeners(CompositeMap map);

    void addCompositeMapListener(SetCompositeMapListener listener);

}
