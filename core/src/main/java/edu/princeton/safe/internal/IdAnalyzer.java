package edu.princeton.safe.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;

import edu.princeton.safe.internal.io.LineHandler;
import edu.princeton.safe.internal.io.TabDelimitedNodeTableVisitor;
import edu.princeton.safe.internal.io.TabDelimitedParser;
import edu.princeton.safe.io.NodeTableConsumer;
import edu.princeton.safe.io.NodeTableVisitor;

public class IdAnalyzer {

    static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*([^a-zA-Z0-9\\s\\-.]).*?");

    public static IdMappingResult analyzeAnnotations(String attributePath,
                                                     NodeTableVisitor visitor)
            throws IOException {

        IdMappingResult result = new IdMappingResult();
        Set<String> nodeIds = extractNodeIds(result, attributePath);

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

    static Set<String> extractNodeIds(IdMappingResult result,
                                      String attributePath)
            throws IOException {
        Set<String> nodeIds = new HashSet<>();

        TabDelimitedParser parser = new TabDelimitedParser(attributePath);
        LineHandler handler = parts -> {
            nodeIds.add(parts[0]);
            computeLineStatistics(result, parts, parser.getTotalLines());
            return true;
        };
        parser.parse(handler);

        return nodeIds;
    }

    static void computeLineStatistics(IdMappingResult result,
                                      String[] parts,
                                      int lineIndex) {
        int totalColumns = parts.length;
        result.columnFrequencies.addTo(totalColumns, 1);
        if (!result.firstLineOfColumnWidth.containsKey(totalColumns)) {
            result.firstLineOfColumnWidth.put(totalColumns, lineIndex);
        }

        String comment = getComment(parts[0]);
        if (comment != null) {
            result.firstCharacterFrequencies.addTo(comment.substring(0, 1), 1);
        }
    }

    public static String getComment(String value) {
        Matcher matcher = COMMENT_PATTERN.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String annotationPath = "src/test/resources/go2.txt";
        // String annotationPath = "src/test/resources/go_bp_140819.txt.gz";
        String networkPath = "src/test/resources/Costanzo_Science_2010.nodes.txt";
        IdMappingResult result = IdAnalyzer.analyzeAnnotations(annotationPath,
                                                               new TabDelimitedNodeTableVisitor(networkPath));
        System.out.println(result.coverage);
        System.out.println(result.totalNetworkNodes);
        System.out.println(result.totalAnnotationNodes);
        System.out.println(result.columnFrequencies);
        System.out.println(result.firstCharacterFrequencies);
        System.out.println(result.firstLineOfColumnWidth);
    }
}
