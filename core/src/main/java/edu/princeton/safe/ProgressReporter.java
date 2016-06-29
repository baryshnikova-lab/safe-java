package edu.princeton.safe;

public interface ProgressReporter {
    void neighborhoodScore(int nodeIndex,
                           int attributeIndex,
                           double score);

    boolean supportsParallel();

    void startNeighborhoodScore(NetworkProvider networkProvider,
                                AnnotationProvider annotationProvider);

    void finishNeighborhoodScore();

    void finishNeighborhood(int nodeIndex);

    void isUnimodal(int attributeIndex,
                    int typeIndex,
                    boolean isIncluded);

    void setStatus(String format,
                   Object... parameters);

    void startUnimodality(AnnotationProvider annotationProvider);

    void finishUnimodality();
}
