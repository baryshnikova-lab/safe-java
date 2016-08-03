package edu.princeton.safe.internal.cytoscape.task;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.io.AttributeReport;
import edu.princeton.safe.internal.io.NodeReport;
import edu.princeton.safe.io.LabelFunction;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class ExportReportsTask extends AbstractTask {

    @Tunable(description = "Output File", required = true)
    public File outputFile;

    @Tunable(description = "Node Name Column", required = true)
    public ListSingleSelection<String> nodeNameColumn;

    SafeSession session;
    CyTable nodeTable;

    public ExportReportsTask(SafeSession session) {

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        nodeTable = network.getDefaultNodeTable();
        List<String> names = SafeUtil.getStringColumnNames(nodeTable)
                                     .collect(Collectors.toList());
        nodeNameColumn = new ListSingleSelection<>(names);

        nodeNameColumn.setSelectedValue(CyNetwork.NAME);

        this.session = session;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("SAFE: Export Reports");

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        CompositeMap compositeMap = session.getCompositeMap();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

        writeReports(landscape, compositeMap, EnrichmentLandscape.TYPE_HIGHEST);
        if (!annotationProvider.isBinary()) {
            writeReports(landscape, compositeMap, EnrichmentLandscape.TYPE_LOWEST);
        }
    }

    private void writeReports(EnrichmentLandscape landscape,
                              CompositeMap compositeMap,
                              int typeIndex)
            throws IOException {

        Long[] nodeMappings = session.getNodeMappings();
        String nameColumn = nodeNameColumn.getSelectedValue();

        LabelFunction label = i -> {
            Long suid = nodeMappings[i];
            CyRow row = nodeTable.getRow(suid);
            return row.get(nameColumn, String.class);
        };

        File directory = outputFile.getParentFile();
        String baseName = trimExtension(outputFile.getName());

        String typeName = typeIndex == EnrichmentLandscape.TYPE_HIGHEST ? "highest" : "lowest";

        try (PrintWriter writer = new PrintWriter(new File(directory,
                                                           String.format("%s-node_properties_annotation-%s.txt",
                                                                         baseName, typeName)))) {
            NodeReport.write(writer, landscape, compositeMap, typeIndex, label);
        }

        try (PrintWriter writer = new PrintWriter(new File(directory,
                                                           String.format("%s-attribute_properties_annotation-%s.txt",
                                                                         baseName, typeName)))) {
            AttributeReport.write(writer, landscape, compositeMap, typeIndex);
        }
    }

    String trimExtension(String name) {
        int index = name.lastIndexOf(".", 0);
        if (index == -1) {
            return name;
        }

        return name.substring(0, index);
    }

}
