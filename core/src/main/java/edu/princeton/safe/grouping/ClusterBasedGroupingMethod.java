package edu.princeton.safe.grouping;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.cluster.Dendrogram;
import edu.princeton.safe.internal.cluster.DendrogramBuilder;
import edu.princeton.safe.internal.cluster.DendrogramNode;
import edu.princeton.safe.internal.cluster.ObservationNode;
import edu.princeton.safe.internal.fastcluster.HierarchicalClusterer;
import edu.princeton.safe.internal.fastcluster.MethodCode;
import edu.princeton.safe.internal.fastcluster.Node;
import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public class ClusterBasedGroupingMethod implements GroupingMethod {

    double threshold;
    DistanceMethod distanceMethod;

    public ClusterBasedGroupingMethod(double threshold,
                                      DistanceMethod distanceMethod) {
        this.threshold = threshold;
        this.distanceMethod = distanceMethod;
    }

    @Override
    public String getId() {
        return distanceMethod.getId();
    }

    @Override
    public void group(EnrichmentLandscape landscape,
                      CompositeMap compositeMap,
                      int typeIndex,
                      DomainConsumer consumer,
                      ProgressReporter progressReporter) {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();

        IntArrayList filteredIndexes = new IntArrayList();
        for (int i = 0; i < totalAttributes; i++) {
            if (compositeMap.isTop(i, typeIndex)) {
                filteredIndexes.add(i);
            }
        }
        int totalFiltered = filteredIndexes.size();
        if (totalFiltered < 2) {
            progressReporter.setStatus("Warning: Less than two attributes remain after filtering", totalFiltered);

            if (totalFiltered == 1) {
                consumer.startDomain(typeIndex);
                int attributeIndex = filteredIndexes.get(0);
                consumer.attribute(attributeIndex);
                consumer.endDomain();
            }
            return;
        }

        progressReporter.setStatus("Top attributes: %d", totalFiltered);

        progressReporter.setStatus("Computing attribute distances...");
        double[][] scores = computeScores(landscape, totalAttributes, filteredIndexes, typeIndex);
        progressReporter.setStatus("Computing dissimilarity matrix...");
        double[] distances = pdist(scores, distanceMethod);

        List<IntArrayList> clusters = computeClusters(totalFiltered, distances, progressReporter, annotationProvider,
                                                      filteredIndexes);

        progressReporter.setStatus("Assigning clusters...");
        // Populate domains with attribute indexes.
        for (IntArrayList cluster : clusters) {
            if (cluster.isEmpty()) {
                continue;
            }
            consumer.startDomain(typeIndex);
            for (IntCursor cursor : cluster) {
                int attributeIndex = filteredIndexes.get(cursor.value);
                consumer.attribute(attributeIndex);
            }
            consumer.endDomain();
        }

    }

    static double[][] computeScores(EnrichmentLandscape result,
                                    int totalAttributes,
                                    IntArrayList attributeIndexes,
                                    int typeIndex) {

        ScoringFunction score = Neighborhood.getScoringFunction(typeIndex);
        List<? extends Neighborhood> neighborhoods = result.getNeighborhoods();
        int filteredAttributes = attributeIndexes.size();
        double[][] scores = new double[filteredAttributes][];
        IntStream.range(0, filteredAttributes)
                 .parallel()
                 .forEach(filteredIndex -> {
                     int attributeIndex = attributeIndexes.get(filteredIndex);
                     scores[filteredIndex] = neighborhoods.stream()
                                                          .mapToDouble(n -> score.get(n, attributeIndex))
                                                          .toArray();
                 });
        return scores;
    }

    List<IntArrayList> computeClusters(int totalObservations,
                                       double[] condensedDistances,
                                       ProgressReporter progressReporter,
                                       AnnotationProvider provider,
                                       IntArrayList filtered) {

        progressReporter.setStatus("Computing cluster tree...");

        int[] members = new int[totalObservations];
        IntStream.range(0, members.length)
                 .forEach(i -> members[i] = 1);

        List<Node> linkages = HierarchicalClusterer.NN_chain_core(totalObservations, condensedDistances, members,
                                                                  MethodCode.METHOD_METR_AVERAGE);
        OptionalDouble maximumDissimilarity = linkages.stream()
                                                      .mapToDouble(linkage -> linkage.dist)
                                                      .max();

        DendrogramBuilder builder = new DendrogramBuilder(totalObservations);
        HierarchicalClusterer.buildClusters(false, linkages, builder);
        DendrogramNode root = builder.getRoot();
        double height = maximumDissimilarity.getAsDouble();
        List<DendrogramNode> roots = Dendrogram.cut(root, threshold * height);

        progressReporter.setStatus("Cluster tree height: %f", height);

        return roots.stream()
                    .map(node -> getObservations(node))
                    .collect(Collectors.toList());
    }

    static IntArrayList getObservations(DendrogramNode node) {
        IntArrayList result = new IntArrayList();
        getObservations(result, node);
        return result;
    }

    static void getObservations(IntArrayList result,
                                DendrogramNode node) {
        if (node instanceof ObservationNode) {
            int observation = ((ObservationNode) node).getObservation();
            result.add(observation);
            return;
        }
        getObservations(result, node.getLeft());
        getObservations(result, node.getRight());
    }

    static double[] pdist(double[][] distances,
                          DistanceMethod method) {
        int totalRows = distances.length;
        double[] result = new double[totalRows * (totalRows - 1) / 2];

        IntStream.range(0, totalRows)
                 .parallel()
                 .forEach(i -> {
                     for (int j = i + 1; j < totalRows; j++) {
                         int resultIndex = getIndex(totalRows, i, j);
                         double value = method.apply(distances[i], distances[j]);
                         result[resultIndex] = value;
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
