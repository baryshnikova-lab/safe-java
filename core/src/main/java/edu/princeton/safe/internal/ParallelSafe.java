package edu.princeton.safe.internal;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

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
import edu.princeton.safe.model.FunctionalGroup;
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
        DefaultSafeResult result = new DefaultSafeResult();
        computeDistances(result);

        computeNeighborhoods(result, networkProvider, annotationProvider);
        computeEnrichment(result);
        applyRestriction(result, restrictionMethod);
        computeGroups(annotationProvider, result, groupingMethod);

        applyColors(result);

        outputMethod.apply(result);
    }

    static void applyRestriction(DefaultSafeResult result,
                                 RestrictionMethod method) {
        if (method == null) {
            return;
        }
        result.neighborhoods.stream()
                            .forEach(n -> n.setHighest(method.shouldInclude(result, n)));
    }

    static void applyColors(DefaultSafeResult result) {
        // TODO Auto-generated method stub
        // assign unique color to each domain
        // compute color for each node
    }

    static List<FunctionalGroup> computeGroups(AnnotationProvider annotationProvider,
                                               DefaultSafeResult result,
                                               GroupingMethod method) {
        return null;
    }

    void computeEnrichment(DefaultSafeResult result) {
        if (annotationProvider.isBinary()) {
            computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter, result.neighborhoods,
                                    backgroundMethod);
        } else {
            int totalPermutations = 1000;
            int seed = 0;
            RandomGenerator generator = new Well44497b(seed);

            int totalNodes = networkProvider.getNodeCount();
            NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                        totalPermutations, totalNodes);
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
        stream.forEach(new Consumer<Neighborhood>() {
            @Override
            public void accept(Neighborhood neighborhood) {
                for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                    final int attributeIndex = j;
                    double[] neighborhoodScore = { 0 };

                    neighborhood.forEachMemberIndex(new IntConsumer() {
                        @Override
                        public void accept(int index) {
                            double value = annotationProvider.getValue(index, attributeIndex);
                            if (!Double.isNaN(value)) {
                                neighborhoodScore[0] += value;
                            }
                        }
                    });

                    double[] randomScores = scoringMethod.computeRandomizedScores(neighborhood, j);
                    SummaryStatistics statistics = new SummaryStatistics();
                    for (int r = 0; r < randomScores.length; r++) {
                        if (!Double.isNaN(randomScores[r])) {
                            statistics.addValue(randomScores[r]);
                        }
                    }

                    double p = 1
                            - Probability.normal(statistics.getMean(), statistics.getVariance(), neighborhoodScore[0]);

                    neighborhood.setPValue(j, p);

                    double score = Neighborhood.computeEnrichmentScore(p);
                    int nodeIndex = neighborhood.getNodeIndex();
                    progressReporter.neighborhoodScore(nodeIndex, j, score);
                }
            }
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
        stream.forEach(new Consumer<Neighborhood>() {
            @Override
            public void accept(Neighborhood neighborhood) {
                int neighborhoodSize = neighborhood.getMemberCount();
                for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                    int totalNodesForFunction = nodeCount.apply(j);
                    int totalNeighborhoodNodesForFunction = neighborhood.getMemberCountForAttribute(j,
                                                                                                    annotationProvider);

                    HypergeometricDistribution distribution = new HypergeometricDistribution(null, totalNodes,
                                                                                             totalNodesForFunction,
                                                                                             neighborhoodSize);
                    double p = 1.0 - distribution.cumulativeProbability(totalNeighborhoodNodesForFunction - 1);
                    neighborhood.setPValue(j, p);

                    double score = Neighborhood.computeEnrichmentScore(p);
                    int nodeIndex = neighborhood.getNodeIndex();
                    progressReporter.neighborhoodScore(nodeIndex, j, score);
                }
            }
        });
        progressReporter.finishNeighborhoodScore();
    }

    static void computeNeighborhoods(DefaultSafeResult result,
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

    void computeDistances(DefaultSafeResult result) {
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
