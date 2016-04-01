package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.NeighborhoodMethod;
import edu.princeton.safe.NetworkProvider;
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

    public ParallelSafe(NetworkProvider networkProvider, AnnotationProvider annotationProvider,
            NeighborhoodMethod neighborhoodMethod, RestrictionMethod restrictionMethod, GroupingMethod groupingMethod,
            OutputMethod outputMethod) {

        this.networkProvider = networkProvider;
        this.annotationProvider = annotationProvider;
        this.neighborhoodMethod = neighborhoodMethod;
        this.restrictionMethod = restrictionMethod;
        this.groupingMethod = groupingMethod;
        this.outputMethod = outputMethod;
    }

    @Override
    public void apply() {
        // compute maximum distance threshold
        // compute neighborhoods for each node
        // compute enrichment for each neighborhood -> p-values
        // detect whether binary or quantitative
        // convert neighborhood p-values to enrichment scores
        // cache enrichment scores to visualize individual attributes later
        // apply restriction method, if any
        // group attributes into domains
        // assign unique color to each domain
        // compute color for each node
        // apply output method
    }

}
