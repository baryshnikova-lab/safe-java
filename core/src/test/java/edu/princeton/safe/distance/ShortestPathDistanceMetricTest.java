package edu.princeton.safe.distance;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.ShortestPathDistanceMetric.NodeDistanceConsumer;
import edu.princeton.safe.distance.ShortestPathDistanceMetric.ShortestPathResult;
import edu.princeton.safe.internal.SparseNetworkProvider;
import edu.princeton.safe.io.NetworkConsumer;
import edu.princeton.safe.io.NetworkParser;

public class ShortestPathDistanceMetricTest {
    protected static final double DEFAULT_DELTA = 1e-8;

    static void makeNetwork(NetworkConsumer consumer) {
        consumer.startNodes();
        consumer.node(0, "A", "A", 0, 0);
        consumer.node(1, "B", "B", 0, 0);
        consumer.node(2, "C", "C", 0, 0);
        consumer.node(3, "D", "D", 0, 0);
        consumer.node(4, "E", "E", 0, 0);
        consumer.node(5, "F", "F", 0, 0);
        consumer.finishNodes();

        consumer.startEdges();
        consumer.edge(3, 0, 0.45);
        consumer.edge(5, 1, 0.41);
        consumer.edge(1, 2, 0.51);
        consumer.edge(4, 2, 0.32);
        consumer.edge(5, 2, 0.29);
        consumer.edge(2, 3, 0.15);
        consumer.edge(4, 3, 0.36);
        consumer.edge(0, 4, 0.21);
        consumer.edge(1, 4, 0.32);
        consumer.edge(0, 5, 0.99);
        consumer.edge(3, 5, 0.38);
        consumer.finishEdges();
    }
    
    @Test
    public void testJohnson() throws IOException {
        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();

        NetworkParser parser = new NetworkParser() {
            @Override
            public void parse(NetworkConsumer consumer) throws IOException {
                makeNetwork(consumer);
            }

            @Override
            public boolean isDirected() {
                return true;
            }
        };
        NetworkProvider networkProvider = new SparseNetworkProvider(parser);

        double[][] expected = { { 0.00, 1.36, 0.53, 0.57, 0.21, 0.95 }, { 1.11, 0.00, 0.51, 0.66, 0.32, 1.04 },
                                { 0.60, 0.94, 0.00, 0.15, 0.81, 0.53 }, { 0.45, 0.79, 0.67, 0.00, 0.66, 0.38 },
                                { 0.81, 1.15, 0.32, 0.36, 0.00, 0.74 }, { 0.89, 0.41, 0.29, 0.44, 0.73, 0.00 }, };

        metric.johnson(networkProvider, (int u,
                                         int v) -> networkProvider.getWeight(u, v),
                       new NodeDistanceConsumer() {
                           @Override
                           public void accept(int fromIndex,
                                              int toIndex,
                                              double distance) {

                               Assert.assertEquals(expected[fromIndex][toIndex], distance, DEFAULT_DELTA);
                           }
                       });
    }

    @Test
    public void testJohnsonUndirected() throws IOException {
        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();

        NetworkParser parser = new NetworkParser() {
            @Override
            public void parse(NetworkConsumer consumer) throws IOException {
                makeNetwork(consumer);
            }

            @Override
            public boolean isDirected() {
                return false;
            }
        };
        NetworkProvider networkProvider = new SparseNetworkProvider(parser);

        double[][] expected = { { 0.00, 0.53, 0.53, 0.45, 0.21, 0.82 }, { 0.53, 0.00, 0.51, 0.66, 0.32, 0.41 },
                                { 0.53, 0.51, 0.00, 0.15, 0.32, 0.29 }, { 0.45, 0.66, 0.15, 0.00, 0.36, 0.38 },
                                { 0.21, 0.32, 0.32, 0.36, 0.00, 0.61 }, { 0.82, 0.41, 0.29, 0.38, 0.61, 0.00 }, };

        metric.johnson(networkProvider, (int u,
                                         int v) -> networkProvider.getWeight(u, v),
                       new NodeDistanceConsumer() {
                           @Override
                           public void accept(int fromIndex,
                                              int toIndex,
                                              double distance) {

                               Assert.assertEquals(expected[fromIndex][toIndex], distance, DEFAULT_DELTA);
                           }
                       });
    }

    @Test
    public void testDijkstra() throws IOException {
        MapBasedDistanceMetric metric = new MapBasedDistanceMetric();

        NetworkParser parser = new NetworkParser() {
            @Override
            public void parse(NetworkConsumer consumer) throws IOException {
                consumer.startNodes();
                consumer.node(0, "A", "A", 0, 0);
                consumer.node(1, "B", "B", 0, 0);
                consumer.node(2, "C", "C", 0, 0);
                consumer.node(3, "D", "D", 0, 0);
                consumer.node(4, "E", "E", 0, 0);
                consumer.node(5, "F", "F", 0, 0);
                consumer.finishNodes();

                consumer.startEdges();
                consumer.edge(3, 0, 0.45);
                consumer.edge(5, 1, 0.41);
                consumer.edge(1, 2, 0.51);
                consumer.edge(4, 2, 0.32);
                consumer.edge(5, 2, 0.29);
                consumer.edge(2, 3, 0.15);
                consumer.edge(4, 3, 0.36);
                consumer.edge(0, 4, 0.21);
                consumer.edge(1, 4, 0.32);
                consumer.edge(0, 5, 0.99);
                consumer.edge(3, 5, 0.38);
                consumer.finishEdges();
            }

            @Override
            public boolean isDirected() {
                return true;
            }
        };
        NetworkProvider networkProvider = new SparseNetworkProvider(parser);
        int source = 0;
        ShortestPathResult shortest = metric.dijkstra(networkProvider, (int u,
                                                                        int v) -> networkProvider.getWeight(u, v),
                                                      source);

        double[] expected = { 0.00, 1.36, 0.53, 0.57, 0.21, 0.95 };
        for (int i = 0; i < shortest.distances.length; i++) {
            Assert.assertEquals(expected[i], shortest.distances[i], DEFAULT_DELTA);
        }
    }
}
