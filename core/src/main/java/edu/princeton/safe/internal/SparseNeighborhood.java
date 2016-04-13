package edu.princeton.safe.internal;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDoubleScatterMap;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.Neighborhood;

public class SparseNeighborhood implements Neighborhood {

    int nodeIndex;

    IntDoubleScatterMap significanceScores;

    IntArrayList memberIndexes;

    public SparseNeighborhood(int nodeIndex) {
        this.nodeIndex = nodeIndex;
        significanceScores = new IntDoubleScatterMap();
        memberIndexes = new IntArrayList();
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
        significanceScores.put(attributeIndex, pValue);
    }

    @Override
    public double getEnrichmentScore(int attributeIndex) {
        double pValue = significanceScores.get(attributeIndex);
        return Neighborhood.computeEnrichmentScore(pValue);
    }

    @Override
    public int getNodeCountForAttribute(int j,
                                        AnnotationProvider annotationProvider) {
        int[] count = { 0 };
        memberIndexes.forEach(new Consumer<IntCursor>() {
            @Override
            public void accept(IntCursor cursor) {
                double value = annotationProvider.getValue(cursor.value, j);
                if (!Double.isNaN(value) && value != 0) {
                    count[0]++;
                }
            }
        });
        return 0;
    }
}
