package edu.princeton.safe.internal.fastcluster;

// Adapted from http://danifold.net/fastcluster.html
public class DoublyLinkedList {
    /*
     * Class for a doubly linked list. Initially, the list is the integer range
     * [0, size]. We provide a forward iterator and a method to delete an index
     * from the list.
     * 
     * Typical use: for (i=L.start; L<size; i=L.succ[I]) or for (i=somevalue;
     * L<size; i=L.succ[I])
     */
    int start;
    int[] succ;
    int[] pred;

    public DoublyLinkedList(int size) {
        // Initialize to the given size.
        start = 0;
        succ = new int[size + 1];
        pred = new int[size + 1];

        for (int i = 0; i < size; ++i) {
            pred[i + 1] = i;
            succ[i] = i + 1;
        }
        // pred[0] is never accessed!
        // succ[size] is never accessed!
    }

    void remove(int idx) {
        // Remove an index from the list.
        if (idx == start) {
            start = succ[idx];
        } else {
            succ[pred[idx]] = succ[idx];
            pred[succ[idx]] = pred[idx];
        }
        succ[idx] = 0; // Mark as inactive
    }

    boolean is_inactive(int idx) {
        return succ[idx] == 0;
    }
}
