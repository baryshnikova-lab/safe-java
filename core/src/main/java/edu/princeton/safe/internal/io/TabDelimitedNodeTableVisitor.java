package edu.princeton.safe.internal.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.IntStream;

import edu.princeton.safe.internal.Util;
import edu.princeton.safe.io.NodeTableConsumer;
import edu.princeton.safe.io.NodeTableVisitor;

public class TabDelimitedNodeTableVisitor implements NodeTableVisitor {

    String nodePath;

    public TabDelimitedNodeTableVisitor(String nodePath) {
        this.nodePath = nodePath;
    }

    @Override
    public void visit(NodeTableConsumer consumer) {
        int[] nodeIndex = { 0 };
        try (BufferedReader reader = Util.getReader(nodePath)) {
            reader.lines()
                  .forEach(line -> {
                      String[] parts = line.split("\t");
                      consumer.startNode(nodeIndex[0]);
                      IntStream.range(0, parts.length)
                               .forEach(i -> {
                                   String part = parts[i];
                                   consumer.cell(i, part);
                               });
                      consumer.endNode();
                      nodeIndex[0]++;
                  });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}