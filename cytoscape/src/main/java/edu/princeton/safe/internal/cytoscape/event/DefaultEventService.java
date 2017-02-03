package edu.princeton.safe.internal.cytoscape.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNode;

import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class DefaultEventService implements EventService {

    Map<Class<?>, List<?>> listenersByClass;

    public DefaultEventService() {
        listenersByClass = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notifyListeners(EnrichmentLandscape landscape) {
        List<SetEnrichmentLandscapeListener> listeners = (List<SetEnrichmentLandscapeListener>) listenersByClass.get(EnrichmentLandscape.class);
        listeners.stream()
                 .forEach(l -> l.set(landscape));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addEnrichmentLandscapeListener(SetEnrichmentLandscapeListener listener) {
        List<SetEnrichmentLandscapeListener> listeners = (List<SetEnrichmentLandscapeListener>) listenersByClass.get(EnrichmentLandscape.class);
        if (listeners == null) {
            listeners = new ArrayList<>();
            listenersByClass.put(EnrichmentLandscape.class, listeners);
        }
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notifyListeners(CompositeMap map) {
        List<SetCompositeMapListener> listeners = (List<SetCompositeMapListener>) listenersByClass.get(CompositeMap.class);
        listeners.stream()
                 .forEach(l -> l.set(map));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addCompositeMapListener(SetCompositeMapListener listener) {
        List<SetCompositeMapListener> listeners = (List<SetCompositeMapListener>) listenersByClass.get(CompositeMap.class);
        if (listeners == null) {
            listeners = new ArrayList<>();
            listenersByClass.put(CompositeMap.class, listeners);
        }
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addNodeSelectionChangedListener(NodeSelectionChangedListener listener) {
        List<NodeSelectionChangedListener> listeners = (List<NodeSelectionChangedListener>) listenersByClass.get(CyNode.class);
        if (listeners == null) {
            listeners = new ArrayList<>();
            listenersByClass.put(CyNode.class, listeners);
        }
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notifyNodeSelectionChangedListeners(LongSet nodeSuids) {
        List<NodeSelectionChangedListener> listeners = (List<NodeSelectionChangedListener>) listenersByClass.get(CyNode.class);
        listeners.stream()
                 .forEach(l -> l.set(nodeSuids));
    }
}
