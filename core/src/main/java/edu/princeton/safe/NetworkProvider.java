package edu.princeton.safe;

import java.util.List;
import java.util.function.IntConsumer;

public interface NetworkProvider {

    int getNodeCount();

    double getDistance(int fromNode,
                       int toNode);

    double getWeight(int fromNode,
                     int toNode);

    String getNodeLabel(int nodeIndex);

    List<String> getNodeIds(int nodeIndex);

    void forEachNeighbor(int nodeIndex,
                         IntConsumer consumer);

}
