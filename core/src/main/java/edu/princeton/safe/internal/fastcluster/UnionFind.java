package edu.princeton.safe.internal.fastcluster;

// Adapted from http://danifold.net/fastcluster.html
public class UnionFind {
    /*
     * Lookup function for a union-find data structure.
     * 
     * The function finds the root of idx by going iteratively through all
     * parent elements until a root is found. An element i is a root if nodes[i]
     * is zero. To make subsequent searches faster, the entry for idx and all
     * its parents is updated with the root element.
     */

    int[] parent;
    int nextparent;

    public UnionFind(int size) {
        parent = new int[size > 0 ? 2 * size - 1 : 0];
        nextparent = size;
    }

    int find(int idx) {
        if (parent[idx] != 0) { // a → b
            int p = idx;
            idx = parent[idx];
            if (parent[idx] != 0) { // a → b → c
                do {
                    idx = parent[idx];
                } while (parent[idx] != 0);
                do {
                    int tmp = parent[p];
                    parent[p] = idx;
                    p = tmp;
                } while (parent[p] != idx);
            }
        }
        return idx;
    }

    void union(int node1,
               int node2) {
        parent[node1] = parent[node2] = nextparent++;
    }

}
