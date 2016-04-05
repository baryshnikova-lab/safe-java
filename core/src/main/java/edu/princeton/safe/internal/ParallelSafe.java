package edu.princeton.safe.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.apache.commons.math3.util.CentralPivotingStrategy;
import org.apache.commons.math3.util.KthSelector;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.FunctionalAttribute;
import edu.princeton.safe.FunctionalGroup;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NeighborhoodMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.NodePair;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;

public class ParallelSafe implements Safe {

    private NetworkProvider networkProvider;
    private AnnotationProvider annotationProvider;
    private NeighborhoodMethod neighborhoodMethod;
    private RestrictionMethod restrictionMethod;
    private GroupingMethod groupingMethod;
    private OutputMethod outputMethod;
    private List<NodePair> distances;
    private double maximumDistanceThreshold;
    private List<Neighborhood> neighborhoods;
    private List<FunctionalAttribute> attributes;
    private List<FunctionalGroup> groups;
    
    private double distancePercentile;

    public ParallelSafe(NetworkProvider networkProvider, AnnotationProvider annotationProvider,
            NeighborhoodMethod neighborhoodMethod, RestrictionMethod restrictionMethod, GroupingMethod groupingMethod,
            OutputMethod outputMethod) {

        this.networkProvider = networkProvider;
        this.annotationProvider = annotationProvider;
        this.neighborhoodMethod = neighborhoodMethod;
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

    private void applyColors(List<FunctionalGroup> groups) {
        // TODO Auto-generated method stub
        // assign unique color to each domain
        // compute color for each node
    }

    private List<FunctionalGroup> computeGroups(List<FunctionalAttribute> attributes) {
        Stream<FunctionalAttribute> filteredAttributes = applyRestriction(attributes);
        // TODO Auto-generated method stub
        return null;
    }

    private Stream<FunctionalAttribute> applyRestriction(List<FunctionalAttribute> attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    private List<FunctionalAttribute> computeEnrichment(List<Neighborhood> neighborhoods) {
        // TODO Auto-generated method stub

        // compute enrichment for each neighborhood -> p-values
        // detect whether binary or quantitative
        // convert neighborhood p-values to enrichment scores
        return null;
    }

    private List<Neighborhood> computeNeighborhoods(List<NodePair> distances, double maximumDistanceThreshold) {
        // TODO Auto-generated method stub
        return null;
    }

    private static double computeMaximumDistanceThreshold(List<NodePair> pairs, double percentileIndex) {
        double[] distances = pairs.stream()
                                  .mapToDouble(d -> d.getDistance())
                                  .toArray();
        Percentile percentile = new Percentile().withEstimationType(EstimationType.R_5)
                                                .withKthSelector(new KthSelector(new CentralPivotingStrategy()));
        return percentile.evaluate(distances, percentileIndex);
    }

    private void computeDistances() {
        if (distances != null) {
            return;
        }

        distances = computeDistances(networkProvider);
        maximumDistanceThreshold = computeMaximumDistanceThreshold(distances, distancePercentile);
    }

    private static List<NodePair> computeDistances(NetworkProvider networkProvider) {
        int totalNodes = networkProvider.getNodeCount();

        // Compute pair-wise distances and filter out NaNs.
        List<NodePair> distances = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            for (int j = 0; j < totalNodes; j++) {
                double distance = networkProvider.getDistance(i, j);
                if (!Double.isNaN(distance)) {
                    NodePair details = new DefaultNodePair(i, j);
                    details.setDistance(distance);
                    distances.add(details);
                }
            }
        }
        return distances;
    }

}
