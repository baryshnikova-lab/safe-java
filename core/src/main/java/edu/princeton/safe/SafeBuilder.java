package edu.princeton.safe;

public interface SafeBuilder {
    SafeBuilder setNetworkProvider(NetworkProvider provider);

    SafeBuilder setAnnotationProvider(AnnotationProvider provider);

    SafeBuilder setDistanceMetric(DistanceMetric method);

    SafeBuilder setRestrictionMethod(RestrictionMethod method);

    SafeBuilder setGroupingMethod(GroupingMethod method);

    SafeBuilder setOutputMethod(OutputMethod method);

    Safe build() throws ConfigurationException;
}
