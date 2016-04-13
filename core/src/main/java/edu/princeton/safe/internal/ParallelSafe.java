package edu.princeton.safe.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.apache.commons.math3.util.CentralPivotingStrategy;
import org.apache.commons.math3.util.KthSelector;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.FunctionalAttribute;
import edu.princeton.safe.FunctionalGroup;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;

public class ParallelSafe implements Safe {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    DistanceMetric distanceMetric;
    RestrictionMethod restrictionMethod;
    GroupingMethod groupingMethod;
    OutputMethod outputMethod;
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
                        OutputMethod outputMethod) {

        this.networkProvider = networkProvider;
        this.annotationProvider = annotationProvider;
        this.distanceMetric = neighborhoodMethod;
        this.restrictionMethod = restrictionMethod;
        this.groupingMethod = groupingMethod;
        this.outputMethod = outputMethod;

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
            computeBinaryEnrichment(annotationProvider, neighborhoods);
        } else {
            computeQuantitativeEnrichment(networkProvider, annotationProvider, neighborhoods);
        }
    }

    static double getEnrichmentThreshold(int totalAttributes) {
        return -Math.log10(0.05 / totalAttributes) / -Neighborhood.LOG10P;
    }

    static void computeQuantitativeEnrichment(NetworkProvider networkProvider,
                                              AnnotationProvider annotationProvider,
                                              Neighborhood[] neighborhoods) {
        int totalNodes = networkProvider.getNodeCount();

        int totalPermutations = 1000;
        int[][] permutations = new int[totalPermutations][];
        int seed = 0;
        RandomDataGenerator random = new RandomDataGenerator(new Well44497b(seed));
        for (int i = 0; i < totalPermutations; i++) {
            permutations[i] = random.nextPermutation(totalNodes, totalNodes);
        }

        AtomicInteger totalSignificant = new AtomicInteger();
        double enrichmentThreshold = getEnrichmentThreshold(annotationProvider.getAttributeCount());

        Arrays.stream(neighborhoods)
              .parallel()
              .forEach(new Consumer<Neighborhood>() {
                  @Override
                  public void accept(Neighborhood neighborhood) {
                      int significant = 0;
                      NormalDistribution distribution = new NormalDistribution();
                      for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                          final int attributeIndex = j;
                          double[] neighborhoodScore = { 0 };
                          double[] randomScore = new double[totalPermutations];
                          neighborhood.forEachNodeIndex(new IntConsumer() {
                              @Override
                              public void accept(int index) {
                                  neighborhoodScore[0] += annotationProvider.getValue(index, attributeIndex);

                                  for (int r = 0; r < totalPermutations; r++) {
                                      int randomIndex = permutations[r][index];
                                      randomScore[r] += annotationProvider.getValue(randomIndex, attributeIndex);
                                  }
                              }
                          });

                          SummaryStatistics statistics = new SummaryStatistics();
                          for (int r = 0; r < totalPermutations; r++) {
                              statistics.addValue(randomScore[r]);
                          }

                          double z = (neighborhoodScore[0] - statistics.getMean()) / statistics.getStandardDeviation();
                          double pValue = 1 - distribution.cumulativeProbability(z);

                          double score = Neighborhood.computeEnrichmentScore(pValue);
                          neighborhood.setSignificance(j, pValue);
                          if (score > enrichmentThreshold) {
                              significant++;
                          }
                      }
                      totalSignificant.addAndGet(significant);
                  }
              });
        System.out.printf("Significant: %d\n", totalSignificant.get());
    }

    static void computeBinaryEnrichment(AnnotationProvider annotationProvider,
                                        Neighborhood[] neighborhoods) {

        int totalNodes = annotationProvider.getNodeCount();
        AtomicInteger totalSignificant = new AtomicInteger();

        double enrichmentThreshold = getEnrichmentThreshold(annotationProvider.getAttributeCount());

        Arrays.stream(neighborhoods)
              .parallel()
              .forEach(new Consumer<Neighborhood>() {
                  @Override
                  public void accept(Neighborhood neighborhood) {
                      int significant = 0;
                      int neighborhoodSize = neighborhood.getNodeCount();
                      for (int j = 0; j < annotationProvider.getAttributeCount(); j++) {
                          int totalNodesForFunction = annotationProvider.getNodeCountForAttribute(j);
                          int totalNeighborhoodNodesForFunction = neighborhood.getNodeCountForAttribute(j,
                                                                                                        annotationProvider);

                          HypergeometricDistribution distribution = new HypergeometricDistribution(totalNodes,
                                                                                                   totalNodesForFunction,
                                                                                                   neighborhoodSize);
                          double p = 1.0 - distribution.cumulativeProbability(totalNeighborhoodNodesForFunction);
                          double score = Neighborhood.computeEnrichmentScore(p);
                          // System.out.printf("%g\t%f\t%f\n", p, score,
                          // enrichmentThreshold);
                          neighborhood.setSignificance(j, p);
                          if (score > enrichmentThreshold) {
                              significant++;
                          }
                      }
                      totalSignificant.addAndGet(significant);
                  }
              });
        System.out.printf("Significant: %d\n", totalSignificant.get());
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
                // neighborhood = new SparseNeighborhood(fromIndex);
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

    static double binomialCoefficient(int n,
                                      int k) {
        double result = 1;
        for (int i = 0; i < k; i++) {
            result *= (double) (n - i) / (k - i);
        }
        return result;
    }
}
