package edu.princeton.safe.internal;

import java.io.IOException;

import edu.princeton.safe.IndexedDoubleConsumer;
import edu.princeton.safe.NetworkProvider;
import edu.princeton.safe.io.AnnotationConsumer;
import edu.princeton.safe.io.AnnotationParser;

public class DenseAnnotationProvider extends DefaultAnnotationProvider {

    double[][] values;

    public DenseAnnotationProvider(NetworkProvider networkProvider,
                                   AnnotationParser parser)
            throws IOException {

        parser.parse(networkProvider, new AnnotationConsumer() {

            @Override
            public void start(String[] labels,
                              int networkNodes) {
                setAttributeLabels(labels);
                int totalAttributes = labels.length;
                isBinary = true;

                values = new double[networkNodes][];
                for (int i = 0; i < networkNodes; i++) {
                    values[i] = Util.nanArray(totalAttributes);
                }
            }

            @Override
            public void value(int nodeIndex,
                              int attributeIndex,
                              double value) {
                if (nodeIndex != -1) {
                    values[nodeIndex][attributeIndex] = value;
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

            @Override
            public void skipped(String nodeId) {
            }
        });
    }

    @Override
    public int getNetworkNodeCount() {
        return values.length;
    }

    @Override
    public double getValue(int nodeIndex,
                           int attributeIndex) {
        return values[nodeIndex][attributeIndex];
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
