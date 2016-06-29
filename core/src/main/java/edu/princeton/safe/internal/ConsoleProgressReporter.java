package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;

public class ConsoleProgressReporter implements ProgressReporter {

    @Override
    public void neighborhoodScore(int nodeIndex,
                                  int attributeIndex,
                                  double score) {

    }

    @Override
    public boolean supportsParallel() {
        return true;
    }

    @Override
    public void startNeighborhoodScore(NetworkProvider networkProvider,
                                       AnnotationProvider annotationProvider) {
    }

    @Override
    public void finishNeighborhoodScore() {
    }

    @Override
    public void finishNeighborhood(int nodeIndex) {
    }

    @Override
    public void startUnimodality(AnnotationProvider annotationProvider) {
    }

    @Override
    public void isUnimodal(int attributeIndex,
                           int typeIndex,
                           boolean isIncluded) {
    }

    @Override
    public void finishUnimodality() {
    }

    @Override
    public void setStatus(String format,
                          Object... parameters) {

        System.out.printf(format, parameters);
        System.out.println();
    }
}
