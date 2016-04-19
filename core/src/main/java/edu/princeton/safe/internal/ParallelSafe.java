package edu.princeton.safe.internal;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.apache.commons.math3.util.CentralPivotingStrategy;
import org.apache.commons.math3.util.KthSelector;

import cern.jet.stat.Probability;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.FunctionalAttribute;
import edu.princeton.safe.FunctionalGroup;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NeighborhoodScoringMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;
import edu.princeton.safe.internal.scoring.RandomizedMemberScoringMethod;

public class ParallelSafe implements Safe {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    DistanceMetric distanceMetric;
    RestrictionMethod restrictionMethod;
    GroupingMethod groupingMethod;
    OutputMethod outputMethod;
    ProgressReporter progressReporter;

    List<NodePair> nodePairs;
    double maximumDistanceThreshold;
    Neighborhood[] neighborhoods;
    List<FunctionalAttribute> attributes;
    List<FunctionalGroup> groups;

    double distancePercentile;

    public ParallelSafe(NetworkProvider networkProvider,
                        AnnotationProvider annotationProvider,
                        DistanceMetric neighborhoodMethod,
                        RestrictionMethod restrictionMethod,
                        GroupingMethod groupingMethod,
                        OutputMethod outputMethod,
                        ProgressReporter progressReporter) {

        this.networkProvider = networkProvider;
        this.annotationProvider = annotationProvider;
        this.distanceMetric = neighborhoodMethod;
        this.restrictionMethod = restrictionMethod;
        this.groupingMethod = groupingMethod;
        this.outputMethod = outputMethod;
        this.progressReporter = progressReporter;

        distancePercentile = 0.5;
    }

    @Override
    public void apply() {
        computeDistances();

        computeNeighborhoods(networkProvider, annotationProvider, nodePairs, maximumDistanceThreshold);
        computeEnrichment(neighborhoods);

        computeGroups(attributes);

        applyColors(groups);

        outputMethod.apply(nodePairs, maximumDistanceThreshold, neighborhoods, attributes, groups);
    }

    void applyColors(List<FunctionalGroup> groups) {
        // TODO Auto-generated method stub
        // assign unique color to each domain
        // compute color for each node
    }

    List<FunctionalGroup> computeGroups(List<FunctionalAttribute> attributes) {
        Stream<FunctionalAttribute> filteredAttributes = applyRestriction(attributes);
        // TODO Auto-generated method stub
        return null;
    }

    Stream<FunctionalAttribute> applyRestriction(List<FunctionalAttribute> attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    void computeEnrichment(Neighborhood[] neighborhoods) {
        if (annotationProvider.isBinary()) {
            computeBinaryEnrichment(networkProvider, annotationProvider, progressReporter, neighborhoods);
        } else {
            int totalPermutations = 1000;
            int seed = 0;
            RandomGenerator generator = new Well44497b(seed);

            int totalNodes = networkProvider.getNodeCount();
            NeighborhoodScoringMethod scoringMethod = new RandomizedMemberScoringMethod(annotationProvider, generator,
                                                                                        totalPermutations, totalNodes);
            computeQuantitativeEnrichment(networkProvider, annotationProvider, scoringMethod, progressReporter,
                                          neighborhoods);
        }
    }

    static void computeQuantitativeEnrichment(NetworkProvider networkProvider,
                                              AnnotationProvider annotationProvider,
                                              NeighborhoodScoringMethod scoringMethod,
                                              ProgressReporter progressReporter,
                                              Neighborhood[] neighborhoods) {

        Stream<Neighborhood> stream = Arrays.stream(neighborhoods);
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

                    neighborhood.forEachNodeIndex(new IntConsumer() {
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

                    neighborhood.setSignificance(j, p);

                    double score = Neighborhood.computeEnrichmentScore(p);
                    int nodeIndex = neighborhood.getNodeIndex();
                    progressReporter.neighborhoodScore(nodeIndex, j - 1, score);
                }
            }
        });
        progressReporter.finishNeighborhoodScore();
    }

    static void computeBinaryEnrichment(NetworkProvider networkProvider,
                                        AnnotationProvider annotationProvider,
                                        ProgressReporter progressReporter,
                                        Neighborhood[] neighborhoods) {

        int totalNodes = annotationProvider.getNodeCount();

        Stream<Neighborhood> stream = Arrays.stream(neighborhoods);
        if (progressReporter.supportsParallel()) {
            stream = stream.parallel();
        }

        progressReporter.startNeighborhoodScore(networkProvider, annotationProvider);
        stream.forEach(new Consumer<Neighborhood>() {
            @Override
            public void accept(Neighborhood neighborhood) {
                int neighborhoodSize = neighborhood.getNodeCount();
                for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                    int totalNodesForFunction = annotationProvider.getNodeCountForAttribute(j);
                    int totalNeighborhoodNodesForFunction = neighborhood.getNodeCountForAttribute(j,
                                                                                                  annotationProvider);

                    HypergeometricDistribution distribution = new HypergeometricDistribution(null, totalNodes,
                                                                                             totalNodesForFunction,
                                                                                             neighborhoodSize);
                    double p = 1.0 - distribution.cumulativeProbability(totalNeighborhoodNodesForFunction);
                    neighborhood.setSignificance(j, p);

                    double score = Neighborhood.computeEnrichmentScore(p);
                    int nodeIndex = neighborhood.getNodeIndex();
                    progressReporter.neighborhoodScore(nodeIndex, j - 1, score);
                }
            }
        });
        progressReporter.finishNeighborhoodScore();
    }

    static Neighborhood[] computeNeighborhoods(NetworkProvider networkProvider,
                                               AnnotationProvider annotationProvider,
                                               List<NodePair> pairs,
                                               double maximumDistanceThreshold) {

        int totalNodes = networkProvider.getNodeCount();
        int totalAttributes = annotationProvider.getAttributeCount();

        Neighborhood[] neighborhoods = new Neighborhood[totalNodes];
        for (NodePair pair : pairs) {
            if (pair.getDistance() >= maximumDistanceThreshold) {
                continue;
            }
            int fromIndex = pair.getFromIndex();
            Neighborhood neighborhood = neighborhoods[fromIndex];
            if (neighborhood == null) {
                neighborhood = new DenseNeighborhood(fromIndex, totalNodes, totalAttributes);
                neighborhoods[fromIndex] = neighborhood;
            }
            neighborhood.addNode(pair.getToIndex());
        }
        return neighborhoods;
    }

    static double computeMaximumDistanceThreshold(List<NodePair> pairs,
                                                  double percentileIndex) {
        double[] distances = pairs.stream()
                                  .mapToDouble(d -> d.getDistance())
                                  .toArray();
        Percentile percentile = new Percentile().withEstimationType(EstimationType.R_5)
                                                .withKthSelector(new KthSelector(new CentralPivotingStrategy()));
        return percentile.evaluate(distances, percentileIndex);
    }

    void computeDistances() {
        if (nodePairs != null) {
            return;
        }

        nodePairs = distanceMetric.computeDistances(networkProvider);
        maximumDistanceThreshold = computeMaximumDistanceThreshold(nodePairs, distancePercentile);
    }
}
