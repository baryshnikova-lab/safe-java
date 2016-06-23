package edu.princeton.safe.io;

public interface DomainConsumer {
    void startDomain(int typeIndex);

    void attribute(int attributeIndex);

    void endDomain();
}
