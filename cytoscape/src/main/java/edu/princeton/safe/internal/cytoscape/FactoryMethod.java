package edu.princeton.safe.internal.cytoscape;

@FunctionalInterface
public interface FactoryMethod<T> {
    T create();
}