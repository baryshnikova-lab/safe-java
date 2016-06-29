package edu.princeton.safe;

import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface GroupingMethod extends Identifiable {

    void group(EnrichmentLandscape result,
               CompositeMap compositeMap,
               int typeIndex,
               DomainConsumer consumer,
               ProgressReporter progressReporter);

}
