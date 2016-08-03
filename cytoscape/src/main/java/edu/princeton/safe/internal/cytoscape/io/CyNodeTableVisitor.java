package edu.princeton.safe.internal.cytoscape.io;

import java.util.List;
import java.util.stream.IntStream;

import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import edu.princeton.safe.io.NodeTableConsumer;
import edu.princeton.safe.io.NodeTableVisitor;

public class CyNodeTableVisitor implements NodeTableVisitor {

    CyTable nodeTable;
    List<String> columnNames;
    int lastNodeIndex;

    public CyNodeTableVisitor(CyTable nodeTable,
                              List<String> columnNames) {
        this.nodeTable = nodeTable;
        this.columnNames = columnNames;
    }

    @Override
    public void visit(NodeTableConsumer consumer) {
        lastNodeIndex = 0;
        nodeTable.getAllRows()
                 .stream()
                 .forEach(row -> visit(consumer, row));
    }

    private void visit(NodeTableConsumer consumer,
                       CyRow row) {
        consumer.startNode(lastNodeIndex);
        IntStream.range(0, columnNames.size())
                 .forEach(index -> {
                     String name = columnNames.get(index);
                     String value = row.get(name, String.class);
                     consumer.cell(index, value);
                 });
        consumer.endNode();
        lastNodeIndex++;
    }

}
