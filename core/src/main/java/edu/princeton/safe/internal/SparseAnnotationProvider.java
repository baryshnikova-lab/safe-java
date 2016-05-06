package edu.princeton.safe.internal;

import java.io.IOException;

import org.apache.commons.math3.linear.DefaultRealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.OpenMapRealMatrix;

import edu.princeton.safe.IndexedDoubleConsumer;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.AnnotationParser;

public class SparseAnnotationProvider extends DefaultAnnotationProvider {

    OpenMapRealMatrix values;

    public SparseAnnotationProvider(NetworkProvider networkProvider,
                                    AnnotationParser parser)
            throws IOException {

        parser.parse(networkProvider, new AnnotationConsumer() {

            @Override
            public void start(String[] labels,
                              int totalNodes) {
                setAttributeLabels(labels);
                int totalAttributes = labels.length;
                values = new OpenMapRealMatrix(totalNodes, totalAttributes);
                isBinary = true;
            }

            @Override
            public void value(int nodeIndex,
                              int attributeIndex,
                              double value) {
                if (nodeIndex != -1) {
                    values.setEntry(nodeIndex, attributeIndex, value);
                }
                if (value != 0 && value != 1) {
                    isBinary = false;
                }
                handleAttributeValue(nodeIndex, attributeIndex, value);
            }

            @Override
            public void finish(int annotationNodes) {
                totalAnnotationNodes = annotationNodes;
            }
        });
    }

    @Override
    public int getNetworkNodeCount() {
        return values.getRowDimension();
    }

    @Override
    public double getValue(int nodeIndex,
                           int attributeIndex) {
        return values.getEntry(nodeIndex, attributeIndex);
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
