package edu.princeton.safe.internal.cytoscape.event;

@FunctionalInterface
public interface PresentationStateChangedListener {
    void set(boolean isClean);
}
