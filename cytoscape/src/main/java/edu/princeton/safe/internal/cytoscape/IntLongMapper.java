package edu.princeton.safe.internal.cytoscape;

@FunctionalInterface
public interface IntLongMapper {
    long map(int value);
}