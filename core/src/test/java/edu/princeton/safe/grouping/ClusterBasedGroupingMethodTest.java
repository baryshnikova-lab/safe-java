package edu.princeton.safe.grouping;

import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.computeClusters;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.computeLinkages;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.computeParents;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.getIndex;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.pdist;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.hppc.IntArrayList;

import edu.princeton.safe.grouping.ClusterBasedGroupingMethod.Linkage;

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

    @Test
    public void testClustering() {
        double[] d = { 2.9155, 1.0000, 3.0414, 3.0414, 2.5495, 3.3541, 2.5000, 2.0616, 2.0616, 1.0000 };
        int n = 5;
        List<Linkage> linkages = computeLinkages(d, n);
        int[] parents = computeParents(linkages, n, 1.1);
        List<IntArrayList> clusters = computeClusters(parents);

        Assert.assertEquals(4L, clusters.stream()
                                        .flatMapToInt(c -> Arrays.stream(c.buffer, 0, c.elementsCount))
                                        .count());

        List<IntArrayList> query1 = clusters.stream()
                                            .filter(c -> c.contains(2))
                                            .collect(Collectors.toList());
        Assert.assertEquals(1, query1.size());
        IntArrayList cluster02 = query1.get(0);
        Assert.assertEquals(2, cluster02.size());
        Assert.assertTrue(cluster02.contains(0));

        List<IntArrayList> query2 = clusters.stream()
                                            .filter(c -> c.contains(3))
                                            .collect(Collectors.toList());
        Assert.assertEquals(1, query2.size());
        IntArrayList cluster34 = query2.get(0);
        Assert.assertEquals(2, cluster34.size());
        Assert.assertTrue(cluster34.contains(4));

    }

    @Test
    public void testClustering2() {
        int n = 16;
        double[][] distances = new double[n][n];
        for (int k = 0; k < 4; k++) {
            int offset = k * 4;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    distances[offset + i][offset + j] = 1;
                }
            }
        }

        double[] d = pdist(distances, new JaccardDistanceMethod(x -> x != 0));
        List<Linkage> linkages = computeLinkages(d, n);
        int[] parents = computeParents(linkages, n, 0.5);
        List<IntArrayList> clusters = computeClusters(parents);
        Assert.assertEquals(4, clusters.size());
    }

    @Test
    public void testClustering3() {
        int n = 16;
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                distances[i][j] = 1;
            }
        }

        double[] d = pdist(distances, new JaccardDistanceMethod(x -> x != 0));
        List<Linkage> linkages = computeLinkages(d, n);
        int[] parents = computeParents(linkages, n, 0.5);
        List<IntArrayList> clusters = computeClusters(parents);
        Assert.assertEquals(1, clusters.size());
    }
}
