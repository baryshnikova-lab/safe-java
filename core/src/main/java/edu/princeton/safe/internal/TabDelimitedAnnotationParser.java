package edu.princeton.safe.internal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectScatterSet;

import edu.princeton.safe.AnnotationConsumer;
import edu.princeton.safe.AnnotationParser;
import edu.princeton.safe.NetworkProvider;

public  class TabDelimitedAnnotationParser implements AnnotationParser {

    ObjectScatterSet<String> skippedNodes;
    int skippedValues;
    int totalValues;
    String path;
    
    public TabDelimitedAnnotationParser(String path) {
        this.path = path;
    }
    
    @Override
    public void parse(NetworkProvider networkProvider,
                      AnnotationConsumer consumer)
            throws IOException {

        if (skippedNodes != null) {
            throw new IOException("Cannot call parse twice for same instance");
        }
        skippedNodes = new ObjectScatterSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path));) {
            // Parse header
            String line = reader.readLine();
            String[] parts = line.split("\t");

            String[] attributeLabels = Arrays.copyOfRange(parts, 1, parts.length);
            int totalNodes = networkProvider.getNodeCount();
            consumer.start(attributeLabels, totalNodes);

            // Create look up for node label -> node index
            ObjectIntHashMap<String> nodeIdsToIndexes = new ObjectIntHashMap<>();
            for (int i = 0; i < totalNodes; i++) {
                nodeIdsToIndexes.put(networkProvider.getNodeId(i), i);
            }

            line = reader.readLine();
            while (line != null) {
                parts = line.split("\t");
                String label = parts[0];
                int nodeIndex = nodeIdsToIndexes.getOrDefault(label, -1);
                if (nodeIndex != -1) {
                    for (int j = 1; j < parts.length; j++) {
                        double value = Double.parseDouble(parts[j]);
                        if (!Double.isNaN(value)) {
                            consumer.value(nodeIndex, j - 1, value);
                        }
                    }
                } else {
                    skippedNodes.add(label);
                    skippedValues++;
                }
                totalValues++;
                line = reader.readLine();
            }
        } finally {
            consumer.finish();
        }
    }
}
