package edu.princeton.safe;

@FunctionalInterface
public interface FactoryMethod<T> {
    T create();
}