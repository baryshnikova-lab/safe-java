package edu.princeton.safe.internal.cytoscape;

import java.util.concurrent.atomic.AtomicInteger;

import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.model.EnrichmentLandscape;

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

    @Override
    public void startUnimodality(AnnotationProvider annotationProvider) {
        counter = new AtomicInteger();
        expectedTotal = annotationProvider.getAttributeCount();
        monitor.setStatusMessage("Computing enrichment landscape...");
        monitor.setProgress(0);
    }

    @Override
    public void isUnimodal(int attributeIndex,
                           int typeIndex,
                           boolean isIncluded) {

        if (typeIndex != EnrichmentLandscape.TYPE_HIGHEST) {
            return;
        }
        int count = counter.incrementAndGet();
        monitor.setProgress((double) count / expectedTotal);
    }

    @Override
    public void finishUnimodality() {
    }

    @Override
    public void setStatus(String format,
                          Object... parameters) {

        String message = String.format(format, parameters);
        monitor.setStatusMessage(message);
    }
}
