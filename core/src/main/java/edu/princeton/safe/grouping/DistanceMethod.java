package edu.princeton.safe.grouping;

import java.util.stream.IntStream;

import edu.princeton.safe.Identifiable;

public interface DistanceMethod extends Identifiable {
    double apply(double[] s,
                 double[] t);

    public static final String CORRELATION_ID = "pearson";

    public static final DistanceMethod CORRELATION = new DistanceMethod() {
        @Override
        public String getId() {
            return CORRELATION_ID;
        }

        @Override
        public double apply(double[] s,
                            double[] t) {
            double[] sums = { 0, 0 };
            IntStream.range(0, s.length)
                     .forEach(i -> {
                         sums[0] += s[i];
                         sums[1] += t[i];
                     });
            double meanS = sums[0] / s.length;
            double meanT = sums[1] / t.length;

            double[] terms = { 0, 0, 0 };
            IntStream.range(0, s.length)
                     .forEach(i -> {
                         // sum(s - mean(s))
                         double sDiff = s[i] - meanS;

                         // sum(t - mean(t))
                         double tDiff = t[i] - meanT;

                         // (s - mean(s)) dot (t - mean(t))
                         terms[0] += sDiff * tDiff;

                         // norm2(s - mean(s)) ^ 2
                         terms[1] += sDiff * sDiff;

                         // norm2(t - mean(t)) ^ 2
                         terms[2] += tDiff * tDiff;
                     });

            // 1 - (s - mean(s)) dot (t - mean(t)) /
            // (norm2(s - mean(s)) * norm2(t - mean(t)))
            return 1.0 - terms[0] / (Math.sqrt(terms[1]) * Math.sqrt(terms[2]));
        }
    };
}
