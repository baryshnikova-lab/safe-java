package edu.princeton.safe.internal.cytoscape.event;

import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface EventService {
    void notifyListeners(EnrichmentLandscape landscape);

    void addEnrichmentLandscapeListener(SetEnrichmentLandscapeListener listener);

    void notifyListeners(CompositeMap map);

    void addCompositeMapListener(SetCompositeMapListener listener);

    void addNodeSelectionChangedListener(NodeSelectionChangedListener listener);

    void notifyNodeSelectionChangedListeners(LongSet nodeSuids);

    void addPresentationStateChangedListener(PresentationStateChangedListener listener);

    void notifyPresentationStateChanged(boolean isClean);
}
