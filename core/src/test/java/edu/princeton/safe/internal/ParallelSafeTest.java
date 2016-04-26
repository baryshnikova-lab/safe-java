package edu.princeton.safe.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ParallelSafeTest {
    private static double DELTA = 0.00001;

    @Test
    public void computeMaximumDistanceThreshold() {
        List<DefaultNeighborhood> pairs = new ArrayList<>();
        SparseNeighborhood neighborhood = new SparseNeighborhood(0, 0);
        pairs.add(neighborhood);
        for (int i = 0; i <= 10; i++) {
            neighborhood.setDistance(i, i);
        }
        double threshold = ParallelSafe.computeMaximumDistanceThreshold(pairs, 50);
        assertEquals(5, threshold, DELTA);
    }

}
