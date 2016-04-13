package edu.princeton.safe;

public interface NetworkProvider {

    int getNodeCount();

    double getDistance(int fromNode,
                       int toNode);

    double getWeight(int fromNode,
                     int toNode);

    String getNodeLabel(int nodeIndex);

    String getNodeId(int nodeIndex);

}
