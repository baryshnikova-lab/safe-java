package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.CompositeMapBuilder;
import edu.princeton.safe.model.CompositeMap;

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

        CompositeMapBuilder builder = session.enrichmentLandscape.getCompositeMapBuilder();
        CyProgressReporter progressReporter = new CyProgressReporter(monitor);
        CompositeMap compositeMap = builder.setGroupingMethod(session.getGroupingMethod())
                                           .setRestrictionMethod(session.getRestrictionMethod())
                                           .setMinimumLandscapeSize(session.getMinimumLandscapeSize())
                                           .setProgressReporter(progressReporter)
                                           .build();

        consumer.accept(compositeMap);
    }

}
