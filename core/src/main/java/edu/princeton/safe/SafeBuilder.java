package edu.princeton.safe;

import edu.princeton.safe.internal.BackgroundMethod;

public interface SafeBuilder {
    SafeBuilder setNetworkProvider(NetworkProvider provider);

    SafeBuilder setAnnotationProvider(AnnotationProvider provider);

    SafeBuilder setDistanceMetric(DistanceMetric method);

    SafeBuilder setBackgroundMethod(BackgroundMethod method);

    SafeBuilder setRestrictionMethod(RestrictionMethod method);

    SafeBuilder setGroupingMethod(GroupingMethod method);

    SafeBuilder setOutputMethod(OutputMethod method);

    Safe build() throws ConfigurationException;

    SafeBuilder addProgressReporter(ProgressReporter reporter);

}
