package edu.princeton.safe.internal;

import java.util.List;
import java.util.stream.Stream;

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
    List<NodePair> distances;
    double maximumDistanceThreshold;
    List<Neighborhood> neighborhoods;
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

        computeNeighborhoods(distances, maximumDistanceThreshold);
        computeEnrichment(neighborhoods);

        computeGroups(attributes);

        applyColors(groups);

        outputMethod.apply(distances, maximumDistanceThreshold, neighborhoods, attributes, groups);
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

    List<FunctionalAttribute> computeEnrichment(List<Neighborhood> neighborhoods) {
        // TODO Auto-generated method stub

        // compute enrichment for each neighborhood -> p-values
        // detect whether binary or quantitative
        // convert neighborhood p-values to enrichment scores
        return null;
    }

    List<Neighborhood> computeNeighborhoods(List<NodePair> distances,
                                            double maximumDistanceThreshold) {
        // TODO Auto-generated method stub
        return null;
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
        if (distances != null) {
            return;
        }

        distances = distanceMetric.computeDistances(networkProvider);
        maximumDistanceThreshold = computeMaximumDistanceThreshold(distances, distancePercentile);
    }

}
