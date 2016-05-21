package edu.princeton.safe;

import edu.princeton.safe.model.DomainDetails;
import edu.princeton.safe.model.SafeResult;

public interface GroupingMethod {

    DomainDetails group(SafeResult result,
                        int typeIndex);

}
