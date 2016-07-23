package edu.princeton.safe.internal.cytoscape;

@FunctionalInterface
public interface ExpansionChangeListener {
    void expansionChanged(boolean isExpanded);
}
