package edu.princeton.safe.internal;

import static edu.princeton.safe.internal.Timeable.time;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.OptionalDouble;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.junit.Test;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.NeighborhoodFactory;
import edu.princeton.safe.NeighborhoodScoringMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.JaccardDistanceMethod;
import edu.princeton.safe.internal.io.AttributeReport;
import edu.princeton.safe.internal.io.TabDelimitedAnnotationParser;
import edu.princeton.safe.internal.io.TabDelimitedNetworkParser;
import edu.princeton.safe.internal.scoring.RandomizedMemberScoringMethod;
import edu.princeton.safe.io.AnnotationParser;
import edu.princeton.safe.io.NetworkParser;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;

public class PerformanceTest {
    void runTimedTest(NetworkParser networkParser,
                      AnnotationProviderFactory annotationProviderFactory,
                      NeighborhoodFactoryFactory neighborhoodFactoryFactory,
                      EnrichmentHandler enrichmentHandler)
            throws Exception {
        int loadRepeats = 1;
        int computeRepeats = 1;

        NetworkProvider networkProvider = time("Load Network", () -> new SparseNetworkProvider(networkParser),
                                               loadRepeats);
        int totalNodes = networkProvider.getNodeCount();
        System.out.printf("Nodes: %d\n", totalNodes);

        AnnotationProvider annotationProvider = time("Load Annotations",
                                                     () -> annotationProviderFactory.create(networkProvider),
                                                     loadRepeats);
        int totalAttributes = annotationProvider.getAttributeCount();
        System.out.printf("Attributes: %d\n", totalAttributes);

        NeighborhoodFactory<DefaultNeighborhood> neighborhoodFactory = neighborhoodFactoryFactory.create(annotationProvider);
        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();
        List<DefaultNeighborhood> neighborhoods = time("Distances",
                                                       () -> metric.computeDistances(networkProvider,
                                                                                     neighborhoodFactory),
                                                       computeRepeats);
        System.out.printf("Distances: %d\n", neighborhoods.stream()
                                                          .flatMapToDouble(n -> n.streamDistances())
                                                          .count());
        OptionalDouble max = time("Max", () -> neighborhoods.stream()
                                                            .flatMapToDouble(n -> n.streamDistances())
                                                            .filter(d -> !Double.isNaN(d))
                                                            .max(),
                                  computeRepeats);
        System.out.printf("Max: %f\n", max.getAsDouble());

        OptionalDouble min = time("Min", () -> neighborhoods.stream()
                                                            .flatMapToDouble(n -> n.streamDistances())
                                                            .filter(d -> !Double.isNaN(d))
                                                            .min(),
                                  computeRepeats);
        System.out.printf("Min: %f\n", min.getAsDouble());

        double threshold = time("Threshold", () -> ParallelSafe.computeMaximumDistanceThreshold(neighborhoods, 0.5),
                                computeRepeats);
        System.out.println(threshold);

        int totalTypes = annotationProvider.isBinary() ? 1 : 2;
        DefaultEnrichmentLandscape landscape = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        landscape.maximumDistanceThreshold = threshold;
        landscape.neighborhoods = neighborhoods;
        landscape.networkProvider = networkProvider;

        time("Neighborhoods", () -> ParallelSafe.computeNeighborhoods(landscape, networkProvider, annotationProvider),
             computeRepeats);

        DefaultProgressReporter progressReporter = new DefaultProgressReporter();
        progressReporter.add(new ConsoleProgressReporter());

        time("Enrichment",
             () -> enrichmentHandler.handle(networkProvider, annotationProvider, progressReporter, neighborhoods),
             computeRepeats);

        int minimumLandscapeSize = 10;
        DefaultCompositeMap compositeMap = new DefaultCompositeMap(annotationProvider);
        RestrictionMethod restrictionMethod = new RadiusBasedRestrictionMethod(minimumLandscapeSize, 65);
        time("Unimodality",
             () -> ParallelSafe.computeUnimodality(landscape, compositeMap, restrictionMethod, progressReporter),
             computeRepeats);

        double significanceThreshold = Neighborhood.getEnrichmentThreshold(totalAttributes);
        JaccardDistanceMethod distanceMethod = new JaccardDistanceMethod(d -> d > significanceThreshold);
        GroupingMethod groupingMethod = new ClusterBasedGroupingMethod(0.75, distanceMethod);
        time("Grouping", () -> ParallelSafe.computeGroups(landscape, compositeMap, groupingMethod, progressReporter),
             computeRepeats);

        time("Domains",
             () -> ParallelSafe.computeDomains(landscape, compositeMap, minimumLandscapeSize, progressReporter),
             computeRepeats);

        compositeMap.getDomains(EnrichmentLandscape.TYPE_HIGHEST)
                    .stream()
                    .forEach(d -> System.out.printf("Domain %s\t%d\n", d.getName(), d.getAttributeCount()));

        int[] count = { 0 };
        int[] count2 = { 0 };
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_HIGHEST,
                                                                                    totalAttributes);
        ScoringFunction score = Neighborhood.getScoringFunction(EnrichmentLandscape.TYPE_HIGHEST);
        landscape.neighborhoods.stream()
                               .forEach(n -> {
                                   for (int j = 0; j < totalAttributes; j++) {
                                       if (isSignificant.test(n, j)) {
                                           count[0]++;
                                       }
                                       if (Double.isFinite(score.get(n, j))) {
                                           count2[0]++;
                                       }
                                   }
                               });
        System.out.printf("Total significant: %d\n", count[0]);
        System.out.printf("Total scores: %d\n", totalAttributes * totalNodes - count2[0]);
        System.out.printf("Enrichment threshold: %f\n", Neighborhood.getEnrichmentThreshold(totalAttributes));

        AttributeReport.write(new PrintWriter(System.out), landscape, compositeMap, EnrichmentLandscape.TYPE_HIGHEST);
    }

    @Test
    public void testElapsedTimeQuantitative() throws Exception {
        NetworkParser networkParser = new TabDelimitedNetworkParser("src/test/resources/Costanzo_Science_2010.nodes.txt",
                                                                    "src/test/resources/Costanzo_Science_2010.edges.txt",
                                                                    false);

        AnnotationParser annotationParser = new TabDelimitedAnnotationParser("src/test/resources/hoepfner_movva_2014_hop_known.txt.gz",
                                                                             0, "#");
        AnnotationProviderFactory annotationProviderFactory = (networkProvider) -> {
            try {
                return new DenseAnnotationProvider(networkProvider, annotationParser);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        NeighborhoodFactoryFactory neighborhoodFactoryFactory = (annotationProvider) -> {
            int totalNodes = annotationProvider.getNetworkNodeCount();
            int totalAttributes = annotationProvider.getAttributeCount();
            return (i) -> new DenseNeighborhood(i, totalNodes, totalAttributes);
        };

        EnrichmentHandler enrichmentHandler = (networkProvider,
                                               annotationProvider,
                                               progressReporter,
                                               neighborhoods) -> {
            int totalNodes = networkProvider.getNodeCount();
            int totalPermutations = 100;
            int seed = 0;

            RandomGenerator generator = new Well44497b(seed);
            NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                        totalPermutations, totalNodes);

            ParallelSafe.computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod,
                                                       progressReporter, neighborhoods);
        };
        runTimedTest(networkParser, annotationProviderFactory, neighborhoodFactoryFactory, enrichmentHandler);
    }

    @Test
    public void testElapsedTimeBinary() throws Exception {
        NetworkParser networkParser = new TabDelimitedNetworkParser("src/test/resources/Costanzo_Science_2010.nodes.txt",
                                                                    "src/test/resources/Costanzo_Science_2010.edges.txt",
                                                                    false);

        AnnotationParser annotationParser = new TabDelimitedAnnotationParser("src/test/resources/go_bp_140819.txt.gz",
                                                                             0, "#");
        AnnotationProviderFactory annotationProviderFactory = (networkProvider) -> {
            try {
                return new SparseAnnotationProvider(networkProvider, annotationParser);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        NeighborhoodFactoryFactory neighborhoodFactoryFactory = (annotationProvider) -> {
            int totalNodes = annotationProvider.getNetworkNodeCount();
            int totalAttributes = annotationProvider.getAttributeCount();
            return (i) -> new DenseNeighborhood(i, totalNodes, totalAttributes);
        };

        EnrichmentHandler enrichmentHandler = (networkProvider,
                                               annotationProvider,
                                               progressReporter,
                                               neighborhoods) -> {
            ParallelSafe.computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter, neighborhoods,
                                                 BackgroundMethod.Network);
        };
        runTimedTest(networkParser, annotationProviderFactory, neighborhoodFactoryFactory, enrichmentHandler);
    }
}

@FunctionalInterface
interface AnnotationProviderFactory {
    AnnotationProvider create(NetworkProvider networkProvider);
}

@FunctionalInterface
interface EnrichmentHandler {
    void handle(NetworkProvider networkProvider,
                AnnotationProvider annotationProvider,
                ProgressReporter progressReporter,
                List<DefaultNeighborhood> neighborhoods);
}

@FunctionalInterface
interface NeighborhoodFactoryFactory {
    NeighborhoodFactory<DefaultNeighborhood> create(AnnotationProvider annotationProvider);
}
