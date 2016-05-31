package edu.princeton.safe.internal.cytoscape;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.LongIntScatterMap;

import edu.princeton.safe.io.NetworkConsumer;
import edu.princeton.safe.io.NetworkParser;

public class CyNetworkParser implements NetworkParser {

    CyNetwork network;
    CyNetworkView view;
    boolean isDirected;
    String nameColumn;
    String idColumn;
    String weightColumn;

    int totalSkippedNodes;
    int totalEdges;
    int totalSkippedEdges;
    LongIntMap nodesBySuid;

    public CyNetworkParser(CyNetworkView view,
                           String nameColumn,
                           String idColumn,
                           String weightColumn) {
        this.view = view;
        this.network = view.getModel();
        this.nameColumn = nameColumn;
        this.idColumn = idColumn;
        this.weightColumn = weightColumn;
    }

    @Override
    public void parse(NetworkConsumer consumer) throws IOException {
        Collection<View<CyEdge>> edges = view.getEdgeViews();
        isDirected = edges.stream()
                          .anyMatch(e -> e.getModel()
                                          .isDirected());

        nodesBySuid = new LongIntScatterMap();
        int[] index = { 0 };
        Collection<View<CyNode>> nodes = view.getNodeViews();
        consumer.startNodes();
        nodes.stream()
             .forEach(new Consumer<View<CyNode>>() {
                 @Override
                 public void accept(View<CyNode> nodeView) {
                     CyNode node = nodeView.getModel();
                     Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
                     Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
                     if (x == null || y == null) {
                         totalSkippedNodes++;
                         return;
                     }

                     CyRow row = network.getRow(node);
                     String name = row.get(nameColumn, String.class);
                     String id = row.get(idColumn, String.class);
                     consumer.node(index[0], name, id, x, y);
                     nodesBySuid.put(node.getSUID(), index[0]);
                     index[0]++;
                 }

             });
        consumer.finishNodes();

        EdgeWeightFunction weight;
        if (weightColumn == null) {
            weight = r -> 1;
        } else {
            weight = r -> r.get(weightColumn, Double.class);
        }

        consumer.startEdges();
        edges.stream()
             .forEach(new Consumer<View<CyEdge>>() {
                 @Override
                 public void accept(View<CyEdge> edgeView) {
                     CyEdge edge = edgeView.getModel();
                     int fromIndex = nodesBySuid.getOrDefault(edge.getSource()
                                                                  .getSUID(),
                                                              -1);
                     int toIndex = nodesBySuid.getOrDefault(edge.getTarget()
                                                                .getSUID(),
                                                            -1);
                     if (fromIndex == -1 || toIndex == -1) {
                         totalSkippedEdges++;
                         return;
                     }

                     CyRow row = network.getRow(edge);
                     consumer.edge(fromIndex, toIndex, weight.get(row));
                     totalEdges++;
                 }
             });
        consumer.finishEdges();

        view = null;
        network = null;
    }

    @Override
    public boolean isDirected() {
        return isDirected;
    }

    int getSkippedNodeCount() {
        return totalSkippedNodes;
    }

    int getSkippedEdgeCount() {
        return totalSkippedEdges;
    }

    int getEdgeCount() {
        return totalEdges;
    }

    LongIntMap getNodeMappings() {
        return nodesBySuid;
    }

    @FunctionalInterface
    static interface EdgeWeightFunction {
        double get(CyRow row);
    }
}
