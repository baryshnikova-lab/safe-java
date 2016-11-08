package edu.princeton.safe.internal.cytoscape.task;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.CompositeMapBuilder;
import edu.princeton.safe.internal.cytoscape.io.CyProgressReporter;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class BuildCompositeMapTask extends AbstractTask {

    SafeSession session;
    BuildCompositeMapTaskConsumer consumer;

    public BuildCompositeMapTask(SafeSession session,
                                 BuildCompositeMapTaskConsumer consumer) {

        this.session = session;
        this.consumer = consumer;
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("SAFE: Compute Composite Map");

        CompositeMapBuilder builder = session.getEnrichmentLandscape()
                                             .getCompositeMapBuilder();
        CyProgressReporter progressReporter = new CyProgressReporter(monitor);
        CompositeMap compositeMap = builder.setGroupingMethod(session.getGroupingMethod())
                                           .setRestrictionMethod(session.getRestrictionMethod())
                                           .setMinimumLandscapeSize(session.getMinimumLandscapeSize())
                                           .setProgressReporter(progressReporter)
                                           .build();

        consumer.accept(compositeMap);

        boolean hasHighestDomains = compositeMap.getDomains(EnrichmentLandscape.TYPE_HIGHEST) != null;
        boolean hasLowestDomains = compositeMap.getDomains(EnrichmentLandscape.TYPE_LOWEST) != null;

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

        boolean hasDomains = annotationProvider.isBinary() && hasHighestDomains;
        hasDomains = hasDomains || !annotationProvider.isBinary() && hasHighestDomains && hasLowestDomains;

        if (!hasDomains) {
            throw new Exception("No domains were found");
        }
    }

}
