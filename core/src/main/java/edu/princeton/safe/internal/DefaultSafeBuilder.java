package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.ConfigurationException;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;
import edu.princeton.safe.SafeBuilder;
import edu.princeton.safe.distance.MapBasedDistanceMetric;

public class DefaultSafeBuilder implements SafeBuilder {

    NetworkProvider networkProvider;
    AnnotationProvider annotationProvider;
    DistanceMetric distanceMetric;
    BackgroundMethod backgroundMethod;
    RestrictionMethod restrictionMethod;
    GroupingMethod groupingMethod;
    OutputMethod outputMethod;
    DefaultProgressReporter progressReporter;
    boolean isDistanceThresholdAbsolute;
    double distanceThreshold;
    int empiricalIterations;

    public DefaultSafeBuilder() {
        progressReporter = new DefaultProgressReporter();
        isDistanceThresholdAbsolute = false;
        distanceThreshold = 0.5;
        empiricalIterations = 1000;
    }

    @Override
    public SafeBuilder setNetworkProvider(NetworkProvider provider) {
        networkProvider = provider;
        return this;
    }

    @Override
    public SafeBuilder setAnnotationProvider(AnnotationProvider provider) {
        annotationProvider = provider;
        return this;
    }

    @Override
    public SafeBuilder setDistanceMetric(DistanceMetric metric) {
        distanceMetric = metric;
        return this;
    }

    @Override
    public SafeBuilder setBackgroundMethod(BackgroundMethod method) {
        backgroundMethod = method;
        return this;
    }

    @Override
    public SafeBuilder setRestrictionMethod(RestrictionMethod method) {
        restrictionMethod = method;
        return this;
    }

    @Override
    public SafeBuilder setGroupingMethod(GroupingMethod method) {
        groupingMethod = method;
        return this;
    }

    @Override
    public SafeBuilder setOutputMethod(OutputMethod method) {
        outputMethod = method;
        return this;
    }

    @Override
    public SafeBuilder setDistancePercentile(double percentile) {
        isDistanceThresholdAbsolute = false;
        distanceThreshold = percentile;
        return this;
    }

    @Override
    public SafeBuilder setDistanceThreshold(double threshold) {
        isDistanceThresholdAbsolute = true;
        distanceThreshold = threshold;
        return this;
    }

    @Override
    public SafeBuilder addProgressReporter(ProgressReporter reporter) {
        progressReporter.add(reporter);
        return this;
    }

    @Override
    public Safe build() throws ConfigurationException {
        validateSettings();
        return new ParallelSafe(networkProvider, annotationProvider, distanceMetric, backgroundMethod,
                                restrictionMethod, groupingMethod, outputMethod, isDistanceThresholdAbsolute,
                                distanceThreshold, empiricalIterations, progressReporter);
    }

    void validateSettings() throws ConfigurationException {
        if (networkProvider == null) {
            throw new ConfigurationException("NetworkProvider was not provided");
        }

        if (annotationProvider == null) {
            throw new ConfigurationException("AnnotationProvider was not provided");
        }

        if (distanceMetric == null) {
            distanceMetric = new MapBasedDistanceMetric();
        }
    }

}
