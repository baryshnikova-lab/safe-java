package edu.princeton.safe.internal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.apache.commons.math3.util.CentralPivotingStrategy;
import org.apache.commons.math3.util.KthSelector;

public class Util {

    static Percentile defaultPercentile = new Percentile().withEstimationType(EstimationType.R_5)
                                                          .withKthSelector(new KthSelector(new CentralPivotingStrategy()));

    public static double[] nanArray(int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = Double.NaN;
        }
        return array;
    }

    public static BufferedReader getReader(String path) throws IOException {
        if (path.endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
        }
        return new BufferedReader(new FileReader(path));
    }

    public static double percentile(double[] values,
                                    double threshold) {
        return defaultPercentile.evaluate(values, threshold);
    }
}
