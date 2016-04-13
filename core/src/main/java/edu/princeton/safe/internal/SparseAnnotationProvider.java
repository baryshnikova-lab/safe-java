package edu.princeton.safe.internal;

import java.io.IOException;

import org.apache.commons.math3.linear.DefaultRealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.OpenMapRealMatrix;

import edu.princeton.safe.AnnotationParser;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.IndexedDoubleConsumer;
import edu.princeton.safe.NetworkProvider;

public class SparseAnnotationProvider implements AnnotationProvider {

    String[] attributeLabels;
    OpenMapRealMatrix values;
    int[] nodesPerAttribute;
    boolean isBinary;

    public SparseAnnotationProvider(NetworkProvider networkProvider,
                                    String path)
            throws IOException {
        AnnotationParser parser = new TabDelimitedAnnotationParser() {

            @Override
            public void start(String[] labels,
                              int totalNodes) {
                attributeLabels = labels;
                int totalAttributes = labels.length;
                values = new OpenMapRealMatrix(totalNodes, totalAttributes);
                nodesPerAttribute = new int[totalAttributes];
                isBinary = true;
            }

            @Override
            public void addValue(int nodeIndex,
                                 int attributeIndex,
                                 double value) {
                values.setEntry(nodeIndex, attributeIndex, value);
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
        return values.getRowDimension();
    }

    @Override
    public int getAttributeCount() {
        return attributeLabels.length;
    }

    @Override
    public double getValue(int nodeIndex,
                           int attributeIndex) {
        return values.getEntry(nodeIndex, attributeIndex);
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
        values.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
            @Override
            public void visit(int row,
                              int column,
                              double value) {
                consumer.accept(row, value);
            }
        }, 0, values.getRowDimension() - 1, attributeIndex, attributeIndex);

    }
}
