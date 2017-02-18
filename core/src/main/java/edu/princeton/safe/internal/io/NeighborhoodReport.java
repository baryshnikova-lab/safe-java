package edu.princeton.safe.internal.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.IntStream;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.io.LabelFunction;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public class NeighborhoodReport {
    public static void write(PrintWriter writer,
                             EnrichmentLandscape landscape,
                             int typeIndex,
                             String nodeColumnName,
                             LabelFunction labelFunction)
            throws IOException {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        writeHeaders(writer, annotationProvider, nodeColumnName);

        ScoringFunction scoringFunction = Neighborhood.getScoringFunction(typeIndex);
        landscape.getNeighborhoods()
                 .stream()
                 .forEach(n -> {
                     String label = labelFunction.get(n.getNodeIndex());
                     writer.print(label);

                     IntStream.range(0, annotationProvider.getAttributeCount())
                              .mapToDouble(j -> scoringFunction.get(n, j))
                              .forEach(enrichment -> {
                                  writer.printf("\t%.3f", enrichment);
                              });

                     writer.println();
                 });

        writer.flush();
    }

    static void writeHeaders(PrintWriter writer,
                             AnnotationProvider annotationProvider,
                             String nodeColumnName) {

        int totalAttributes = annotationProvider.getAttributeCount();
        double threshold = Neighborhood.getEnrichmentThreshold(totalAttributes);

        writer.println("## This file lists the -log10 scores that represent the enrichment of each node's neighborhood for ach of the tested attributes.");
        writer.println("## High values (close to 1) indicate high enrichment. Low values (close to 0) indicate low enrichment.");
        writer.printf("## Any value higher than %.3f is considered significant (-log10 of p-value = 0.05, corrected for multiple testing and scaled to [0,1] range).\n",
                      threshold);

        writer.print(nodeColumnName);

        IntStream.range(0, totalAttributes)
                 .mapToObj(j -> annotationProvider.getAttributeLabel(j))
                 .forEach(label -> {
                     writer.print("\t");
                     writer.print(label);
                 });
        writer.println();
    }
}
