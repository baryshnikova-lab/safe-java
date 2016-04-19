package edu.princeton.safe;

public interface ProgressReporter {
    void neighborhoodScore(int nodeIndex, int attributeIndex, double score);

    boolean supportsParallel();

    void startNeighborhoodScore(NetworkProvider networkProvider,
                                AnnotationProvider annotationProvider);

    void finishNeighborhoodScore();
}
