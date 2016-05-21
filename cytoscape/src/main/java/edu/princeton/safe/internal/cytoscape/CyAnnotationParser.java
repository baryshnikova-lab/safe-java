package edu.princeton.safe.internal.cytoscape;

import java.io.IOException;
import java.util.HashMap;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import com.carrotsearch.hppc.IntArrayList;

import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.AnnotationParser;

public abstract class CyAnnotationParser implements AnnotationParser {

    CyNetworkView view;
    String idColumn;
    int totalSkippedNodes;

    public CyAnnotationParser(CyNetworkView view,
                              String idColumn) {
        this.view = view;
        this.idColumn = idColumn;
    }

    abstract boolean handleRow(NetworkProvider networkProvider,
                               AnnotationConsumer consumer,
                               CyRow row,
                               String id,
                               IntArrayList indexes);

    abstract String[] getAttributeLabels();

    @Override
    public void parse(NetworkProvider networkProvider,
                      AnnotationConsumer consumer)
            throws IOException {

        CyNetwork network = view.getModel();
        CyTable table = network.getDefaultNodeTable();

        int totalNetworkNodes = networkProvider.getNodeCount();

        // Create look up for node label -> node index
        HashMap<String, IntArrayList> nodeIdsToIndexes = new HashMap<>();
        for (int i = 0; i < totalNetworkNodes; i++) {
            String key = networkProvider.getNodeId(i);
            IntArrayList list = nodeIdsToIndexes.get(key);
            if (list == null) {
                list = new IntArrayList();
                nodeIdsToIndexes.put(key, list);
            }
            list.add(i);
        }

        int totalAnnotationNodes = 0;
        totalSkippedNodes = 0;

        String[] attributeLabels = getAttributeLabels();
        consumer.start(attributeLabels, totalNetworkNodes);
        try {
            for (View<CyNode> nodeView : view.getNodeViews()) {
                CyNode node = nodeView.getModel();
                CyRow row = table.getRow(node);
                String id = row.get(idColumn, String.class);
                IntArrayList indexes = nodeIdsToIndexes.get(id);
                if (indexes == null) {
                    totalSkippedNodes++;
                } else {
                    handleRow(networkProvider, consumer, row, id, indexes);
                    totalAnnotationNodes++;
                }
            }
        } finally {
            consumer.finish(totalAnnotationNodes);
        }
    }

    public int getSkippedNodeCount() {
        return totalSkippedNodes;
    }

}
