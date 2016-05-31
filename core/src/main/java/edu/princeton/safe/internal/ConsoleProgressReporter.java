package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;

public class ConsoleProgressReporter implements ProgressReporter {

    NetworkProvider networkProvider;
    int currentNodeIndex;

    @Override
    public void neighborhoodScore(int nodeIndex,
                                  int attributeIndex,
                                  double score) {

        if (nodeIndex != currentNodeIndex) {
            System.out.printf("\n%s", networkProvider.getNodeLabel(nodeIndex));
            currentNodeIndex = nodeIndex;
        }

        System.out.printf("\t%.3f", score);
    }

    @Override
    public boolean supportsParallel() {
        return false;
    }

    @Override
    public void startNeighborhoodScore(NetworkProvider networkProvider,
                                       AnnotationProvider annotationProvider) {

        this.networkProvider = networkProvider;
        currentNodeIndex = -1;
    }

    @Override
    public void finishNeighborhoodScore() {
        System.out.println();
    }

    @Override
    public void finishNeighborhood(int nodeIndex) {
    }

}
