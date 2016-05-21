package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class SafeTaskFactory implements TaskFactory {

    private SafeSession session;

    public SafeTaskFactory(SafeSession session) {
        this.session = session;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new SafeTask(session));
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
