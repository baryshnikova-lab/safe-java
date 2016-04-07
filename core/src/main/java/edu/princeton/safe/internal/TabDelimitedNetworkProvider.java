package edu.princeton.safe.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.SparseRealMatrix;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.internal.distance.MapBasedDistanceMetric;

public class TabDelimitedNetworkProvider implements NetworkProvider {

    List<Node> nodes;
    SparseRealMatrix weights;
    
    public TabDelimitedNetworkProvider(String nodePath, String edgePath) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(nodePath))) {
            nodes = new ArrayList<>();
            stream.forEach(new Consumer<String>() {
                @Override
                public void accept(String line) {
                    String[] parts = line.split("\t");
                    Node node = new Node();
                    node.label = parts[0];
                    node.x = Double.parseDouble(parts[2]);
                    node.y = Double.parseDouble(parts[3]);
                    nodes.add(node);
                }
            });
        }
        int totalNodes = nodes.size();
        weights = new OpenMapRealMatrix(totalNodes, totalNodes);
        
    }
    
    @Override
    public int getNodeCount() {
        return nodes.size();
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

    private class Node {
        String label;
        double x;
        double y;
    }
    
}
