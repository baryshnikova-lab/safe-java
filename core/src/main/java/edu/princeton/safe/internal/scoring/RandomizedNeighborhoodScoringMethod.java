package edu.princeton.safe.internal.scoring;

import java.util.function.IntConsumer;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.NeighborhoodScoringMethod;
import edu.princeton.safe.model.Neighborhood;

/**
 * Computes the neighborhood score from a random permutation of a fixed number
 * of neighborhoods. Neighborhoods contributing to the score may be of different
 * sizes.
 */
public class RandomizedNeighborhoodScoringMethod implements NeighborhoodScoringMethod {

    AnnotationProvider annotationProvider;
    int[] permutations;
    Neighborhood[] neighborhoods;

    public RandomizedNeighborhoodScoringMethod(AnnotationProvider annotationProvider,
                                               RandomGenerator generator,
                                               int totalPermutations,
                                               Neighborhood[] neighborhoods) {

        this.annotationProvider = annotationProvider;
        this.neighborhoods = neighborhoods;

        RandomDataGenerator random = new RandomDataGenerator(generator);
        permutations = random.nextPermutation(neighborhoods.length, totalPermutations);
    }

    @Override
    public double[] computeRandomizedScores(Neighborhood current,
                                            int attributeIndex) {
        int totalPermutations = permutations.length;
        double[] scores = new double[totalPermutations];
        for (int r = 0; r < totalPermutations; r++) {
            final int permutationIndex = r;
            Neighborhood randomNeighborhood = neighborhoods[permutations[r]];
            randomNeighborhood.forEachMemberIndex(new IntConsumer() {
                @Override
                public void accept(int index) {
                    double value = annotationProvider.getValue(index, attributeIndex);
                    if (!Double.isNaN(value)) {
                        scores[permutationIndex] += value;
                    }
                }
            });
        }
        return scores;
    }
}
