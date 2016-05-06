package edu.princeton.safe.internal;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class DenseNeighborhood extends DefaultNeighborhood {

    double[] significanceScores;
    double[] distances;

    public DenseNeighborhood(int nodeIndex,
                             int nodeCount,
                             int totalAttributes) {
        super(nodeIndex, totalAttributes);
        significanceScores = new double[totalAttributes];

        distances = IntStream.range(0, nodeCount)
                             .mapToDouble(n -> Double.NaN)
                             .toArray();
    }

    @Override
    public void setPValue(int attributeIndex,
                          double pValue) {
        significanceScores[attributeIndex] = pValue;
    }

    @Override
    double getSignificance(int attributeIndex) {
        return significanceScores[attributeIndex];
    }

    @Override
    DoubleStream streamDistances() {
        return Arrays.stream(distances);
    }

    @Override
    void applyDistanceThreshold(double maximumDistanceThreshold) {
        for (int i = 0; i < distances.length; i++) {
            double distance = distances[i];
            if (Double.isNaN(distance) || distance > maximumDistanceThreshold) {
                continue;
            }
            addMember(i);
        }
    }

    @Override
    public void setNodeDistance(int nodeIndex,
                                double distance) {
        distances[nodeIndex] = distance;
    }

    @Override
    public double getMemberDistance(int memberIndex) {
        int nodeIndex = memberIndexes.get(memberIndex);
        return distances[nodeIndex];
    }
}
