package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;

public abstract class DefaultAnnotationProvider implements AnnotationProvider {

    String[] attributeLabels;
    int[] zerosPerAttribute;
    int[] nonZerosPerAttribute;
    boolean isBinary;

    void setAttributeLabes(String[] labels) {
        attributeLabels = labels;

        int totalAttributes = labels.length;
        zerosPerAttribute = new int[totalAttributes];
        nonZerosPerAttribute = new int[totalAttributes];
    }

    @Override
    public int getAttributeCount() {
        return attributeLabels.length;
    }

    @Override
    public int getNodeCountForAttribute(int attributeIndex) {
        int count = nonZerosPerAttribute[attributeIndex];
        if (!isBinary) {
            count += zerosPerAttribute[attributeIndex];
        }
        return count;
    }

    void handleAttributeValue(int nodeIndex,
                              int attributeIndex,
                              double value) {
        if (nodeIndex == -1) {
            return;
        }

        if (value == 0) {
            zerosPerAttribute[attributeIndex]++;
        } else {
            nonZerosPerAttribute[attributeIndex]++;
        }
    }

    @Override
    public boolean isBinary() {
        return isBinary;
    }

    @Override
    public String getAttributeLabel(int attributeIndex) {
        return attributeLabels[attributeIndex];
    }

}
