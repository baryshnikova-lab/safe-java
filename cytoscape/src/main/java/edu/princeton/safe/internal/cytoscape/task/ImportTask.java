package edu.princeton.safe.internal.cytoscape.task;

import java.io.File;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.EnrichmentLandscapeBuilder;
import edu.princeton.safe.FactoryMethod;
import edu.princeton.safe.internal.DefaultEnrichmentLandscapeBuilder;
import edu.princeton.safe.internal.DenseAnnotationProvider;
import edu.princeton.safe.internal.SparseNetworkProvider;
import edu.princeton.safe.internal.cytoscape.io.CyNetworkParser;
import edu.princeton.safe.internal.cytoscape.io.CyProgressReporter;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.io.TabDelimitedAnnotationParser;
import edu.princeton.safe.model.EnrichmentLandscape;

public class ImportTask extends AbstractTask {

    SafeSession session;
    ImportTaskConsumer consumer;
    FactoryMethod<TabDelimitedAnnotationParser> parserFactory;

    public ImportTask(SafeSession session,
                      ImportTaskConsumer consumer,
                      FactoryMethod<TabDelimitedAnnotationParser> parserFactory) {
        this.session = session;
        this.consumer = consumer;
        this.parserFactory = parserFactory;
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("SAFE: Compute Enrichment Landscape");

        CyNetworkView view = session.getNetworkView();

        String nameColumn = session.getNameColumn();
        String idColumn = session.getIdColumn();
        String weightColumn = session.getWeightColumn();

        if (idColumn == null) {
            idColumn = nameColumn;
        }

        monitor.setStatusMessage("Loading network...");

        CyNetworkParser parser = new CyNetworkParser(view, nameColumn, idColumn, weightColumn);
        SparseNetworkProvider networkProvider = new SparseNetworkProvider(parser);

        monitor.showMessage(Level.INFO, String.format("Nodes imported: %d", networkProvider.getNodeCount()));
        monitor.showMessage(Level.INFO, String.format("Nodes skipped: %d", parser.getSkippedNodeCount()));
        monitor.showMessage(Level.INFO, String.format("Edges imported: %d", parser.getEdgeCount()));
        monitor.showMessage(Level.INFO, String.format("Edges skipped: %d", parser.getSkippedEdgeCount()));

        consumer.accept(parser.getNodeMappings());

        monitor.setStatusMessage("Loading annotations...");

        AnnotationProvider annotationProvider;
        File annotationFile = session.getAnnotationFile();
        if (annotationFile != null) {
            TabDelimitedAnnotationParser annotationParser = parserFactory.create();
            annotationProvider = new DenseAnnotationProvider(networkProvider, annotationParser);
            Set<String> missingNodes = annotationParser.getMissingNodes();
            int totalMissingNodes = missingNodes.size();
            int totalAnnotatedNodes = annotationProvider.getNetworkNodeCount() - totalMissingNodes;

            monitor.showMessage(Level.INFO,
                                String.format("Attributes imported: %d", annotationProvider.getAttributeCount()));
            monitor.showMessage(Level.INFO, String.format("Nodes annotated: %d", totalAnnotatedNodes));
            monitor.showMessage(Level.INFO,
                                String.format("Unannotated nodes in network: %s", String.join(", ", missingNodes)));
            monitor.showMessage(Level.INFO, String.format("Total unannotated nodes in network: %d", totalMissingNodes));
        } else {
            throw new RuntimeException();
        }

        EnrichmentLandscapeBuilder builder = new DefaultEnrichmentLandscapeBuilder();
        EnrichmentLandscape result = builder.setNetworkProvider(networkProvider)
                                            .setAnnotationProvider(annotationProvider)
                                            .setDistanceMetric(session.getDistanceMetric())
                                            .setDistanceThresholdAbsolute(session.isDistanceThresholdAbsolute())
                                            .setDistanceThreshold(session.getDistanceThreshold())
                                            .setBackgroundMethod(session.getBackgroundMethod())
                                            .setQuantitativeIterations(session.getQuantitativeIterations())
                                            .setProgressReporter(new CyProgressReporter(monitor))
                                            .build();

        consumer.accept(result);
    }

}
