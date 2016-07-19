package edu.princeton.safe.grouping;

import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.getIndex;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.pdist;

import org.junit.Assert;
import org.junit.Test;

public class ClusterBasedGroupingMethodTest {
    static final double DEFAULT_DELTA = 1e-8;

    @Test
    public void testPDistJaccard() {
        double[][] distances = { { 1, 0, 1, 0, 1, 0 }, { 1, 1, 1, 1, 1, 1 }, { 0, 0, 0, 0, 0, 0 },
                                 { 1, 0, 0, 0, 0, 0 } };
        double[] result = pdist(distances, new JaccardDistanceMethod(d -> d != 0));
        double[] expected = { 0.5, 1, 2.0 / 3, 1, 5.0 / 6, 1 };
        Assert.assertArrayEquals(expected, result, DEFAULT_DELTA);
    }

    @Test
    public void testPDistCorrelation() {
        double[][] distances = { { 1, 2, 3 }, { 3, 2, 1 }, { 3, 1, 3 }, { 1, 3, 1 } };
        double[] result = pdist(distances, DistanceMethod.CORRELATION);
        double[] expected = { 2, 1, 1, 1, 1, 2 };
        Assert.assertArrayEquals(expected, result, DEFAULT_DELTA);
    }

    @Test
    public void testGetIndex() {
        Assert.assertEquals(0, getIndex(2, 0, 1), DEFAULT_DELTA);
        Assert.assertEquals(0, getIndex(3, 0, 1), DEFAULT_DELTA);
        Assert.assertEquals(0, getIndex(10, 0, 1), DEFAULT_DELTA);
        Assert.assertEquals(0, getIndex(100, 0, 1), DEFAULT_DELTA);

        Assert.assertEquals(1, getIndex(3, 0, 2), DEFAULT_DELTA);
        Assert.assertEquals(8, getIndex(10, 0, 9), DEFAULT_DELTA);
        Assert.assertEquals(98, getIndex(100, 0, 99), DEFAULT_DELTA);

        Assert.assertEquals(2, getIndex(3, 1, 2), DEFAULT_DELTA);
    }

}
