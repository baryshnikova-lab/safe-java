package edu.princeton.safe.internal.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.io.LabelFunction;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;

public class NodeReport {
    public static void write(PrintWriter writer,
                             EnrichmentLandscape landscape,
                             CompositeMap compositeMap,
                             int typeIndex,
                             LabelFunction label)
            throws IOException {

        NetworkProvider networkProvider = landscape.getNetworkProvider();
        LabelFunction labelFunction;
        if (label == null) {
            labelFunction = i -> networkProvider.getNodeLabel(i);
        } else {
            labelFunction = label;
        }

        writeHeaders(writer);

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();

        List<? extends Domain> domains = compositeMap.getDomains(typeIndex);
        SignificancePredicate predicate = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);

        landscape.getNeighborhoods()
                 .stream()
                 .forEach(neighborhood -> {
                     int nodeIndex = neighborhood.getNodeIndex();

                     writer.print(labelFunction.get(nodeIndex));
                     writer.print("\t");

                     writer.print(networkProvider.getNodeIds(nodeIndex)
                                                 .stream()
                                                 .findFirst()
                                                 .orElse(null));
                     writer.print("\t");

                     // To match MATLAB output, 1 means no domain. 2+ means node
                     // has a top domain.
                     Domain topDomain = compositeMap.getTopDomain(nodeIndex, typeIndex);
                     int topDomainId = topDomain == null ? 1 : topDomain.getIndex() + 2;

                     writer.print(topDomainId);
                     writer.print("\t");

                     double maximumEnrichment = compositeMap.getMaximumEnrichment(nodeIndex, typeIndex);
                     writer.printf("%.3f", maximumEnrichment);
                     writer.print("\t");

                     int[] enrichedDomains = { 0 };
                     int[] totalEnrichedAttributesByDomain = domains.stream()
                                                                    .mapToInt(domain -> {
                                                                        int[] count = { 0 };
                                                                        domain.forEachAttribute(attributeIndex -> {
                                                                            if (predicate.test(neighborhood,
                                                                                               attributeIndex)) {
                                                                                count[0]++;
                                                                            }
                                                                        });
                                                                        if (count[0] > 0) {
                                                                            enrichedDomains[0]++;
                                                                        }
                                                                        return count[0];
                                                                    })
                                                                    .toArray();

                     writer.print(enrichedDomains[0]);
                     writer.print("\t");

                     for (int i = 0; i < totalEnrichedAttributesByDomain.length; i++) {
                         if (i != 0) {
                             writer.print(",");
                         }
                         writer.print(totalEnrichedAttributesByDomain[i]);
                     }
                     writer.println();
                 });

        writer.flush();
    }

    static void writeHeaders(PrintWriter writer) {
        writer.print("Node label");
        writer.print("\tNode label ORF");
        writer.print("\tDomain (predominant)");
        writer.print("\tNeighborhood score [max=1, min=0] (predominant)");
        writer.print("\tTotal number of enriched domains");
        writer.print("\tNumber of enriched attributes per domain\n");
    }
}
