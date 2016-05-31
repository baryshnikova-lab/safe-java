package edu.princeton.safe;

import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface EnrichmentLandscapeBuilder {

    EnrichmentLandscapeBuilder setNetworkProvider(NetworkProvider provider);

    EnrichmentLandscapeBuilder setAnnotationProvider(AnnotationProvider provider);

    EnrichmentLandscapeBuilder setDistanceMetric(DistanceMetric metric);

    EnrichmentLandscapeBuilder setDistanceThresholdAbsolute(boolean isAbsolute);

    EnrichmentLandscapeBuilder setDistanceThreshold(double threshold);

    EnrichmentLandscapeBuilder setBackgroundMethod(BackgroundMethod method);

    EnrichmentLandscapeBuilder setQuantitativeIterations(int iterations);

    EnrichmentLandscapeBuilder setRandomSeed(int seed);

    EnrichmentLandscapeBuilder setProgressReporter(ProgressReporter reporter);

    EnrichmentLandscape build();
}
