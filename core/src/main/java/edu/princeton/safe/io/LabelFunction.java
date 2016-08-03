package edu.princeton.safe.io;

@FunctionalInterface
public interface LabelFunction {
    String get(int index);
}
