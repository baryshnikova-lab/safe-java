package edu.princeton.safe.internal.fastcluster;

// Adapted from http://danifold.net/fastcluster.html
public enum MethodCode {
    METHOD_METR_SINGLE(0), METHOD_METR_COMPLETE(1), METHOD_METR_AVERAGE(2), METHOD_METR_WEIGHTED(3), METHOD_METR_WARD(
            4), METHOD_METR_WARD_D(4), METHOD_METR_CENTROID(5), METHOD_METR_MEDIAN(6), METHOD_METR_WARD_D2(7);

    int value;

    MethodCode(int value) {
        this.value = value;
    }

}
