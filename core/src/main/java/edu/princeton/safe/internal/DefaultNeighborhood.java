package edu.princeton.safe.internal;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.DoubleStream;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.model.Neighborhood;

public abstract class DefaultNeighborhood implements Neighborhood {

    int nodeIndex;
    IntArrayList memberIndexes;
    int[] nodeCountsPerAttribute;
    double[] pValues;
    double[] enrichmentScores;

    public DefaultNeighborhood(int nodeIndex,
                               int totalAttributes) {
        this.nodeIndex = nodeIndex;
        memberIndexes = new IntArrayList();
        nodeCountsPerAttribute = new int[totalAttributes];
        pValues = new double[totalAttributes];
        enrichmentScores = new double[totalAttributes];

        for (int j = 0; j < totalAttributes; j++) {
            nodeCountsPerAttribute[j] = -1;
        }
    }

    abstract DoubleStream streamDistances();

    abstract void applyDistanceThreshold(double maximumDistanceThreshold);

    @Override
    public int getNodeIndex() {
        return nodeIndex;
    }

    @Override
    public int getMemberCount() {
        return memberIndexes.size();
    }

    @Override
    public void addMember(int nodeIndex) {
        memberIndexes.add(nodeIndex);
    }

    @Override
    public int getMember(int memberIndex) {
        return memberIndexes.get(memberIndex);
    }

    @Override
    public void forEachMemberIndex(IntConsumer action) {
        Consumer<? super IntCursor> consumer = (IntCursor c) -> action.accept(c.value);
        memberIndexes.forEach(consumer);
    }

    @Override
    public int getMemberCountForAttribute(int j,
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
        return enrichmentScores[attributeIndex];
    }

    @Override
    public void setPValue(int attributeIndex,
                          double pValue) {
        pValues[attributeIndex] = pValue;
        enrichmentScores[attributeIndex] = Neighborhood.computeEnrichmentScore(pValue);
    }

    @Override
    public double getPValue(int attributeIndex) {
        return pValues[attributeIndex];
    }

}
