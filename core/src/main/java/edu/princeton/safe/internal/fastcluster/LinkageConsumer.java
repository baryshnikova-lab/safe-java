package edu.princeton.safe.internal.fastcluster;

public interface LinkageConsumer {
    void accept(int node1,
                int node2,
                double dist);
}
