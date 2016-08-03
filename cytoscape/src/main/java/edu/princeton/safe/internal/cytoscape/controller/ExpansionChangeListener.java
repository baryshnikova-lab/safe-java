package edu.princeton.safe.internal.cytoscape.controller;

@FunctionalInterface
public interface ExpansionChangeListener {
    void expansionChanged(boolean isExpanded);
}
