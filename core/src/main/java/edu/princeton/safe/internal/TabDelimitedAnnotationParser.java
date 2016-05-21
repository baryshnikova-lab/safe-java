package edu.princeton.safe.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectScatterSet;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.AnnotationParser;

public class TabDelimitedAnnotationParser implements AnnotationParser {

    ObjectScatterSet<String> skippedNodes;
    int skippedLines;
    int totalLines;
    String path;
    private Set<String> unmappedNodeNames;

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

        try (BufferedReader reader = Util.getReader(path)) {
            // Parse header
            String line = reader.readLine();
            String[] headerParts = line.split("\t");

            String[] attributeLabels = Arrays.copyOfRange(headerParts, 1, headerParts.length);
            int totalNodes = networkProvider.getNodeCount();
            consumer.start(attributeLabels, totalNodes);

            // Create look up for node label -> node index
            HashMap<String, IntArrayList> nodeIdsToIndexes = new HashMap<>();
            for (int i = 0; i < totalNodes; i++) {
                String key = networkProvider.getNodeId(i);
                IntArrayList list = nodeIdsToIndexes.get(key);
                if (list == null) {
                    list = new IntArrayList();
                    nodeIdsToIndexes.put(key, list);
                }
                list.add(i);
            }

            HashMap<String, IntArrayList> notSeen = new HashMap<>(nodeIdsToIndexes);

            line = reader.readLine();
            while (line != null) {
                String[] parts = line.split("\t");
                String label = parts[0];
                IntArrayList indexes = nodeIdsToIndexes.get(label);
                if (indexes == null) {
                    skippedNodes.add(label);
                    skippedLines++;
                } else {
                    notSeen.remove(label);

                    indexes.forEach(new Consumer<IntCursor>() {
                        @Override
                        public void accept(IntCursor cursor) {
                            int nodeIndex = cursor.value;
                            for (int j = 1; j < parts.length; j++) {
                                double value = Double.parseDouble(parts[j]);
                                consumer.value(nodeIndex, j - 1, value);
                            }
                        }
                    });
                }
                totalLines++;
                line = reader.readLine();
            }

            unmappedNodeNames = notSeen.values()
                                       .stream()
                                       .flatMapToInt(l -> Arrays.stream(l.buffer, 0, l.elementsCount))
                                       .mapToObj(i -> networkProvider.getNodeLabel(i))
                                       .collect(Collectors.toSet());
        } finally {
            consumer.finish(totalLines);
        }
    }

    public Set<String> getMissingNodes() {
        return unmappedNodeNames;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public int getSkippedLines() {
        return skippedLines;
    }
}
