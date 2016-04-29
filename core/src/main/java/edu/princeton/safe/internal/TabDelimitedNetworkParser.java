package edu.princeton.safe.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

import com.carrotsearch.hppc.ObjectIntHashMap;

import edu.princeton.safe.NetworkConsumer;
import edu.princeton.safe.NetworkParser;

public class TabDelimitedNetworkParser implements NetworkParser {

    private String nodePath;
    private String edgePath;
    private boolean isDirected;

    public TabDelimitedNetworkParser(String nodePath,
                                     String edgePath,
                                     boolean isDirected)
            throws IOException {

        this.nodePath = nodePath;
        this.edgePath = edgePath;
        this.isDirected = isDirected;
    }

    @Override
    public void parse(NetworkConsumer consumer) throws IOException {
        // Create look up for node label -> node index
        ObjectIntHashMap<String> nodeIdsToIndexes = new ObjectIntHashMap<>();
        int[] index = { 0 };

        consumer.startNodes();
        try (BufferedReader reader = Util.getReader(nodePath)) {
            reader.lines()
                  .forEach(new Consumer<String>() {
                      @Override
                      public void accept(String line) {
                          String[] parts = line.split("\t");
                          String label = parts[0];
                          String id = parts[1];
                          double x = Double.parseDouble(parts[2]);
                          double y = Double.parseDouble(parts[3]);
                          consumer.node(index[0], label, id, x, y);

                          nodeIdsToIndexes.put(label, index[0]);
                          index[0]++;
                      }
                  });
        } finally {
            consumer.finishNodes();
        }

        consumer.startEdges();
        try (BufferedReader reader = Util.getReader(edgePath)) {
            reader.lines()
                  .forEach(new Consumer<String>() {
                      @Override
                      public void accept(String line) {
                          String[] parts = line.split("\t");
                          int fromIndex = nodeIdsToIndexes.get(parts[0]);
                          int toIndex = nodeIdsToIndexes.get(parts[1]);
                          double weight = 1;
                          consumer.edge(fromIndex, toIndex, weight);
                      }
                  });
        } finally {
            consumer.finishEdges();
        }
    }

    @Override
    public boolean isDirected() {
        return isDirected;
    }

}
