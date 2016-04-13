package edu.princeton.safe.internal;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.princeton.safe.NodePair;

public class ParallelSafeTest {
    private static double DELTA = 0.00001;

    @Test
    public void computeMaximumDistanceThreshold() {
        List<NodePair> pairs = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            NodePair pair = new DefaultNodePair(i, i);
            pair.setDistance(i);
            pairs.add(pair);
        }
        double threshold = ParallelSafe.computeMaximumDistanceThreshold(pairs, 50);
        assertEquals(5, threshold, DELTA);
    }

    @Test
    public void binomialCoefficient() {
        assertEquals(1, ParallelSafe.binomialCoefficient(0, 0), DELTA);

        assertEquals(1, ParallelSafe.binomialCoefficient(4, 4), DELTA);
        assertEquals(5, ParallelSafe.binomialCoefficient(5, 4), DELTA);
        assertEquals(15, ParallelSafe.binomialCoefficient(6, 4), DELTA);
        assertEquals(35, ParallelSafe.binomialCoefficient(7, 4), DELTA);
        assertEquals(70, ParallelSafe.binomialCoefficient(8, 4), DELTA);
    }

}
