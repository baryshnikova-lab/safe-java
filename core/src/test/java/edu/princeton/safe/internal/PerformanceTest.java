package edu.princeton.safe.internal;

import static edu.princeton.safe.internal.Timeable.time;

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
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.internal.scoring.RandomizedMemberScoringMethod;
import edu.princeton.safe.io.AnnotationParser;
import edu.princeton.safe.io.NetworkParser;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;

public class PerformanceTest {
    @Test
    public void testElapsedTimeQuantitative() throws Exception {
        int loadRepeats = 1;
        int computeRepeats = 1;

        NetworkParser networkParser = new TabDelimitedNetworkParser("src/test/resources/Costanzo_Science_2010.nodes.txt",
                                                                    "src/test/resources/Costanzo_Science_2010.edges.txt",
                                                                    false);
        NetworkProvider networkProvider = time("Load Network", () -> new SparseNetworkProvider(networkParser),
                                               loadRepeats);
        int totalNodes = networkProvider.getNodeCount();
        System.out.printf("Nodes: %d\n", totalNodes);

        AnnotationParser annotationParser = new TabDelimitedAnnotationParser("src/test/resources/hoepfner_movva_2014_hop_known.txt.gz");
        AnnotationProvider annotationProvider = time("Load Annotations",
                                                     () -> new DenseAnnotationProvider(networkProvider,
                                                                                       annotationParser),
                                                     loadRepeats);
        int totalAttributes = annotationProvider.getAttributeCount();
        System.out.printf("Attributes: %d\n", totalAttributes);

        NeighborhoodFactory<DefaultNeighborhood> factory = (i) -> new DenseNeighborhood(i, totalNodes, totalAttributes);

        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();
        List<DefaultNeighborhood> neighborhoods = time("Distances",
                                                       () -> metric.computeDistances(networkProvider, factory),
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

        int totalTypes = 2;
        DefaultEnrichmentLandscape result = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        result.maximumDistanceThreshold = threshold;
        result.neighborhoods = neighborhoods;

        time("Neighborhoods", () -> ParallelSafe.computeNeighborhoods(result, networkProvider, annotationProvider),
             computeRepeats);

        int totalPermutations = 1000;
        int seed = 0;
        RandomGenerator generator = new Well44497b(seed);
        NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                    totalPermutations, totalNodes);

        DefaultProgressReporter progressReporter = new DefaultProgressReporter();
        // progressReporter.add(new
        // FileProgressReporter("neighborhood.quantitative.txt"));
        time("Quantitative Enrichment",
             () -> ParallelSafe.computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod,
                                                              progressReporter, neighborhoods),
             computeRepeats);

        RestrictionMethod restrictionMethod = new RadiusBasedRestrictionMethod(65);
        time("Unimodality", () -> ParallelSafe.computeUnimodality(result, restrictionMethod), computeRepeats);

        GroupingMethod groupingMethod = new ClusterBasedGroupingMethod(0.75, DistanceMethod.JACCARD);
        time("Grouping", () -> ParallelSafe.computeGroups(annotationProvider, result, groupingMethod), computeRepeats);
    }

    @Test
    public void testElapsedTimeBinary() throws Exception {
        int loadRepeats = 1;
        int computeRepeats = 1;

        NetworkParser networkParser = new TabDelimitedNetworkParser("src/test/resources/Costanzo_Science_2010.nodes.txt",
                                                                    "src/test/resources/Costanzo_Science_2010.edges.txt",
                                                                    false);
        NetworkProvider networkProvider = time("Load Network", () -> new SparseNetworkProvider(networkParser),
                                               loadRepeats);
        int totalNodes = networkProvider.getNodeCount();
        System.out.printf("Nodes: %d\n", totalNodes);

        AnnotationParser annotationParser = new TabDelimitedAnnotationParser("src/test/resources/go_bp_140819.txt.gz");
        AnnotationProvider annotationProvider = time("Load Annotations",
                                                     () -> new SparseAnnotationProvider(networkProvider,
                                                                                        annotationParser),
                                                     loadRepeats);
        int totalAttributes = annotationProvider.getAttributeCount();
        System.out.printf("Attributes: %d\n", totalAttributes);

        // NeighborhoodFactory<DefaultNeighborhood> factory = (i) -> new
        // SparseNeighborhood(i, totalAttributes);
        NeighborhoodFactory<DefaultNeighborhood> factory = (i) -> new DenseNeighborhood(i, totalNodes, totalAttributes);

        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();
        List<DefaultNeighborhood> neighborhoods = time("Distances",
                                                       () -> metric.computeDistances(networkProvider, factory),
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

        int totalTypes = 1;
        DefaultEnrichmentLandscape result = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        result.maximumDistanceThreshold = threshold;
        result.neighborhoods = neighborhoods;

        time("Neighborhoods", () -> ParallelSafe.computeNeighborhoods(result, networkProvider, annotationProvider),
             computeRepeats);

        DefaultProgressReporter progressReporter = new DefaultProgressReporter();
        // progressReporter.add(FileProgressReporter("neighborhood.binary.txt"));

        time("Binary Enrichment",
             () -> ParallelSafe.computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter,
                                                        neighborhoods, BackgroundMethod.Network),
             computeRepeats);

        RestrictionMethod restrictionMethod = new RadiusBasedRestrictionMethod(65);
        time("Unimodality", () -> ParallelSafe.computeUnimodality(result, restrictionMethod), computeRepeats);

        GroupingMethod groupingMethod = new ClusterBasedGroupingMethod(0.75, DistanceMethod.JACCARD);
        time("Grouping", () -> ParallelSafe.computeGroups(annotationProvider, result, groupingMethod), computeRepeats);
    }
}
