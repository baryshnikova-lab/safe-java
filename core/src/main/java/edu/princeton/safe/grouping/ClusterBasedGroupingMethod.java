package edu.princeton.safe.grouping;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;
import org.opencompare.hac.ClusteringBuilder;
import org.opencompare.hac.HierarchicalAgglomerativeClusterer;
import org.opencompare.hac.agglomeration.AgglomerationMethod;
import org.opencompare.hac.agglomeration.SingleLinkage;
import org.opencompare.hac.dendrogram.Dendrogram;
import org.opencompare.hac.dendrogram.DendrogramBuilder;
import org.opencompare.hac.dendrogram.DendrogramNode;
import org.opencompare.hac.dendrogram.MergeNode;
import org.opencompare.hac.dendrogram.ObservationNode;
import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.model.SafeResult;

public class ClusterBasedGroupingMethod {

    double threshold;

    public ClusterBasedGroupingMethod(double threshold) {
        this.threshold = threshold;
    }

    void group(SafeResult result,
               DistanceMethod method) {
        AnnotationProvider annotationProvider = result.getAnnotationProvider();
        List<? extends Neighborhood> neighborhoods = result.getNeighborhoods();
        int totalAttributes = annotationProvider.getAttributeCount();
        double[][] scores = new double[totalAttributes][];
        IntStream.range(0, totalAttributes)
                 .parallel()
                 .forEach(new IntConsumer() {
                     @Override
                     public void accept(int attributeIndex) {
                         scores[attributeIndex] = neighborhoods.stream()
                                                               .mapToDouble(n -> n.getEnrichmentScore(attributeIndex))
                                                               .toArray();
                     }
                 });
        double[] distances = pdist(scores, method);
        DendrogramBuilder builder = new DendrogramBuilder(totalAttributes);
        buildCluster(distances, scores.length, builder);
    }

    static void buildCluster(double[] condensedDistances,
                             int totalObservations,
                             ClusteringBuilder builder) {
        Experiment experiment = new Experiment() {
            @Override
            public int getNumberOfObservations() {
                return totalObservations;
            }
        };
        DissimilarityMeasure dissimilarityMeasure = new DissimilarityMeasure() {
            @Override
            public double computeDissimilarity(Experiment experiment,
                                               int i,
                                               int j) {
                int n = experiment.getNumberOfObservations();
                if (i == j) {
                    return 0;
                }
                if (i > j) {
                    return condensedDistances[getIndex(n, j, i)];
                }
                return condensedDistances[getIndex(n, i, j)];
            }
        };
        AgglomerationMethod agglomerationMethod = new SingleLinkage();
        HierarchicalAgglomerativeClusterer clusterer = new HierarchicalAgglomerativeClusterer(experiment,
                                                                                              dissimilarityMeasure,
                                                                                              agglomerationMethod);
        clusterer.cluster(builder);
    }

    static void cut(Dendrogram dendrogram,
                    double threshold,
                    ClusterConsumer consumer) {
        FibonacciHeap<DendrogramNode> nodes = new FibonacciHeap<>();
        pushNode(nodes, dendrogram.getRoot(), consumer);
        while (!nodes.isEmpty()) {
            FibonacciHeapNode<DendrogramNode> heapNode = nodes.removeMin();
            double dissimilarity = heapNode.getKey();
            if (dissimilarity < threshold) {
                consumer.startCluster();
                collect(heapNode.getData(), consumer);
                consumer.endCluster();
            } else {
                DendrogramNode node = heapNode.getData();
                pushNode(nodes, node.getLeft(), consumer);
                pushNode(nodes, node.getRight(), consumer);
            }
        }
    }

    static void collect(DendrogramNode root,
                        ClusterConsumer consumer) {
        if (root == null) {
            return;
        }
        collect(root.getLeft(), consumer);
        collect(root.getRight(), consumer);
        if (root instanceof ObservationNode) {
            int observation = ((ObservationNode) root).getObservation();
            consumer.addMember(observation);
        }
    }

    static void pushNode(FibonacciHeap<DendrogramNode> nodes,
                         DendrogramNode node,
                         ClusterConsumer consumer) {
        if (node instanceof ObservationNode) {
            consumer.startCluster();
            collect(node, consumer);
            consumer.endCluster();
        } else if (node instanceof MergeNode) {
            MergeNode mergeNode = (MergeNode) node;
            nodes.insert(new FibonacciHeapNode<DendrogramNode>(node), mergeNode.getDissimilarity());
        }
    }

    static double[] pdist(double[][] distances,
                          DistanceMethod method) {
        int totalRows = distances.length;
        double[] result = new double[totalRows * (totalRows - 1) / 2];

        IntStream.range(0, totalRows)
                 .parallel()
                 .forEach(new IntConsumer() {
                     @Override
                     public void accept(int i) {
                         for (int j = i + 1; j < totalRows; j++) {
                             int resultIndex = getIndex(totalRows, i, j);
                             double value = method.apply(distances[i], distances[j]);
                             result[resultIndex] = value;
                         }
                     }
                 });
        return result;
    }

    static int getIndex(int totalRows,
                        int i,
                        int j) {
        // Assumes upper triangle only, where i < j < totalRows.
        // For i == j, assume -1.
        // For i > j, flip i and j.
        return i * (totalRows * 2 - i - 1) / 2 + j - i - 1;
    }

    static interface ClusterConsumer {

        void startCluster();

        void addMember(int observation);

        void endCluster();
    }
}
