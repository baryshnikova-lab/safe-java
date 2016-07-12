package edu.princeton.safe.grouping;

import java.util.stream.IntStream;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.ProgressReporter;
import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class NullGroupingMethod implements GroupingMethod {

    public static final GroupingMethod instance = new NullGroupingMethod();

    NullGroupingMethod() {
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void group(EnrichmentLandscape result,
                      CompositeMap compositeMap,
                      int typeIndex,
                      DomainConsumer consumer,
                      ProgressReporter progressReporter) {

        AnnotationProvider annotationProvider = result.getAnnotationProvider();
        int attributeCount = annotationProvider.getAttributeCount();

        IntStream.range(0, attributeCount)
                 .filter(j -> compositeMap.isTop(j, typeIndex))
                 .forEach(j -> {
                     consumer.startDomain(typeIndex);
                     consumer.attribute(j);
                     consumer.endDomain();
                 });
    }

}
