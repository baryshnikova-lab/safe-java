package edu.princeton.safe.internal.cytoscape.task;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.io.NeighborhoodReport;
import edu.princeton.safe.io.LabelFunction;
import edu.princeton.safe.model.EnrichmentLandscape;

public class ExportNeighborhoodReportsTask extends BaseExportReportsTask {

    public ExportNeighborhoodReportsTask(SafeSession session) {
        super(session);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("SAFE: Export Reports");

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

        writeReports(landscape, EnrichmentLandscape.TYPE_HIGHEST);
        if (!annotationProvider.isBinary()) {
            writeReports(landscape, EnrichmentLandscape.TYPE_LOWEST);
        }

        taskMonitor.setStatusMessage("Reports exported successfully");
    }

    private void writeReports(EnrichmentLandscape landscape,
                              int typeIndex)
            throws IOException {

        Long[] nodeMappings = session.getNodeMappings();
        String nameColumn = nodeNameColumn.getSelectedValue();
        LabelFunction label = getLabelFunction(nodeMappings, nameColumn);

        File directory = outputFile.getParentFile();
        String baseName = trimExtension(outputFile.getName());

        String typeName = typeIndex == EnrichmentLandscape.TYPE_HIGHEST ? "highest" : "lowest";

        try (PrintWriter writer = new PrintWriter(new File(directory,
                                                           String.format("%s-neighborhood_scores_annotation-%s.txt",
                                                                         baseName, typeName)))) {
            NeighborhoodReport.write(writer, landscape, typeIndex, label);
        }
    }
}
