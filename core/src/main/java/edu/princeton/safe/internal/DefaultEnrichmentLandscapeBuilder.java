package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.EnrichmentLandscapeBuilder;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.model.EnrichmentLandscape;

public class DefaultEnrichmentLandscapeBuilder implements EnrichmentLandscapeBuilder {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    DistanceMetric distanceMetric;
    boolean isDistanceThresholdAbsolute;
    double distanceThreshold;
    BackgroundMethod backgroundMethod;
    int quantitativeIterations;
    int randomSeed;
    ProgressReporter progressReporter;

    @Override
    public EnrichmentLandscapeBuilder setNetworkProvider(NetworkProvider provider) {
        this.networkProvider = provider;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setAnnotationProvider(AnnotationProvider provider) {
        this.annotationProvider = provider;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setDistanceMetric(DistanceMetric metric) {
        this.distanceMetric = metric;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setDistanceThresholdAbsolute(boolean isAbsolute) {
        this.isDistanceThresholdAbsolute = isAbsolute;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setDistanceThreshold(double threshold) {
        this.distanceThreshold = threshold;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setBackgroundMethod(BackgroundMethod method) {
        this.backgroundMethod = method;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setQuantitativeIterations(int iterations) {
        this.quantitativeIterations = iterations;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setRandomSeed(int seed) {
        this.randomSeed = seed;
        return this;
    }

    @Override
    public EnrichmentLandscapeBuilder setProgressReporter(ProgressReporter reporter) {
        this.progressReporter = reporter;
        return this;
    }

    @Override
    public EnrichmentLandscape build() {
        int totalTypes = annotationProvider.isBinary() ? 1 : 2;
        DefaultEnrichmentLandscape result = new DefaultEnrichmentLandscape(annotationProvider, totalTypes);
        ParallelSafe.computeDistances(networkProvider, annotationProvider, distanceMetric, isDistanceThresholdAbsolute,
                                      distanceThreshold, result);

        ParallelSafe.computeNeighborhoods(result, networkProvider, annotationProvider);
        ParallelSafe.computeEnrichment(networkProvider, annotationProvider, backgroundMethod, quantitativeIterations,
                                       randomSeed, progressReporter, result);
        return result;
    }

}
