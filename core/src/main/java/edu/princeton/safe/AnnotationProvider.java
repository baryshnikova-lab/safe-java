package edu.princeton.safe;

public interface AnnotationProvider {

    int getNodeCount();

    int getAttributeCount();

    double getValue(int nodeIndex,
                    int attributeIndex);

    int getNodeCountForAttribute(int attributeIndex);

    boolean isBinary();

    String getAttributeLabel(int attributeIndex);

    void forEachAttributeValue(int attributeIndex,
                               IndexedDoubleConsumer consumer);
}
