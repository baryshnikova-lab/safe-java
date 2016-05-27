package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class SafeTaskFactory implements TaskFactory {

    private SafeSession session;
    private SafeController controller;

    public SafeTaskFactory(SafeSession session,
                           SafeController controller) {
        this.session = session;
        this.controller = controller;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new SafeTask(session, controller));
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
