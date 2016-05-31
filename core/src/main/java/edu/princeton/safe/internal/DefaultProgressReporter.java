package edu.princeton.safe.internal;

import java.util.LinkedList;
import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;

public class DefaultProgressReporter implements ProgressReporter {

    List<ProgressReporter> children;

    public DefaultProgressReporter() {
        children = new LinkedList<>();
    }

    @Override
    public void neighborhoodScore(int nodeIndex,
                                  int attributeIndex,
                                  double score) {
        children.stream()
                .forEach(c -> c.neighborhoodScore(nodeIndex, attributeIndex, score));
    }

    @Override
    public boolean supportsParallel() {
        return children.stream()
                       .allMatch(c -> c.supportsParallel());
    }

    @Override
    public void startNeighborhoodScore(NetworkProvider networkProvider,
                                       AnnotationProvider annotationProvider) {
        children.stream()
                .forEach(c -> c.startNeighborhoodScore(networkProvider, annotationProvider));
    }

    @Override
    public void finishNeighborhoodScore() {
        children.stream()
                .forEach(c -> c.finishNeighborhoodScore());
    }

    public void add(ProgressReporter reporter) {
        children.add(reporter);
    }

    @Override
    public void finishNeighborhood(int nodeIndex) {
    }

}
