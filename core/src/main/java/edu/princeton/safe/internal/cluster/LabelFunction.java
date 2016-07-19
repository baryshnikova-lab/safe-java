package edu.princeton.safe.internal.cluster;

@FunctionalInterface interface LabelFunction {
    String get(int index);
}