package edu.princeton.safe.internal.fastcluster;

// Adapted from http://danifold.net/fastcluster.html
public class Node {
    public int node1;
    public int node2;
    public double dist;

    public Node(int node1,
                int node2,
                double dist) {

        this.node1 = node1;
        this.node2 = node2;
        this.dist = dist;
    }

}