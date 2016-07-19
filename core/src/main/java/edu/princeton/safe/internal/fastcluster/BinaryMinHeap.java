package edu.princeton.safe.internal.fastcluster;

// Adapted from http://danifold.net/fastcluster.html
public class BinaryMinHeap {
    /*
     * Class for a binary min-heap. The data resides in an array A. The elements
     * of A are not changed but two lists I and R of indices are generated which
     * point to elements of A and backwards.
     * 
     * The heap tree structure is
     * 
     * H[2*i+1] H[2*i+2] \ / \ / ≤ ≤ \ / \ / H[i]
     * 
     * where the children must be less or equal than their parent. Thus, H[0]
     * contains the minimum. The lists I and R are made such that H[i] = A[I[i]]
     * and R[I[i]] = i.
     * 
     * This implementation is not designed to handle NaN values.
     */

    double[] A;
    int size;
    int[] I;
    int[] R;

    BinaryMinHeap(double[] members) {
        // Allocate memory and initialize the lists I and R to the identity.
        // This
        // does not make it a heap. Call heapify afterwards!
        A = members;
        size = members.length;
        I = new int[size];
        R = new int[size];

        for (int i = 0; i < size; i++) {
            R[i] = I[i] = i;
        }
    }

    void heapify() {
        // Arrange the indices I and R so that H[i] := A[I[i]] satisfies the
        // heap
        // condition H[i] < H[2*i+1] and H[i] < H[2*i+2] for each i.
        //
        // Complexity: Θ(size)
        // Reference: Cormen, Leiserson, Rivest, Stein, Introduction to
        // Algorithms,
        // 3rd ed., 2009, Section 6.3 “Building a heap”
        int idx;
        for (idx = (size >> 1); idx > 0;) {
            --idx;
            update_geq_(idx);
        }
    }

    int argmin() {
        // Return the minimal element.
        return I[0];
    }

    void heap_pop() {
        // Remove the minimal element from the heap.
        --size;
        I[0] = I[size];
        R[I[0]] = 0;
        update_geq_(0);
    }

    void remove(int idx) {
        // Remove an element from the heap.
        --size;
        R[I[size]] = R[idx];
        I[R[idx]] = I[size];
        if (H(size) <= A[idx]) {
            update_leq_(R[idx]);
        } else {
            update_geq_(R[idx]);
        }
    }

    void replace(int idxold,
                 int idxnew,
                 double val) {
        R[idxnew] = R[idxold];
        I[R[idxnew]] = idxnew;
        if (val <= A[idxold])
            update_leq(idxnew, val);
        else
            update_geq(idxnew, val);
    }

    void update(int idx,
                double val) {
        // Update the element A[i] with val and re-arrange the indices to
        // preserve
        // the heap condition.
        if (val <= A[idx])
            update_leq(idx, val);
        else
            update_geq(idx, val);
    }

    void update_leq(int idx,
                    double val) {
        // Use this when the new value is not more than the old value.
        A[idx] = val;
        update_leq_(R[idx]);
    }

    void update_geq(int idx,
                    double val) {
        // Use this when the new value is not less than the old value.
        A[idx] = val;
        update_geq_(R[idx]);
    }

    void update_leq_(int i) {
        int j;
        for (; (i > 0) && (H(i) < H(j = (i - 1) >> 1)); i = j)
            heap_swap(i, j);
    }

    void update_geq_(int i) {
        int j;
        for (; (j = 2 * i + 1) < size; i = j) {
            if (H(j) >= H(i)) {
                ++j;
                if (j >= size || H(j) >= H(i))
                    break;
            } else if (j + 1 < size && H(j + 1) < H(j))
                ++j;
            heap_swap(i, j);
        }
    }

    void heap_swap(int i,
                   int j) {
        // Swap two indices.
        int tmp = I[i];
        I[i] = I[j];
        I[j] = tmp;
        R[I[i]] = i;
        R[I[j]] = j;
    }

    double H(int i) {
        return A[I[i]];
    }

}
