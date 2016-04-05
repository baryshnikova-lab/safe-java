package edu.princeton.safe;

public interface NetworkProvider {

    int getNodeCount();

    double getDistance(int i, int j);

}
