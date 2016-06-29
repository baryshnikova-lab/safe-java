package edu.princeton.safe.restriction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public abstract class DistanceBasedRestrictionMethod implements RestrictionMethod {

    int minimumLandscapeSize;

    public DistanceBasedRestrictionMethod(int minimumLandscapeSize) {
        this.minimumLandscapeSize = minimumLandscapeSize;
    }

    protected abstract boolean isIncluded(EnrichmentLandscape result,
                                          double[] distances);

    @Override
    public void applyRestriction(EnrichmentLandscape landscape,
                                 CompositeMap compositeMap,
                                 ProgressReporter progressReporter) {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();

        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();

        boolean isBinary = annotationProvider.isBinary();
        SignificancePredicate highest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_HIGHEST,
                                                                              totalAttributes);
        SignificancePredicate lowest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_LOWEST,
                                                                             totalAttributes);
        progressReporter.startUnimodality(annotationProvider);
        IntStream.range(0, totalAttributes)
                 .parallel()
                 .forEach(j -> {
                     boolean isHighest = isIncluded(landscape, neighborhoods, highest, j);
                     compositeMap.setTop(j, EnrichmentLandscape.TYPE_HIGHEST, isHighest);
                     progressReporter.isUnimodal(j, EnrichmentLandscape.TYPE_HIGHEST, isHighest);

                     if (!isBinary) {
                         boolean isLowest = isIncluded(landscape, neighborhoods, lowest, j);
                         compositeMap.setTop(j, EnrichmentLandscape.TYPE_LOWEST, isLowest);
                         progressReporter.isUnimodal(j, EnrichmentLandscape.TYPE_LOWEST, isLowest);
                     }
                 });
        progressReporter.finishUnimodality();
    }

    boolean isIncluded(EnrichmentLandscape result,
                       List<? extends Neighborhood> neighborhoods,
                       SignificancePredicate isSignificant,
                       int attributeIndex) {

        // Indexes of nodes significantly enriched for given attribute
        int[] nodes = neighborhoods.stream()
                                   .filter(n -> isSignificant.test(n, attributeIndex))
                                   .mapToInt(n -> n.getNodeIndex())
                                   .toArray();

        if (nodes.length < 5 || nodes.length < minimumLandscapeSize) {
            return false;
        }

        double[] distances = new double[nodes.length * nodes.length];
        int i = 0;
        for (int n = 0; n < nodes.length; n++) {
            for (int m = 0; m < nodes.length; m++) {
                distances[i] = neighborhoods.get(nodes[n])
                                            .getNodeDistance(nodes[m]);
                i++;
            }
        }
        Arrays.sort(distances);
        boolean isUnimodal = isIncluded(result, distances);
        return isUnimodal;
    }
}
