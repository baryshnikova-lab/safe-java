package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportTaskFactory implements TaskFactory {

    private SafeSession session;
    private ImportTaskConsumer consumer;

    public ImportTaskFactory(SafeSession session,
                             ImportTaskConsumer consumer) {
        this.session = session;
        this.consumer = consumer;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new ImportTask(session, consumer));
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
