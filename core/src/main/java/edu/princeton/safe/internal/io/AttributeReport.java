package edu.princeton.safe.internal.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.EnrichmentLandscape;

public class AttributeReport {
    public static void write(PrintWriter writer,
                             EnrichmentLandscape landscape,
                             CompositeMap compositeMap,
                             int typeIndex)
            throws IOException {

        writeHeaders(writer);

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

        List<? extends Domain> domains = compositeMap.getDomains(typeIndex);
        domains.stream()
               .forEach(domain -> {
                   int domainIndex = domain.getIndex();

                   domain.forEachAttribute(attributeIndex -> {
                       writer.print(attributeIndex);
                       writer.print("\t");

                       writer.print(annotationProvider.getAttributeLabel(attributeIndex));
                       writer.print("\t");

                       // To match MATLAB output, domain ids start at 2.
                       writer.print(domainIndex + 2);
                       writer.println();
                   });
               });

        writer.flush();
    }

    static void writeHeaders(PrintWriter writer) {
        writer.print("Attribute Id");
        writer.print("\tAttribute name");
        writer.print("\tDomain Id\n");
    }
}
