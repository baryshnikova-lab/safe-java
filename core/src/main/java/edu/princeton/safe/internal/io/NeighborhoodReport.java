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
                             LabelFunction labelFunction)
            throws IOException {

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        writeHeaders(writer, annotationProvider);

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
                             AnnotationProvider annotationProvider) {
        writer.print("ORF");

        IntStream.range(0, annotationProvider.getAttributeCount())
                 .mapToObj(j -> annotationProvider.getAttributeLabel(j))
                 .forEach(label -> {
                     writer.print("\t");
                     writer.print(label);
                 });
        writer.println();
    }
}
