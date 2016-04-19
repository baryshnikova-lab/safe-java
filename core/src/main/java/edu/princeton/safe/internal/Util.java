package edu.princeton.safe.internal;

public class Util {

    public static double[] nanArray(int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = Double.NaN;
        }
        return array;
    }

}
