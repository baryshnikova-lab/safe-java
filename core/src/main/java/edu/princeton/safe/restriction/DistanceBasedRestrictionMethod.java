package edu.princeton.safe.restriction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public abstract class DistanceBasedRestrictionMethod implements RestrictionMethod {

    protected abstract boolean isIncluded(EnrichmentLandscape result,
                                          double[] distances);

    @Override
    public void applyRestriction(EnrichmentLandscape result) {
        AnnotationProvider annotationProvider = result.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();

        List<? extends Neighborhood> neighborhoods = result.getNeighborhoods();

        boolean isBinary = annotationProvider.isBinary();
        SignificancePredicate isHighest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_HIGHEST,
                                                                                totalAttributes);
        SignificancePredicate isLowest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_LOWEST,
                                                                               totalAttributes);

        IntStream.range(0, totalAttributes)
                 .parallel()
                 .forEach(j -> {
                     result.setTop(j, EnrichmentLandscape.TYPE_HIGHEST,
                                   isIncluded(result, neighborhoods, n -> isHighest.test(n, j)));
                     if (!isBinary) {
                         result.setTop(j, EnrichmentLandscape.TYPE_LOWEST,
                                       isIncluded(result, neighborhoods, n -> isLowest.test(n, j)));
                     }
                 });
    }

    boolean isIncluded(EnrichmentLandscape result,
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
