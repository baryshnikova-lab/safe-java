package edu.princeton.safe.internal;

import java.io.IOException;

import edu.princeton.safe.AnnotationParser;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.IndexedDoubleConsumer;
import edu.princeton.safe.NetworkProvider;

public class DenseAnnotationProvider implements AnnotationProvider {

    String[] attributeLabels;
    double[][] values;
    int[] nodesPerAttribute;
    boolean isBinary;
    
    public DenseAnnotationProvider(NetworkProvider networkProvider,
                                          String path)
            throws IOException {
        AnnotationParser parser = new TabDelimitedAnnotationParser() {

            @Override
            public void start(String[] labels,
                              int totalNodes) {
                attributeLabels = labels;
                int totalAttributes = labels.length;
                values = new double[totalNodes][totalAttributes];
                nodesPerAttribute = new int[totalAttributes];
                isBinary = true;
            }

            @Override
            public void addValue(int nodeIndex,
                                 int attributeIndex,
                                 double value) {
                values[nodeIndex][attributeIndex] = value;
                nodesPerAttribute[attributeIndex]++;
                if (value != 0 && value != 1) {
                    isBinary = false;
                }
            }
        };
        parser.parse(networkProvider, path);
    }

    @Override
    public int getNodeCount() {
        return values.length;
    }

    @Override
    public int getAttributeCount() {
        return attributeLabels.length;
    }

    @Override
    public double getValue(int nodeIndex,
                           int attributeIndex) {
        return values[nodeIndex][attributeIndex];
    }

    @Override
    public int getNodeCountForAttribute(int attributeIndex) {
        return nodesPerAttribute[attributeIndex];
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
    public void forEachAttributeValue(int attributeIndex,
                                      IndexedDoubleConsumer consumer) {
        for (int i = 0; i < values.length; i++) {
            double value = values[i][attributeIndex];
            if (Double.isNaN(value)) {
                continue;
            }
            consumer.accept(i, value);
        }
    }
}
