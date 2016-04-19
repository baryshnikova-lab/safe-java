package edu.princeton.safe.internal;

import static edu.princeton.safe.internal.Timeable.time;

import java.util.List;
import java.util.Optional;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.junit.Test;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NeighborhoodScoringMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.internal.distance.MapBasedDistanceMetric;
import edu.princeton.safe.internal.scoring.RandomizedMemberScoringMethod;

public class PerformanceTest {
    @Test
    public void testElapsedTime() throws Exception {
        int loadRepeats = 1;
        int computeRepeats = 1;
        NetworkProvider networkProvider = time("Load Network",
                                               () -> new TabDelimitedNetworkProvider("src/test/resources/Costanzo_Science_2010.nodes.txt",
                                                                                     "src/test/resources/Costanzo_Science_2010.edges.txt"),
                                               loadRepeats);
        System.out.printf("Nodes: %d\n", networkProvider.getNodeCount());

        AnnotationProvider annotationProvider = time("Load Annotations",
                                                     () -> new DenseAnnotationProvider(networkProvider,
                                                                                       "src/test/resources/hoepfner_movva_2014_hop_known.txt"),
                                                     loadRepeats);
        System.out.printf("Attributes: %d\n", annotationProvider.getAttributeCount());

        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();
        List<NodePair> pairs = time("Distances", () -> metric.computeDistances(networkProvider), computeRepeats);
        System.out.printf("Distances: %d\n", pairs.size());
        Optional<Double> max = time("Max", () -> pairs.stream()
                                                      .map(p -> p.getDistance())
                                                      .max(Double::compare),
                                    computeRepeats);
        System.out.printf("Max: %f\n", max.get());

        Optional<Double> min = time("Min", () -> pairs.stream()
                                                      .map(p -> p.getDistance())
                                                      .min(Double::compare),
                                    computeRepeats);
        System.out.printf("Min: %f\n", min.get());

        double threshold = time("Threshold", () -> ParallelSafe.computeMaximumDistanceThreshold(pairs, 0.5),
                                computeRepeats);
        System.out.println(threshold);

        Neighborhood[] neighborhoods = time("Neighborhoods",
                                            () -> ParallelSafe.computeNeighborhoods(networkProvider, annotationProvider,
                                                                                    pairs, threshold),
                                            computeRepeats);

        ProgressReporter progressReporter = new FileProgressReporter("neighborhood.binary.txt");
        time("Binary Enrichment", () -> ParallelSafe.computeBinaryEnrichment(networkProvider, annotationProvider,
                                                                             progressReporter, neighborhoods),
             computeRepeats);

        int totalPermutations = 1000;
        int seed = 0;
        RandomGenerator generator = new Well44497b(seed);
        int totalNodes = networkProvider.getNodeCount();
        NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                    totalPermutations, totalNodes);

        ProgressReporter progressReporter2 = new FileProgressReporter("neighborhood.quantitative.txt");
        time("Quantitative Enrichment",
             () -> ParallelSafe.computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod,
                                                              progressReporter2, neighborhoods),
             computeRepeats);
    }
}
