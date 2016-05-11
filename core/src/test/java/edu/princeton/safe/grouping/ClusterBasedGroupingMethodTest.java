package edu.princeton.safe.grouping;

import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.buildCluster;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.cut;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.getIndex;
import static edu.princeton.safe.grouping.ClusterBasedGroupingMethod.pdist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.opencompare.hac.dendrogram.Dendrogram;
import org.opencompare.hac.dendrogram.DendrogramBuilder;

import com.carrotsearch.hppc.IntArrayList;

import edu.princeton.safe.grouping.ClusterBasedGroupingMethod.ClusterConsumer;

public class ClusterBasedGroupingMethodTest {
    static final double DEFAULT_DELTA = 1e-8;

    @Test
    public void testPDistJaccard() {
        double[][] distances = { { 1, 0, 1, 0, 1, 0 }, { 1, 1, 1, 1, 1, 1 }, { 0, 0, 0, 0, 0, 0 },
                                 { 1, 0, 0, 0, 0, 0 } };
        double[] result = pdist(distances, DistanceMethod.JACCARD);
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
        DendrogramBuilder builder = new DendrogramBuilder(n);
        buildCluster(d, n, builder);
        Dendrogram dendrogram = builder.getDendrogram();

        int[] clusterIndex = { 0 };
        List<IntArrayList> clusters = new ArrayList<>();
        cut(dendrogram, 1.1, new ClusterConsumer() {

            @Override
            public void startCluster() {
                IntArrayList cluster = new IntArrayList();
                clusters.add(cluster);
            }

            @Override
            public void endCluster() {
                clusterIndex[0]++;
            }

            @Override
            public void addMember(int observation) {
                IntArrayList cluster = clusters.get(clusterIndex[0]);
                cluster.add(observation);
            }
        });

        Assert.assertEquals(5L, clusters.stream()
                                        .flatMapToInt(c -> Arrays.stream(c.buffer, 0, c.elementsCount))
                                        .count());

        List<IntArrayList> query1 = clusters.stream()
                                            .filter(c -> c.contains(1))
                                            .collect(Collectors.toList());
        Assert.assertEquals(1, query1.size());
        IntArrayList cluster1 = query1.get(0);
        Assert.assertEquals(1, cluster1.size());

        List<IntArrayList> query2 = clusters.stream()
                                            .filter(c -> c.contains(2))
                                            .collect(Collectors.toList());
        Assert.assertEquals(1, query2.size());
        IntArrayList cluster02 = query2.get(0);
        Assert.assertEquals(2, cluster02.size());
        Assert.assertTrue(cluster02.contains(0));

        List<IntArrayList> query3 = clusters.stream()
                                            .filter(c -> c.contains(3))
                                            .collect(Collectors.toList());
        Assert.assertEquals(1, query3.size());
        IntArrayList cluster34 = query3.get(0);
        Assert.assertEquals(2, cluster34.size());
        Assert.assertTrue(cluster34.contains(4));

    }
}
