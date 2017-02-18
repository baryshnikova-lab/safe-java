package edu.princeton.safe.internal.cytoscape.task;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.io.AttributeReport;
import edu.princeton.safe.internal.io.DomainReport;
import edu.princeton.safe.internal.io.NodeReport;
import edu.princeton.safe.io.LabelFunction;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class ExportReportsTask extends BaseExportReportsTask {

    public ExportReportsTask(SafeSession session) {
        super(session);
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

        taskMonitor.setStatusMessage("Reports exported successfully");
    }

    private void writeReports(EnrichmentLandscape landscape,
                              CompositeMap compositeMap,
                              int typeIndex)
            throws IOException {

        Long[] nodeMappings = session.getNodeMappings();
        String nameColumn = nodeNameColumn.getSelectedValue();
        LabelFunction label = getLabelFunction(nodeMappings, nameColumn);

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

        try (PrintWriter writer = new PrintWriter(new File(directory,
                                                           String.format("%s-domain_properties_annotation-%s.txt",
                                                                         baseName, typeName)))) {
            DomainReport.write(writer, compositeMap, typeIndex);
        }
    }

}
