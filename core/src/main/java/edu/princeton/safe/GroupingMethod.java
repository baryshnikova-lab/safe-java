package edu.princeton.safe;

import edu.princeton.safe.model.DomainDetails;
import edu.princeton.safe.model.EnrichmentLandscape;

public interface GroupingMethod {

    DomainDetails group(EnrichmentLandscape result,
                        int typeIndex);

}
