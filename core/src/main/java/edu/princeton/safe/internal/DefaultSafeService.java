package edu.princeton.safe.internal;

import edu.princeton.safe.SafeBuilder;
import edu.princeton.safe.SafeService;

public class DefaultSafeService implements SafeService {

	@Override
	public SafeBuilder createBuilder() {
		return new DefaultSafeBuilder();
	}

}
