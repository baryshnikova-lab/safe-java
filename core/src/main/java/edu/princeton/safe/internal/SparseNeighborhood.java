package edu.princeton.safe.internal;

import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

import com.carrotsearch.hppc.IntDoubleScatterMap;
import com.carrotsearch.hppc.cursors.IntDoubleCursor;

public class SparseNeighborhood extends DefaultNeighborhood {

    IntDoubleScatterMap distances;

    public SparseNeighborhood(int nodeIndex,
                              int totalAttributes) {
        super(nodeIndex, totalAttributes);
        distances = new IntDoubleScatterMap();
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
                if (Double.isNaN(cursor.value) || cursor.value > maximumDistanceThreshold) {
                    return;
                }
                addMember(cursor.key);
            };
        });
    }

    @Override
    public void setNodeDistance(int nodeIndex,
                                double distance) {

        distances.put(nodeIndex, distance);
    }

    @Override
    public double getMemberDistance(int memberIndex) {
        int nodeIndex = memberIndexes.get(memberIndex);
        return distances.get(nodeIndex);
    }
}
