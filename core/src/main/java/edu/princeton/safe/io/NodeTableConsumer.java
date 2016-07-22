package edu.princeton.safe.io;

public interface NodeTableConsumer {
    void startNode(int nodeId);

    void cell(int columnId,
              String value);

    void endNode();
}