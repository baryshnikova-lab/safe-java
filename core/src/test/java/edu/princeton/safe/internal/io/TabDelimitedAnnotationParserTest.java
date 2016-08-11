package edu.princeton.safe.internal.io;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import edu.princeton.safe.internal.DenseAnnotationProvider;
import edu.princeton.safe.internal.SparseNetworkProvider;
import edu.princeton.safe.io.NetworkConsumer;
import edu.princeton.safe.io.NetworkParser;

public class TabDelimitedAnnotationParserTest {

    @Test
    public void testNaN() throws Exception {
        SparseNetworkProvider networkProvider = new SparseNetworkProvider(new NetworkParser() {
            @Override
            public void parse(NetworkConsumer consumer) throws IOException {
                consumer.startNodes();
                int index = 0;
                for (String name : new String[] { "A", "B", "C", "D" }) {
                    consumer.node(index, name, name, 0, 0);
                    index++;
                }
                consumer.finishNodes();
                consumer.startEdges();
                consumer.finishEdges();
            }

            @Override
            public boolean isDirected() {
                return false;
            }
        });

        TabDelimitedAnnotationParser parser = new TabDelimitedAnnotationParser("src/test/resources/testAnnotation1.txt",
                                                                               1, "#");
        DenseAnnotationProvider annotationProvider = new DenseAnnotationProvider(networkProvider, parser);

        Assert.assertEquals(6, parser.totalLines);
        Assert.assertEquals(2, parser.skippedLines);

        Assert.assertTrue(Double.isNaN(annotationProvider.getValue(1, 2)));
        Assert.assertTrue(Double.isNaN(annotationProvider.getValue(2, 0)));
        Assert.assertTrue(Double.isNaN(annotationProvider.getValue(3, 1)));
    }
}
