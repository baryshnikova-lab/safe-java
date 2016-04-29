package edu.princeton.safe.internal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class Util {

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

}
