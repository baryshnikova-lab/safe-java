package edu.princeton.safe;

public interface NeighborhoodFactory<T extends Neighborhood> {
    T create(int nodeIndex);
}