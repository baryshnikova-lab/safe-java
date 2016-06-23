package edu.princeton.safe;

import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface GroupingMethod extends Identifiable {

    void group(EnrichmentLandscape result,
               int typeIndex,
               DomainConsumer consumer);

}
