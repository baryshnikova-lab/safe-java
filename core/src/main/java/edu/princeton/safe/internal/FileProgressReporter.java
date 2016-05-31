package edu.princeton.safe.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.model.Neighborhood;

public class FileProgressReporter implements ProgressReporter {

    AtomicInteger totalSignificant;
    double enrichmentThreshold;
    PrintWriter writer;
    NetworkProvider networkProvider;
    int currentNodeIndex;
    String outputPath;

    public FileProgressReporter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void startNeighborhoodScore(NetworkProvider networkProvider,
                                       AnnotationProvider annotationProvider) {
        this.networkProvider = networkProvider;
        totalSignificant = new AtomicInteger();
        int totalAttributes = annotationProvider.getAttributeCount();
        enrichmentThreshold = Neighborhood.getEnrichmentThreshold(totalAttributes);
        currentNodeIndex = -1;

        try {
            writer = new PrintWriter(new FileWriter(outputPath));
            writer.printf("ORF");
            for (int j = 0; j < totalAttributes; j++) {
                writer.print("\t");
                writer.print(annotationProvider.getAttributeLabel(j));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishNeighborhoodScore() {
        System.out.printf("Significant: %d\n", totalSignificant.get());
        writer.close();
    }

    @Override
    public void neighborhoodScore(int nodeIndex,
                                  int attributeIndex,
                                  double score) {

        if (nodeIndex != currentNodeIndex) {
            writer.printf("\n%s", networkProvider.getNodeLabel(nodeIndex));
            currentNodeIndex = nodeIndex;
        }

        writer.printf("\t%.3f", score);

        if (score > enrichmentThreshold) {
            totalSignificant.incrementAndGet();
        }
    }

    @Override
    public boolean supportsParallel() {
        return false;
    }

    @Override
    public void finishNeighborhood(int nodeIndex) {
    }
}
