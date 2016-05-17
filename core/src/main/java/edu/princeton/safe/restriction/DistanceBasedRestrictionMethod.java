package edu.princeton.safe.restriction;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public abstract class DistanceBasedRestrictionMethod implements RestrictionMethod {

    protected abstract boolean isIncluded(SafeResult result,
                                          double[] distances);

    @Override
    public void applyRestriction(SafeResult result) {
        AnnotationProvider annotationProvider = result.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        double threshold = Neighborhood.getEnrichmentThreshold(totalAttributes);

        List<? extends Neighborhood> neighborhoods = result.getNeighborhoods();

        boolean isBinary = annotationProvider.isBinary();

        IntStream.range(0, totalAttributes)
                 .parallel()
                 .forEach(new IntConsumer() {
                     @Override
                     public void accept(int j) {
                         result.setTop(j, SafeResult.TYPE_HIGHEST,
                                            isIncluded(result, neighborhoods,
                                                       n -> n.getEnrichmentScore(j) > threshold));
                         if (!isBinary) {
                             result.setTop(j, SafeResult.TYPE_LOWEST,
                                                isIncluded(result, neighborhoods,
                                                           n -> Neighborhood.computeEnrichmentScore(1
                                                                   - n.getPValue(j)) > threshold));
                         }
                     }
                 });
    }

    boolean isIncluded(SafeResult result,
                       List<? extends Neighborhood> neighborhoods,
                       Predicate<Neighborhood> filter) {
        int[] nodes = neighborhoods.stream()
                                   .filter(filter)
                                   .mapToInt(n -> n.getNodeIndex())
                                   .toArray();

        double[] distances = new double[nodes.length * nodes.length];
        int i = 0;
        for (int n = 0; n < nodes.length; n++) {
            for (int m = 0; m < nodes.length; m++) {
                distances[i] = neighborhoods.get(n)
                                            .getNodeDistance(m);
            }
        }
        Arrays.sort(distances);
        return isIncluded(result, distances);

    }
}
