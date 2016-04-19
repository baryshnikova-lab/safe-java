package edu.princeton.safe.internal.scoring;

import java.util.function.IntConsumer;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.Neighborhood;
import edu.princeton.safe.NeighborhoodScoringMethod;

/**
 * Computes the neighborhood score by mapping the node indexes of a single
 * neighborhood to a random permutation (resulting in different attribute
 * values). Each score in the result is computed from a neighborhood of the same
 * size.
 */
public class RandomizedMemberScoringMethod implements NeighborhoodScoringMethod {

    AnnotationProvider annotationProvider;
    int[][] permutations;

    public RandomizedMemberScoringMethod(AnnotationProvider annotationProvider,
                                         RandomGenerator generator,
                                         int totalPermutations,
                                         int totalNodes) {

        this.annotationProvider = annotationProvider;

        RandomDataGenerator random = new RandomDataGenerator(generator);
        permutations = new int[totalPermutations][];
        for (int i = 0; i < totalPermutations; i++) {
            permutations[i] = random.nextPermutation(totalNodes, totalNodes);
        }
    }

    @Override
    public double[] computeRandomizedScores(Neighborhood neighborhood,
                                            int attributeIndex) {

        int totalPermutations = permutations.length;
        double[] scores = new double[totalPermutations];
        neighborhood.forEachNodeIndex(new IntConsumer() {
            @Override
            public void accept(int index) {
                for (int i = 0; i < totalPermutations; i++) {
                    int randomIndex = permutations[i][index];
                    double value = annotationProvider.getValue(randomIndex, attributeIndex);
                    if (!Double.isNaN(value)) {
                        scores[i] += value;
                    }
                }
            }
        });
        return scores;
    }

}
