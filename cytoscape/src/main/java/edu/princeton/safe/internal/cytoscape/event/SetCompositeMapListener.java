package edu.princeton.safe.internal.cytoscape.event;

import edu.princeton.safe.model.CompositeMap;

@FunctionalInterface
public interface SetCompositeMapListener {
    void set(CompositeMap map);
}
