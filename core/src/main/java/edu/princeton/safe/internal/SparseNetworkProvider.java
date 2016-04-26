package edu.princeton.safe.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.SparseRealMatrix;

import com.carrotsearch.hppc.BitSet;

import edu.princeton.safe.NetworkConsumer;
import edu.princeton.safe.NetworkParser;
import edu.princeton.safe.NetworkProvider;

public class SparseNetworkProvider implements NetworkProvider {
    List<Node> nodes;
    SparseRealMatrix weights;
    BitSet[] neighbors;

    public SparseNetworkProvider(NetworkParser parser) throws IOException {
        parser.parse(new NetworkConsumer() {

            @Override
            public void startNodes() {
                nodes = new ArrayList<>();
            }

            @Override
            public void startEdges() {
                int totalNodes = nodes.size();
                weights = new OpenMapRealMatrix(totalNodes, totalNodes);

                neighbors = new BitSet[totalNodes];
                for (int i = 0; i < neighbors.length; i++) {
                    neighbors[i] = new BitSet((int) (totalNodes / 4));
                }
            }

            @Override
            public void node(int nodeIndex,
                             String label,
                             String id,
                             double x,
                             double y) {
                Node node = new Node();
                node.label = label;
                node.id = id;
                node.x = x;
                node.y = y;
                nodes.add(node);
            }

            @Override
            public void finishNodes() {
            }

            @Override
            public void finishEdges() {
            }

            @Override
            public void edge(int fromIndex,
                             int toIndex,
                             double weight) {
                weights.setEntry(fromIndex, toIndex, weight);
                neighbors[fromIndex].set(toIndex);
                
                if (!parser.isDirected()) {
                    weights.setEntry(toIndex, fromIndex, weight);
                    neighbors[toIndex].set(fromIndex);
                }
            }
        });
    }

    @Override
    public int getNodeCount() {
        return nodes.size();
    }

    @Override
    public void forEachNeighbor(int nodeIndex,
                                IntConsumer consumer) {
        BitSet nodes = neighbors[nodeIndex];
        int index = nodes.nextSetBit(0);
        while (index != -1) {
            consumer.accept(index);
            index = nodes.nextSetBit(index + 1);
        }
    }

    @Override
    public double getDistance(int fromNodeIndex,
                              int toNodeIndex) {
        Node node1 = nodes.get(fromNodeIndex);
        Node node2 = nodes.get(toNodeIndex);
        return Math.sqrt(Math.pow(node2.x - node1.x, 2) + Math.pow(node2.y - node1.y, 2));
    }

    @Override
    public double getWeight(int fromNode,
                            int toNode) {
        return weights.getEntry(fromNode, toNode);
    }

    @Override
    public String getNodeLabel(int nodeIndex) {
        return nodes.get(nodeIndex).label;
    }

    @Override
    public String getNodeId(int nodeIndex) {
        return nodes.get(nodeIndex).id;
    }

    private class Node {
        String id;
        String label;
        double x;
        double y;
    }
}
