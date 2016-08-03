package edu.princeton.safe.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.internal.io.TabDelimitedAnnotationParser;
import edu.princeton.safe.internal.io.TabDelimitedNodeTableVisitor;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.NodeTableConsumer;
import edu.princeton.safe.io.NodeTableVisitor;

public class IdAnalyzer {

    public static IdMappingResult analyzeAnnotations(String attributePath,
                                                     NodeTableVisitor visitor)
            throws IOException {

        Set<String> nodeIds = extractNodeIds(attributePath);

        IdMappingResult result = new IdMappingResult();
        result.totalAnnotationNodes = nodeIds.size();

        computePerColumnHitRates(result, nodeIds, visitor);

        return result;
    }

    static void computePerColumnHitRates(IdMappingResult result,
                                         Set<String> nodeIds,
                                         NodeTableVisitor visitor) {

        IntIntMap columnCounts = new IntIntHashMap();

        NodeTableConsumer consumer = new NodeTableConsumer() {
            @Override
            public void startNode(int nodeId) {
                result.totalNetworkNodes++;
            }

            @Override
            public void cell(int columnId,
                             String value) {
                if (nodeIds.contains(value)) {
                    columnCounts.addTo(columnId, 1);
                }
            }

            @Override
            public void endNode() {
            }
        };
        visitor.visit(consumer);

        result.coverage = columnCounts;
    }

    static Set<String> extractNodeIds(String attributePath) throws IOException {
        Set<String> nodeIds = new HashSet<>();

        // Make a no-op NetworkProvider that will cause all parsed attributes
        // for all nodes to be marked as "skipped".
        NetworkProvider networkProvider = new NetworkProvider() {

            @Override
            public double getWeight(int fromNode,
                                    int toNode) {
                return 0;
            }

            @Override
            public String getNodeLabel(int nodeIndex) {
                return null;
            }

            @Override
            public String getNodeId(int nodeIndex) {
                return null;
            }

            @Override
            public int getNodeCount() {
                return 0;
            }

            @Override
            public double getDistance(int fromNode,
                                      int toNode) {
                return 0;
            }

            @Override
            public void forEachNeighbor(int nodeIndex,
                                        IntConsumer consumer) {
            }
        };

        // Make a minimal AnnotationConsumer that only handles "skipped"
        // nodes
        AnnotationConsumer annotationConsumer = new AnnotationConsumer() {

            @Override
            public void value(int nodeIndex,
                              int attributeIndex,
                              double value) {
            }

            @Override
            public void start(String[] attributeLabels,
                              int totalNetworkNodes) {
            }

            @Override
            public void skipped(String nodeId) {
                nodeIds.add(nodeId);
            }

            @Override
            public void finish(int totalAnnotationNodes) {
            }
        };

        TabDelimitedAnnotationParser parser = new TabDelimitedAnnotationParser(attributePath);
        parser.parse(networkProvider, annotationConsumer);

        return nodeIds;
    }

    public static void main(String[] args) throws Exception {
        IdAnalyzer.analyzeAnnotations("src/test/resources/go_bp_140819.txt.gz",
                                      new TabDelimitedNodeTableVisitor("src/test/resources/Costanzo_Science_2010.nodes.txt"));
    }
}
