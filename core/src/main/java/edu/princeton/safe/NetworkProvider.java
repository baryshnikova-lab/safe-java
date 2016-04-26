package edu.princeton.safe;

import java.util.function.IntConsumer;

public interface NetworkProvider {

    int getNodeCount();

    double getDistance(int fromNode,
                       int toNode);

    double getWeight(int fromNode,
                     int toNode);

    String getNodeLabel(int nodeIndex);

    String getNodeId(int nodeIndex);

    void forEachNeighbor(int nodeIndex,
                         IntConsumer consumer);

}
