package edu.princeton.safe.internal.cytoscape.io;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
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
    boolean forceUndirected;

    int totalSkippedNodes;
    int totalEdges;
    int totalSkippedEdges;
    int totalMissingWeights;
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
        forceUndirected = true;
    }

    @Override
    public void parse(NetworkConsumer consumer) throws IOException {
        Collection<View<CyEdge>> edges = view.getEdgeViews();
        isDirected = edges.stream()
                          .anyMatch(e -> e.getModel()
                                          .isDirected());

        CyTable nodeTable = network.getDefaultNodeTable();
        CyColumn idColumnModel = nodeTable.getColumn(idColumn);
        boolean idIsList = idColumnModel.getType()
                                        .equals(List.class);

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

                     List<String> ids;
                     if (idIsList) {
                         ids = row.getList(idColumn, String.class);
                     } else {
                         String id = row.get(idColumn, String.class);
                         ids = Collections.singletonList(id);
                     }

                     consumer.node(index[0], name, ids, x, y);
                     nodesBySuid.put(node.getSUID(), index[0]);
                     index[0]++;
                 }

             });
        consumer.finishNodes();

        EdgeWeightFunction weight;
        if (weightColumn == null) {
            weight = r -> 1;
        } else {
            weight = r -> {
                Double value = r.get(weightColumn, Double.class);
                if (value == null) {
                    totalMissingWeights++;
                    return Double.NaN;
                }
                if (!Double.isFinite(value)) {
                    totalMissingWeights++;
                }
                return value.doubleValue();
            };
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
                     double edgeWeight = weight.get(row);
                     consumer.edge(fromIndex, toIndex, edgeWeight);

                     if (isDirected() && !edge.isDirected()) {
                         consumer.edge(toIndex, fromIndex, edgeWeight);
                     }
                     totalEdges++;
                 }
             });
        consumer.finishEdges();

        view = null;
        network = null;
    }

    @Override
    public boolean isDirected() {
        return isDirected && !forceUndirected;
    }

    public int getSkippedNodeCount() {
        return totalSkippedNodes;
    }

    public int getSkippedEdgeCount() {
        return totalSkippedEdges;
    }

    public int getEdgeCount() {
        return totalEdges;
    }

    public int getMissingWeightCount() {
        return totalMissingWeights;
    }
    
    public LongIntMap getNodeMappings() {
        return nodesBySuid;
    }

    @FunctionalInterface
    static interface EdgeWeightFunction {
        double get(CyRow row);
    }
}
