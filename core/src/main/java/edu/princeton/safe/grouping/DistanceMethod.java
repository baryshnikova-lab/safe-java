package edu.princeton.safe.grouping;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

@FunctionalInterface
public interface DistanceMethod {
    double apply(double[] s,
                 double[] t);

    public static final DistanceMethod JACCARD = new DistanceMethod() {
        @Override
        public double apply(double[] s,
                            double[] t) {
            int all = 0;
            int any = 0;
            for (int i = 0; i < s.length; i++) {
                if (s[i] != 0 && t[i] != 0) {
                    all++;
                    any++;
                } else if (s[i] != 0 || t[i] != 0) {
                    any++;
                }
            }
            return 1 - (double) all / any;
        }
    };

    public static final DistanceMethod CORRELATION = new DistanceMethod() {
        @Override
        public double apply(double[] s,
                            double[] t) {
            double[] sums = { 0, 0 };
            IntStream.range(0, s.length)
                     .forEach(new IntConsumer() {
                         @Override
                         public void accept(int i) {
                             sums[0] += s[i];
                             sums[1] += t[i];
                         }
                     });
            double meanS = sums[0] / s.length;
            double meanT = sums[1] / t.length;

            double[] terms = { 0, 0, 0 };
            IntStream.range(0, s.length)
                     .forEach(new IntConsumer() {
                         @Override
                         public void accept(int i) {
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
                         }
                     });

            // 1 - (s - mean(s)) dot (t - mean(t)) /
            // (norm2(s - mean(s)) * norm2(t - mean(t)))
            return 1.0 - terms[0] / (Math.sqrt(terms[1]) * Math.sqrt(terms[2]));
        }
    };
}