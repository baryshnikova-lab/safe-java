package edu.princeton.safe;

import edu.princeton.safe.model.Neighborhood;

public interface NeighborhoodFactory<T extends Neighborhood> {
    T create(int nodeIndex);
}