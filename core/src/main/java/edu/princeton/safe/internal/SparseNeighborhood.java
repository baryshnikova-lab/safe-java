package edu.princeton.safe.internal;

import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

import com.carrotsearch.hppc.IntDoubleScatterMap;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;

public class SparseNeighborhood extends DefaultNeighborhood {

    IntDoubleScatterMap significanceScores;
    IntDoubleScatterMap distances;

    public SparseNeighborhood(int nodeIndex,
                              int totalAttributes) {
        super(nodeIndex, totalAttributes);
        significanceScores = new IntDoubleScatterMap();
        distances = new IntDoubleScatterMap();
    }

    @Override
    public void setSignificance(int attributeIndex,
                                double pValue) {
        significanceScores.put(attributeIndex, pValue);
    }

    @Override
    double getSignificance(int attributeIndex) {
        return significanceScores.get(attributeIndex);
    }

    @Override
    DoubleStream streamDistances() {
        return StreamSupport.stream(distances.spliterator(), false)
                            .mapToDouble(c -> c.value);
    }

    @Override
    void applyDistanceThreshold(double maximumDistanceThreshold) {
        distances.forEach(new Consumer<IntDoubleCursor>() {
            public void accept(IntDoubleCursor cursor) {
                if (Double.isNaN(cursor.value) || cursor.value >= maximumDistanceThreshold) {
                    return;
                }
                addNode(cursor.key);
            };
        });
    }
    
    @Override
    public void setDistance(int nodeIndex,
                            double distance) {
        
        distances.put(nodeIndex, distance);
    }
}
