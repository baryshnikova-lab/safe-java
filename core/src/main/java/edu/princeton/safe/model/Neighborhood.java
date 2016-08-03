package edu.princeton.safe.model;

import java.util.function.IntConsumer;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.SignificancePredicate;

public interface Neighborhood {

    static final double LOG10P = -16;

    static final ScoringFunction HIGHEST_SCORE = (n,
                                                  j) -> n.getEnrichmentScore(j);

    static final ScoringFunction LOWEST_SCORE = (n,
                                                 j) -> computeEnrichmentScore(1 - n.getPValue(j));

    static double computeEnrichmentScore(double pValue) {
        return Math.min(-Math.log10(pValue), -LOG10P) / -LOG10P;
    }

    static double getEnrichmentThreshold(int totalAttributes) {
        return -Math.log10(0.05 / totalAttributes) / -LOG10P;
    }

    static SignificancePredicate getSignificancePredicate(int analysisType,
                                                          int totalAttributes) {
        double threshold = getEnrichmentThreshold(totalAttributes);
        ScoringFunction score = getScoringFunction(analysisType);
        return (n,
                j) -> score.get(n, j) > threshold;
    }

    static ScoringFunction getScoringFunction(int analysisType) {
        switch (analysisType) {
        case EnrichmentLandscape.TYPE_HIGHEST:
            return HIGHEST_SCORE;
        case EnrichmentLandscape.TYPE_LOWEST:
            return LOWEST_SCORE;
        default:
            throw new RuntimeException();
        }
    }

    int getMemberCount();

    double getPValue(int attributeIndex);

    void setPValue(int attributeIndex,
                   double pValue);

    double getEnrichmentScore(int attributeIndex);

    void forEachMemberIndex(IntConsumer action);

    int getNodeIndex();

    void addMember(int index);

    int getMemberCountForAttribute(int attributeIndex,
                                   AnnotationProvider annotationProvider);

    double getNodeDistance(int nodeIndex);

    void setNodeDistance(int nodeIndex,
                         double distance);

    double getMemberDistance(int memberIndex);

    int getMember(int memberIndex);

}
