package edu.princeton.safe.io;

public interface NetworkConsumer {
    void startNodes();

    void node(int nodeIndex,
              String label,
              String id,
              double x,
              double y);

    void finishNodes();

    void startEdges();

    void edge(int fromIndex,
              int toIndex,
              double weight);

    void finishEdges();
}
