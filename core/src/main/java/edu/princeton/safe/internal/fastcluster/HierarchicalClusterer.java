package edu.princeton.safe.internal.fastcluster;

import java.util.ArrayList;
import java.util.List;

// Adapted from http://danifold.net/fastcluster.html
public class HierarchicalClusterer {

    static double D_(int N,
                     double[] D,
                     int r_,
                     int c_) {
        int idx = idxD(N, D, r_, c_);
        return D[idx];
    }

    static int idxD(int N,
                    double[] D,
                    int r,
                    int c) {
        return ((2 * N - 3 - r) * r >> 1) + c - 1;
    }

    public static List<Node> NN_chain_core(int N,
                                           double[] D,
                                           int[] members,
                                           MethodCode method) {
        /*
         * N: integer D: condensed distance matrix N*(N-1)/2 Z2: output data
         * structure
         * 
         * This is the NN-chain algorithm, described on page 86 in the following
         * book:
         * 
         * Fionn Murtagh, Multidimensional Clustering Algorithms, Vienna,
         * Würzburg: Physica-Verlag, 1985.
         */
        int i;

        int[] NN_chain = new int[N];
        int NN_chain_tip = 0;

        int idx1, idx2;

        double size1 = Double.NaN;
        double size2 = Double.NaN;
        DoublyLinkedList active_nodes = new DoublyLinkedList(N);

        double min;

        for (int idxDD = 0; idxDD != (N * (N - 1) >> 1); idxDD++) {
            if (Double.isNaN(D[idxDD])) {
                throw new IllegalArgumentException("NaN");
            }
        }

        List<Node> Z2 = new ArrayList<>();

        for (int j = 0; j < N - 1; ++j) {
            if (NN_chain_tip <= 3) {
                NN_chain[0] = idx1 = active_nodes.start;
                NN_chain_tip = 1;

                idx2 = active_nodes.succ[idx1];
                min = D_(N, D, idx1, idx2);
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i]) {
                    if (D_(N, D, idx1, i) < min) {
                        min = D_(N, D, idx1, i);
                        idx2 = i;
                    }
                }
            } // a: idx1 b: idx2
            else {
                NN_chain_tip -= 3;
                idx1 = NN_chain[NN_chain_tip - 1];
                idx2 = NN_chain[NN_chain_tip];
                min = idx1 < idx2 ? D_(N, D, idx1, idx2) : D_(N, D, idx2, idx1);
            } // a: idx1 b: idx2

            do {
                NN_chain[NN_chain_tip] = idx2;

                for (i = active_nodes.start; i < idx2; i = active_nodes.succ[i]) {
                    if (D_(N, D, i, idx2) < min) {
                        min = D_(N, D, i, idx2);
                        idx1 = i;
                    }
                }
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i]) {
                    if (D_(N, D, idx2, i) < min) {
                        min = D_(N, D, idx2, i);
                        idx1 = i;
                    }
                }

                idx2 = idx1;
                idx1 = NN_chain[NN_chain_tip++];

            } while (idx2 != NN_chain[NN_chain_tip - 2]);

            Z2.add(new Node(idx1, idx2, min));

            if (idx1 > idx2) {
                int tmp = idx1;
                idx1 = idx2;
                idx2 = tmp;
            }

            if (method == MethodCode.METHOD_METR_AVERAGE || method == MethodCode.METHOD_METR_WARD) {
                size1 = members[idx1];
                size2 = members[idx2];
                members[idx2] += members[idx1];
            }

            // Remove the smaller index from the valid indices (active_nodes).
            active_nodes.remove(idx1);

            switch (method) {
            case METHOD_METR_SINGLE:
                /*
                 * Single linkage.
                 * 
                 * Characteristic: new distances are never longer than the old
                 * distances.
                 */
                // Update the distance matrix in the range [start, idx1).
                for (i = active_nodes.start; i < idx1; i = active_nodes.succ[i])
                    f_single(D, idxD(N, D, i, idx2), D_(N, D, i, idx1));
                // Update the distance matrix in the range (idx1, idx2).
                for (; i < idx2; i = active_nodes.succ[i])
                    f_single(D, idxD(N, D, i, idx2), D_(N, D, idx1, i));
                // Update the distance matrix in the range (idx2, N).
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i])
                    f_single(D, idxD(N, D, idx2, i), D_(N, D, idx1, i));
                break;

            case METHOD_METR_COMPLETE:
                /*
                 * Complete linkage.
                 * 
                 * Characteristic: new distances are never shorter than the old
                 * distances.
                 */
                // Update the distance matrix in the range [start, idx1).
                for (i = active_nodes.start; i < idx1; i = active_nodes.succ[i])
                    f_complete(D, idxD(N, D, i, idx2), D_(N, D, i, idx1));
                // Update the distance matrix in the range (idx1, idx2).
                for (; i < idx2; i = active_nodes.succ[i])
                    f_complete(D, idxD(N, D, i, idx2), D_(N, D, idx1, i));
                // Update the distance matrix in the range (idx2, N).
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i])
                    f_complete(D, idxD(N, D, idx2, i), D_(N, D, idx1, i));
                break;

            case METHOD_METR_AVERAGE: {
                /*
                 * Average linkage.
                 * 
                 * Shorter and longer distances can occur.
                 */
                // Update the distance matrix in the range [start, idx1).
                double s = size1 / (size1 + size2);
                double t = size2 / (size1 + size2);
                for (i = active_nodes.start; i < idx1; i = active_nodes.succ[i])
                    f_average(D, idxD(N, D, i, idx2), D_(N, D, i, idx1), s, t);
                // Update the distance matrix in the range (idx1, idx2).
                for (; i < idx2; i = active_nodes.succ[i])
                    f_average(D, idxD(N, D, i, idx2), D_(N, D, idx1, i), s, t);
                // Update the distance matrix in the range (idx2, N).
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i])
                    f_average(D, idxD(N, D, idx2, i), D_(N, D, idx1, i), s, t);
                break;
            }

            case METHOD_METR_WEIGHTED:
                /*
                 * Weighted linkage.
                 * 
                 * Shorter and longer distances can occur.
                 */
                // Update the distance matrix in the range [start, idx1).
                for (i = active_nodes.start; i < idx1; i = active_nodes.succ[i])
                    f_weighted(D, idxD(N, D, i, idx2), D_(N, D, i, idx1));
                // Update the distance matrix in the range (idx1, idx2).
                for (; i < idx2; i = active_nodes.succ[i])
                    f_weighted(D, idxD(N, D, i, idx2), D_(N, D, idx1, i));
                // Update the distance matrix in the range (idx2, N).
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i])
                    f_weighted(D, idxD(N, D, idx2, i), D_(N, D, idx1, i));
                break;

            case METHOD_METR_WARD:
                /*
                 * Ward linkage.
                 * 
                 * Shorter and longer distances can occur, not smaller than
                 * min(d1,d2) but maybe bigger than max(d1,d2).
                 */
                // Update the distance matrix in the range [start, idx1).
                // t_float v = static_cast<t_float>(members[i]);
                for (i = active_nodes.start; i < idx1; i = active_nodes.succ[i])
                    f_ward(D, idxD(N, D, i, idx2), D_(N, D, i, idx1), min, size1, size2, members[i]);
                // Update the distance matrix in the range (idx1, idx2).
                for (; i < idx2; i = active_nodes.succ[i])
                    f_ward(D, idxD(N, D, i, idx2), D_(N, D, idx1, i), min, size1, size2, members[i]);
                // Update the distance matrix in the range (idx2, N).
                for (i = active_nodes.succ[idx2]; i < N; i = active_nodes.succ[i])
                    f_ward(D, idxD(N, D, idx2, i), D_(N, D, idx1, i), min, size1, size2, members[i]);
                break;

            default:
                throw new RuntimeException("Invalid method.");
            }
        }

        return Z2;
    }

    static List<Node> generic_linkage(int N,
                                      double[] D,
                                      double[] members,
                                      MethodCode method) {
        /*
         * N: integer, number of data points D: condensed distance matrix
         * N*(N-1)/2 Z2: output data structure
         */
        int N_1 = N - 1;
        int i, j;
        int idx1, idx2;

        int[] n_nghbr = new int[N_1];
        double[] mindist = new double[N_1];
        int[] row_repr = new int[N];

        DoublyLinkedList active_nodes = new DoublyLinkedList(N);

        BinaryMinHeap nn_distances = new BinaryMinHeap(mindist);
        int node1, node2;
        double size1 = Double.NaN;
        double size2 = Double.NaN;

        double min;
        int idx;

        for (i = 0; i < N; i++) {
            row_repr[i] = i;

            // Initialize the minimal distances:
            // Find the nearest neighbor of each point.
            // n_nghbr[i] = argmin_{j>i} D(i,j) for i in range(N-1)
            int idxDD = 0;
            for (i = 0; i < N_1; ++i) {
                min = Double.MAX_VALUE;
                for (idx = j = i + 1; j < N; ++j, idxDD++) {
                    if (D[idxDD] < min) {
                        min = D[idxDD];
                        idx = j;
                    } else if (Double.isNaN(D[idxDD]))
                        throw new IllegalArgumentException("NaN");
                }
                mindist[i] = min;
                n_nghbr[i] = idx;
            }
        }

        // Put the minimal distances into a heap structure to make the repeated
        // global minimum searches fast.
        nn_distances.heapify();

        List<Node> Z2 = new ArrayList<>();

        // Main loop: We have N-1 merging steps.
        for (i = 0; i < N_1; ++i) {
            /*
             * Here is a special feature that allows fast bookkeeping and
             * updates of the minimal distances.
             * 
             * mindist[i] stores a lower bound on the minimum distance of the
             * point i to all points of higher index:
             * 
             * mindist[i] ≥ min_{j>i} D(i,j)
             * 
             * Normally, we have equality. However, this minimum may become
             * invalid due to the updates in the distance matrix. The rules are:
             * 
             * 1) If mindist[i] is equal to D(i, n_nghbr[i]), this is the
             * correct minimum and n_nghbr[i] is a nearest neighbor.
             * 
             * 2) If mindist[i] is smaller than D(i, n_nghbr[i]), this might not
             * be the correct minimum. The minimum needs to be recomputed.
             * 
             * 3) mindist[i] is never bigger than the true minimum. Hence, we
             * never miss the true minimum if we take the smallest mindist
             * entry, re-compute the value if necessary (thus maybe increasing
             * it) and looking for the now smallest mindist entry until a valid
             * minimal entry is found. This step is done in the lines below.
             * 
             * The update process for D below takes care that these rules are
             * fulfilled. This makes sure that the minima in the rows
             * D(i,i+1:)of D are re-calculated when necessary but re-calculation
             * is avoided whenever possible.
             * 
             * The re-calculation of the minima makes the worst-case runtime of
             * this algorithm cubic in N. We avoid this whenever possible, and
             * in most cases the runtime appears to be quadratic.
             */
            idx1 = nn_distances.argmin();
            if (method != MethodCode.METHOD_METR_SINGLE) {
                while (mindist[idx1] < D_(N, D, idx1, n_nghbr[idx1])) {
                    // Recompute the minimum mindist[idx1] and n_nghbr[idx1].
                    n_nghbr[idx1] = j = active_nodes.succ[idx1]; // exists,
                                                                 // maximally
                                                                 // N-1
                    min = D_(N, D, idx1, j);
                    for (j = active_nodes.succ[j]; j < N; j = active_nodes.succ[j]) {
                        if (D_(N, D, idx1, j) < min) {
                            min = D_(N, D, idx1, j);
                            n_nghbr[idx1] = j;
                        }
                    }
                    /*
                     * Update the heap with the new true minimum and search for
                     * the (possibly different) minimal entry.
                     */
                    nn_distances.update_geq(idx1, min);
                    idx1 = nn_distances.argmin();
                }
            }

            nn_distances.heap_pop(); // Remove the current minimum from the
                                     // heap.
            idx2 = n_nghbr[idx1];

            // Write the newly found minimal pair of nodes to the output array.
            node1 = row_repr[idx1];
            node2 = row_repr[idx2];

            if (method == MethodCode.METHOD_METR_AVERAGE || method == MethodCode.METHOD_METR_WARD
                    || method == MethodCode.METHOD_METR_CENTROID) {
                size1 = members[idx1];
                size2 = members[idx2];
                members[idx2] += members[idx1];
            }
            Z2.add(new Node(node1, node2, mindist[idx1]));

            // Remove idx1 from the list of active indices (active_nodes).
            active_nodes.remove(idx1);
            // Index idx2 now represents the new (merged) node with label N+i.
            row_repr[idx2] = N + i;

            // Update the distance matrix
            switch (method) {
            case METHOD_METR_SINGLE:
                /*
                 * Single linkage.
                 * 
                 * Characteristic: new distances are never longer than the old
                 * distances.
                 */
                // Update the distance matrix in the range [start, idx1).
                for (j = active_nodes.start; j < idx1; j = active_nodes.succ[j]) {
                    f_single(D, idxD(N, D, j, idx2), D_(N, D, j, idx1));
                    if (n_nghbr[j] == idx1)
                        n_nghbr[j] = idx2;
                }
                // Update the distance matrix in the range (idx1, idx2).
                for (; j < idx2; j = active_nodes.succ[j]) {
                    f_single(D, idxD(N, D, j, idx2), D_(N, D, idx1, j));
                    // If the new value is below the old minimum in a row,
                    // update
                    // the mindist and n_nghbr arrays.
                    if (D_(N, D, j, idx2) < mindist[j]) {
                        nn_distances.update_leq(j, D_(N, D, j, idx2));
                        n_nghbr[j] = idx2;
                    }
                }
                // Update the distance matrix in the range (idx2, N).
                // Recompute the minimum mindist[idx2] and n_nghbr[idx2].
                if (idx2 < N_1) {
                    min = mindist[idx2];
                    for (j = active_nodes.succ[idx2]; j < N; j = active_nodes.succ[j]) {
                        f_single(D, idxD(N, D, idx2, j), D_(N, D, idx1, j));
                        if (D_(N, D, idx2, j) < min) {
                            n_nghbr[idx2] = j;
                            min = D_(N, D, idx2, j);
                        }
                    }
                    nn_distances.update_leq(idx2, min);
                }
                break;

            case METHOD_METR_COMPLETE:
                /*
                 * Complete linkage.
                 * 
                 * Characteristic: new distances are never shorter than the old
                 * distances.
                 */
                // Update the distance matrix in the range [start, idx1).
                for (j = active_nodes.start; j < idx1; j = active_nodes.succ[j]) {
                    f_complete(D, idxD(N, D, j, idx2), D_(N, D, j, idx1));
                    if (n_nghbr[j] == idx1)
                        n_nghbr[j] = idx2;
                }
                // Update the distance matrix in the range (idx1, idx2).
                for (; j < idx2; j = active_nodes.succ[j])
                    f_complete(D, idxD(N, D, j, idx2), D_(N, D, idx1, j));
                // Update the distance matrix in the range (idx2, N).
                for (j = active_nodes.succ[idx2]; j < N; j = active_nodes.succ[j])
                    f_complete(D, idxD(N, D, idx2, j), D_(N, D, idx1, j));
                break;

            case METHOD_METR_AVERAGE: {
                /*
                 * Average linkage.
                 * 
                 * Shorter and longer distances can occur.
                 */
                // Update the distance matrix in the range [start, idx1).
                double s = size1 / (size1 + size2);
                double t = size2 / (size1 + size2);
                for (j = active_nodes.start; j < idx1; j = active_nodes.succ[j]) {
                    f_average(D, idxD(N, D, j, idx2), D_(N, D, j, idx1), s, t);
                    if (n_nghbr[j] == idx1)
                        n_nghbr[j] = idx2;
                }
                // Update the distance matrix in the range (idx1, idx2).
                for (; j < idx2; j = active_nodes.succ[j]) {
                    f_average(D, idxD(N, D, j, idx2), D_(N, D, idx1, j), s, t);
                    if (D_(N, D, j, idx2) < mindist[j]) {
                        nn_distances.update_leq(j, D_(N, D, j, idx2));
                        n_nghbr[j] = idx2;
                    }
                }
                // Update the distance matrix in the range (idx2, N).
                if (idx2 < N_1) {
                    n_nghbr[idx2] = j = active_nodes.succ[idx2]; // exists,
                                                                 // maximally
                                                                 // N-1
                    f_average(D, idxD(N, D, idx2, j), D_(N, D, idx1, j), s, t);
                    min = D_(N, D, idx2, j);
                    for (j = active_nodes.succ[j]; j < N; j = active_nodes.succ[j]) {
                        f_average(D, idxD(N, D, idx2, j), D_(N, D, idx1, j), s, t);
                        if (D_(N, D, idx2, j) < min) {
                            min = D_(N, D, idx2, j);
                            n_nghbr[idx2] = j;
                        }
                    }
                    nn_distances.update(idx2, min);
                }
                break;
            }
            default:
                throw new RuntimeException("Invalid method");
            }

        }
        return Z2;
    }

    static void f_single(double[] D,
                         int idxb,
                         double a) {
        if (D[idxb] > a) {
            D[idxb] = a;
        }
    }

    static void f_complete(double[] D,
                           int idxb,
                           double a) {
        if (D[idxb] < a) {
            D[idxb] = a;
        }
    }

    static void f_average(double[] D,
                          int idxb,
                          double a,
                          double s,
                          double t) {
        D[idxb] = s * a + t * (D[idxb]);
        if (Double.isNaN(D[idxb])) {
            throw new IllegalArgumentException("NaN");
        }
    }

    static void f_weighted(double[] D,
                           int idxb,
                           double a) {
        D[idxb] = (a + D[idxb]) * .5;
        if (Double.isNaN(D[idxb])) {
            throw new IllegalArgumentException("NaN");
        }
    }

    static void f_ward(double[] D,
                       int idxb,
                       double a,
                       double c,
                       double s,
                       double t,
                       double v) {
        D[idxb] = ((v + s) * a - v * c + (v + t) * (D[idxb])) / (s + t + v);
        if (Double.isNaN(D[idxb])) {
            throw new IllegalArgumentException("NaN");
        }
    }

    static void f_centroid(double[] D,
                           int idxb,
                           double a,
                           double stc,
                           double s,
                           double t) {
        D[idxb] = s * a - stc + t * (D[idxb]);
        if (Double.isNaN(D[idxb])) {
            throw new IllegalArgumentException("NaN");
        }
    }

    static void f_median(double[] D,
                         int idxb,
                         double a,
                         double c_4) {
        D[idxb] = (a + (D[idxb])) * .5 - c_4;
        if (Double.isNaN(D[idxb])) {
            throw new IllegalArgumentException("NaN");
        }
    }

    public static void buildClusters(boolean sorted,
                                     List<Node> clusterResult,
                                     LinkageConsumer consumer) {

        int N = clusterResult.size() + 1;

        // The array "nodes" is a union-find data structure for the cluster
        // identities (only needed for unsorted cluster_result input).
        UnionFind nodes = new UnionFind(sorted ? 0 : N);
        if (!sorted) {
            clusterResult.sort((l1,
                                l2) -> Double.compare(l1.dist, l2.dist));
        }

        int node1, node2;

        for (Node node : clusterResult) {
            // Get two data points whose clusters are merged in step i.
            if (sorted) {
                node1 = node.node1;
                node2 = node.node2;
            } else {
                // Find the cluster identifiers for these points.
                node1 = nodes.find(node.node1);
                node2 = nodes.find(node.node2);
                // Merge the nodes in the union-find data structure by making
                // them
                // children of a new node.
                nodes.union(node1, node2);
            }

            consumer.accept(node1, node2, node.dist);
        }
    }
}
