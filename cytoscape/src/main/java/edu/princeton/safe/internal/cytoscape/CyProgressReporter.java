package edu.princeton.safe.internal.cytoscape;

import java.util.concurrent.atomic.AtomicInteger;

import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;

public class CyProgressReporter implements ProgressReporter {

    TaskMonitor monitor;

    AtomicInteger counter;
    int expectedTotal;

    public CyProgressReporter(TaskMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void neighborhoodScore(int nodeIndex,
                                  int attributeIndex,
                                  double score) {
    }

    @Override
    public boolean supportsParallel() {
        return true;
    }

    @Override
    public void startNeighborhoodScore(NetworkProvider networkProvider,
                                       AnnotationProvider annotationProvider) {

        counter = new AtomicInteger();
        expectedTotal = networkProvider.getNodeCount();
        monitor.setStatusMessage("Computing enrichment landscape...");
        monitor.setProgress(0);
    }

    @Override
    public void finishNeighborhoodScore() {
    }

    @Override
    public void finishNeighborhood(int nodeIndex) {
        int count = counter.incrementAndGet();
        monitor.setProgress((double) count / expectedTotal);
    }
}
