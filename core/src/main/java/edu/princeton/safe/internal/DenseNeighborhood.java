package edu.princeton.safe.internal;

import java.util.function.IntConsumer;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.Neighborhood;

public class DenseNeighborhood implements Neighborhood {

    int nodeIndex;

    double[] significanceScores;

    int[] nodeCountsPerAttribute;
    IntArrayList memberIndexes;

    public DenseNeighborhood(int nodeIndex,
                             int nodeCount,
                             int attributeCount) {
        this.nodeIndex = nodeIndex;
        significanceScores = new double[attributeCount];
        memberIndexes = new IntArrayList();
        nodeCountsPerAttribute = new int[attributeCount];

        for (int j = 0; j < attributeCount; j++) {
            nodeCountsPerAttribute[j] = -1;
        }
    }

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
    public void setSignificance(int attributeIndex,
                                double pValue) {
        significanceScores[attributeIndex] = pValue;
    }

    @Override
    public double getEnrichmentScore(int attributeIndex) {
        double pValue = significanceScores[attributeIndex];
        return Neighborhood.computeEnrichmentScore(pValue);
    }

    @Override
    public int getNodeCountForAttribute(int j,
                                        AnnotationProvider annotationProvider) {
        int count = nodeCountsPerAttribute[j];
        if (count != -1) {
            return count;
        }

        count = 0;
        for (int i = 0; i < memberIndexes.size(); i++) {
            int index = memberIndexes.get(i);
            double value = annotationProvider.getValue(index, j);
            if (!Double.isNaN(value) && value != 0) {
                count++;
            }
        }
        nodeCountsPerAttribute[j] = count;
        return count;
    }
}
