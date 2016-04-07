package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.ConfigurationException;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.OutputMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.Safe;
import edu.princeton.safe.SafeBuilder;
import edu.princeton.safe.internal.distance.MapBasedDistanceMetric;

public class DefaultSafeBuilder implements SafeBuilder {

    private NetworkProvider networkProvider;
    private AnnotationProvider annotationProvider;
    private DistanceMetric distanceMetric;
    private RestrictionMethod restrictionMethod;
    private GroupingMethod groupingMethod;
    private OutputMethod outputMethod;

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
    public Safe build() throws ConfigurationException {
        validateSettings();
        // TODO Auto-generated method stub
        return new ParallelSafe(networkProvider, annotationProvider, distanceMetric, restrictionMethod,
                groupingMethod, outputMethod);
    }

    private void validateSettings() throws ConfigurationException {
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
