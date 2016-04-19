package edu.princeton.safe;

import java.util.function.IntConsumer;

public interface Neighborhood {

    static final double LOG10P = -16;

    static double computeEnrichmentScore(double pValue) {
        return Math.min(-Math.log10(pValue), -LOG10P) / -LOG10P;
    }

    static double getEnrichmentThreshold(int totalAttributes) {
        return -Math.log10(0.05 / totalAttributes) / -LOG10P;
    }

    int getNodeCount();

    void setSignificance(int attributeIndex,
                         double pValue);

    double getEnrichmentScore(int attributeIndex);

    void forEachNodeIndex(IntConsumer action);

    int getNodeIndex();

    void addNode(int index);

    int getNodeCountForAttribute(int j,
                                 AnnotationProvider annotationProvider);
}
