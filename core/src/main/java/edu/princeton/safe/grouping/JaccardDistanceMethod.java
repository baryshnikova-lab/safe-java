package edu.princeton.safe.grouping;

import java.util.function.DoublePredicate;

public class JaccardDistanceMethod implements DistanceMethod {

    public static final String ID = "jaccard";
    
    private DoublePredicate predicate;

    public JaccardDistanceMethod(DoublePredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public double apply(double[] s,
                        double[] t) {
        int all = 0;
        int any = 0;
        for (int i = 0; i < s.length; i++) {
            boolean sSignificant = predicate.test(s[i]);
            boolean tSignificant = predicate.test(t[i]);

            if (sSignificant && tSignificant) {
                all++;
                any++;
            } else if (sSignificant || tSignificant) {
                any++;
            }
        }

        double coefficient;
        if (any == 0) {
            coefficient = 1;
        } else {
            coefficient = (double) all / any;
        }

        return 1 - coefficient;
    }
}