package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class BuildCompositeMapTaskFactory implements TaskFactory {

    SafeSession session;
    BuildCompositeMapTaskConsumer consumer;

    public BuildCompositeMapTaskFactory(SafeSession session,
                                        BuildCompositeMapTaskConsumer consumer) {

        this.session = session;
        this.consumer = consumer;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new BuildCompositeMapTask(session, consumer));
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
