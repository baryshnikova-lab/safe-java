package edu.princeton.safe.internal.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;

public class DomainReport {
    public static void write(PrintWriter writer,
                             CompositeMap compositeMap,
                             int typeIndex)
            throws IOException {

        writeHeaders(writer);

        List<? extends Domain> domains = compositeMap.getDomains(typeIndex);

        domains.stream()
               .forEach(domain -> {
                   // Domain indexes in MATLAB version start at 2
                   writer.print(domain.getIndex() + 2);
                   writer.print("\t");
                   writer.print(domain.getName());
                   writer.print("\t");

                   double[] color = domain.getColor();
                   writer.print((int) Math.round(color[0] * 255));
                   writer.print("\t");
                   writer.print((int) Math.round(color[1] * 255));
                   writer.print("\t");
                   writer.print((int) Math.round(color[2] * 255));
                   writer.println();

               });

        writer.flush();
    }

    static void writeHeaders(PrintWriter writer) {
        writer.print("Domain number");
        writer.print("\tDomain name");
        writer.print("\tRGB\n");
    }
}
