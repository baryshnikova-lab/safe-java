package edu.princeton.safe.io;

public interface NodeTableVisitor {
    void visit(NodeTableConsumer consumer);
}