package edu.princeton.safe.internal.cytoscape;

import java.util.Arrays;
import java.util.Collection;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;

import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.internal.cytoscape.event.EventService;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.model.EnrichmentLandscape;

public class SelectionTracker implements RowsSetListener {

    final EventService eventService;

    SafeSession session;

    Long nodeTableId;
    boolean isEnabled;
    LongSet nodeSuids;

    public SelectionTracker(EventService eventService) {
        this.eventService = eventService;

        eventService.addEnrichmentLandscapeListener(landscape -> {
            setEnrichmentLandscape(landscape);
        });

        nodeSuids = new LongScatterSet();
    }

    public void setSession(SafeSession session) {
        this.session = session;

        isEnabled = false;
        nodeTableId = null;
        nodeSuids.clear();
    }

    void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        if (session == null) {
            notifyListeners();
            return;
        }

        isEnabled = true;

        Long[] nodeMappings = session.getNodeMappings();
        if (nodeMappings == null) {
            notifyListeners();
            return;
        }

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTableId = nodeTable.getSUID();

        Arrays.stream(nodeMappings)
              .forEach(suid -> {
                  CyRow row = nodeTable.getRow(suid);
                  boolean selected = row.get(CyNetwork.SELECTED, Boolean.class);
                  if (selected) {
                      nodeSuids.add(suid);
                  }
              });

        notifyListeners();
    }

    @Override
    public void handleEvent(RowsSetEvent event) {
        if (nodeTableId == null || !isEnabled) {
            return;
        }

        if (event.getSource()
                 .getSUID() != nodeTableId) {
            return;
        }

        int initialSize = nodeSuids.size();
        Collection<RowSetRecord> records = event.getColumnRecords(CyNetwork.SELECTED);
        records.stream()
               .forEach(record -> {
                   CyRow row = record.getRow();
                   Long suid = row.get(CyNetwork.SUID, Long.class);
                   if (Boolean.TRUE.equals(record.getValue())) {
                       nodeSuids.add(suid);
                   } else {
                       nodeSuids.removeAll(suid);
                   }
               });

        if (nodeSuids.size() != initialSize) {
            notifyListeners();
        }
    }

    private void notifyListeners() {
        eventService.notifyNodeSelectionChangedListeners(nodeSuids);
    }
}
