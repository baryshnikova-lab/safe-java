package edu.princeton.safe.internal;

import edu.princeton.safe.AnnotationProvider;

public abstract class DefaultAnnotationProvider implements AnnotationProvider {

    String[] attributeLabels;
    int[] networkZerosPerAttribute;
    int[] networkNonZerosPerAttribute;
    int[] annotationZerosPerAttribute;
    int[] annotationNonZerosPerAttribute;
    boolean isBinary;
    int totalAnnotationNodes;

    void setAttributeLabels(String[] labels) {
        attributeLabels = labels;

        int totalAttributes = labels.length;
        networkZerosPerAttribute = new int[totalAttributes];
        networkNonZerosPerAttribute = new int[totalAttributes];
        annotationZerosPerAttribute = new int[totalAttributes];
        annotationNonZerosPerAttribute = new int[totalAttributes];
    }

    @Override
    public int getAttributeCount() {
        return attributeLabels.length;
    }

    @Override
    public int getNetworkNodeCountForAttribute(int attributeIndex) {
        int count = networkNonZerosPerAttribute[attributeIndex];
        if (!isBinary) {
            count += networkZerosPerAttribute[attributeIndex];
        }
        return count;
    }

    @Override
    public int getAnnotationNodeCountForAttribute(int attributeIndex) {
        int count = annotationNonZerosPerAttribute[attributeIndex];
        if (!isBinary) {
            count += annotationZerosPerAttribute[attributeIndex];
        }
        return count;
    }

    void handleAttributeValue(int nodeIndex,
                              int attributeIndex,
                              double value) {
        if (value == 0) {
            annotationZerosPerAttribute[attributeIndex]++;
        } else {
            annotationNonZerosPerAttribute[attributeIndex]++;
        }
        if (nodeIndex == -1) {
            return;
        }

        if (value == 0) {
            networkZerosPerAttribute[attributeIndex]++;
        } else {
            networkNonZerosPerAttribute[attributeIndex]++;
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

    @Override
    public int getAnnotationNodeCount() {
        return totalAnnotationNodes;
    }

}
