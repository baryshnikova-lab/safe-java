package edu.princeton.safe.internal.cytoscape.task;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.io.LabelFunction;

public abstract class BaseExportReportsTask extends AbstractTask {

    @Tunable(description = "Output File", required = true)
    public File outputFile;

    @Tunable(description = "Node Name Column", required = true)
    public ListSingleSelection<String> nodeNameColumn;

    SafeSession session;
    CyTable nodeTable;

    public BaseExportReportsTask(SafeSession session) {

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        nodeTable = network.getDefaultNodeTable();
        List<String> names = SafeUtil.getStringColumnNames(nodeTable)
                                     .collect(Collectors.toList());
        nodeNameColumn = new ListSingleSelection<>(names);

        nodeNameColumn.setSelectedValue(CyNetwork.NAME);

        this.session = session;
    }

    String trimExtension(String name) {
        int index = name.lastIndexOf(".", 0);
        if (index == -1) {
            return name;
        }

        return name.substring(0, index);
    }

    LabelFunction getLabelFunction(Long[] nodeMappings, String nameColumn) {
        return i -> {
            Long suid = nodeMappings[i];
            CyRow row = nodeTable.getRow(suid);
            return row.get(nameColumn, String.class);
        };
    }

}
