package edu.princeton.safe.internal;

import java.util.ArrayList;
import java.util.List;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.io.DomainConsumer;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.DomainDetails;

public class DefaultDomainDetails implements DomainDetails {

    List<DefaultDomain>[] domainsByType;
    DomainConsumer consumer;
    DefaultDomain[][] topDomain;
    double[][] cumulativeOpacity;

    @SuppressWarnings("unchecked")
    public DefaultDomainDetails(AnnotationProvider provider) {
        int totalTypes = provider.isBinary() ? 1 : 2;
        domainsByType = new List[totalTypes];

        int totalNodes = provider.getNetworkNodeCount();
        topDomain = new DefaultDomain[totalTypes][totalNodes];
        cumulativeOpacity = new double[totalTypes][totalNodes];

        consumer = new DefaultDomainConsumer();
    }

    @Override
    public List<? extends Domain> getDomains(int typeIndex) {
        return domainsByType[typeIndex];
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
