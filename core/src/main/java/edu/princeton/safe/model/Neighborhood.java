package edu.princeton.safe.model;

import java.util.function.IntConsumer;

import edu.princeton.safe.AnnotationProvider;

public interface Neighborhood {

    static final double LOG10P = -16;

    static double computeEnrichmentScore(double pValue) {
        return Math.min(-Math.log10(pValue), -LOG10P) / -LOG10P;
    }

    static double getEnrichmentThreshold(int totalAttributes) {
        return -Math.log10(0.05 / totalAttributes) / -LOG10P;
    }

    int getMemberCount();

    double getPValue(int attributeIndex);

    void setPValue(int attributeIndex,
                   double pValue);

    double getEnrichmentScore(int attributeIndex);

    void setHighest(boolean significant);

    boolean isHighest();

    void forEachMemberIndex(IntConsumer action);

    int getNodeIndex();

    void addMember(int index);

    int getMemberCountForAttribute(int attributeIndex,
                                   AnnotationProvider annotationProvider);

    double getNodeDistance(int nodeIndex);

    void setNodeDistance(int nodeIndex,
                         double distance);

    double getMemberDistance(int memberIndex);

    boolean isLowest();

    void setLowest(boolean significant);

}
