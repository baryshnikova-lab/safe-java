package edu.princeton.safe.internal;

import java.util.ArrayList;
import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;

public class DefaultCompositeMap implements CompositeMap {

    List<DefaultDomain>[] domainsByType;
    DomainConsumer consumer;
    DefaultDomain[][] topDomain;
    double[][] maximumEnrichment;
    boolean[][] isTop;

    @SuppressWarnings("unchecked")
    public DefaultCompositeMap(AnnotationProvider provider) {
        int totalTypes = provider.isBinary() ? 1 : 2;
        domainsByType = new List[totalTypes];

        int totalNodes = provider.getNetworkNodeCount();
        topDomain = new DefaultDomain[totalTypes][totalNodes];
        maximumEnrichment = new double[totalTypes][totalNodes];

        int totalAttributes = provider.getAttributeCount();
        isTop = new boolean[totalTypes][totalAttributes];

        consumer = new DefaultDomainConsumer();
    }

    @Override
    public List<? extends Domain> getDomains(int typeIndex) {
        return domainsByType[typeIndex];
    }

    @Override
    public boolean isTop(int attributeIndex,
                         int typeIndex) {
        return isTop[typeIndex][attributeIndex];
    }

    @Override
    public void setTop(int attributeIndex,
                       int typeIndex,
                       boolean value) {
        isTop[typeIndex][attributeIndex] = value;
    }

    void addDomain(int typeIndex,
                   DefaultDomain domain) {
        List<DefaultDomain> domains = domainsByType[typeIndex];
        if (domains == null) {
            domains = new ArrayList<>();
            domainsByType[typeIndex] = domains;
        }
        domains.add(domain);
    }

    @Override
    public Domain getTopDomain(int nodeIndex,
                               int typeIndex) {
        return topDomain[typeIndex][nodeIndex];
    }

    @Override
    public double getMaximumEnrichment(int nodeIndex,
                                       int typeIndex) {
        return maximumEnrichment[typeIndex][nodeIndex];
    }

    DomainConsumer getConsumer() {
        return consumer;
    }

    class DefaultDomainConsumer implements DomainConsumer {
        DefaultDomain domain;

        @Override
        public void startDomain(int typeIndex) {
            domain = new DefaultDomain();
            addDomain(typeIndex, domain);
        }

        @Override
        public void attribute(int attributeIndex) {
            domain.addAttribute(attributeIndex);
        }

        @Override
        public void endDomain() {
        }

    }

}
