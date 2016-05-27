package edu.princeton.safe.internal.cytoscape;

import java.io.File;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.DenseAnnotationProvider;
import edu.princeton.safe.internal.SparseNetworkProvider;
import edu.princeton.safe.internal.TabDelimitedAnnotationParser;

public class SafeTask extends AbstractTask {

    private SafeSession session;
    private SafeController controller;

    public SafeTask(SafeSession session,
                    SafeController controller) {
        this.session = session;
        this.controller = controller;
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("SAFE Analysis");

        CyNetworkView view = session.getNetworkView();

        String nameColumn = session.getNameColumn();
        String idColumn = session.getIdColumn();

        if (idColumn == null) {
            idColumn = nameColumn;
        }

        monitor.setStatusMessage("Loading network...");

        CyNetworkParser parser = new CyNetworkParser(view, nameColumn, idColumn, null);
        SparseNetworkProvider networkProvider = new SparseNetworkProvider(parser);

        monitor.showMessage(Level.INFO, String.format("Nodes imported: %d", networkProvider.getNodeCount()));
        monitor.showMessage(Level.INFO, String.format("Nodes skipped: %d", parser.getSkippedNodeCount()));
        monitor.showMessage(Level.INFO, String.format("Edges imported: %d", parser.getEdgeCount()));
        monitor.showMessage(Level.INFO, String.format("Edges skipped: %d", parser.getSkippedEdgeCount()));

        monitor.setStatusMessage("Loading annotations...");

        File annotationFile = session.getAnnotationFile();
        if (annotationFile != null) {
            TabDelimitedAnnotationParser annotationParser = new TabDelimitedAnnotationParser(annotationFile.getAbsolutePath());
            AnnotationProvider annotationProvider = new DenseAnnotationProvider(networkProvider, annotationParser);
            Set<String> missingNodes = annotationParser.getMissingNodes();
            int totalMissingNodes = missingNodes.size();
            int totalAnnotatedNodes = annotationProvider.getNetworkNodeCount() - totalMissingNodes;

            monitor.showMessage(Level.INFO,
                                String.format("Attributes imported: %d", annotationProvider.getAttributeCount()));
            monitor.showMessage(Level.INFO, String.format("Nodes annotated: %d", totalAnnotatedNodes));
            monitor.showMessage(Level.INFO, String.format("Nodes not annotated: %d", totalMissingNodes));
            monitor.showMessage(Level.INFO,
                                String.format("Ids of nodes not annotated: %s", String.join(", ", missingNodes)));

            controller.setAttributes(annotationProvider);
        }

    }

}
