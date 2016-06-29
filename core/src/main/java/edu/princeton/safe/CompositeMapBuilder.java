package edu.princeton.safe;

import edu.princeton.safe.model.CompositeMap;

public interface CompositeMapBuilder {

    CompositeMapBuilder setRestrictionMethod(RestrictionMethod method);

    CompositeMapBuilder setGroupingMethod(GroupingMethod method);

    CompositeMapBuilder setMinimumLandscapeSize(int minimum);

    CompositeMapBuilder setProgressReporter(ProgressReporter reporter);

    CompositeMap build();

}
