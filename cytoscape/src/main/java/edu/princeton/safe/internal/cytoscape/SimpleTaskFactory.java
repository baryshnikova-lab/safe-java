package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.princeton.safe.FactoryMethod;

public class SimpleTaskFactory implements TaskFactory {
    FactoryMethod<Task> factory;

    public SimpleTaskFactory(FactoryMethod<Task> factory) {
        this.factory = factory;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(factory.create());
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
