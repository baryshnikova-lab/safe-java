package edu.princeton.safe.internal.cytoscape.event;

import com.carrotsearch.hppc.LongSet;

@FunctionalInterface
public interface NodeSelectionChangedListener {
    void set(LongSet selectedNodeSuids);
}
