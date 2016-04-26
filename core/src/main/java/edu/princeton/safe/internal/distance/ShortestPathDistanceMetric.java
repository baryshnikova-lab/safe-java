package edu.princeton.safe.internal.distance;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NeighborhoodFactory;
import edu.princeton.safe.NetworkProvider;

public abstract class ShortestPathDistanceMetric implements DistanceMetric {

    abstract EdgeWeightFunction getEdgeWeightFunction(NetworkProvider networkProvider);

    @Override
    public <T extends Neighborhood> List<T> computeDistances(NetworkProvider networkProvider,
                                                             NeighborhoodFactory<T> neighborhoodFactory) {
        EdgeWeightFunction weightFunction = getEdgeWeightFunction(networkProvider);
        int totalNodes = networkProvider.getNodeCount();

        List<T> neighborhoods = IntStream.range(0, totalNodes)
                                         .mapToObj(i -> neighborhoodFactory.create(i))
                                         .collect(Collectors.toList());

        johnson(networkProvider, weightFunction, (fromIndex,
                                                  toIndex,
                                                  distance) -> neighborhoods.get(fromIndex)
                                                                            .setDistance(toIndex, distance));
        return neighborhoods;
    }

    void johnson(NetworkProvider networkProvider,
                 EdgeWeightFunction weight,
                 NodeDistanceConsumer consumer) {

        ShortestPathResult transformation = johnsonBellmanFord(networkProvider);
        EdgeWeightFunction transformed = (int u,
                                          int v) -> weight.get(u, v) + transformation.distances[u]
                                                  - transformation.distances[v];

        int totalNodes = networkProvider.getNodeCount();
        IntStream.range(0, totalNodes)
                 .parallel()
                 .forEach(new IntConsumer() {
                     @Override
                     public void accept(int fromIndex) {
                         ShortestPathResult shortest = dijkstra(networkProvider, transformed, fromIndex);
                         computeDistances(shortest, fromIndex, consumer);
                     }
                 });
    }

    private void computeDistances(ShortestPathResult result,
                                  int fromIndex,
                                  NodeDistanceConsumer consumer) {
        for (int toIndex = 0; toIndex < result.distances.length; toIndex++) {
            double distance = result.distances[toIndex];
            if (Double.isFinite(distance)) {
                consumer.accept(fromIndex, toIndex, distance);
            }
        }
    }

    ShortestPathResult dijkstra(NetworkProvider networkProvider,
                                EdgeWeightFunction weight,
                                int sourceIndex) {
        int totalNodes = networkProvider.getNodeCount();

        ShortestPathResult result = new ShortestPathResult(totalNodes, sourceIndex);

        @SuppressWarnings("unchecked")
        FibonacciHeapNode<Integer>[] heapNodes = new FibonacciHeapNode[totalNodes];

        FibonacciHeap<Integer> nodes = new FibonacciHeap<Integer>();

        for (int i = 0; i < totalNodes; i++) {
            FibonacciHeapNode<Integer> heapNode = new FibonacciHeapNode<>(i);
            heapNodes[i] = heapNode;
            nodes.insert(heapNode, result.distances[i]);
        }
        result.distances[sourceIndex] = 0;

        while (!nodes.isEmpty()) {
            int nextIndex = nodes.removeMin()
                                 .getData();
            final int fromIndex = nextIndex;
            networkProvider.forEachNeighbor(fromIndex, new IntConsumer() {
                @Override
                public void accept(int toIndex) {
                    double oldWeight = result.distances[toIndex];
                    double newWeight = result.distances[fromIndex] + weight.get(fromIndex, toIndex);
                    if (newWeight < oldWeight) {
                        result.distances[toIndex] = newWeight;
                        result.predecessors[toIndex] = fromIndex;
                        nodes.decreaseKey(heapNodes[toIndex], newWeight);
                    }

                }
            });
        }
        return result;
    }

    /**
     * Bellman-Ford shortest path algorithm using the Johnson transformation.
     * The source node is the implicitly created "q" from the Johnson
     * transformation with index = networkProvider.getNodeCount().
     * 
     * @param networkProvider
     * @return
     */
    ShortestPathResult johnsonBellmanFord(NetworkProvider networkProvider) {
        int totalNodes = networkProvider.getNodeCount();

        int sourceIndex = totalNodes;

        int totalAnalysisNodes = totalNodes + 1;
        ShortestPathResult result = new ShortestPathResult(totalAnalysisNodes, sourceIndex);

        for (int i = 0; i < totalNodes; i++) {
            for (int j = 0; j < totalNodes; j++) {
                final int fromIndex = j;
                networkProvider.forEachNeighbor(fromIndex, new IntConsumer() {
                    @Override
                    public void accept(int toIndex) {
                        double weight = networkProvider.getWeight(fromIndex, toIndex);
                        double distance = result.distances[fromIndex] + weight;
                        if (distance < result.distances[toIndex]) {
                            result.distances[toIndex] = distance;
                            result.predecessors[toIndex] = fromIndex;
                        }
                    }
                });
            }

            // Handle edges from source node
            for (int toIndex = 0; toIndex < totalNodes; toIndex++) {
                double distance = result.distances[sourceIndex];
                if (distance < result.distances[toIndex]) {
                    // All weights are 0 in this case
                    result.distances[toIndex] = distance;
                    result.predecessors[toIndex] = sourceIndex;
                }
            }
        }

        // Check for negative cycle
        for (int j = 0; j < totalNodes; j++) {
            final int fromIndex = j;
            networkProvider.forEachNeighbor(fromIndex, new IntConsumer() {
                @Override
                public void accept(int toIndex) {
                    double weight = networkProvider.getWeight(fromIndex, toIndex);
                    double distance = result.distances[fromIndex] + weight;
                    if (distance < result.distances[toIndex]) {
                        throw new RuntimeException("Negative cycle detected in network");
                    }
                }
            });
        }

        return result;
    }

    static class ShortestPathResult {
        double[] distances;
        int[] predecessors;

        ShortestPathResult(int totalNodes,
                           int sourceIndex) {
            distances = new double[totalNodes];
            predecessors = new int[totalNodes];

            for (int i = 0; i < distances.length; i++) {
                distances[i] = Double.POSITIVE_INFINITY;
                predecessors[i] = -1;
            }
            distances[sourceIndex] = 0;
        }
    }

    @FunctionalInterface
    static interface EdgeWeightFunction {
        double get(int fromIndex,
                   int toIndex);
    }

    @FunctionalInterface
    static interface NodeDistanceConsumer {
        void accept(int fromIndex,
                    int toIndex,
                    double distance);
    }
}
