package edu.princeton.safe.internal;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import cern.jet.stat.Probability;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.NeighborhoodFactory;
import edu.princeton.safe.NeighborhoodScoringMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;
import edu.princeton.safe.internal.scoring.RandomizedMemberScoringMethod;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public class ParallelSafe implements Safe {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    DistanceMetric distanceMetric;
    RestrictionMethod restrictionMethod;
    GroupingMethod groupingMethod;
    BackgroundMethod backgroundMethod;
    OutputMethod outputMethod;
    ProgressReporter progressReporter;

    boolean isDistanceThresholdAbsolute;
    double distanceThreshold;
    int empiricalIterations;

    public ParallelSafe(NetworkProvider networkProvider,
                        AnnotationProvider annotationProvider,
                        DistanceMetric neighborhoodMethod,
                        BackgroundMethod backgroundMethod,
                        RestrictionMethod restrictionMethod,
                        GroupingMethod groupingMethod,
                        OutputMethod outputMethod,
                        boolean isDistanceThresholdAbsolute,
                        double distancePercentile,
                        int empiricalIterations,
                        ProgressReporter progressReporter) {

        this.networkProvider = networkProvider;
        this.annotationProvider = annotationProvider;
        this.distanceMetric = neighborhoodMethod;
        this.backgroundMethod = backgroundMethod;
        this.restrictionMethod = restrictionMethod;
        this.groupingMethod = groupingMethod;
        this.outputMethod = outputMethod;
        this.progressReporter = progressReporter;

        this.isDistanceThresholdAbsolute = isDistanceThresholdAbsolute;
        this.distanceThreshold = distancePercentile;
        this.empiricalIterations = empiricalIterations;
    }

    @Override
    public void apply() {
        int totalTypes = annotationProvider.isBinary() ? 1 : 2;
        DefaultEnrichmentLandscape result = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        computeDistances(networkProvider, annotationProvider, distanceMetric, isDistanceThresholdAbsolute,
                         distanceThreshold, result);

        computeNeighborhoods(result, networkProvider, annotationProvider);

        int quantitativeIterations = 1000;
        int randomSeed = 0;
        computeEnrichment(networkProvider, annotationProvider, backgroundMethod, quantitativeIterations, randomSeed,
                          progressReporter, result);

        computeUnimodality(result, restrictionMethod);
        computeGroups(result, groupingMethod);

        int minimumLandscapeSize = 10;
        applyColors(result, minimumLandscapeSize);

        outputMethod.apply(result);
    }

    static void computeUnimodality(DefaultEnrichmentLandscape result,
                                   RestrictionMethod restrictionMethod) {

        if (restrictionMethod != null) {
            restrictionMethod.applyRestriction(result);
            return;
        }

        int totalAttributes = result.getAnnotationProvider()
                                    .getAttributeCount();

        IntStream.range(0, result.isTop.length)
                 .forEach(typeIndex -> {
                     IntStream.range(0, totalAttributes)
                              .forEach(i -> result.setTop(i, typeIndex, true));
                     return;
                 });
    }

    static void applyColors(DefaultEnrichmentLandscape landscape,
                            int minimumLandscapeSize) {

        computeColors(landscape, EnrichmentLandscape.TYPE_HIGHEST, minimumLandscapeSize);

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        if (!annotationProvider.isBinary()) {
            computeColors(landscape, EnrichmentLandscape.TYPE_LOWEST, minimumLandscapeSize);
        }

        assignColors(landscape);
    }

    static void assignColors(DefaultEnrichmentLandscape landscape) {
        // TODO Auto-generated method stub
        // assign unique color to each domain
        // compute color for each node
    }

    static void computeColors(DefaultEnrichmentLandscape landscape,
                              int typeIndex,
                              int minimumLandscapeSize) {

        AnnotationProvider annotationProvider = landscape.annotationProvider;
        int totalAttributes = annotationProvider.getAttributeCount();

        ScoringFunction score = Neighborhood.getScoringFunction(typeIndex);
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);

        List<DefaultDomain> domains = landscape.domains.domainsByType[typeIndex];
        int totalDomains = domains.size();

        List<DefaultNeighborhood> neighborhoods = landscape.neighborhoods;
        neighborhoods.stream()
                     .forEach(neighborhood -> {
                         int[] totalSignificantByDomain = new int[totalDomains];
                         double[] cumulativeEnrichmentByDomain = new double[totalDomains];

                         IntStream.range(0, totalDomains)
                                  .forEach(i -> {
                                      DefaultDomain domain = domains.get(i);
                                      domain.forEachAttribute(j -> {
                                          if (isSignificant.test(neighborhood, j)) {
                                              totalSignificantByDomain[i]++;
                                          }
                                          cumulativeEnrichmentByDomain[i] += score.get(neighborhood, j);
                                      });
                                  });

                         OptionalInt maximumSignificant = Arrays.stream(totalSignificantByDomain)
                                                                .max();
                         if (!maximumSignificant.isPresent()) {
                             return;
                         }
                         int maximum = maximumSignificant.getAsInt();
                         if (maximum > 0) {
                             int[] topDomains = IntStream.range(0, totalDomains)
                                                         .filter(i -> totalSignificantByDomain[i] == maximum)
                                                         .toArray();

                             OptionalDouble cumulativeEnrichment = Arrays.stream(topDomains)
                                                                         .mapToDouble(i -> cumulativeEnrichmentByDomain[i])
                                                                         .max();

                             if (!cumulativeEnrichment.isPresent()) {
                                 return;
                             }

                             double cumulativeEnrichment2 = cumulativeEnrichment.getAsDouble();
                             OptionalInt topDomainIndex = Arrays.stream(topDomains)
                                                                .filter(i -> cumulativeEnrichmentByDomain[i] == cumulativeEnrichment2)
                                                                .findFirst();

                             if (!topDomainIndex.isPresent()) {
                                 return;
                             }

                             int domainIndex = topDomainIndex.getAsInt();
                             DefaultDomain domain = domains.get(domainIndex);
                             OptionalDouble enrichment = StreamSupport.stream(domain.attributeIndexes.spliterator(),
                                                                              false)
                                                                      .mapToDouble(c -> score.get(neighborhood,
                                                                                                  c.value))
                                                                      .max();

                             int nodeIndex = neighborhood.getNodeIndex();
                             landscape.domains.cumulativeOpacity[typeIndex][nodeIndex] = enrichment.getAsDouble();
                             landscape.domains.topDomain[typeIndex][nodeIndex] = domain;
                         }
                     });

        if (minimumLandscapeSize > 0) {
            minimizeColors(landscape, typeIndex, minimumLandscapeSize);
        }
    }

    static void minimizeColors(DefaultEnrichmentLandscape landscape,
                               int minimumLandscapeSize) {
        minimizeColors(landscape, EnrichmentLandscape.TYPE_HIGHEST, minimumLandscapeSize);
        if (!landscape.annotationProvider.isBinary()) {
            minimizeColors(landscape, EnrichmentLandscape.TYPE_LOWEST, minimumLandscapeSize);
        }
    }

    static void minimizeColors(DefaultEnrichmentLandscape landscape,
                               int typeIndex,
                               int minimumLandscapeSize) {
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);

        List<DefaultDomain> domains = landscape.domains.domainsByType[typeIndex];
        int totalDomains = domains.size();
        IntStream.range(0, totalDomains)
                 .forEach(i -> domains.get(i).index = i);

        int[] totalSignificantByDomain = new int[totalDomains];

        List<DefaultNeighborhood> neighborhoods = landscape.neighborhoods;
        neighborhoods.stream()
                     .forEach(neighborhood -> {
                         domains.stream()
                                .forEach(domain -> {
                                    domain.forEachAttribute(attributeIndex -> {
                                        if (isSignificant.test(neighborhood, attributeIndex)) {
                                            totalSignificantByDomain[domain.index]++;
                                        }
                                    });
                                });
                     });

        List<DefaultDomain> result = domains.stream()
                                            .filter(domain -> totalSignificantByDomain[domain.index] >= minimumLandscapeSize)
                                            .collect(Collectors.toList());

        System.out.printf("Total domains: %d\n", domains.size());
        System.out.printf("Minimized domains: %d\n", result.size());

        // Re-index the filtered domain list
        IntStream.range(0, result.size())
                 .forEach(i -> result.get(i).index = i);

        landscape.domains.domainsByType[typeIndex] = result;
    }

    static void computeGroups(DefaultEnrichmentLandscape landscape,
                              GroupingMethod method) {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        DefaultDomainDetails details = new DefaultDomainDetails(annotationProvider);
        method.group(landscape, EnrichmentLandscape.TYPE_HIGHEST, details.getConsumer());

        if (!annotationProvider.isBinary()) {
            method.group(landscape, EnrichmentLandscape.TYPE_LOWEST, details.getConsumer());
        }

        landscape.domains = details;
    }

    static void computeEnrichment(NetworkProvider networkProvider,
                                  AnnotationProvider annotationProvider,
                                  BackgroundMethod backgroundMethod,
                                  int quantitativeIterations,
                                  int randomSeed,
                                  ProgressReporter progressReporter,
                                  DefaultEnrichmentLandscape result) {
        if (annotationProvider.isBinary()) {
            computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter, result.neighborhoods,
                                    backgroundMethod);
        } else {
            RandomGenerator generator = new Well44497b(randomSeed);

            int totalNodes = networkProvider.getNodeCount();
            NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                        quantitativeIterations,
                                                                                        totalNodes);
            computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod, progressReporter,
                                          result.neighborhoods);
        }
    }

    static void computeQuantitativeEnrichment(NetworkProvider networkProvider,
                                              AnnotationProvider annotationProvider,
                                              NeighborhoodScoringMethod scoringMethod,
                                              ProgressReporter progressReporter,
                                              List<? extends Neighborhood> neighborhoods) {

        Stream<? extends Neighborhood> stream = neighborhoods.stream();
        if (progressReporter.supportsParallel()) {
            stream = stream.parallel();
        }
        progressReporter.startNeighborhoodScore(networkProvider, annotationProvider);
        stream.forEach(neighborhood -> {
            int nodeIndex = neighborhood.getNodeIndex();
            for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                final int attributeIndex = j;
                double[] neighborhoodScore = { 0 };

                neighborhood.forEachMemberIndex(index -> {
                    double value = annotationProvider.getValue(index, attributeIndex);
                    if (!Double.isNaN(value)) {
                        neighborhoodScore[0] += value;
                    }
                });

                double[] randomScores = scoringMethod.computeRandomizedScores(neighborhood, j);
                SummaryStatistics statistics = new SummaryStatistics();
                for (int r = 0; r < randomScores.length; r++) {
                    if (!Double.isNaN(randomScores[r])) {
                        statistics.addValue(randomScores[r]);
                    }
                }

                double p = 1 - Probability.normal(statistics.getMean(), statistics.getVariance(), neighborhoodScore[0]);

                neighborhood.setPValue(j, p);

                double score = Neighborhood.computeEnrichmentScore(p);
                progressReporter.neighborhoodScore(nodeIndex, j, score);
            }
            progressReporter.finishNeighborhood(nodeIndex);
        });
        progressReporter.finishNeighborhoodScore();
    }

    static void computeBinaryEnrichment(NetworkProvider networkProvider,
                                        AnnotationProvider annotationProvider,
                                        ProgressReporter progressReporter,
                                        List<? extends Neighborhood> neighborhoods,
                                        BackgroundMethod backgroundMethod) {

        int totalNodes;
        IntIntFunction nodeCount;

        switch (backgroundMethod) {
        case Network:
            totalNodes = annotationProvider.getNetworkNodeCount();
            nodeCount = j -> annotationProvider.getNetworkNodeCountForAttribute(j);
            break;
        case Annotation:
            totalNodes = annotationProvider.getAnnotationNodeCount();
            nodeCount = j -> annotationProvider.getAnnotationNodeCountForAttribute(j);
            break;
        default:
            throw new RuntimeException("Unexpected background method");
        }

        Stream<? extends Neighborhood> stream = neighborhoods.stream();
        if (progressReporter.supportsParallel()) {
            stream = stream.parallel();
        }

        progressReporter.startNeighborhoodScore(networkProvider, annotationProvider);
        stream.forEach(neighborhood -> {
            int nodeIndex = neighborhood.getNodeIndex();
            int neighborhoodSize = neighborhood.getMemberCount();
            for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                int totalNodesForFunction = nodeCount.apply(j);
                int totalNeighborhoodNodesForFunction = neighborhood.getMemberCountForAttribute(j, annotationProvider);

                HypergeometricDistribution distribution = new HypergeometricDistribution(null, totalNodes,
                                                                                         totalNodesForFunction,
                                                                                         neighborhoodSize);
                double p = 1.0 - distribution.cumulativeProbability(totalNeighborhoodNodesForFunction - 1);
                neighborhood.setPValue(j, p);

                double score = Neighborhood.computeEnrichmentScore(p);
                progressReporter.neighborhoodScore(nodeIndex, j, score);
            }
            progressReporter.finishNeighborhood(nodeIndex);

        });
        progressReporter.finishNeighborhoodScore();
    }

    static void computeNeighborhoods(DefaultEnrichmentLandscape result,
                                     NetworkProvider networkProvider,
                                     AnnotationProvider annotationProvider) {

        result.neighborhoods.stream()
                            .forEach(n -> n.applyDistanceThreshold(result.maximumDistanceThreshold));
    }

    static double computeMaximumDistanceThreshold(List<? extends DefaultNeighborhood> neighborhoods,
                                                  double percentileIndex) {

        double[] distances = neighborhoods.stream()
                                          .flatMapToDouble(n -> n.streamDistances())
                                          .toArray();
        return Util.percentile(distances, percentileIndex);
    }

    static void computeDistances(NetworkProvider networkProvider,
                                 AnnotationProvider annotationProvider,
                                 DistanceMetric distanceMetric,
                                 boolean isDistanceThresholdAbsolute,
                                 double distanceThreshold,
                                 DefaultEnrichmentLandscape result) {
        if (result.neighborhoods != null) {
            return;
        }

        int totalNodes = networkProvider.getNodeCount();
        int totalAttributes = annotationProvider.getAttributeCount();

        NeighborhoodFactory<DefaultNeighborhood> factory;
        if (annotationProvider.isBinary()) {
            factory = (int i) -> new SparseNeighborhood(i, totalAttributes);
        } else {
            factory = (int i) -> new DenseNeighborhood(i, totalNodes, totalAttributes);
        }

        result.neighborhoods = distanceMetric.computeDistances(networkProvider, factory);
        if (isDistanceThresholdAbsolute) {
            result.maximumDistanceThreshold = distanceThreshold;
        } else {
            result.maximumDistanceThreshold = computeMaximumDistanceThreshold(result.neighborhoods, distanceThreshold);
        }
    }
}
