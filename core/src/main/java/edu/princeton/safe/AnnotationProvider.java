package edu.princeton.safe;

public interface AnnotationProvider {

    int getNetworkNodeCount();

    int getAnnotationNodeCount();

    int getAttributeCount();

    double getValue(int nodeIndex,
                    int attributeIndex);

    int getNetworkNodeCountForAttribute(int attributeIndex);

    int getAnnotationNodeCountForAttribute(int attributeIndex);

    boolean isBinary();

    String getAttributeLabel(int attributeIndex);

    void forEachAttributeValue(int attributeIndex,
                               IndexedDoubleConsumer consumer);
}
