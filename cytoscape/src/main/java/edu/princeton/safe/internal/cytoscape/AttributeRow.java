package edu.princeton.safe.internal.cytoscape;

public class AttributeRow {
    int index;
    String name;
    double score;

    AttributeRow(int index,
                 String name,
                 double score) {
        this.index = index;
        this.name = name;
        this.score = score;
    }
}