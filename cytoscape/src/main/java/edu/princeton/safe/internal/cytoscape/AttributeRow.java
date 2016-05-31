package edu.princeton.safe.internal.cytoscape;

public class AttributeRow {
    int index;
    String name;
    long totalSignificant;

    AttributeRow(int index,
                 String name,
                 long totalSignificant) {
        this.index = index;
        this.name = name;
        this.totalSignificant = totalSignificant;
    }
}