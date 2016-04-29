package edu.princeton.safe.internal;

import java.util.function.IntConsumer;
import java.util.stream.DoubleStream;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.Neighborhood;

public abstract class DefaultNeighborhood implements Neighborhood {

    int nodeIndex;
    IntArrayList memberIndexes;
    int[] nodeCountsPerAttribute;

    public DefaultNeighborhood(int nodeIndex,
                               int totalAttributes) {
        this.nodeIndex = nodeIndex;
        memberIndexes = new IntArrayList();
        nodeCountsPerAttribute = new int[totalAttributes];

        for (int j = 0; j < totalAttributes; j++) {
            nodeCountsPerAttribute[j] = -1;
        }
    }

    abstract double getSignificance(int attributeIndex);
    abstract DoubleStream streamDistances();
    abstract void applyDistanceThreshold(double maximumDistanceThreshold);

    @Override
    public int getNodeIndex() {
        return nodeIndex;
    }

    @Override
    public int getNodeCount() {
        return memberIndexes.size();
    }

    @Override
    public void addNode(int nodeIndex) {
        memberIndexes.add(nodeIndex);
    }

    @Override
    public void forEachNodeIndex(IntConsumer action) {
        memberIndexes.forEach((IntCursor c) -> action.accept(c.value));
    }

    @Override
    public int getNodeCountForAttribute(int j,
                                        AnnotationProvider annotationProvider) {
        int count = nodeCountsPerAttribute[j];
        if (count != -1) {
            return count;
        }

        count = 0;
        boolean isBinary = annotationProvider.isBinary();
        for (int i = 0; i < memberIndexes.size(); i++) {
            int index = memberIndexes.get(i);
            double value = annotationProvider.getValue(index, j);
            if (isBinary && value == 1) {
                count++;
            } else if (!isBinary && !Double.isNaN(value)) {
                count++;
            }
        }
        nodeCountsPerAttribute[j] = count;
        return count;
    }

    @Override
    public double getEnrichmentScore(int attributeIndex) {
        double pValue = getSignificance(attributeIndex);
        return Neighborhood.computeEnrichmentScore(pValue);
    }

}
