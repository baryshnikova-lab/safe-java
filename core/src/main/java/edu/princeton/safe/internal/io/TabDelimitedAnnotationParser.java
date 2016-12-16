package edu.princeton.safe.internal.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.internal.IdAnalyzer;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.AnnotationParser;

public class TabDelimitedAnnotationParser extends TabDelimitedParser implements AnnotationParser {

    Set<String> unmappedNodeNames;
    HashMap<String, IntArrayList> nodeIdsToIndexes;
    HashMap<String, IntArrayList> notSeen;

    int labelLineIndex;
    String commentCharacter;
    int expectedColumns;

    public TabDelimitedAnnotationParser(String path,
                                        int labelLineIndex,
                                        String commentCharacter) {
        super(path);
        this.labelLineIndex = labelLineIndex;
        this.commentCharacter = commentCharacter;
    }

    @Override
    public void parse(NetworkProvider networkProvider,
                      AnnotationConsumer consumer)
            throws IOException {

        if (unmappedNodeNames != null) {
            throw new IOException("Cannot call parse twice for same instance");
        }

        // Create look up for node label -> node index
        nodeIdsToIndexes = new HashMap<>();
        int totalNodes = networkProvider.getNodeCount();
        for (int i = 0; i < totalNodes; i++) {
            int idIndex = i;
            networkProvider.getNodeIds(i)
                           .forEach(key -> {
                               IntArrayList list = nodeIdsToIndexes.get(key);
                               if (list == null) {
                                   list = new IntArrayList();
                                   nodeIdsToIndexes.put(key, list);
                               }
                               list.add(idIndex);
                           });
        }

        notSeen = new HashMap<>(nodeIdsToIndexes);
        LineHandler handler = parts -> {
            String comment = IdAnalyzer.getComment(parts[0]);
            if (comment != null && comment.startsWith(commentCharacter)) {
                return false;
            }

            if (totalLines == labelLineIndex) {
                expectedColumns = parts.length - 1;
                if (isProbablyHeader(parts)) {
                    String[] attributeLabels = Arrays.copyOfRange(parts, 1, parts.length);
                    consumer.start(attributeLabels, totalNodes);
                    return false;
                } else {
                    // Header is likely missing so we generate attribute names
                    // and treat line like values
                    String[] attributeLabels = new String[expectedColumns];
                    for (int i = 0; i < expectedColumns; i++) {
                        attributeLabels[i] = String.format("Attribute %d", i + 1);
                    }
                    consumer.start(attributeLabels, totalNodes);
                }
            }

            if (parts.length != expectedColumns + 1) {
                return false;
            }

            return handleParts(consumer, parts, nodeIdsToIndexes, notSeen);
        };

        try {
            parse(handler);

            unmappedNodeNames = notSeen.values()
                                       .stream()
                                       .flatMapToInt(l -> Arrays.stream(l.buffer, 0, l.elementsCount))
                                       .mapToObj(i -> networkProvider.getNodeLabel(i))
                                       .collect(Collectors.toSet());
        } finally {
            consumer.finish(totalLines);
        }
    }

    boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    boolean isProbablyHeader(String[] parts) {
        return Arrays.stream(parts, 1, parts.length)
                     .anyMatch(s -> !isDouble(s) && s.chars()
                                                     .anyMatch(c -> Character.isLetter(c)));
    }

    boolean handleParts(AnnotationConsumer consumer,
                        String[] parts,
                        Map<String, IntArrayList> nodeIdsToIndexes,
                        Map<String, IntArrayList> notSeen) {
        String label = parts[0];
        IntArrayList indexes = nodeIdsToIndexes.get(label);
        if (indexes == null) {
            consumer.skipped(label);
            return false;
        } else {
            notSeen.remove(label);

            indexes.forEach((Consumer<? super IntCursor>) (cursor) -> {
                int nodeIndex = cursor.value;
                for (int j = 1; j < parts.length; j++) {
                    double value = Util.parseDouble(parts[j]);
                    consumer.value(nodeIndex, j - 1, value);
                }
            });
        }
        return true;
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
