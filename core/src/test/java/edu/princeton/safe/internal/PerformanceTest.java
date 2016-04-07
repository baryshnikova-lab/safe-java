package edu.princeton.safe.internal;

import static edu.princeton.safe.internal.Timeable.time;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import edu.princeton.safe.NodePair;
import edu.princeton.safe.internal.distance.MapBasedDistanceMetric;

public class PerformanceTest {
    @Test
    public void testElapsedTime() throws Exception {
        int loadRepeats = 1;
        int computeRepeats = 1;
        TabDelimitedNetworkProvider provider = time("Load Network",
                                                    () -> new TabDelimitedNetworkProvider("src/test/resources/nodes.txt",
                                                                                          "src/test/resources/edges.txt"),
                                                    loadRepeats);
        System.out.printf("Nodes: %d\n", provider.getNodeCount());

        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();
        List<NodePair> pairs = time("Distances", () -> metric.computeDistances(provider), computeRepeats);
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
    }
}
