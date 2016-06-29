package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.CompositeMapBuilder;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.model.CompositeMap;

public class DefaultCompositeMapBuilder implements CompositeMapBuilder {

    DefaultEnrichmentLandscape landscape;
    RestrictionMethod restrictionMethod;
    GroupingMethod groupingMethod;
    int minimumLandscapeSize;
    ProgressReporter progressReporter;

    public DefaultCompositeMapBuilder(DefaultEnrichmentLandscape landscape) {
        this.landscape = landscape;
    }

    @Override
    public CompositeMapBuilder setRestrictionMethod(RestrictionMethod method) {
        restrictionMethod = method;
        return this;
    }

    @Override
    public CompositeMapBuilder setGroupingMethod(GroupingMethod method) {
        groupingMethod = method;
        return this;
    }

    @Override
    public CompositeMapBuilder setMinimumLandscapeSize(int minimum) {
        minimumLandscapeSize = minimum;
        return this;
    }

    @Override
    public CompositeMapBuilder setProgressReporter(ProgressReporter reporter) {
        this.progressReporter = reporter;
        return this;
    }

    @Override
    public CompositeMap build() {
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

        DefaultCompositeMap compositeMap = new DefaultCompositeMap(annotationProvider);

        ParallelSafe.computeUnimodality(landscape, compositeMap, restrictionMethod, progressReporter);
        ParallelSafe.computeGroups(landscape, compositeMap, groupingMethod, progressReporter);
        ParallelSafe.computeDomains(landscape, compositeMap, minimumLandscapeSize, progressReporter);

        return compositeMap;
    }

}
