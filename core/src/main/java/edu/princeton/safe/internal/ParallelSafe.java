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
import edu.princeton.safe.io.DomainConsumer;
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
        DefaultEnrichmentLandscape landscape = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        computeDistances(networkProvider, annotationProvider, distanceMetric, isDistanceThresholdAbsolute,
                         distanceThreshold, landscape);

        computeNeighborhoods(landscape, networkProvider, annotationProvider);

        int quantitativeIterations = 1000;
        int randomSeed = 0;
        computeEnrichment(networkProvider, annotationProvider, backgroundMethod, quantitativeIterations, randomSeed,
                          progressReporter, landscape);

        DefaultCompositeMap compositeMap = new DefaultCompositeMap(annotationProvider);
        computeUnimodality(landscape, compositeMap, restrictionMethod, progressReporter);
        computeGroups(landscape, compositeMap, groupingMethod, progressReporter);

        int minimumLandscapeSize = 10;
        computeDomains(landscape, compositeMap, minimumLandscapeSize, progressReporter);

        outputMethod.apply(landscape);
    }

    static void computeUnimodality(DefaultEnrichmentLandscape landscape,
                                   DefaultCompositeMap compositeMap,
                                   RestrictionMethod restrictionMethod,
                                   ProgressReporter progressReporter) {

        if (restrictionMethod != null) {
            restrictionMethod.applyRestriction(landscape, compositeMap, progressReporter);
            return;
        }

        int totalAttributes = landscape.getAnnotationProvider()
                                       .getAttributeCount();

        IntStream.range(0, compositeMap.isTop.length)
                 .forEach(typeIndex -> {
                     IntStream.range(0, totalAttributes)
                              .forEach(i -> compositeMap.setTop(i, typeIndex, true));
                     return;
                 });
    }

    static void computeDomains(DefaultEnrichmentLandscape landscape,
                               DefaultCompositeMap compositeMap,
                               int minimumLandscapeSize,
                               ProgressReporter progressReporter) {

        computeDomains(landscape, compositeMap, EnrichmentLandscape.TYPE_HIGHEST, minimumLandscapeSize,
                       progressReporter);

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        if (!annotationProvider.isBinary()) {
            computeDomains(landscape, compositeMap, EnrichmentLandscape.TYPE_LOWEST, minimumLandscapeSize,
                           progressReporter);
        }
    }

    static void computeDomains(DefaultEnrichmentLandscape landscape,
                               DefaultCompositeMap compositeMap,
                               int typeIndex,
                               int minimumLandscapeSize,
                               ProgressReporter progressReporter) {

        switch (typeIndex) {
        case EnrichmentLandscape.TYPE_HIGHEST:
            progressReporter.setStatus("Computing highest most significant domains...");
            break;
        case EnrichmentLandscape.TYPE_LOWEST:
            progressReporter.setStatus("Computing lowest most significant domains...");
            break;
        default:
            throw new RuntimeException();
        }

        AnnotationProvider annotationProvider = landscape.annotationProvider;
        int totalAttributes = annotationProvider.getAttributeCount();

        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);

        ScoringFunction score = Neighborhood.getScoringFunction(typeIndex);

        List<DefaultDomain> domains = compositeMap.domainsByType[typeIndex];
        if (domains == null) {
            return;
        }

        List<DefaultNeighborhood> neighborhoods = landscape.neighborhoods;
        neighborhoods.stream()
                     .forEach(neighborhood -> computeDomains(compositeMap, typeIndex, score, isSignificant, domains,
                                                             neighborhood));

        if (minimumLandscapeSize > 0) {
            minimizeDomains(landscape, compositeMap, typeIndex, minimumLandscapeSize, progressReporter);
        }
    }

    static void computeDomains(DefaultCompositeMap compositeMap,
                               int typeIndex,
                               ScoringFunction score,
                               SignificancePredicate isSignificant,
                               List<DefaultDomain> domains,
                               DefaultNeighborhood neighborhood) {

        int totalDomains = domains.size();
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
            OptionalDouble enrichment = StreamSupport.stream(domain.attributeIndexes.spliterator(), false)
                                                     .mapToDouble(c -> score.get(neighborhood, c.value))
                                                     .max();

            int nodeIndex = neighborhood.getNodeIndex();
            compositeMap.cumulativeOpacity[typeIndex][nodeIndex] = enrichment.getAsDouble();
            compositeMap.topDomain[typeIndex][nodeIndex] = domain;
        }

    }

    static void minimizeDomains(DefaultEnrichmentLandscape landscape,
                                DefaultCompositeMap compositeMap,
                                int typeIndex,
                                int minimumLandscapeSize,
                                ProgressReporter progressReporter) {
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);

        List<DefaultDomain> domains = compositeMap.domainsByType[typeIndex];
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

        // Reset each domain index to -1
        IntStream.range(0, totalDomains)
                 .forEach(i -> domains.get(i).index = -1);

        progressReporter.setStatus("Total domains: %d", domains.size());
        progressReporter.setStatus("Total domains (after filtering): %d", result.size());

        // Re-index the filtered domain list
        IntStream.range(0, result.size())
                 .forEach(i -> result.get(i).index = i);

        compositeMap.domainsByType[typeIndex] = result;

        progressReporter.setStatus("Nodes with domain: %d", Arrays.stream(compositeMap.topDomain[typeIndex])
                                                                  .filter(d -> d != null)
                                                                  .count());

        // Remove domains that have been filtered out
        DefaultDomain[] topDomains = compositeMap.topDomain[typeIndex];
        IntStream.range(0, topDomains.length)
                 .forEach(i -> {
                     DefaultDomain domain = topDomains[i];
                     if (domain != null && domain.index == -1) {
                         topDomains[i] = null;
                     }
                 });

        progressReporter.setStatus("Nodes with domain (after filtering): %d",
                                   Arrays.stream(compositeMap.topDomain[typeIndex])
                                         .filter(d -> d != null)
                                         .count());
    }

    static void computeGroups(DefaultEnrichmentLandscape landscape,
                              DefaultCompositeMap compositeMap,
                              GroupingMethod method,
                              ProgressReporter progressReporter) {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        DomainConsumer domainConsumer = compositeMap.getConsumer();
        method.group(landscape, compositeMap, EnrichmentLandscape.TYPE_HIGHEST, domainConsumer, progressReporter);

        if (!annotationProvider.isBinary()) {
            method.group(landscape, compositeMap, EnrichmentLandscape.TYPE_LOWEST, domainConsumer, progressReporter);
        }
    }

    static void computeEnrichment(NetworkProvider networkProvider,
                                  AnnotationProvider annotationProvider,
                                  BackgroundMethod backgroundMethod,
                                  int quantitativeIterations,
                                  int randomSeed,
                                  ProgressReporter progressReporter,
                                  DefaultEnrichmentLandscape landscape) {
        if (annotationProvider.isBinary()) {
            computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter, landscape.neighborhoods,
                                    backgroundMethod);
        } else {
            RandomGenerator generator = new Well44497b(randomSeed);

            int totalNodes = networkProvider.getNodeCount();
            NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                        quantitativeIterations,
                                                                                        totalNodes);
            computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod, progressReporter,
                                          landscape.neighborhoods);
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
                if (Double.isFinite(p) && p < 0) {
                    p = 0;
                }
                neighborhood.setPValue(j, p);

                double score = Neighborhood.computeEnrichmentScore(p);
                progressReporter.neighborhoodScore(nodeIndex, j, score);
            }
            progressReporter.finishNeighborhood(nodeIndex);

        });
        progressReporter.finishNeighborhoodScore();
    }

    static void computeNeighborhoods(DefaultEnrichmentLandscape landscape,
                                     NetworkProvider networkProvider,
                                     AnnotationProvider annotationProvider) {

        landscape.neighborhoods.stream()
                               .forEach(n -> n.applyDistanceThreshold(landscape.maximumDistanceThreshold));
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
                                 DefaultEnrichmentLandscape landscape) {

        if (landscape.neighborhoods != null) {
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

        landscape.neighborhoods = distanceMetric.computeDistances(networkProvider, factory);
        if (isDistanceThresholdAbsolute) {
            landscape.maximumDistanceThreshold = distanceThreshold;
        } else {
            landscape.maximumDistanceThreshold = computeMaximumDistanceThreshold(landscape.neighborhoods,
                                                                                 distanceThreshold);
        }
    }
}
